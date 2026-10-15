"""Verify an installable APK against its Firebase Android OAuth registration."""

import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import struct
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
        if (directory / "apksigner").is_file():
            return directory
    raise ValueError("Android SDK apksigner is required")


def run(command):
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode:
        raise ValueError(result.stderr.strip() or result.stdout.strip() or "SDK verification failed")
    return result.stdout


def signer_fingerprints(output):
    # The debug APK has one self-signed certificate. Hash the public DER bytes
    # instead of relying on localized or SDK-specific fingerprint captions.
    certificates = re.findall(
        r"-----BEGIN CERTIFICATE-----\s*([A-Za-z0-9+/=\s]+?)\s*-----END CERTIFICATE-----",
        output,
    )
    if len(certificates) != 1:
        raise ValueError("Expected one debug APK signing certificate")
    der = base64.b64decode(re.sub(r"\s+", "", certificates[0]), validate=True)
    return [hashlib.sha1(der).hexdigest()]


def manifest_attributes(data):
    # Android binary XML layout: androidfw/ResourceTypes.h.
    u16 = lambda at: struct.unpack_from("<H", data, at)[0]
    u32 = lambda at: struct.unpack_from("<I", data, at)[0]
    if len(data) < 8 or u16(0) != 3 or u32(4) != len(data):
        raise ValueError("Invalid compiled Android manifest")
    strings = []
    offset = u16(2)
    while offset + 8 <= len(data):
        kind, header, size = struct.unpack_from("<HHI", data, offset)
        if header < 8 or size < header or offset + size > len(data):
            raise ValueError("Invalid Android manifest chunk")
        if kind == 1:
            count, _, flags, start, _ = struct.unpack_from("<IIIII", data, offset + 8)
            for i in range(count):
                position = offset + start + u32(offset + header + i * 4)
                if flags & 0x100:
                    for _ in range(2):
                        length = data[position];position += 1
                        if length & 0x80:
                            length = ((length & 0x7F) << 8) | data[position];position += 1
                    strings.append(data[position:position + length].decode("utf-8"))
                else:
                    length = u16(position);position += 2
                    if length & 0x8000:
                        length = ((length & 0x7FFF) << 16) | u16(position);position += 2
                    strings.append(data[position:position + length * 2].decode("utf-16le"))
        elif kind == 0x102 and strings[u32(offset + header + 4)] == "manifest":
            start, stride, count = struct.unpack_from("<HHH", data, offset + header + 8)
            if stride < 20 or header + start + count * stride > size:
                raise ValueError("Invalid manifest attributes")
            result = {}
            for i in range(count):
                at = offset + header + start + i * stride
                name, raw = u32(at + 4), u32(at + 8)
                value = strings[raw] if raw != 0xFFFFFFFF else (
                    strings[u32(at + 16)] if data[at + 15] == 3 else u32(at + 16))
                result[strings[name]] = value
            return result
        offset += size
    raise ValueError("Compiled manifest has no root element")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("config", type=Path)
    args = parser.parse_args()
    tools = sdk_tools()
    run([str(tools / "apksigner"), "verify", str(args.apk)])
    certificate = run(["keytool", "-printcert", "-rfc", "-jarfile", str(args.apk)])
    fingerprints = signer_fingerprints(certificate)
    with zipfile.ZipFile(args.apk) as archive:
        resources = archive.read("resources.arsc")
        manifest = manifest_attributes(archive.read("AndroidManifest.xml"))
    validate_oauth(json.loads(args.config.read_text()), manifest.get("package"), fingerprints, resources)
    print("Verified APK signature, Firebase package/SHA-1 registration and embedded web client ID.")
    print("APK signing SHA-1:", ", ".join(fingerprints))
    print("APK version:", manifest.get("versionName"), manifest.get("versionCode"))


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        print("APK OAuth verification failed:", error, file=sys.stderr)
        sys.exit(1)
