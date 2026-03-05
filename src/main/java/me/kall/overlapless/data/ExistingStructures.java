package me.kall.overlapless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class ExistingStructures {
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> EXISTING_STRUCTURES = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    private static void record(ResourceLocation dimension, long chunk, ExistingStructure existingStructure) {
        EXISTING_STRUCTURES
                .computeIfAbsent(dimension, key -> Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()))
                .computeIfAbsent(chunk, key -> ObjectSets.synchronize(new ObjectOpenHashSet<>()))
                .add(existingStructure);
    }

    private static @Nullable Set<ExistingStructure> get(ResourceLocation dimension, long chunk) {
        return EXISTING_STRUCTURES.getOrDefault(dimension, Long2ObjectMaps.emptyMap()).get(chunk);
    }

    public static void shutdown() {
        if (EXISTING_STRUCTURES.isEmpty()) return;
        EXISTING_STRUCTURES.clear();
    }

    private static boolean overlap(int minY1, int maxY1, int minY2, int maxY2) {
        return Math.max(minY1, minY2) <= Math.min(maxY1, maxY2);
    }

    public static @Nullable Set<ExistingStructure> getInChunk(ResourceLocation dimension, long chunk) {
        return EXISTING_STRUCTURES.getOrDefault(dimension, Long2ObjectMaps.emptyMap()).get(chunk);
    }

    public static @Nullable ExistingStructure getAnyExisting(@NotNull StructureStart pendingStructure, ServerLevel level) {
        if (Config.getUnskippableStructures().contains(Overlapless.getName(pendingStructure.getStructure()))) return null;

        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        ResourceLocation dimension = level.dimension().location();

        int pendingMinY = pendingBox.minY();
        int pendingMaxY = pendingBox.maxY();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());
        ExistingStructure existing = null;

        checking: {
            synchronized (EXISTING_STRUCTURES) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        long existingChunk = ChunkPos.asLong(chunkX, chunkZ);
                        Set<ExistingStructure> structures = get(dimension, existingChunk);
                        if (structures == null) continue;
                        for (ExistingStructure existingStructure : structures) {
                            if (overlap(existingStructure.minY(), existingStructure.maxY(), pendingMinY, pendingMaxY)) {
                                existing = existingStructure;
                                break checking;
                            }
                        }
                    }
                }
            }
        }

        return existing;
    }

    public static void afterStructureGeneration(@NotNull StructureStart pendingStructure, @NotNull ServerLevel level) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());

        ResourceLocation dimension = level.dimension().location();

        ExistingStructure existingStructure = new ExistingStructure(pendingBox.minY(), pendingBox.maxY(), Overlapless.getName(pendingStructure.getStructure()).toString());

        synchronized (EXISTING_STRUCTURES) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    record(dimension, ChunkPos.asLong(chunkX, chunkZ), existingStructure);
                }
            }
        }
    }
}
