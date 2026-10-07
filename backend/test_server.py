import copy
import json
import threading
import unittest
from dataclasses import replace
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.error import HTTPError
from urllib.request import Request, urlopen
from unittest.mock import patch

from server import CATALOG, Config, Engine, Gateway, ProviderError, ValidationError, validate_request, validate_response

TOPIC = {"topic": "人为什么一定要努力？", "philosopherIds": ["zhuangzi", "kant"], "messages": []}
RESPONSE = {"topic": "不可信话题", "messages": [{"id": "turn-1", "philosopherId": "zhuangzi", "displayName": "错误名称", "content": "思想推演：可以检视努力依据的尺度。", "responseTo": None, "type": "argument"}]}


class FakeProvider(BaseHTTPRequestHandler):
    seen = []
    reply = RESPONSE
    redirect = None
    def log_message(self, *_args):
        pass
    def do_POST(self):
        payload = json.loads(self.rfile.read(int(self.headers["Content-Length"])))
        type(self).seen.append((self.path, payload, self.headers.get("Authorization")))
        if type(self).redirect:
            self.send_response(302)
            self.send_header("Location", type(self).redirect)
            self.end_headers()
            return
        body = json.dumps({"choices": [{"finish_reason": "stop", "message": {"content": json.dumps(type(self).reply)}}]}).encode()
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)
    def do_GET(self):
        type(self).seen.append((self.path, None, self.headers.get("Authorization")))
        self.send_response(200)
        self.end_headers()


