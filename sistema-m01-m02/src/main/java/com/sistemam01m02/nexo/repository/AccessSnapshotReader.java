package com.sistemam01m02.nexo.repository;

import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.sistemam01m02.shared.error.ApiException;

/** Read projections only: no passwords, emails, token values, hashes or entity mutation methods. */
@Repository
public class AccessSnapshotReader {
 public record Subject(String alias,Set<String> roles,boolean active,int refreshCount) {}
 public record Snapshot(Map<String,Set<String>> catalog,List<Subject> subjects,boolean complete,String target) {}
 private final JdbcTemplate db;
 public AccessSnapshotReader(JdbcTemplate db){this.db=db;}

 public boolean authorized(String owner,long version,Set<String> expected) {
  int valid=db.queryForObject("SELECT COUNT(*) FROM app_user WHERE id=UNHEX(REPLACE(?,'-','')) AND session_version=? AND (status='ACTIVE' OR (status='LOCKED' AND locked_until<=UTC_TIMESTAMP(6)))",Integer.class,owner,version);
  if(valid!=1)return false;
  Set<String> current=new HashSet<>(db.queryForList("SELECT CONCAT('ROLE_',r.code) FROM role r JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=UNHEX(REPLACE(?,'-','')) AND r.active=true UNION SELECT p.code FROM permission p JOIN role_permission rp ON rp.permission_id=p.id JOIN role r ON r.id=rp.role_id JOIN user_role ur ON ur.role_id=r.id WHERE ur.user_id=UNHEX(REPLACE(?,'-','')) AND r.active=true",String.class,owner,owner));
  return current.equals(expected);
 }
 @Transactional(readOnly=true)
 public Snapshot read(String target,boolean all,int minute){
  Map<String,Set<String>> catalog=new TreeMap<>();
  db.query("SELECT r.code,p.code FROM role r LEFT JOIN role_permission rp ON rp.role_id=r.id LEFT JOIN permission p ON p.id=rp.permission_id WHERE r.active=true",(org.springframework.jdbc.core.RowCallbackHandler)rs->{var set=catalog.computeIfAbsent(rs.getString(1),k->new TreeSet<>());if(rs.getString(2)!=null)set.add(rs.getString(2));});
  if(catalog.size()>8)throw new ApiException(422,"CATALOG_LIMIT","El laboratorio admite hasta 8 roles activos");
  List<String> ids=all?db.queryForList("SELECT HEX(id) FROM app_user ORDER BY id LIMIT 101",String.class):List.of(target.replace("-","").toUpperCase(Locale.ROOT));
  if(ids.size()>100)throw new ApiException(422,"SNAPSHOT_LIMIT","Selecciona un snapshot propio: límite de 100 cuentas por análisis administrativo");
  String targetHex=target.replace("-","").toUpperCase(Locale.ROOT);
  if(!ids.contains(targetHex))throw new ApiException(404,"NOT_FOUND","Cuenta no disponible");
  List<Subject> subjects=new ArrayList<>();int i=0;
  for(String id:ids){
   var states=db.queryForList("SELECT status,locked_until FROM app_user WHERE id=UNHEX(?)",id);
   if(states.isEmpty())throw new ApiException(404,"NOT_FOUND","Cuenta no disponible");
   var state=states.getFirst(); boolean active="ACTIVE".equals(state.get("status"));
   if("LOCKED".equals(state.get("status"))&&state.get("locked_until")!=null){
    // Matches current bearer behavior for expired locks at the observation time.
    var until=(java.sql.Timestamp)state.get("locked_until"); active=!until.toInstant().isAfter(Instant.now());
   }
   Set<String> roles=new TreeSet<>(db.queryForList("SELECT r.code FROM user_role ur JOIN role r ON ur.role_id=r.id WHERE ur.user_id=UNHEX(?) AND r.active=true",String.class,id));
   int refresh=db.queryForObject("SELECT COUNT(*) FROM account_token WHERE user_id=UNHEX(?) AND kind='REFRESH' AND consumed_at IS NULL AND expires_at>?",Integer.class,id,java.sql.Timestamp.from(Instant.now().plusSeconds(minute*60L)));
   subjects.add(new Subject(id.equals(targetHex)?"objetivo":"cuenta-"+(++i),roles,active,active?refresh:0));
  }
  return new Snapshot(catalog,List.copyOf(subjects),all,"objetivo");
 }
}
