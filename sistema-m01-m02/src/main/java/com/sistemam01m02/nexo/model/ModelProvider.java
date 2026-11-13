package com.sistemam01m02.nexo.model;
import java.util.*;
import java.util.function.Consumer;
/** Provider boundary. No provider can access databases, files, tools or IAM credentials. */
public interface ModelProvider {
 record Message(String role, String content) {}
 record Generation(String text, boolean truncated) {}
 Map<String,Object> status();
 String plan(List<Message> messages);
 default String plan(List<Message> messages,Map<String,Object> schema){return plan(messages);}
 Generation stream(List<Message> messages, Consumer<String> onDelta);
}
