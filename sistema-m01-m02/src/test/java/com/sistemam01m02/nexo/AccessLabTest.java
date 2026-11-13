package com.sistemam01m02.nexo;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import com.sistemam01m02.nexo.service.AccessLab;
import com.sistemam01m02.nexo.repository.AccessSnapshotReader;
import com.sistemam01m02.nexo.repository.AccessSnapshotReader.*;
import com.sistemam01m02.shared.error.ApiException;
import tools.jackson.databind.json.JsonMapper;

class AccessLabTest {
 private final AccessSnapshotReader reader=mock(AccessSnapshotReader.class);
 private final AccessLab lab=new AccessLab(reader);
 private final String owner=UUID.randomUUID().toString();
 @Test void syntheticParityAndExpiration(){
  for(var entry:Map.of("ascenso",60,"consulta",60,"compartido",70).entrySet()){
   var r=JsonMapper.builder().build().valueToTree(lab.run(new AccessLab.Request("synthetic",entry.getKey(),null,null,null,5,null,null),owner,Set.of("profile:read")));
   assertEquals(entry.getValue(),r.path("requested").path("risk").path("score").asInt());
   assertEquals(entry.getKey().equals("consulta")?60:0,r.path("alternative_evaluated").path("simulation").path("risk").path("score").asInt());
  }
  var r=JsonMapper.builder().build().valueToTree(lab.run(new AccessLab.Request("synthetic","ascenso",null,null,null,20,null,null),owner,Set.of("profile:read")));
  assertEquals(55,r.path("requested").path("risk").path("score").asInt());verifyNoInteractions(reader);
 }
 @Test void foreignSnapshotAndSharedPatchDeniedBeforeReading(){
  assertThrows(ApiException.class,()->lab.run(new AccessLab.Request("live",null,UUID.randomUUID().toString(),Set.of("ADMIN"),Set.of(),0,null,null),owner,Set.of("profile:read")));
  assertThrows(ApiException.class,()->lab.run(new AccessLab.Request("live",null,null,null,Set.of(),0,"USER",Set.of()),owner,Set.of("profile:read")));
  verifyNoInteractions(reader);
 }
 @Test void immutableInputsUnknownPermissionsAndPartialSnapshot(){
  var catalog=Map.of("USER",Set.of("profile:read"),"ADMIN",Set.of("profile:read","users:read","users:write"));
  var s=new Snapshot(catalog,List.of(new Subject("objetivo",Set.of("USER"),true,2)),false,"objetivo");
  var r=JsonMapper.builder().build().valueToTree(lab.report(s,Map.of("objetivo",Set.of("USER")),Map.of(),Set.of("profile:read"),0,false));
  assertEquals(10,r.path("requested").path("risk").path("score").asInt());
  assertEquals("unknown",r.path("requested").path("remaining_active_writers").asString());
  assertEquals(Set.of("USER"),s.subjects().getFirst().roles());assertEquals(2,s.subjects().getFirst().refreshCount());
  assertEquals(0,r.path("alternative_evaluated").path("simulation").path("risk").path("score").asInt());
  assertThrows(ApiException.class,()->lab.report(s,Map.of(),Map.of("USER",Set.of("shell:exec")),Set.of(),0,false));
 }
 @Test void revokingCriticalPermissionSignalsWriterLossWithoutRevocation(){
  var s=new Snapshot(Map.of("ADMIN",Set.of("profile:read","users:write")),List.of(new Subject("u-1",Set.of("ADMIN"),true,2)),true,"u-1");
  var r=JsonMapper.builder().build().valueToTree(lab.report(s,Map.of(),Map.of("ADMIN",Set.of("profile:read")),Set.of("profile:read"),0,true));
  assertEquals(40,r.path("requested").path("risk").path("score").asInt());
  assertEquals(0,r.path("requested").path("affected").get(0).path("sessions_invalidated_by_change").asInt());
 }
}
