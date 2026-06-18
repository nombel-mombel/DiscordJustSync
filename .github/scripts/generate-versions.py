#!/usr/bin/env python3
import requests
import json
import os

resp = requests.get("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")
piston = resp.json()

all_versions = piston["versions"]
latest_version = piston["latest"]["release"]

versions = os.listdir("versions")

output = {}

for version in versions:
    last_version = ""
    with open(f"versions/{version}/gradle.properties") as file:
        properties = file.readlines()
        for line in properties:
            if line.startswith("last_minecraft_version"):
                last_version = line.split("=")[1].strip()
    if len(last_version) == 0:
        last_version = latest_version

    for i in range(len(all_versions)):
        if all_versions[i]["id"] == last_version:
            break

    version_range = [version]
    for i in range(i, len(all_versions)):
        v = all_versions[i]
        if v["id"] == version:
            break
        if v["type"] != "release":
            continue
        version_range.append(v["id"])
    output[version] = version_range

print(json.dumps(output, indent=4))
