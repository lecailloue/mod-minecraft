#!/usr/bin/env python3
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import genlib as G
G.clean()
import content, worldgen, gear, mobs
content.build()
gear.build()
mobs.build()
worldgen.build()
try:
    import models
except ImportError:
    models = None
if models: models.build()
G.finish()
n = sum(len(f) for _, _, f in os.walk(G.RES))
print(f'{len(G.BLOCKS)} blocs, {len(G.ITEMS)} objets, {len(G.LANG)} traductions, {n} fichiers')
