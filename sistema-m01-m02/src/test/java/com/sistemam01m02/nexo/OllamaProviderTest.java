package com.sistemam01m02.nexo;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.util.*;
import com.sistemam01m02.nexo.model.*;
import com.sistemam01m02.shared.error.ApiException;

class OllamaProviderTest {
 NexoSettings settings(String base){return new NexoSettings("ollama",base,"test-model",10,64,7,8,100);}
 @Test void streamsRealHttpProtocolButStubModel()throws Exception{
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/chat",x->{x.getRequestBody().readAllBytes();x.sendResponseHeaders(200,0);try(var out=x.getResponseBody()){
   out.write("{\"message\":{\"content\":\"Hola \"},\"done\":false}\n".getBytes());out.flush();
   out.write("{\"message\":{\"content\":\"prueba\"},\"done\":true,\"done_reason\":\"length\"}\n".getBytes());
  }});server.start();try{
   var model=new OllamaProvider(settings("http://127.0.0.1:"+server.getAddress().getPort()));var chunks=new ArrayList<String>();
   var result=model.stream(List.of(new ModelProvider.Message("user","Hola")),chunks::add);
   assertEquals("Hola prueba",result.text());
   assertTrue(result.truncated());
   assertEquals(2,chunks.size());
   assertThrows(ApiException.class,()->model.plan(List.of(new ModelProvider.Message("user","Hola"))));
  }finally{server.stop(0);}
 }
 @Test void rejectsExternalUrlsRedirectsAndMissingCompletion()throws Exception{
  assertThrows(IllegalArgumentException.class,()->new OllamaProvider(settings("https://example.com")));
  assertThrows(IllegalArgumentException.class,()->new OllamaProvider(settings("http://localhost:11434?secret=x")));
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
  server.createContext("/api/chat",x->{x.getRequestBody().readAllBytes();x.getResponseHeaders().add("Location","http://example.com/");x.sendResponseHeaders(302,-1);x.close();});server.start();
  try{assertThrows(ApiException.class,()->new OllamaProvider(settings("http://127.0.0.1:"+server.getAddress().getPort())).stream(List.of(),s->{}));}finally{server.stop(0);}
 }
}