class GatewayTest(unittest.TestCase):
    def setUp(self):
        FakeProvider.seen = []
        FakeProvider.reply = copy.deepcopy(RESPONSE)
        FakeProvider.redirect = None
        self.provider = ThreadingHTTPServer(("127.0.0.1", 0), FakeProvider)
        threading.Thread(target=self.provider.serve_forever, daemon=True).start()
        self.gateway = Gateway(("127.0.0.1", 0), Engine(Config(f"http://127.0.0.1:{self.provider.server_port}/v1", "test-model", "test-only-token")))
        threading.Thread(target=self.gateway.serve_forever, daemon=True).start()
        self.base = f"http://127.0.0.1:{self.gateway.server_port}"

    def tearDown(self):
        self.gateway.shutdown()
        self.gateway.server_close()
        self.provider.shutdown()
        self.provider.server_close()

    def post(self, data):
        return urlopen(Request(self.base + "/round-table", json.dumps(data).encode(), {"Content-Type": "application/json"}), timeout=5)

    def test_http_roundtrip_context_and_canonical_names(self):
        with urlopen(self.base + "/health") as response:
            self.assertTrue(json.load(response)["configured"])
        self.assertEqual([], FakeProvider.seen)  # Health check never invokes the provider.
        with self.post(TOPIC) as response:
            result = json.load(response)
        self.assertEqual(TOPIC["topic"], result["topic"])
        self.assertEqual("庄子", result["messages"][0]["displayName"])
        path, payload, authorization = FakeProvider.seen[0]
        self.assertEqual("/v1/chat/completions", path)
        self.assertEqual("test-model", payload["model"])
        self.assertEqual({"type": "json_object"}, payload["response_format"])
        self.assertEqual("Bearer test-only-token", authorization)
        follow = dict(TOPIC, messages=result["messages"] + [{"id": "user-1", "philosopherId": "user", "displayName": "您", "content": "庄子怎么看康德？", "type": "user", "responseTo": None}])
        FakeProvider.reply["messages"][0].update(id="turn-2", responseTo="user-1")
        with self.post(follow) as response:
            self.assertEqual("user-1", json.load(response)["messages"][0]["responseTo"])
        sent = json.loads(FakeProvider.seen[1][1]["messages"][1]["content"])
        self.assertEqual(2, len(sent["messages"]))
        self.assertEqual("庄子", sent["participants"][0]["name"])

    def test_neutral_summary_round_trip_and_sources(self):
        prior = copy.deepcopy(RESPONSE["messages"])
        request = dict(TOPIC, messages=prior, mode="summary", speakerIds=["zhuangzi"])
        FakeProvider.reply = dict(topic="ignored", messages=[], summary=dict(disagreement=[dict(text="如何衡量努力？", messageIds=["turn-1"])], commonGround=[]))
        with self.post(request) as response:
            result = json.load(response)
        self.assertEqual([], result["messages"])
        self.assertEqual("turn-1", result["summary"]["disagreement"][0]["messageIds"][0])

    def test_discussion_and_summary_share_one_provider_call(self):
        old = copy.deepcopy(RESPONSE["messages"])
        request = dict(TOPIC, messages=old, includeSummary=True)
        FakeProvider.reply = copy.deepcopy(RESPONSE)
        FakeProvider.reply["messages"][0].update(id="turn-2", responseTo="turn-1")
        FakeProvider.reply["summary"] = dict(disagreement=[dict(text="外部标准与自己的尺度之间的分歧。", messageIds=["turn-1", "turn-2"])], commonGround=[])
        with self.post(request) as response:
            result = json.load(response)
        self.assertEqual(1, len(FakeProvider.seen))
        self.assertEqual("turn-2", result["messages"][0]["id"])
        self.assertEqual(["turn-1", "turn-2"], result["summary"]["disagreement"][0]["messageIds"])

    def test_summary_rejects_invented_sources(self):
        request = validate_request(dict(TOPIC, messages=RESPONSE["messages"], mode="summary"))
        with self.assertRaises(ProviderError):
            validate_response(dict(topic="x", messages=[], summary=dict(disagreement=[dict(text="没有依据", messageIds=["fake"])], commonGround=[])), request)

    def test_excluded_speaker_is_rejected(self):
        request = validate_request(dict(TOPIC, speakerIds=["kant"]))
        with self.assertRaises(ProviderError):
            validate_response(RESPONSE, request)

    def test_invalid_input_never_calls_provider(self):
        with self.assertRaises(HTTPError) as failed:
            self.post(dict(TOPIC, philosopherIds=["fake"]))
        self.assertEqual(400, failed.exception.code)
        self.assertEqual([], FakeProvider.seen)

    def test_bad_model_output_is_not_presented_as_success(self):
        FakeProvider.reply["messages"][0]["philosopherId"] = "fake"
        with self.assertRaises(HTTPError) as failed:
            self.post(TOPIC)
        self.assertEqual(502, failed.exception.code)
        body = failed.exception.read().decode()
        self.assertNotIn("test-only-token", body)
        self.assertNotIn("Bearer", body)

    def test_unconfigured_service(self):
        self.gateway.engine = Engine(Config())
        with urlopen(self.base + "/health") as response:
            self.assertFalse(json.load(response)["configured"])
        with self.assertRaises(HTTPError) as failed:
            self.post(TOPIC)
        self.assertEqual(503, failed.exception.code)

    def test_upstream_redirect_does_not_forward_credentials(self):
        FakeProvider.redirect = f"http://127.0.0.1:{self.provider.server_port}/unexpected"
        with self.assertRaises(HTTPError) as failed:
            self.post(TOPIC)
        self.assertEqual(502, failed.exception.code)
        self.assertEqual(1, len(FakeProvider.seen))

    def test_qwen_non_thinking_parameter(self):
        self.gateway.engine = Engine(replace(self.gateway.engine.config, model="qwen3.8-max", enable_thinking=False))
        with self.post(TOPIC) as response:
            self.assertEqual(200, response.status)
        payload = FakeProvider.seen[0][1]
        self.assertEqual("qwen3.8-max", payload["model"])
        self.assertIs(False, payload["enable_thinking"])
        self.assertFalse(payload["stream"])


    def test_request_size_and_rate_limit(self):
        with self.assertRaises(HTTPError) as failed:
            self.post(dict(TOPIC, topic="a" * 140000))
        self.assertEqual(413, failed.exception.code)
        for _ in range(8):
            self.assertTrue(self.gateway.allow("test-ip"))
        self.assertFalse(self.gateway.allow("test-ip"))


class ValidationTest(unittest.TestCase):
    def test_duplicates_context_and_unknown_reply(self):
        with self.assertRaises(ValidationError):
            validate_request(dict(TOPIC, philosopherIds=["kant", "kant"]))
        request = validate_request(TOPIC)
        bad = copy.deepcopy(RESPONSE)
        bad["messages"][0]["responseTo"] = "missing"
        with self.assertRaises(ProviderError):
            validate_response(bad, request)

    def test_no_credential_leak_in_config_repr(self):
        self.assertNotIn("secret-value", repr(Config("https://example.com/v1", "model", "secret-value")))

    def test_upstream_configuration(self):
        with patch.dict("os.environ", {"AI_UPSTREAM_BASE_URL": "http://example.com/v1"}):
            with self.assertRaises(ValueError):
                Config.from_env()


if __name__ == "__main__":
    unittest.main()
