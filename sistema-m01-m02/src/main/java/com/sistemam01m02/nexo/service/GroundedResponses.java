package com.sistemam01m02.nexo.service;

import com.sistemam01m02.shared.error.ApiException;
import java.util.*;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

final class GroundedResponses {
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final Map<String,String> FOCUS = Map.of(
      "permissions","Revisa primero los permisos que se ganan o se pierden y las operaciones que habilitan.",
      "tokens","Revisa primero qué tokens vigentes conocidos se verían afectados y a qué horizonte.",
      "alternative","Compara primero el escenario solicitado con la alternativa del catálogo.");
  private static final Map<String,String> TITLES = Map.of(
      "results","Informe de resultados", "summary","Resumen de trabajo", "incidents","Informe de incidencias");
  private GroundedResponses() {}

  static Map<String,Object> reportSchema(int count) {
    return Map.of("type","object","additionalProperties",false,"required",List.of("title","first"),
        "properties",Map.of("title",Map.of("type","string","enum",TITLES.keySet()),
          "first",Map.of("type","integer","minimum",1,"maximum",count)));
  }

  static Map<String,Object> simulationSchema() {
    return Map.of("type","object","additionalProperties",false,"required",List.of("focus"),
        "properties",Map.of("focus",Map.of("type","string","enum",FOCUS.keySet())));
  }

  static List<String> excerpts(String source) {
    return Arrays.stream(source.split("\\R+|(?<=[.!?;])\\s+"))
        .map(String::strip).filter(s -> !s.isBlank()).toList();
  }

  static String report(List<String> sources, String plan) {
    JsonNode root = parse(plan);
    String title = TITLES.get(root.path("title").asString(""));
    JsonNode first = root.path("first");
    if (root.size()!=2 || title==null || !first.isIntegralNumber() || !first.canConvertToInt()) throw invalid();
    int index = first.asInt() - 1;
    if (index < 0 || index >= sources.size()) throw invalid();
    List<String> ordered = new ArrayList<>(sources);
    ordered.addFirst(ordered.remove(index));
    StringBuilder result = new StringBuilder(title).append("\n\nDatos aportados\n");
    for (int i=0; i<ordered.size(); i++) result.append("\n").append(i+1).append(". ").append(ordered.get(i));
    return result.append("\n\nVerificación pendiente: comprueba los datos aportados con sus fuentes.").toString();
  }

  static String simulation(Map<String,Object> data, String plan) {
    JsonNode choice = parse(plan);
    String focus = FOCUS.get(choice.path("focus").asString(""));
    if (choice.size()!=1 || focus==null) throw invalid();
    JsonNode report = JSON.valueToTree(data);
    JsonNode requested = report.path("requested");
    JsonNode alternative = report.path("alternative_evaluated");
    boolean synthetic = report.path("source").asString().equals("synthetic");
    String unit = synthetic ? "tokens sintéticos conocidos" : "tokens de renovación (refresh) vigentes conocidos";
    long count = 0;
    for (JsonNode row : requested.path("affected")) count += row.path("sessions_invalidated_by_change").asLong();
    StringBuilder text = new StringBuilder(focus)
        .append("\n\nEscenario solicitado: riesgo ").append(requested.path("risk").path("label").asString())
        .append(", ").append(requested.path("risk").path("score").asInt()).append("/100.")
        .append("\n").append(count).append(" ").append(unit).append(" afectados en la hipótesis.");
    if (!synthetic) text.append(" El total de sesiones y de JWT de acceso es desconocido.");
    if (alternative.has("simulation")) {
      text.append("\nAlternativa: ");
      if (alternative.path("no_change_needed").asBoolean()) text.append("conservar el estado actual");
      else text.append(report.path("least_privilege_candidates").path(0).path("roles"));
      text.append(", ").append(alternative.path("simulation").path("risk").path("score").asInt()).append("/100.");
    } else text.append("\nEl catálogo evaluado no contiene una alternativa para la tarea.");
    return text.append("\nReversión propuesta: revisar roles y permisos y solicitar un nuevo inicio de sesión. ")
        .append("La simulación no aplica cambios.").toString();
  }

  private static JsonNode parse(String plan) {
    try {
      var node=JSON.readTree(plan);
      if(node!=null && node.isObject()) return node;
    } catch(RuntimeException ignored) {
      // Invalid model output follows the same path as an invalid plan.
    }
    throw invalid();
  }

  private static ApiException invalid() {
    return new ApiException(502,"INVALID_MODEL_PLAN","Ollama no produjo una estructura válida. Reformula la solicitud.");
  }
}
