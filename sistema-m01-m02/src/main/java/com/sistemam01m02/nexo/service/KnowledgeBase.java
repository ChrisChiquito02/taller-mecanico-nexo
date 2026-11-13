package com.sistemam01m02.nexo.service;

import java.util.*;
import java.text.Normalizer;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/** Small lexical RAG, curated at build time. No filesystem traversal or user-shared index. */
@Service
public class KnowledgeBase {
 public record Document(String id,String title,String source,String permission,String text) {}
 private final List<Document> documents;
 public KnowledgeBase(){
  List<Document> loaded=new ArrayList<>();
  for(String id:List.of("arquitectura","identidad","laboratorio","operacion","seguridad"))try{
   String raw=new ClassPathResource("nexo/"+id+".md").getContentAsString(StandardCharsets.UTF_8);
   String[] parts=raw.split("\n",4);
   loaded.add(new Document(id,parts[0],parts[1],parts[2],parts[3]));
  }catch(java.io.IOException e){throw new IllegalStateException("Documento autorizado ausente",e);}
  documents=List.copyOf(loaded);
 }
 public List<Document> search(String query,Set<String> permissions){
  Set<String> words=new HashSet<>(Arrays.asList(normalize(query).split("[^a-z0-9]+")));
  words.removeIf(w->w.length()<4 || Set.of("para","como","puede","sobre","with","that","this").contains(w));
  return documents.stream().filter(d->permissions.contains(d.permission()))
   .filter(d->score(d,words)>0).sorted(Comparator.<Document>comparingInt(d->score(d,words)).reversed()).limit(3).toList();
 }
 public Optional<Document> get(String id,Set<String> permissions){return documents.stream().filter(d->d.id().equals(id)&&permissions.contains(d.permission())).findFirst();}
 private int score(Document d,Set<String> words){String s=normalize(d.title()+" "+d.text());return (int)words.stream().filter(s::contains).count();}
 static String normalize(String s){return Normalizer.normalize(s.toLowerCase(Locale.ROOT),Normalizer.Form.NFD).replaceAll("\\p{M}","");}
}
