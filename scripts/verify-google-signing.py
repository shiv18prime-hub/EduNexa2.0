"""Verify an installable APK against its Firebase Android OAuth registration."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile


def validate_oauth(config, package, fingerprints, resources):
    if package != "com.edunexa.app":
        raise ValueError(f"Unexpected APK package: {package}")
    clients = [
        c for c in config.get("client", [])
        if c.get("client_info", {}).get("android_client_info", {}).get("package_name") == package
    ]
    if len(clients) != 1:
        raise ValueError("Expected one Firebase configuration for the APK package")
    oauth = clients[0].get("oauth_client", [])
    registered = {
        re.sub(r"[^0-9a-f]", "", c.get("android_info", {}).get("certificate_hash", "").lower())
        for c in oauth
        if c.get("client_type") == 1
        and c.get("android_info", {}).get("package_name") == package
    }
    actual = {re.sub(r"[^0-9a-f]", "", value.lower()) for value in fingerprints}
    if not actual or any(len(value) != 40 for value in actual) or not actual.issubset(registered):
        raise ValueError(
            "APK signing SHA-1 has no matching Android OAuth client in google-services.json: "
            + ", ".join(sorted(actual))
            + ". Restore the registered signing key before publishing an APK."
        )
    web_clients = {c.get("client_id", "") for c in oauth if c.get("client_type") == 3}
    if not any(
        value and (value.encode("utf-8") in resources or value.encode("utf-16le") in resources)
        for value in web_clients
    ):
        raise ValueError("APK does not contain its configured Google web client ID")


def sdk_tools():
    root = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if not root:
        raise ValueError("ANDROID_HOME or ANDROID_SDK_ROOT is required")
    directories = list((Path(root) / "build-tools").glob("*"))
    directories.sort(key=lambda p: tuple(int(n) for n in re.findall(r"\d+", p.name)), reverse=True)
    for directory in directories:
        if (directory / "apksigner").is_file() and (directory / "aapt").is_file():
            return directory
    raise ValueError("Android SDK build tools (apksigner and aapt) are required")


def run(command):
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode:
        raise ValueError(result.stderr.strip() or result.stdout.strip() or "SDK verification failed")
    return result.stdout


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("config", type=Path)
    args = parser.parse_args()
    tools = sdk_tools()
    verified = run([str(tools / "apksigner"), "verify", "--print-certs", str(args.apk)])
    fingerprints = re.findall(r"Signer #\d+ certificate SHA-1 digest:\s*([0-9a-fA-F:]+)", verified)
    badging = run([str(tools / "aapt"), "dump", "badging", str(args.apk)])
    package = re.search(r"^package: name='([^']+)'", badging, re.MULTILINE)
    if not package:
        raise ValueError("Could not read the APK package")
    with zipfile.ZipFile(args.apk) as archive:
        resources = archive.read("resources.arsc")
    validate_oauth(json.loads(args.config.read_text()), package.group(1), fingerprints, resources)
    print("Verified APK signature, Firebase package/SHA-1 registration and embedded web client ID.")
    print("APK signing SHA-1:", ", ".join(fingerprints))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        print("APK OAuth verification failed:", error, file=sys.stderr)
        sys.exit(1)
