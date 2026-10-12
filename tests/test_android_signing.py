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
    def test_classic_sdk_signer_output(self):
        text = f"Signer #1 certificate SHA-1 digest: {REGISTERED}\n"
        self.assertEqual(signing.signer_fingerprints(text), [REGISTERED])

    def test_sdk_range_signer_output(self):
        text = f"Signer (minSdkVersion=24, maxSdkVersion=2147483647) certificate SHA-1 digest: {REGISTERED}\n"
        self.assertEqual(signing.signer_fingerprints(text), [REGISTERED])

    def test_public_key_and_source_stamp_are_not_app_signers(self):
        text = (f"Signer #1 public key SHA-1 digest: {REGISTERED}\n"
                f"Source Stamp Signer certificate SHA-1 digest: {REGISTERED}\n")
        self.assertEqual(signing.signer_fingerprints(text), [])

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
