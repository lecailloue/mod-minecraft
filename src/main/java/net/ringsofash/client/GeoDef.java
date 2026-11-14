package net.ringsofash.client;

import com.google.gson.Gson;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/** Geometrie d'un modele, lue depuis assets/ringsofash/geo/<nom>.json (genere par tools/geo.py). */
public final class GeoDef {
    public static class Box { public float[] o; public float[] s; public int[] uv; public float inflate; public boolean mirror; }
    public static class Part { public String name; public String parent; public float[] pivot; public float[] rot; public Box[] boxes; }
    public int tw, th;
    /** hauteur du modele en pixels (sol a y=24, arme exclue) : sert a calibrer la taille a l'ecran */
    public float height;
    /** etendue verticale (px) autour du point de prise : armes rendues comme objets */
    public float ymin, ymax;
    public Part[] parts;

    private static final Map<String, GeoDef> CACHE = new HashMap<>();

    public static synchronized GeoDef get(String name) {
        return CACHE.computeIfAbsent(name, n -> {
            try (InputStream in = GeoDef.class.getResourceAsStream("/assets/ringsofash/geo/" + n + ".json")) {
                if (in == null) throw new IllegalStateException("Modele introuvable: " + n);
                return new Gson().fromJson(new InputStreamReader(in, StandardCharsets.UTF_8), GeoDef.class);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static LayerDefinition layer(String name) {
        GeoDef def = get(name);
        MeshDefinition mesh = new MeshDefinition();
        Map<String, PartDefinition> made = new HashMap<>();
        for (Part p : def.parts) {
            PartDefinition parent = p.parent == null ? mesh.getRoot() : made.get(p.parent);
            CubeListBuilder b = CubeListBuilder.create();
            for (Box bx : p.boxes) {
                b.texOffs(bx.uv[0], bx.uv[1]).mirror(bx.mirror).addBox(bx.o[0], bx.o[1], bx.o[2], bx.s[0], bx.s[1], bx.s[2], new CubeDeformation(bx.inflate));
            }
            made.put(p.name, parent.addOrReplaceChild(p.name, b, PartPose.offsetAndRotation(p.pivot[0], p.pivot[1], p.pivot[2], p.rot[0], p.rot[1], p.rot[2])));
        }
        return LayerDefinition.create(mesh, def.tw, def.th);
    }
}
