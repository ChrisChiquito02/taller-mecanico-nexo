package com.sistemam01m02.nexo.service;

import java.util.Arrays;
import java.util.regex.Pattern;

/** Extracts literal text. Attachments never enter the planner or the model. */
final class AttachmentReader {
  private static final Pattern INVENTORY_CODE = Pattern.compile(
      "(?imu)^\\s*c[oó]digo\\h+de\\h+inventario\\h*[:=]\\h*([a-z0-9][a-z0-9_-]{0,63})\\h*$");

  private AttachmentReader() {}

  static String read(String question, NexoService.Attachment file) {
    String request = KnowledgeBase.normalize(question);
    if (request.contains("codigo") && request.contains("inventario")) {
      var matches = INVENTORY_CODE.matcher(file.text());
      if (!matches.find()) return "No encontré una línea con un código de inventario válido.";
      String code = matches.group(1);
      if (matches.find()) return "Encontré varias líneas de código de inventario. Indica cuál deseas extraer.";
      return "Código de inventario: " + code;
    }
    String excerpt = String.join("\n", Arrays.stream(file.text().split("[\\r\\n.!?]+"))
        .map(String::strip).filter(line -> !line.isEmpty()).limit(1)
        .map(line -> line.substring(0, Math.min(line.length(), 160))).toList());
    return "Extracto literal de " + file.name() + " (datos del archivo):\n" + excerpt;
  }
}
