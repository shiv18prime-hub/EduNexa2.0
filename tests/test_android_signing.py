import importlib.util
from pathlib import Path
import unittest


SPEC = importlib.util.spec_from_file_location(
    "signing", Path(__file__).resolve().parents[1] / "scripts" / "verify-google-signing.py"
)
signing = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(signing)
REGISTERED = "a3870e8af31dbe0fa9f02eb55a66bfc6bb2a38ea"
PR_SIGNER = "0649681f55d72dec5df7eeac0a35cff254ac009a"
WEB = "123-example.apps.googleusercontent.com"
CONFIG = {"client": [{
    "client_info": {"android_client_info": {"package_name": "com.edunexa.app"}},
    "oauth_client": [
        {"client_type": 1, "android_info": {
            "package_name": "com.edunexa.app", "certificate_hash": REGISTERED}},
        {"client_type": 3, "client_id": WEB},
    ],
}]}


class AndroidSigningTest(unittest.TestCase):
    def certificate(self):
        return (Path(__file__).parent / "fixtures" / "debug_certificate.pem").read_text()

    def test_real_apk_certificate_fingerprint(self):
        self.assertEqual(signing.signer_fingerprints(self.certificate()), [REGISTERED])

    def test_localized_captions_do_not_change_certificate_identity(self):
        text = self.certificate().replace("Signer #1:", "Unterzeichner 1:").replace("Certificate #1:", "Zertifikat 1:")
        self.assertEqual(signing.signer_fingerprints(text), [REGISTERED])

    def test_missing_or_ambiguous_debug_certificate_is_rejected(self):
        for text in ["", self.certificate() * 2]:
            with self.assertRaises(ValueError):
                signing.signer_fingerprints(text)

    def test_registered_push_is_accepted_and_unregistered_pr_is_rejected(self):
        signing.validate_oauth(CONFIG, "com.edunexa.app", [REGISTERED], WEB.encode())
        with self.assertRaisesRegex(ValueError, "no matching Android OAuth client"):
            signing.validate_oauth(CONFIG, "com.edunexa.app", [PR_SIGNER], WEB.encode())

    def test_missing_web_client_and_wrong_package_are_rejected(self):
        with self.assertRaises(ValueError):
            signing.validate_oauth(CONFIG, "com.edunexa.app", [REGISTERED], b"")
        with self.assertRaises(ValueError):
            signing.validate_oauth(CONFIG, "com.other.app", [REGISTERED], WEB.encode())


if __name__ == "__main__":
    unittest.main()
