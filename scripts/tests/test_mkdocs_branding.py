import re
import unittest
from pathlib import Path


ROOT = Path(__file__).resolve().parents[2]


class MkDocsBrandingTest(unittest.TestCase):
    def test_engibase_assets_cover_theme_and_viewport_variants(self):
        config_path = ROOT / ".mkdocs" / "mkdocs.yml"
        config = config_path.read_text(encoding="utf-8")
        css = (ROOT / "stylesheets/extra.css").read_text(encoding="utf-8")

        self.assertNotEqual(config_path.parent.resolve(), ROOT.resolve())
        self.assertIn("docs_dir: ..", config)

        self.assertRegex(
            config,
            re.compile(r"^\s*logo:\s+assets/engibase/engibase-logo-light\.png$", re.MULTILINE),
        )
        self.assertRegex(
            config,
            re.compile(r"^\s*favicon:\s+assets/engibase/engibase-favicon\.ico$", re.MULTILINE),
        )

        for asset in (
            "engibase-logo-light.png",
            "engibase-logo-dark.png",
            "engibase-mobile-light.png",
            "engibase-mobile-dark.png",
        ):
            self.assertIn(f'url("../assets/engibase/{asset}")', css)

        self.assertIn('[data-md-color-scheme="default"]', css)
        self.assertIn('[data-md-color-scheme="slate"]', css)
        self.assertRegex(
            css,
            re.compile(r"@media[^\{]*\([^)]*max-width", re.IGNORECASE),
        )


if __name__ == "__main__":
    unittest.main()
