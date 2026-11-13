package com.sistemam01m02.nexo.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OutputConstraintsTest {
  @Test void detectsDisjointBoundsForTheSameOutput() {
    assertTrue(OutputConstraints.parse("Escribe exactamente tres líneas y al menos cinco líneas.").clarification().isPresent());
    assertTrue(OutputConstraints.parse("Devuelve al menos 9 palabras y como máximo 4 palabras").clarification().isPresent());
    assertTrue(OutputConstraints.parse("Write at least 8 paragraphs and at most 2 paragraphs.").clarification().orElseThrow().contains("Which"));
  }

  @Test void leavesAlternativesQuotesAndCompatibleConstraintsToTheModel() {
    for (String text : new String[] {
        "Escribe exactamente tres líneas y al menos dos líneas.",
        "Escribe exactamente tres líneas y exactamente cuatro palabras.",
        "Escribe exactamente tres líneas o al menos cinco líneas.",
        "Analiza la frase: «Escribe exactamente tres líneas y al menos cinco líneas.»",
        "Escribe exactamente tres líneas y exactamente cinco líneas para otra sección."}) {
      assertTrue(OutputConstraints.parse(text).clarification().isEmpty(), text);
    }
  }
  @Test void checksSingleAndCombinedLimitsOnActualOutput() {
    var words=OutputConstraints.parse("Responde con exactamente dos palabras y al menos una palabra. Saluda.");
    assertTrue(words.accepts("¡Hola, vecina!"));
    assertFalse(words.accepts("¡Hola! ¿Cómo estás?"));
    var lines=OutputConstraints.parse("Write exactly two lines and at most three lines.");
    assertTrue(lines.accepts("Red\r\nBlue"));
    assertFalse(lines.accepts("Red and blue."));
    var mixed=OutputConstraints.parse("Escribe exactamente 2 párrafos y al menos 4 palabras y como máximo 6 palabras.");
    assertTrue(mixed.accepts("Primer párrafo.\n\nSegundo párrafo."));
    assertFalse(mixed.accepts("Todo en un solo párrafo."));
    assertTrue(OutputConstraints.parse("Write exactly two sentences.").accepts("One is red. Another is blue."));
    assertTrue(OutputConstraints.parse("Escribe al menos una palabra y exactamente dos líneas y como máximo cero palabras.")
        .clarification().isPresent());
  }
}
