"""Mini-outil de modelisation : boites -> JSON de geometrie (lu par le mod) + texture + apercu isometrique."""
import random, math, os
from PIL import Image, ImageDraw
from tex import rgb, shade, mix, vnoise
from genlib import A, wjson, save

class Box:
    def __init__(self, o, s, color, inflate=0.0, art=None, pal=None, mirror=False, noise=0.10, faces=None):
        self.o, self.s, self.color, self.inflate = o, s, rgb(color) if isinstance(color, str) else color, inflate
        self.art, self.pal, self.mirror, self.noise, self.faces = art or {}, pal or {}, mirror, noise, faces or {}
        self.uv = None

class Part:
    def __init__(self, name, parent, pivot, rot=(0, 0, 0)):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.boxes = []
    def box(self, o, s, color, **kw):
        self.boxes.append(Box(o, s, color, **kw)); return self

class Model:
    def __init__(self, name, tw=64, th=64, seed=1):
        self.name, self.tw, self.th, self.seed = name, tw, th, seed
        self.parts = []
    def part(self, name, pivot, parent=None, rot=(0, 0, 0)):
        p = Part(name, parent, pivot, rot); self.parts.append(p); return p
    def get(self, name):
        return next(p for p in self.parts if p.name == name)

    # ---------- UV
    def pack(self):
        boxes = [b for p in self.parts for b in p.boxes]
        order = sorted(boxes, key=lambda b: -(b.s[1] + b.s[2]))
        while True:
            x = y = rowh = 0; ok = True
            for b in order:
                w, h, d = b.s
                bw, bh = 2 * (w + d), d + h
                if bw > self.tw: ok = False; break
                if x + bw > self.tw:
                    x = 0; y += rowh; rowh = 0
                if y + bh > self.th: ok = False; break
                b.uv = (x, y); x += bw; rowh = max(rowh, bh)
            if ok: return
            if self.th < self.tw: self.th *= 2
            else: self.tw *= 2

    def face_rects(self, b):
        u, v = b.uv; w, h, d = b.s
        return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
                'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + d + w + d, v + d, w, h)}

    def texture(self):
        rnd = random.Random(self.seed)
        im = Image.new('RGBA', (self.tw, self.th), (0, 0, 0, 0))
        n = vnoise(max(self.tw, self.th), 4, rnd)
        for p in self.parts:
            for b in p.boxes:
                for face, (x0, y0, fw, fh) in self.face_rects(b).items():
                    base = b.faces.get(face, b.color)
                    if isinstance(base, str): base = rgb(base)
                    lum = {'top': 1.12, 'bottom': 0.72, 'front': 1.0, 'back': 0.9, 'left': 0.94, 'right': 0.94}[face]
                    for yy in range(fh):
                        for xx in range(fw):
                            f = lum * (1 + (n[(y0 + yy) % len(n)][(x0 + xx) % len(n)] - 0.5) * 2 * b.noise + (rnd.random() - 0.5) * b.noise)
                            if face not in ('top', 'bottom') and fh > 3: f *= 1.06 - 0.14 * yy / fh
                            if 0 <= x0 + xx < self.tw and 0 <= y0 + yy < self.th:
                                im.putpixel((x0 + xx, y0 + yy), shade(base, f) + (255,))
                    art = b.art.get(face)
                    if art:
                        for yy, row in enumerate(art):
                            for xx, ch in enumerate(row):
                                if ch in '. ' or yy >= fh or xx >= fw: continue
                                c = b.pal[ch]; c = rgb(c) if isinstance(c, str) else c
                                im.putpixel((x0 + xx, y0 + yy), c + (255,))
        return im

    # ---------- export
    def export(self, tex_path=None):
        self.pack()
        parts = []
        byname = {p.name: p for p in self.parts}
        for p in self.parts:
            px, py, pz = p.pivot
            if p.parent:
                qx, qy, qz = byname[p.parent].pivot
                off = (px - qx, -(py - qy), pz - qz)
            else:
                off = (px, 24 - py, pz)
            boxes = []
            for b in p.boxes:
                (x0, y0, z0), (w, h, d) = b.o, b.s
                boxes.append({'o': [x0 - px, py - y0 - h, z0 - pz], 's': [w, h, d], 'uv': list(b.uv), 'inflate': b.inflate, 'mirror': b.mirror})
            parts.append({'name': p.name, 'parent': p.parent, 'pivot': list(off), 'rot': [math.radians(r) for r in p.rot], 'boxes': boxes})
        wjson(f'{A}/geo/{self.name}.json', {'tw': self.tw, 'th': self.th, 'parts': parts})
        save(self.texture(), tex_path or f'{A}/textures/entity/{self.name}.png')
        return self

    def retexture(self, name, recolor, seed=None, art=None):
        """variante de texture (meme geometrie) : recolor = fonction couleur -> couleur"""
        old = [(b, b.color, dict(b.faces), b.art) for p in self.parts for b in p.boxes]
        s = self.seed
        for b, c, f, a in old:
            b.color = recolor(c); b.faces = {k: recolor(rgb(v) if isinstance(v, str) else v) for k, v in f.items()}
        if seed: self.seed = seed
        save(self.texture(), f'{A}/textures/entity/{name}.png')
        for b, c, f, a in old: b.color, b.faces = c, f
        self.seed = s

    def preview(self, path, scale=6):
        """apercu isometrique grossier (sans rotations)"""
        faces = []
        for p in self.parts:
            for b in p.boxes:
                (x, y, z), (w, h, d) = b.o, b.s
                c = b.color
                # vue depuis l'avant-droite : on voit avant (-z), cote (-x) et dessus
                def pr(X, Y, Z): return ((X + Z) * 0.8, -Y + (X - Z) * 0.4)
                top = [pr(x, y + h, z), pr(x + w, y + h, z), pr(x + w, y + h, z + d), pr(x, y + h, z + d)]
                front = [pr(x, y, z), pr(x + w, y, z), pr(x + w, y + h, z), pr(x, y + h, z)]
                side = [pr(x + w, y, z), pr(x + w, y, z + d), pr(x + w, y + h, z + d), pr(x + w, y + h, z)]
                key = (x + w / 2) - (z + d / 2) + (y + h / 2) * 0.02
                faces.append((key, [(top, shade(c, 1.15)), (front, shade(c, 1.0)), (side, shade(c, 0.78))]))
        faces.sort(key=lambda f: f[0])
        pts = [pt for _, fs in faces for poly, _ in fs for pt in poly]
        minx, maxx = min(p[0] for p in pts), max(p[0] for p in pts)
        miny, maxy = min(p[1] for p in pts), max(p[1] for p in pts)
        W, H = int((maxx - minx) * scale) + 20, int((maxy - miny) * scale) + 20
        im = Image.new('RGB', (W, H), (60, 70, 90)); dr = ImageDraw.Draw(im)
        for _, fs in faces:
            for poly, c in fs:
                dr.polygon([((px - minx) * scale + 10, (py - miny) * scale + 10) for px, py in poly], fill=c, outline=(20, 20, 20))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
