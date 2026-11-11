package net.ringsofash.data;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Donnees du monde : village d'arrivee dans la Hautemarche. */
public class LandsData extends SavedData {
    public boolean hasArrival;
    public long arrival;

    public static LandsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new SavedData.Factory<>(LandsData::new, LandsData::load, null), "ringsofash_lands");
    }

    public static LandsData load(CompoundTag tag, HolderLookup.Provider provider) {
        LandsData d = new LandsData();
        d.hasArrival = tag.getBoolean("hasArrival");
        d.arrival = tag.getLong("arrival");
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        tag.putBoolean("hasArrival", hasArrival);
        tag.putLong("arrival", arrival);
        return tag;
    }

    public void setArrival(BlockPos p) { arrival = p.asLong(); hasArrival = true; setDirty(); }
}
