package com.sistemam01m02.nexo.repository;

import com.sistemam01m02.shared.error.ApiException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class TurnReservations {
  private static final String OWNER = "owner_id=UNHEX(REPLACE(?,'-',''))";
  private static final String ACTIVE = "expires_at>UTC_TIMESTAMP(6)";
  private final JdbcTemplate db;

  public TurnReservations(JdbcTemplate db) {
    this.db = db;
  }

  public void acquire(String owner, String chat, String turn, int lifetimeSeconds) {
    db.update("DELETE FROM nexo_turn_reservation WHERE " + OWNER + " AND NOT (" + ACTIVE + ")", owner);
    try {
      db.update("""
          INSERT INTO nexo_turn_reservation(owner_id,conversation_id,turn_id,expires_at)
          VALUES(UNHEX(REPLACE(?,'-','')),?,?,TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP(6)))
          """, owner, chat, turn, lifetimeSeconds);
    } catch (DuplicateKeyException e) {
      throw new ApiException(409, "BUSY", "Ya tienes una respuesta en curso");
    }
  }

  @Transactional
  public boolean acceptRequest(String owner, int limit) {
    db.update("""
        INSERT INTO nexo_request_quota(owner_id,expires_at,requests)
        VALUES(UNHEX(REPLACE(?,'-','')),TIMESTAMPADD(SECOND,60,UTC_TIMESTAMP(6)),0)
        ON DUPLICATE KEY UPDATE owner_id=owner_id
        """, owner);
    return db.update("""
        UPDATE nexo_request_quota
           SET requests=IF(expires_at<=UTC_TIMESTAMP(6),1,requests+1),
               expires_at=IF(expires_at<=UTC_TIMESTAMP(6),TIMESTAMPADD(SECOND,60,UTC_TIMESTAMP(6)),expires_at)
         WHERE owner_id=UNHEX(REPLACE(?,'-',''))
           AND (expires_at<=UTC_TIMESTAMP(6) OR requests<?)
        """, owner, limit)==1;
  }

  public boolean renew(String owner, String turn, int seconds) {
    return db.update("UPDATE nexo_turn_reservation SET expires_at=TIMESTAMPADD(SECOND,?,UTC_TIMESTAMP(6)) WHERE "
        + OWNER + " AND turn_id=? AND " + ACTIVE + " AND NOT cancelled", seconds, owner, turn)==1;
  }

  public boolean running(String owner, String chat) {
    return !db.queryForList("SELECT 1 FROM nexo_turn_reservation WHERE " + OWNER
        + " AND conversation_id=? AND " + ACTIVE + " AND NOT cancelled", owner, chat).isEmpty();
  }

  public boolean cancel(String owner, String chat, String turn) {
    return db.update("UPDATE nexo_turn_reservation SET cancelled=TRUE WHERE " + OWNER
        + " AND conversation_id=? AND turn_id=? AND " + ACTIVE, owner, chat, turn) == 1;
  }

  public boolean active(String owner, String turn, boolean lock) {
    return !db.queryForList("SELECT 1 FROM nexo_turn_reservation WHERE " + OWNER
        + " AND turn_id=? AND " + ACTIVE + " AND NOT cancelled" + (lock ? " FOR UPDATE" : ""),
        owner, turn).isEmpty();
  }

  public void release(String owner, String turn) {
    db.update("DELETE FROM nexo_turn_reservation WHERE " + OWNER + " AND turn_id=?", owner, turn);
  }

  public void purge() {
    db.update("DELETE FROM nexo_turn_reservation WHERE NOT (" + ACTIVE + ")");
    db.update("DELETE FROM nexo_request_quota WHERE expires_at<=UTC_TIMESTAMP(6)");
  }
}
