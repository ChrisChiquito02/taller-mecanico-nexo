package com.sistemam01m02.nexo.repository;

import java.util.*;
import java.time.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.sistemam01m02.shared.error.ApiException;

@Repository
public class ConversationStore {
 private final JdbcTemplate db;
 private final TurnReservations reservations;
 public ConversationStore(JdbcTemplate db,TurnReservations reservations){this.db=db;this.reservations=reservations;}
 private static final String OWNER="owner_id=UNHEX(REPLACE(?,'-','')) AND auth_scope=? AND expires_at>UTC_TIMESTAMP(6)";
 public List<Map<String,Object>> list(String owner,String scope){
  return db.queryForList("SELECT id,title,created_at,expires_at FROM nexo_conversation WHERE "+OWNER+" ORDER BY created_at DESC LIMIT 30",owner,scope);
 }
 @Transactional
 public String create(String owner,String scope,int days){
  // Serialize quota checks per owner, without modifying the IAM row.
  db.queryForObject("SELECT HEX(id) FROM app_user WHERE id=UNHEX(REPLACE(?,'-','')) FOR UPDATE",String.class,owner);
  if(db.queryForObject("SELECT COUNT(*) FROM nexo_conversation WHERE owner_id=UNHEX(REPLACE(?,'-','')) AND expires_at>UTC_TIMESTAMP(6)",Integer.class,owner)>=30)
   throw new ApiException(429,"CHAT_LIMIT","Elimina una conversación antes de crear otra (máximo 30)");
  String id=UUID.randomUUID().toString();
  db.update("INSERT INTO nexo_conversation VALUES(?,UNHEX(REPLACE(?,'-','')),?,'Conversación NEXO',UTC_TIMESTAMP(6),?)",id,owner,scope,java.sql.Timestamp.from(Instant.now().plus(Duration.ofDays(days))));return id;
 }
 public void requireOwner(String id,String owner,String scope){
  if(db.queryForObject("SELECT COUNT(*) FROM nexo_conversation WHERE id=? AND "+OWNER,Integer.class,id,owner,scope)!=1)
   throw new ApiException(404,"CHAT_NOT_FOUND","Conversación no disponible");
 }
 public List<Map<String,Object>> messages(String id,String owner,String scope){
  requireOwner(id,owner,scope);
  return db.queryForList("SELECT speaker,content,metadata,created_at FROM nexo_message WHERE conversation_id=? ORDER BY id LIMIT 80",id);
 }
 @Transactional
 public void append(String id,String owner,String scope,String turn,String prompt,String answer,String metadata){
  if(!reservations.active(owner,turn,true))throw new ApiException(409,"TURN_CANCELLED","Generación detenida");
  if(db.queryForList("SELECT id FROM nexo_conversation WHERE id=? AND "+OWNER+" FOR UPDATE",id,owner,scope).isEmpty())
   throw new ApiException(404,"CHAT_NOT_FOUND","Conversación no disponible");
  if(db.queryForObject("SELECT COUNT(*) FROM nexo_message WHERE conversation_id=?",Integer.class,id)>=80)throw new ApiException(429,"CHAT_FULL","Crea una conversación nueva (máximo 40 turnos)");
  db.update("INSERT INTO nexo_message(conversation_id,speaker,content,metadata,created_at) VALUES(?,'user',?,'{}',UTC_TIMESTAMP(6)),(?,'assistant',?,?,UTC_TIMESTAMP(6))",id,prompt,id,answer,metadata);
  reservations.release(owner,turn);
 }
 public void delete(String id,String owner,String scope){requireOwner(id,owner,scope);db.update("DELETE FROM nexo_conversation WHERE id=? AND "+OWNER,id,owner,scope);}
 public void audit(String actor,String code){db.update("INSERT INTO nexo_audit(actor_fingerprint,event_code,created_at) VALUES(?,?,UTC_TIMESTAMP(6))",actor,code);}
 public int turnsToday(String actor){return db.queryForObject("SELECT COUNT(*) FROM nexo_audit WHERE actor_fingerprint=? AND event_code='TURN_STARTED' AND created_at>UTC_TIMESTAMP(6)-INTERVAL 1 DAY",Integer.class,actor);}
 public void purge(){db.update("DELETE FROM nexo_conversation WHERE expires_at<=UTC_TIMESTAMP(6)");db.update("DELETE FROM nexo_audit WHERE created_at<UTC_TIMESTAMP(6)-INTERVAL 30 DAY");}
}
