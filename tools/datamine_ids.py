#!/usr/bin/env python3
"""Read 16 Nov 2026 Clash game asset CSVs and extract raw GlobalIDs.

This is research output: no inferred IDs.  Run from GitHub Actions to access
Supercell's compressed static-data assets from the published fingerprint.
"""
import csv
import io
import json
import lzma
from pathlib import Path
import requests

FINGERPRINT = "0f8af5ebc59dc7b88f884643c1fb8b57928113d6"
BASE_URL = "https://game-assets.clashofclans.com/" + FINGERPRINT
DATASETS = [
    "logic/buildings.csv", "logic/characters.csv", "logic/spells.csv",
    "logic/guardians.csv", "logic/seasonal_defense.csv",
    "logic/seasonal_defense_modules.csv", "logic/seasonal_defense_archetypes.csv",
    "logic/character_items.csv",
    "logic/heroes.csv", "logic/pets.csv", "logic/traps.csv",
    "logic/upgrade_data.csv", "logic/upgrade_tasks.csv",
]

def unpack(data):
    if data[:4] == b"Sig:":
        data = data[68:]
    if data[:4] == b"SCLZ":
        import lzham
        size = int.from_bytes(data[5:9], "little")
        return lzham.decompress(data[9:], size, {"dict_size_log2": data[4]})
    if data[:4] == bytes.fromhex("28b52ffd"):
        import zstandard
        return zstandard.ZstdDecompressor().decompress(data)
    if data[:1] == bytes([0x5D]):
        return lzma.LZMADecompressor().decompress(data[:9] + bytes(4) + data[9:])
    if b"," in data[:400]:
        return data
    raise ValueError("Unrecognised file header " + data[:24].hex())

def rows_from(dataset):
    resp=requests.get(BASE_URL+"/"+dataset,timeout=45)
    resp.raise_for_status()
    raw=unpack(resp.content).decode("utf-8-sig")
    records=list(csv.reader(io.StringIO(raw)))
    if len(records)<2:
        raise ValueError("No records in "+dataset)
    fields=records[0]
    # The second row declares source field types; entity entries start at row three.
    return fields,records[2:]

def run():
    results={}
    for dataset in DATASETS:
        try:
            fields,rows=rows_from(dataset)
            entries=[]
            seen=set()
            for row in rows:
                if not row or not row[0].strip(): continue
                values={k:row[i].strip() if i<len(row) else "" for i,k in enumerate(fields)}
                n=values.get("Name","").strip()
                rawid=values.get("GlobalID","").strip()
                if not n: continue
                if rawid and not rawid.isdigit(): continue
                if not rawid and ("buildings" in dataset or "characters" in dataset or "spells" in dataset):
                    # Actual named entity might have intentionally no source ID.
                    continue
                if (n,rawid) in seen:continue
                seen.add((n,rawid))
                entry={"name":n}
                if rawid: entry["id"]=int(rawid)
                for key in ("TID","BuildingLevel","LaboratoryLevel","TownHallLevel","UpgradeTimeH","UpgradeTimeD","UpgradeTimeM","VillageType","HousingSpace","ProductionBuilding","SpellForgeLevel","SeasonalDefense"):
                    v=values.get(key)
                    if v:entry[key]=v
                entries.append(entry)
            results[dataset]={"count":len(entries),"fields":fields[:15],"items":entries}
            print(dataset,":",len(entries),"entities; new:",[e for e in entries if (e.get("id",0)>=1000105 and e.get("id",0)<=1000125) or ("Squad" in e["name"] and not "builder" in e["name"].lower())][:30],flush=True)
        except Exception as ex:
            print("ERROR",dataset,type(ex).__name__,str(ex)[:400],flush=True)
            results[dataset]={"error":str(ex)}
    out=Path("docs/datamine/game_ids_2026-11-16.json")
    out.parent.mkdir(parents=True,exist_ok=True)
    out.write_text(json.dumps({"fingerprint":FINGERPRINT,"datasets":results},ensure_ascii=False,indent=2)+"\n",encoding="utf8")
    if any(results[d].get("error") for d in ("logic/buildings.csv","logic/characters.csv","logic/spells.csv")):
        raise RuntimeError("Core asset extraction failed; inspect above errors")

if __name__=="__main__":
    run()
