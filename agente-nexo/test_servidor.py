import hashlib
import http.client
import json
from pathlib import Path
import threading
import unittest
from unittest.mock import patch
import urllib.error
from servidor import NexoServer

class PanelBoundaryTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.server = NexoServer(0)
        cls.thread = threading.Thread(target=cls.server.serve_forever, kwargs={"poll_interval":0.02}, daemon=True)
        cls.thread.start()

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown(); cls.server.server_close(); cls.thread.join()

    def post(self, body=None, origin=None, host=None, content_type="application/json", raw=None):
        data = json.dumps(body or {"scenario":"ascenso","at_minute":5,"use_ai":False}).encode() if raw is None else raw
        connection = http.client.HTTPConnection("127.0.0.1", self.server.server_port, timeout=3)
        headers = {"Content-Type":content_type, "Origin":origin or self.server.origin}
        if host: headers["Host"] = host
        connection.request("POST", "/api/simulate", body=data, headers=headers)
        response = connection.getresponse(); status = response.status
        payload = json.loads(response.read()); connection.close()
        return status, payload

    def test_cross_site_and_rebound_host_cannot_invoke_model(self):
        with patch("servidor.run") as model:
            self.assertEqual(self.post(origin="https://other.example.test")[0],403)
            self.assertEqual(self.post(host="other.example.test")[0],403)
            model.assert_not_called()

    def test_no_arbitrary_files_secrets_or_large_inputs_accepted(self):
        with patch("servidor.run") as model:
            self.assertEqual(self.post({"scenario":"../../.env.local","at_minute":5,"use_ai":True})[0],400)
            self.assertEqual(self.post({"scenario":"ascenso","at_minute":5,"use_ai":True,"password":"private"})[0],400)
            self.assertEqual(self.post(raw=b"x"*2049)[0],413)
            self.assertEqual(self.post(content_type="text/plain")[0],415)
            model.assert_not_called()

    def test_simulation_matches_engine_and_keeps_fixture_unchanged(self):
        source = Path(__file__).parent / 'escenario.json'
        before = hashlib.sha256(source.read_bytes()).hexdigest()
        status, report = self.post({"scenario":"ascenso","at_minute":20,"use_ai":False})
        self.assertEqual(status,200)
        self.assertEqual(report['requested']['risk']['score'],55)
        self.assertEqual(report['requested']['affected'][0]['sessions_invalidated_by_change'],1)
        self.assertEqual(report['ai']['status'],'not_requested')
        self.assertEqual(before,hashlib.sha256(source.read_bytes()).hexdigest())

    def test_model_failure_never_returns_fake_ai_success(self):
        with patch("servidor.run",side_effect=urllib.error.URLError('unavailable')):
            status, response = self.post({"scenario":"ascenso","at_minute":5,"use_ai":True})
            self.assertEqual(status,503)
            self.assertNotIn('ai',response)

    def test_second_request_does_not_start_another_inference(self):
        self.server.inference_lock.acquire()
        try:
            with patch('servidor.run') as model:
                self.assertEqual(self.post()[0],409)
                model.assert_not_called()
        finally: self.server.inference_lock.release()

if __name__=='__main__': unittest.main()
