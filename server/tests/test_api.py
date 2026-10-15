import os, sys
os.environ["TT_NO_AUTOLOAD"] = "1"
sys.path.insert(0, os.path.join(os.path.dirname(__file__), ".."))
from fastapi.testclient import TestClient
import config, app as appmod


class T:
    def transcribe(self, p): return "um so like hello world"
class C:
    def clean(self, t): return "Hello world."
class Boom:
    def transcribe(self, p): raise RuntimeError("bad audio")


def client(tr, tmp_path, monkeypatch):
    monkeypatch.setattr(config, "VAULT", tmp_path)
    return TestClient(appmod.create_app(tr, C()))


def test_ok_and_vault(tmp_path, monkeypatch):
    c = client(T(), tmp_path, monkeypatch)
    r = c.post("/transcribe", files={"audio": ("a.m4a", b"xx")})
    assert r.status_code == 200 and r.json() == {"text": "Hello world."}
    files = list((tmp_path / "Sessions").glob("*.md"))
    assert len(files) == 1 and "Hello world." in files[0].read_text()


def test_errors(tmp_path, monkeypatch):
    c = client(Boom(), tmp_path, monkeypatch)
    assert c.post("/transcribe", files={"audio": ("a.m4a", b"")}).status_code == 400
    r = c.post("/transcribe", files={"audio": ("a.m4a", b"xx")})
    assert r.status_code == 500 and "error" in r.json()
