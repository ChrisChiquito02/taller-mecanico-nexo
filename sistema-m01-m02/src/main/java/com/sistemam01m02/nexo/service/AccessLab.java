package com.sistemam01m02.nexo.service;

import com.sistemam01m02.nexo.repository.AccessSnapshotReader;
import com.sistemam01m02.nexo.repository.AccessSnapshotReader.*;
import com.sistemam01m02.shared.error.ApiException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AccessLab {
 public record Request(String source,String scenario,String target,Set<String> roles,Set<String> required,int minute,String patchRole,Set<String> patchPermissions) {}
 private static final Map<String,Integer> WEIGHTS=Map.of("profile:read",2,"users:read",15,"users:write",35);
 private static final Map<String,String> RESOURCES=Map.of("profile:read","GET /api/users/me; POST /api/auth/change-password","users:read","GET /api/admin/users","users:write","PUT /api/admin/users/{id}/roles; DELETE /api/admin/users/{id}");
 private final AccessSnapshotReader reader;
 public AccessLab(AccessSnapshotReader reader){this.reader=reader;}
 public Map<String,Object> run(Request request,String owner,Set<String> permissions){
  if(request==null || request.minute()<0 || request.minute()>10080)throw invalid();
  if("synthetic".equals(request.source()))return fixture(request.scenario(),request.minute());
  if(!"live".equals(request.source()))throw invalid();
  boolean admin=permissions.containsAll(Set.of("users:read","users:write"));
  String target=request.target()==null||request.target().isBlank()?owner:request.target();
  try{UUID.fromString(target);}catch(Exception e){throw invalid();}
  if(!target.equals(owner)&&!admin)throw new ApiException(403,"FORBIDDEN","Solo puedes simular tu propia cuenta");
  if(request.patchRole()!=null&&!request.patchRole().isBlank()&&!admin)throw new ApiException(403,"FORBIDDEN","El cambio compartido requiere permisos administrativos");
  Snapshot s=reader.read(target,admin,request.minute());
  Map<String,Set<String>> assign=new TreeMap<>(),patch=new TreeMap<>();
  if(request.patchRole()!=null&&!request.patchRole().isBlank())patch.put(request.patchRole(),request.patchPermissions()==null?Set.of():request.patchPermissions());
  else {if(request.roles()==null||request.roles().isEmpty())throw invalid();assign.put(s.target(),request.roles());}
  return report(s,assign,patch,request.required()==null?Set.of("profile:read"):request.required(),request.minute(),false);
 }
 private Map<String,Object> fixture(String scenario,int minute){
  String file=switch(scenario==null?"":scenario){case "ascenso"->"escenario.json";case "consulta"->"escenario-lectura.json";case "compartido"->"escenario-permisos.json";default->throw invalid();};
  try{
   var root=JsonMapper.builder().build().readTree(new ClassPathResource("nexo/"+file).getInputStream());
   Map<String,Set<String>> catalog=new TreeMap<>(),assign=new TreeMap<>(),patch=new TreeMap<>();
   root.path("roles").properties().forEach(e->catalog.put(e.getKey(),strings(e.getValue())));
   root.path("change").path("assignments").properties().forEach(e->assign.put(e.getKey(),strings(e.getValue())));
   root.path("change").path("permission_patch").properties().forEach(e->patch.put(e.getKey(),strings(e.getValue())));
   List<Subject> subjects=new ArrayList<>();
   for(var u:root.path("users")){
    int count=0;boolean active="ACTIVE".equals(u.path("status").asString());
    for(var t:u.path("sessions"))if(active&&t.path("expires_minute").asInt()>minute && ("access".equals(t.path("kind").asString())?t.path("sv").asLong()==u.path("session_version").asLong():!t.path("consumed").asBoolean()))count++;
    subjects.add(new Subject(u.path("id").asString(),strings(u.path("roles")),active,count));
   }
   return report(new Snapshot(catalog,subjects,true,"u-2"),assign,patch,strings(root.path("required_permissions")),minute,true);
  }catch(java.io.IOException e){throw new IllegalStateException(e);}
 }
 private Set<String> strings(tools.jackson.databind.JsonNode node){Set<String> set=new TreeSet<>();node.forEach(n->set.add(n.asString()));return set;}
 public Map<String,Object> report(Snapshot s,Map<String,Set<String>> assignments,Map<String,Set<String>> patch,Set<String> required,int minute,boolean synthetic){
  if(s.catalog().size()>8 || !WEIGHTS.keySet().containsAll(required) || !s.catalog().keySet().containsAll(patch.keySet()))throw invalid();
  for(var perms:s.catalog().values())if(!WEIGHTS.keySet().containsAll(perms))throw new ApiException(422,"UNKNOWN_PERMISSION","El catálogo contiene permisos sin peso definido; actualiza la metodología antes de calcular");
  for(var roles:assignments.values())if(roles.isEmpty()||!s.catalog().keySet().containsAll(roles))throw invalid();
  for(var perms:patch.values())if(!WEIGHTS.keySet().containsAll(perms))throw invalid();
  var requested=calculate(s,assignments,patch);
  List<Map<String,Object>> candidates=new ArrayList<>(); List<String> names=new ArrayList<>(s.catalog().keySet());Collections.sort(names);
  for(int mask=1;mask<(1<<names.size());mask++){
   Set<String> roles=new TreeSet<>();for(int i=0;i<names.size();i++)if((mask&(1<<i))!=0)roles.add(names.get(i));
   Set<String> effective=effective(roles,s.catalog());
   if(effective.containsAll(required)){effective.removeAll(required);candidates.add(Map.of("roles",roles,"extra_permissions",effective,"extra_weight",weight(effective)));}
  }
  candidates.sort(Comparator.<Map<String,Object>>comparingInt(c->(int)c.get("extra_weight")).thenComparingInt(c->((Set<?>)c.get("roles")).size()).thenComparing(c->c.get("roles").toString()));
  Map<String,Object> alternative=new LinkedHashMap<>();
  if(!candidates.isEmpty()){
   @SuppressWarnings("unchecked") Set<String> roles=(Set<String>)candidates.getFirst().get("roles");
   Map<String,Set<String>> change=new TreeMap<>();
   for(var user:s.subjects())if((assignments.containsKey(user.alias())||!Collections.disjoint(user.roles(),patch.keySet()))&&!user.roles().equals(roles))change.put(user.alias(),roles);
   alternative.put("change",change);alternative.put("no_change_needed",change.isEmpty());alternative.put("simulation",calculate(s,change,Map.of()));
  }
  Map<String,Object> out=new LinkedHashMap<>();out.put("agent","NEXO-Δ Predictive Access Lab");out.put("mode","simulation_only");out.put("source",synthetic?"synthetic":"authorized_live_read");out.put("at_minute",minute);out.put("snapshot_complete",s.complete());out.put("required_permissions",required);out.put("requested",requested);out.put("least_privilege_candidates",candidates.stream().limit(8).toList());out.put("alternative_evaluated",alternative);
  out.put("limitations",List.of("Rúbrica educativa, no probabilidad ni certificación.",synthetic?"Tokens de acceso y refresh sintéticos conocidos.":"Número de JWT reales desconocido; solo refresh vigentes conocidos. Riesgo parcial.",s.complete()?"Snapshot completo al instante de lectura, sujeto a concurrencia posterior.":"Snapshot propio parcial: no determina último administrador.","Los permisos del catálogo son hipotéticos: no existe endpoint IAM para editarlos.","No se modifican cuentas, roles, permisos ni tokens. El informe y la auditoría NEXO sí pueden persistirse."));return out;
 }
 private Map<String,Object> calculate(Snapshot s,Map<String,Set<String>> assignments,Map<String,Set<String>> patch){
  Map<String,Set<String>> catalog=new TreeMap<>(s.catalog());catalog.putAll(patch);
  List<Map<String,Object>> affected=new ArrayList<>();int gains=0,invalidated=0,writers=0;
  for(var user:s.subjects()){
   Set<String> roles=assignments.getOrDefault(user.alias(),user.roles());
   Set<String> before=effective(user.roles(),s.catalog()),after=effective(roles,catalog),added=new TreeSet<>(after),lost=new TreeSet<>(before);added.removeAll(before);lost.removeAll(after);
   if(user.active()&&after.contains("users:write"))writers++;
   if(!before.equals(after)||assignments.containsKey(user.alias())){
    int disruption=assignments.containsKey(user.alias())?user.refreshCount():0;gains+=weight(added);invalidated+=disruption;
    Map<String,Object> row=new LinkedHashMap<>();row.put("user",user.alias());row.put("before_roles",user.roles());row.put("proposed_roles",roles);row.put("before_permissions",before);row.put("proposed_permissions",after);row.put("gained",added);row.put("lost",lost);row.put("resources_exposed",added.stream().map(RESOURCES::get).toList());row.put("sessions_invalidated_by_change",disruption);row.put("sessions_still_valid",user.refreshCount()-disruption);affected.add(row);
   }
  }
  int sessionPoints=Math.min(20,5*Math.min(4,invalidated)),lock=s.complete()&&writers==0?40:0,score=Math.min(100,gains+sessionPoints+lock);
  Map<String,Set<String>> restore=new TreeMap<>(),restorePatch=new TreeMap<>();for(var u:s.subjects())if(assignments.containsKey(u.alias()))restore.put(u.alias(),u.roles());for(String r:patch.keySet())restorePatch.put(r,s.catalog().get(r));
  return Map.of("affected",affected,"remaining_active_writers",s.complete()?writers:"unknown","risk",Map.of("score",score,"label",score>=60?"alto":score>=25?"medio":"bajo","privilege_gain_points",gains,"session_disruption_points",sessionPoints,"no_writer_points",lock),"rollback_proposal_only",Map.of("assignments",restore,"permission_patch",restorePatch,"warning","Revisión humana y nuevo inicio de sesión. Nunca restaurar session_version ni tokens consumidos."));
 }
 private Set<String> effective(Set<String> roles,Map<String,Set<String>> catalog){Set<String> p=new TreeSet<>();roles.forEach(r->p.addAll(catalog.getOrDefault(r,Set.of())));return p;}
 private int weight(Set<String> permissions){return permissions.stream().mapToInt(WEIGHTS::get).sum();}
 private ApiException invalid(){return new ApiException(400,"INVALID_SCENARIO","Escenario, roles, permisos u horizonte no válidos");}
}
