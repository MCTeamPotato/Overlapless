package me.kall.overlapless.data;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.overlapless.Overlapless;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class ExistingStructures extends SavedData {
    private final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> existing = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    public void save(ResourceLocation dimension, long chunk, ExistingStructure existingStructure) {
        if (
                this.existing
                .computeIfAbsent(dimension, key -> Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()))
                .computeIfAbsent(chunk, key -> ObjectSets.synchronize(new ObjectOpenHashSet<>()))
                .add(existingStructure)
        ) {
            this.setDirty();
        }
    }

    public Set<ExistingStructure> get(ResourceLocation dimension, long chunk) {
        return this.existing.getOrDefault(dimension, Long2ObjectMaps.emptyMap()).getOrDefault(chunk, Collections.emptySet());
    }

    public void remove(ResourceLocation dimension, long chunk) {
        Long2ObjectMap<Set<ExistingStructure>> chunks = this.existing.get(dimension);
        if (chunks == null) return;

        chunks.remove(chunk);

        if (chunks.isEmpty()) {
            this.existing.remove(dimension);
        }

        this.setDirty();
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        Overlapless.LOGGER.info("Overlapless: Saving existing structure data...");
        long begin = System.nanoTime();
        CompoundTag root = new CompoundTag();
        for (var dimEntry : this.existing.object2ObjectEntrySet()) {
            ResourceLocation dimension = dimEntry.getKey();
            Long2ObjectMap<Set<ExistingStructure>> chunks = dimEntry.getValue();

            CompoundTag dimensionTag = new CompoundTag();

            for (var chunkEntry : chunks.long2ObjectEntrySet()) {
                long chunkPos = chunkEntry.getLongKey();
                Set<ExistingStructure> structures = chunkEntry.getValue();

                ListTag listTag = new ListTag();
                for (ExistingStructure structure : structures) {
                    listTag.add(StringTag.valueOf(structure.toString()));
                }

                dimensionTag.put(Long.toString(chunkPos), listTag);
            }

            root.put(dimension.toString(), dimensionTag);
        }

        tag.put("ExistingStructures", root);
        double elapsed = (double) (System.nanoTime() - begin);
        Overlapless.LOGGER.info("Overlapless: Existing structure data is saved. Time cost: {} seconds", elapsed / 1_000_000_000.0D);
        return tag;
    }

    public static @NotNull ExistingStructures load(@NotNull CompoundTag tag) {
        Overlapless.LOGGER.info("Overlapless: Loading existing structure data...");
        long begin = System.nanoTime();
        ExistingStructures data = new ExistingStructures();

        CompoundTag chunks = tag.getCompound("ExistingStructures");

        for (String dimKey : chunks.getAllKeys()) {
            ResourceLocation dimension = ResourceLocation.parse(dimKey);
            CompoundTag dimensionTag = chunks.getCompound(dimKey);

            Long2ObjectMap<Set<ExistingStructure>> chunkMap = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

            for (String chunkKey : dimensionTag.getAllKeys()) {
                long chunkPos = Long.parseLong(chunkKey);
                ListTag listTag = dimensionTag.getList(chunkKey, CompoundTag.TAG_STRING);

                Set<ExistingStructure> structures = ObjectSets.synchronize(new ObjectOpenHashSet<>());
                for (int i = 0; i < listTag.size(); i++) {
                    structures.add(ExistingStructure.fromString(listTag.getString(i)));
                }

                chunkMap.put(chunkPos, structures);
            }

            data.existing.put(dimension, chunkMap);
        }

        double elapsed = (double) (System.nanoTime() - begin);
        Overlapless.LOGGER.info("Overlapless: Existing structure data is loaded. Time cost: {} seconds", elapsed / 1_000_000_000.0D);
        return data;
    }

    public static @NotNull ExistingStructures get(@NotNull ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(ExistingStructures::load, ExistingStructures::new, "OverlaplessExistingStructures");
    }
}
