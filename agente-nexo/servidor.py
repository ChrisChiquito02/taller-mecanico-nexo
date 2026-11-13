#!/usr/bin/env python3
"""Panel local de NEXO-Δ: solo fixtures sintéticos, sin acceso al IAM."""
import argparse
import copy
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
from pathlib import Path
import threading
import urllib.error
import webbrowser

from nexo import run

BASE = Path(__file__).resolve().parent
MODEL = "qwen2.5-coder:7b"
SCENARIOS = {
    "ascenso": {"file": "escenario.json", "title": "Convertir USER en ADMIN"},
    "consulta": {"file": "escenario-lectura.json", "title": "Dar acceso de consulta"},
    "compartido": {"file": "escenario-permisos.json", "title": "Agregar escritura a USER"},
}
STATIC = {"/": ("index.html", "text/html; charset=utf-8"),
          "/app.js": ("app.js", "text/javascript; charset=utf-8"),
          "/styles.css": ("styles.css", "text/css; charset=utf-8")}

class NexoServer(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, port):
        super().__init__(("127.0.0.1", port), Handler)
        self.origin = f"http://127.0.0.1:{self.server_port}"
        self.inference_lock = threading.Lock()

class Handler(BaseHTTPRequestHandler):
    server_version = "NEXO"
    sys_version = ""

    def log_message(self, *args):
        pass  # Sin cuerpos, cookies, informes ni credenciales en logs.

    def reply(self, status, body, kind="application/json; charset=utf-8"):
        if not isinstance(body, bytes):
            body = json.dumps(body, ensure_ascii=False).encode()
        self.send_response(status)
        self.send_header("Content-Type", kind)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.send_header("Referrer-Policy", "no-referrer")
        self.send_header("Content-Security-Policy", "default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; base-uri 'none'; frame-ancestors 'none'; form-action 'self'")
        self.end_headers()
        try:
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass

    def host_allowed(self):
        # Comprobar Host también en GET impide servir el panel a través de DNS rebinding.
        if self.headers.get("Host") != self.server.origin.removeprefix("http://"):
            self.reply(403, {"error": "Acceso local rechazado"})
            return False
        return True

    def do_GET(self):
        if not self.host_allowed(): return
        if self.path == "/api/scenarios":
            self.reply(200, {"scenarios": [{"id": key, "title": value["title"]} for key, value in SCENARIOS.items()], "model": MODEL})
            return
        if self.path not in STATIC:
            self.reply(404, {"error": "Ruta no disponible"}); return
        name, kind = STATIC[self.path]
        self.reply(200, (BASE/"panel"/name).read_bytes(), kind)

    def do_POST(self):
        if not self.host_allowed(): return
        if self.path != "/api/simulate":
            self.reply(404, {"error": "Ruta no disponible"}); return
        if self.headers.get("Origin") != self.server.origin:
            self.reply(403, {"error": "Origen no permitido"}); return
        if self.headers.get("Content-Type", "").split(";", 1)[0].strip().lower() != "application/json":
            self.reply(415, {"error": "Se requiere JSON"}); return
        if self.headers.get("Transfer-Encoding"):
            self.reply(400, {"error": "Solicitud no válida"}); return
        try:
            size = int(self.headers.get("Content-Length", "0"))
            if not 0 < size <= 2048:
                self.reply(413, {"error": "Solicitud fuera del tamaño permitido"}); return
            self.connection.settimeout(10)
            data = json.loads(self.rfile.read(size))
            if not isinstance(data, dict) or set(data) != {"scenario", "at_minute", "use_ai"}:
                raise ValueError()
            if not isinstance(data["scenario"], str) or data["scenario"] not in SCENARIOS:
                raise ValueError()
            if type(data["at_minute"]) is not int or not 0 <= data["at_minute"] <= 10080:
                raise ValueError()
            if type(data["use_ai"]) is not bool: raise ValueError()
        except (ValueError, TypeError, TimeoutError):
            self.reply(400, {"error": "Elige un escenario y un minuto entero entre 0 y 10080"}); return
        if not self.server.inference_lock.acquire(blocking=False):
            self.reply(409, {"error": "Hay un análisis en curso. Espera a que termine"}); return
        try:
            source = json.loads((BASE/SCENARIOS[data["scenario"]]["file"]).read_text())
            snapshot = copy.deepcopy(source)
            snapshot["at_minute"] = data["at_minute"]
            report = run(snapshot, use_ai=data["use_ai"], model=MODEL)
            self.reply(200, report)
        except urllib.error.URLError:
            self.reply(503, {"error": "No se pudo completar la inferencia local. Abre Ollama y comprueba que qwen2.5-coder:7b está instalado. No se sustituyó la IA por una respuesta simulada"})
        except Exception:
            self.reply(503, {"error": "No se pudo completar el análisis. No se realizaron cambios. Puedes revisar el escenario o volver a intentarlo"})
        finally:
            self.server.inference_lock.release()

    def do_OPTIONS(self):
        if self.host_allowed(): self.reply(403, {"error": "No se permite acceso desde otros sitios"})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--port", type=int, default=0, help="0 elige un puerto local libre")
    parser.add_argument("--open", action="store_true", help="Abrir el navegador")
    args = parser.parse_args()
    with NexoServer(args.port) as server:
        print(f"NEXO-Δ disponible en {server.origin}", flush=True)
        print("Datos sintéticos. Pulsa Ctrl+C para cerrar el panel.", flush=True)
        if args.open: webbrowser.open(server.origin)
        try: server.serve_forever()
        except KeyboardInterrupt: pass

if __name__ == "__main__": main()
