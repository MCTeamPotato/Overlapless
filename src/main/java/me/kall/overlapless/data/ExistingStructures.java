package me.kall.overlapless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ExistingStructures {
    public static final ReadWriteLock LOCK = new ReentrantReadWriteLock();
    public static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> EXISTING_STRUCTURES = new Object2ObjectOpenHashMap<>();

    private static void record(ResourceLocation dimension, long chunk, ExistingStructure existingStructure) {
        EXISTING_STRUCTURES
                .computeIfAbsent(dimension, key -> new Long2ObjectOpenHashMap<>())
                .computeIfAbsent(chunk, key -> new ObjectOpenHashSet<>())
                .add(existingStructure);
    }

    @SubscribeEvent
    public static void shutdown(LevelEvent.Save event) {
        LOCK.writeLock().lock();
        try {
            if (EXISTING_STRUCTURES.isEmpty()) return;
            EXISTING_STRUCTURES.clear();
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private static boolean overlap(int minY1, int maxY1, int minY2, int maxY2) {
        return Math.max(minY1, minY2) <= Math.min(maxY1, maxY2);
    }

    public static @Nullable Set<ExistingStructure> getInChunk(ResourceLocation dimension, long chunk) {
        return EXISTING_STRUCTURES.getOrDefault(dimension, Long2ObjectMaps.emptyMap()).get(chunk);
    }

    public static @Nullable ExistingStructure getAnyExisting(int minX, int maxX, int minZ, int maxZ, int minY, int maxY, ResourceLocation id, ResourceLocation dimension) {
        if (Config.getUnskippableStructures().contains(id)) return null;

        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);
        ExistingStructure existing = null;

        Long2ObjectMap<Set<ExistingStructure>> chunks = EXISTING_STRUCTURES.get(dimension);
        if (chunks == null) return null;

        checking: {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    long existingChunk = ChunkPos.asLong(chunkX, chunkZ);
                    Set<ExistingStructure> structures = chunks.get(existingChunk);
                    if (structures == null) continue;
                    for (ExistingStructure existingStructure : structures) {
                        if (overlap(existingStructure.minY(), existingStructure.maxY(), minY, maxY)) {
                            existing = existingStructure;
                            break checking;
                        }
                    }
                }
            }
        }

        return existing;
    }

    public static void afterStructureGeneration(int minX, int maxX, int minZ, int maxZ, int minY, int maxY, @NotNull ResourceLocation id, ResourceLocation dimension) {
        int minChunkX = SectionPos.blockToSectionCoord(minX);
        int maxChunkX = SectionPos.blockToSectionCoord(maxX);
        int minChunkZ = SectionPos.blockToSectionCoord(minZ);
        int maxChunkZ = SectionPos.blockToSectionCoord(maxZ);

        ExistingStructure existingStructure = new ExistingStructure(minY, maxY, id.toString());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                record(dimension, ChunkPos.asLong(chunkX, chunkZ), existingStructure);
            }
        }
    }
}