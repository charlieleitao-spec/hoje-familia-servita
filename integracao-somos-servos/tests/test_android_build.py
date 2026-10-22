import re
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location("prepare_blog", ROOT / "scripts/preparar-blog-android.py")
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class AndroidPreparation(unittest.TestCase):
    def fixture(self, root):
        (root / "somos-servos.html").write_text(re.sub(r"const API_URL = '[^']*';", "const API_URL = '';", (ROOT / "somos-servos.html").read_text()))
        manifest = root / "android/app/src/main/AndroidManifest.xml"
        manifest.parent.mkdir(parents=True)
        manifest.write_text('<manifest/>')

    def test_injects_endpoint_without_changing_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            original = (root / "somos-servos.html").read_text()
            url = "https://script.google.com/macros/s/TEST/exec"
            result = module.preparar(root, url)
            self.assertIn("const API_URL = '" + url + "';", result.read_text())
            self.assertEqual((root / "somos-servos.html").read_text(), original)

    def test_rejects_missing_or_untrusted_endpoint(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            for url in ["", "https://evil.test/exec", "https://script.google.com/macros/s/X/dev"]:
                with self.assertRaises(ValueError):
                    module.preparar(root, url)
            self.assertFalse((root / "android/app/src/main/assets/somos-servos.html").exists())

    def test_release_uses_endpoint_from_canonical_html(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            (root / "somos-servos.html").write_text((ROOT / "somos-servos.html").read_text())
            result = module.preparar(root)
            self.assertEqual(result.read_text(), (ROOT / "somos-servos.html").read_text())

    def test_native_route_and_build_copy_are_present(self):
        java = (ROOT / "MainActivity-revisado.java").read_text()
        self.assertIn('OFFLINE = HOME + "somos-servos.html"', java)
        self.assertIn('getAssets().open("somos-servos.html")', java)
        self.assertIn('shouldInterceptRequest', java)
        self.assertNotIn('setAllowUniversalAccessFromFileURLs(true)', java)
        workflow = (ROOT / ".github/workflows/build-android.yml").read_text()
        self.assertLess(workflow.index('unzip -q android-source.zip'), workflow.index('python3 scripts/preparar-blog-android.py'))
        self.assertIn('href="./somos-servos.html"', (ROOT / "index.html").read_text())

    def test_debug_allows_empty_but_never_untrusted_endpoint(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            self.fixture(root)
            result = module.preparar(root, allow_unconfigured=True)
            self.assertIn("const API_URL = '';", result.read_text())
            for url in ["https://evil.test/exec", "https://script.google.com/macros/s/X/dev"]:
                with self.assertRaises(ValueError):
                    module.preparar(root, url, allow_unconfigured=True)


if __name__ == "__main__":
    unittest.main()
