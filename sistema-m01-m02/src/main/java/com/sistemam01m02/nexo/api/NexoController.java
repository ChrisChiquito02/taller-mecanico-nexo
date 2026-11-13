package com.sistemam01m02.nexo.api;
import com.sistemam01m02.nexo.service.*;
import com.sistemam01m02.nexo.service.NexoService.*;
import com.sistemam01m02.shared.error.ApiException;
import java.util.*;
import java.io.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.json.JsonMapper;

@RestController
@RequestMapping("/api/nexo")
@PreAuthorize("hasAuthority('profile:read')")
@EnableScheduling
public class NexoController {
 private final NexoService service;
 private final JsonMapper json=JsonMapper.builder().build();
 public NexoController(NexoService service){this.service=service;}
 private Actor actor(Authentication a){
  var jwt=((org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken)a).getToken();
  Number version=jwt.getClaim("sv");
  return new Actor(a.getName(),a.getAuthorities().stream().map(g->g.getAuthority()).collect(java.util.stream.Collectors.toUnmodifiableSet()),version.longValue(),jwt.getExpiresAt());
 }
 @GetMapping("/status") public Object status(Authentication a){return service.status(actor(a));}
 @GetMapping("/tools") public Object tools(Authentication a){return service.tools(actor(a));}
 @GetMapping("/documents/{id}") public Object document(Authentication a,@PathVariable String id){return service.document(actor(a),id);}
 @GetMapping("/conversations") public Object list(Authentication a){return service.list(actor(a));}
 @PostMapping(value="/conversations",consumes="application/json") public Object create(Authentication a,@RequestBody Map<String,Object> body){if(!body.isEmpty())throw new ApiException(400,"INVALID_INPUT","No se requieren campos");return service.create(actor(a));}
 @GetMapping("/conversations/{id}") public Object get(Authentication a,@PathVariable UUID id){return service.messages(actor(a),id.toString());}
 @DeleteMapping("/conversations/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void delete(Authentication a,@PathVariable UUID id){service.delete(actor(a),id.toString());}
 @PostMapping(value="/simulate",consumes="application/json") public Object simulate(Authentication a,@RequestBody AccessLab.Request request){return service.simulate(actor(a),request);}
 @DeleteMapping("/conversations/{id}/turns/{turn}")
 @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
 public void cancel(Authentication a,@PathVariable UUID id,@PathVariable UUID turn){service.cancel(actor(a),id.toString(),turn.toString());}
 @PostMapping(value="/conversations/{id}/messages",consumes="application/json",produces="application/x-ndjson")
 public void chat(Authentication a,@PathVariable UUID id,@RequestBody Turn turn,HttpServletResponse response)throws IOException{
  response.setContentType("application/x-ndjson");response.setCharacterEncoding("UTF-8");response.setHeader("Cache-Control","no-store");response.setHeader("X-Accel-Buffering","no");
  try{service.chat(actor(a),id.toString(),turn,event->{try{write(response,event);}catch(IOException e){throw new UncheckedIOException(e);}});}
  catch(ApiException e){if(!response.isCommitted())throw e;write(response,Map.of("type","error","code",e.code(),"text",e.getMessage()));}
  catch(UncheckedIOException ignored){/* Client disconnected: generation is aborted, no partial answer persisted. */}
 }
 private void write(HttpServletResponse response,Map<String,Object> event)throws IOException{
  var output=response.getOutputStream();
  output.write((json.writeValueAsString(event)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
  output.flush();
 }
}
