package com.sistemam01m02.nexo.service;

import com.sistemam01m02.shared.error.ApiException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroundedResponsesTest {
  @Test void reportReordersOnlyVerifiedSourceIds() {
    var source=List.of("Prueba A aprobada.", "Prueba B fallida.");
    var text=GroundedResponses.report(source,"{\"title\":\"results\",\"first\":2}");
    assertTrue(text.indexOf(source.get(1))<text.indexOf(source.get(0)));
    for(String first:List.of("0","3","\"1\"","1.0","[]")){
      assertThrows(ApiException.class,()->GroundedResponses.report(source,
          "{\"title\":\"results\",\"first\":"+first+"}"));
    }
  }

  @Test void liveExplanationNeverLabelsKnownTokensAsTotalSessions() {
    var result=Map.<String,Object>of("source","authorized_live_read","requested",
        Map.of("risk",Map.of("label","medio","score",45),
               "affected",List.of(Map.of("sessions_invalidated_by_change",4))),
        "alternative_evaluated",Map.of());
    String text=GroundedResponses.simulation(result,"{\"focus\":\"tokens\"}");
    assertTrue(text.contains("4 tokens de renovación (refresh) vigentes conocidos"));
    assertTrue(text.contains("El total de sesiones y de JWT de acceso es desconocido."));
    assertThrows(ApiException.class,()->GroundedResponses.simulation(result,
        "{\"focus\":\"tokens\",\"count\":99}"));
  }
}
