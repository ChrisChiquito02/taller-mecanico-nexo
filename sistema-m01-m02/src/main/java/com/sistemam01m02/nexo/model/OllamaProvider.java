package com.sistemam01m02.nexo.model;

import com.sistemam01m02.shared.error.ApiException;
import java.net.*;
import java.net.http.*;
import java.io.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class OllamaProvider implements ModelProvider {
 private final NexoSettings settings;
 private final URI base;
 private final JsonMapper json = JsonMapper.builder().build();
 private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
   .followRedirects(HttpClient.Redirect.NEVER).proxy(new ProxySelector(){
    public List<Proxy> select(URI uri){return List.of(Proxy.NO_PROXY);}
    public void connectFailed(URI u,SocketAddress s,IOException e){}
   }).build();
 public OllamaProvider(NexoSettings settings) {
  this.settings=settings; base=URI.create(settings.baseUrl());
  // A local/private service only. Public model APIs are deliberately not enabled.
  if (!Set.of("127.0.0.1","localhost","host.docker.internal","ollama").contains(base.getHost())
      || !"http".equals(base.getScheme()) || base.getUserInfo()!=null || base.getQuery()!=null
      || base.getFragment()!=null || !Set.of("","/").contains(base.getPath()))
   throw new IllegalArgumentException("NEXO_BASE_URL debe apuntar al servicio Ollama local autorizado");
 }
 public Map<String,Object> status() {
  boolean connected=false;
  if (!settings.provider().equals("disabled")) try {
   var r=client.send(HttpRequest.newBuilder(base.resolve("/api/tags")).timeout(Duration.ofSeconds(3)).GET().build(),HttpResponse.BodyHandlers.ofInputStream());
   try(var body=r.body()) {
    var root=json.readTree(body.readNBytes(65537));
    if(r.statusCode()==200 && root.path("models").isArray())
     for(var model:root.path("models")) if(settings.model().equals(model.path("name").asString())) connected=true;
   }
  } catch(Exception ignored) { }
  return Map.of("provider",settings.provider(),"model",settings.model(),"connected",connected,
   "privacy","local","guide","Abre Ollama e instala el modelo configurado. No hay respuestas simuladas ni fallback externo.",
   "maxTokens",settings.maxTokens(),"retentionDays",settings.retentionDays());
 }
 public String plan(List<Message> messages) {
  return structured(messages,"json");
 }
 public String plan(List<Message> messages,Map<String,Object> schema) {
  return structured(messages,schema);
 }
 private String structured(List<Message> messages,Object format) {
  var result=generate(messages,null,format);
  if(result.truncated())throw new ApiException(502,"INVALID_MODEL_PLAN","Ollama no terminó de seleccionar una herramienta");
  return result.text();
 }
 public Generation stream(List<Message> messages,Consumer<String> delta) {return generate(messages,delta,null);}
 private Generation generate(List<Message> messages,Consumer<String> delta,Object format) {
  if(settings.provider().equals("disabled")) throw unavailable();
  Map<String,Object> payload=new LinkedHashMap<>();
  payload.put("model",settings.model()); payload.put("messages",messages); payload.put("stream",true);
  payload.put("options",Map.of("temperature",0.2,"num_predict",format!=null?180:settings.maxTokens(),"num_ctx",8192));
  if(format!=null) payload.put("format",format);
  var request=HttpRequest.newBuilder(base.resolve("/api/chat")).timeout(Duration.ofSeconds(settings.timeoutSeconds()))
    .header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
  // A deadline closes stalled streams as well as the initial HTTP request.
  try(var timer=Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory())) {
   var response=client.send(request,HttpResponse.BodyHandlers.ofInputStream());
   try(var input=response.body()) {
    var deadline=timer.schedule(()->{try{input.close();}catch(IOException ignored){}},settings.timeoutSeconds(),TimeUnit.SECONDS);
    if(response.statusCode()!=200) throw unavailable();
    var reader=new InputStreamReader(input,java.nio.charset.StandardCharsets.UTF_8);
    StringBuilder result=new StringBuilder(); boolean done=false; boolean truncated=false; int total=0;
    while(!done) {
     StringBuilder line=new StringBuilder(); int c;
     while((c=reader.read())!=-1 && c!='\n') {if(++total>300000 || line.length()>32000)throw unavailable();line.append((char)c);}
     if(line.isEmpty()) {if(c==-1)break;continue;}
     var item=json.readTree(line.toString()); if(item.has("error"))throw unavailable();
     String token=item.path("message").path("content").asString("");
     if(result.length()+token.length()>14000)throw unavailable();
     result.append(token); if(delta!=null&&!token.isEmpty())delta.accept(token);
     done=item.path("done").asBoolean(false);
     if(done)truncated=!"stop".equals(item.path("done_reason").asString(""));
    }
    deadline.cancel(false);
    if(!done || result.isEmpty())throw unavailable();
    return new Generation(result.toString(),truncated);
   } finally {timer.shutdownNow();}
  } catch(RuntimeException e){throw e;} catch(IOException | InterruptedException e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();throw unavailable();}
 }
 private ApiException unavailable(){return new ApiException(503,"MODEL_UNAVAILABLE","Ollama no completó la respuesta. Comprueba el modelo y su conexión local; no se generó una respuesta de reemplazo.");}
}
