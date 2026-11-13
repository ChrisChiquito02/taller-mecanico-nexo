from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import urllib.request, json, hashlib, time
class Proxy(BaseHTTPRequestHandler):
    def relay(self):
        payload = self.rfile.read(int(self.headers.get("Content-Length", "0"))) if self.command == "POST" else None
        started = time.monotonic()
        with urllib.request.urlopen(urllib.request.Request("http://127.0.0.1:11434"+self.path, data=payload, headers={"Content-Type":"application/json"}),timeout=190) as upstream:
            self.send_response(upstream.status)
            self.send_header("Content-Type",upstream.headers["Content-Type"])
            self.end_headers()
            parts=[]
            for line in upstream:
                self.wfile.write(line); self.wfile.flush()
                if payload:
                    event=json.loads(line)
                    parts.append(event.get("message",{}).get("content",""))
                    if event.get("done"):
                        request=json.loads(payload)
                        print(json.dumps({"path":self.path,"model":event.get("model"),"plan":request.get("format")=="json","roles":[m["role"] for m in request["messages"]],"request_sha256":hashlib.sha256(payload).hexdigest(),"answer_sha256":hashlib.sha256("".join(parts).encode()).hexdigest(),"eval_count":event.get("eval_count"),"done_reason":event.get("done_reason"),"seconds":round(time.monotonic()-started,3)}),flush=True)
    do_GET=relay
    do_POST=relay
ThreadingHTTPServer(("127.0.0.1",41434),Proxy).serve_forever()
