package net.ringsofash.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

import java.util.HashSet;
import java.util.Set;

/** Tout ce que le mod retient d'un joueur : reputation, corruption, voyage, quete, bestiaire. */
public class PlayerLore implements INBTSerializable<CompoundTag> {
    public final int[] rep = new int[People.values().length];
    public int corruption;
    public int eye;
    public boolean arrived;
    public boolean hasOverworldPos, hasLandsPos;
    public long overworldPos, landsPos;
    public int quest;
    public boolean rewarded;
    public int grow;
    public final Set<String> bestiary = new HashSet<>();

    public int rep(People p) { return rep[p.ordinal()]; }

    public int general() {
        int s = 0;
        for (int v : rep) s += v;
        return Math.round(s / (float) rep.length);
    }

    public void addRep(People p, int amount) {
        rep[p.ordinal()] = Math.max(-100, Math.min(100, rep[p.ordinal()] + amount));
    }

    public void setOverworld(BlockPos p) { overworldPos = p.asLong(); hasOverworldPos = true; }
    public void setLands(BlockPos p) { landsPos = p.asLong(); hasLandsPos = true; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag t = new CompoundTag();
        t.putIntArray("rep", rep);
        t.putInt("corruption", corruption);
        t.putInt("eye", eye);
        t.putBoolean("arrived", arrived);
        t.putBoolean("hasOw", hasOverworldPos);
        t.putBoolean("hasLands", hasLandsPos);
        t.putLong("ow", overworldPos);
        t.putLong("lands", landsPos);
        t.putInt("quest", quest);
        t.putBoolean("rewarded", rewarded);
        ListTag l = new ListTag();
        for (String s : bestiary) l.add(StringTag.valueOf(s));
        t.put("bestiary", l);
        return t;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag t) {
        int[] r = t.getIntArray("rep");
        for (int i = 0; i < rep.length && i < r.length; i++) rep[i] = r[i];
        corruption = t.getInt("corruption");
        eye = t.getInt("eye");
        arrived = t.getBoolean("arrived");
        hasOverworldPos = t.getBoolean("hasOw");
        hasLandsPos = t.getBoolean("hasLands");
        overworldPos = t.getLong("ow");
        landsPos = t.getLong("lands");
        quest = t.getInt("quest");
        rewarded = t.getBoolean("rewarded");
        bestiary.clear();
        ListTag l = t.getList("bestiary", Tag.TAG_STRING);
        for (int i = 0; i < l.size(); i++) bestiary.add(l.getString(i));
    }
}
