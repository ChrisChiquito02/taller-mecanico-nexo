package com.sistemam01m02.nexo.service;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Checks an explicit conjunction about the size of a single output. */
final class OutputConstraints {
  private static final String BOUND = "(exactamente|al menos|como minimo|como maximo|a lo sumo|hasta|exactly|at least|at most)";
  private static final String COUNT = "([0-9]{1,9}|cero|una?|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|zero|one|two|three|four|five|six|seven|eight|nine|ten)";
  private static final String UNIT = "(palabras?|lineas?|frases?|parrafos?|words?|lines?|sentences?|paragraphs?)";
  private static final String LIMIT = BOUND + "\\s+" + COUNT + "\\s+" + UNIT + "\\b";
  private static final String PREPOSITION = "(?:(?:con|en|with|in)\\s+)?";
  private static final String CONJUNCTION = "\\s+(?:y|and)\\s+";
  private static final Pattern REQUEST = Pattern.compile(
      "^(?:responde|escribe|redacta|devuelve|genera|usa|answer|write|return|use)\\s+"
      + PREPOSITION + LIMIT + "(?:" + CONJUNCTION + PREPOSITION + LIMIT
      + ")*(?=\\s*(?:[.!?¿]|$))");
  private static final Pattern LIMIT_PATTERN = Pattern.compile(LIMIT);
  private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}]*"
      + "(?:['’\\-][\\p{L}\\p{N}][\\p{L}\\p{N}\\p{M}]*)*");
  private static final Map<String,Integer> NUMBERS = Map.ofEntries(
      Map.entry("cero",0), Map.entry("una",1), Map.entry("un",1), Map.entry("uno",1),
      Map.entry("dos",2), Map.entry("tres",3), Map.entry("cuatro",4), Map.entry("cinco",5),
      Map.entry("seis",6), Map.entry("siete",7), Map.entry("ocho",8), Map.entry("nueve",9), Map.entry("diez",10),
      Map.entry("zero",0), Map.entry("one",1), Map.entry("two",2), Map.entry("three",3),
      Map.entry("four",4), Map.entry("five",5), Map.entry("six",6), Map.entry("seven",7),
      Map.entry("eight",8), Map.entry("nine",9), Map.entry("ten",10));
  private record Range(int min, int max) {}

  private OutputConstraints() {}

  private record Limit(String unit, Range range, String text) {}

  record Requirements(List<Limit> limits, boolean english) {
    boolean isEmpty() { return limits.isEmpty(); }

    Optional<String> clarification() {
      for (int i = 0; i < limits.size(); i++) {
        Limit first = limits.get(i);
        for (Limit second : limits.subList(i + 1, limits.size())) {
          if (!first.unit().equals(second.unit())) continue;
          if (Math.max(first.range().min(), second.range().min()) <= Math.min(first.range().max(), second.range().max())) continue;
          return Optional.of(english
              ? "The requirements “" + first.text() + "” and “" + second.text() + "” conflict. Which should I follow?"
              : "Las restricciones «" + first.text() + "» y «" + second.text() + "» son incompatibles. ¿Cuál debo seguir?");
        }
      }
      return Optional.empty();
    }

    boolean accepts(String answer) {
      return limits.stream().allMatch(limit -> {
        long size = count(answer.strip(), limit.unit(), english);
        return size >= limit.range().min() && size <= limit.range().max();
      });
    }

    String instruction() {
      Map<String,Range> combined = new LinkedHashMap<>();
      for (Limit limit : limits) {
        combined.merge(limit.unit(), limit.range(), (a,b) ->
            new Range(Math.max(a.min(), b.min()), Math.min(a.max(), b.max())));
      }
      var targets = combined.entrySet().stream().map(entry -> {
        Range range = entry.getValue();
        String bound = range.min() == range.max() ? "=" + range.min()
            : range.max() == Integer.MAX_VALUE ? "≥" + range.min()
            : range.min() == 0 ? "≤" + range.max() : range.min() + "–" + range.max();
        return entry.getKey() + " " + bound;
      }).toList();
      return english
          ? "Required output counts: " + targets + ". Return only the requested content. Use one physical line per required line, without introductory text or a code fence. Count the units before answering."
          : "Conteos obligatorios: " + targets + ". Devuelve solo el contenido solicitado. Usa un salto de línea real para cada línea adicional. Omite introducciones y bloques de código. Cuenta las unidades antes de responder.";
    }

    String correction(String answer) {
      var sizes = limits.stream().map(Limit::unit).distinct()
          .map(unit -> unit + "=" + count(answer.strip(), unit, english)).toList();
      return english
          ? instruction() + " The preceding attempt measured " + sizes + ". Produce a fresh answer."
          : instruction() + " El intento anterior midió " + sizes + ". Genera una respuesta nueva.";
    }

    String failureMessage() {
      String requested = String.join(", ", limits.stream().map(Limit::text).toList());
      return english
          ? "Ollama did not produce an answer within these limits: " + requested + ". Try relaxing a limit or narrowing the request."
          : "Ollama no generó una respuesta que respete estos límites: " + requested
              + ". Prueba flexibilizar un límite o acotar la petición.";
    }
  }

  static Requirements parse(String prompt) {
    var request = REQUEST.matcher(KnowledgeBase.normalize(prompt.strip()));
    if (!request.find()) return new Requirements(List.of(), false);
    var matches = LIMIT_PATTERN.matcher(request.group());
    List<Limit> limits = new ArrayList<>();
    while (matches.find()) {
      limits.add(new Limit(singular(matches.group(3)), range(matches.group(1), matches.group(2)), matches.group()));
    }
    return new Requirements(List.copyOf(limits), request.group(1).matches("exactly|at least|at most"));
  }

  private static long count(String answer, String unit, boolean english) {
    if (answer.isEmpty()) return 0;
    return switch (unit) {
      case "palabra", "word" -> WORD.matcher(answer).results().count();
      case "linea", "line" -> answer.lines().filter(line -> !line.isBlank()).count();
      case "parrafo", "paragraph" -> answer.split("\\R\\h*\\R+").length;
      case "frase", "sentence" -> {
        var iterator = BreakIterator.getSentenceInstance(english ? Locale.ENGLISH : Locale.forLanguageTag("es"));
        iterator.setText(answer);
        long sentences = 0;
        for (int start = iterator.first(), end = iterator.next();
             end != BreakIterator.DONE; start = end, end = iterator.next()) {
          if (WORD.matcher(answer.substring(start, end)).find()) sentences++;
        }
        yield sentences;
      }
      default -> throw new IllegalArgumentException("Unidad de salida desconocida: " + unit);
    };
  }

  private static String singular(String unit) {
    return unit.endsWith("s") ? unit.substring(0, unit.length() - 1) : unit;
  }

  private static Range range(String bound, String count) {
    int n = Character.isDigit(count.charAt(0)) ? Integer.parseInt(count) : NUMBERS.get(count);
    return switch (bound) {
      case "al menos", "como minimo", "at least" -> new Range(n, Integer.MAX_VALUE);
      case "como maximo", "a lo sumo", "hasta", "at most" -> new Range(0, n);
      default -> new Range(n, n);
    };
  }
}
