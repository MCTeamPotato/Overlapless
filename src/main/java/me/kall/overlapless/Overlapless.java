package me.kall.overlapless;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.ChunkTickEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Mod(Overlapless.MOD_ID)
@Mod.EventBusSubscriber(modid = Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);

    //TODO: Saved Data for those whose ChunkStatus is not FULL?
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> EXISTING = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    public static ResourceLocation getName(Structure structure) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return RegistryEntry.NONE;
        ResourceLocation id = server.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id == null ? RegistryEntry.NONE : id;
    }

    private static boolean overlap(int minY1, int maxY1, int minY2, int maxY2) {
        return Math.max(minY1, minY2) <= Math.min(maxY1, maxY2);
    }

    @SubscribeEvent
    public static void chunkTick(ChunkTickEvent.@NotNull Post event) {
        if (ThreadLocalRandom.current().nextBoolean() && ThreadLocalRandom.current().nextBoolean()) {
            ResourceLocation dimension = event.getLevel().dimension().location();
            Long2ObjectMap<Set<ExistingStructure>> chunks = EXISTING.get(dimension);
            if (chunks == null) return;
            long chunk = event.getChunk().getPos().toLong();
            if (chunks.containsKey(chunk)) {
                chunks.remove(chunk);
                if (chunks.isEmpty()) EXISTING.remove(dimension);
            }
        }
    }

    public static @Nullable ExistingStructure getAnyExisting(@NotNull BoundingBox pendingBox, ServerLevel level) {
        int pendingMinY = pendingBox.minY();
        int pendingMaxY = pendingBox.maxY();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());
        ExistingStructure existing = null;

        checking: {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    long existingChunk = ChunkPos.asLong(chunkX, chunkZ);
                    Set<ExistingStructure> structures = Overlapless.EXISTING.getOrDefault(level.dimension().location(), Long2ObjectMaps.emptyMap()).get(existingChunk);
                    if (structures == null) continue;
                    for (ExistingStructure existingStructure : structures) {
                        if (Overlapless.overlap(existingStructure.minY(), existingStructure.maxY(), pendingMinY, pendingMaxY)) {
                            existing = existingStructure;
                            break checking;
                        }
                    }
                }
            }
        }

        return existing;
    }

    public static void afterGenerateStructure(@NotNull StructureStart pendingStructure, @NotNull ServerLevel level) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());

        ResourceLocation dimension = level.dimension().location();

        ExistingStructure existingStructure = new ExistingStructure(pendingBox.minY(), pendingBox.maxY(), Overlapless.getName(pendingStructure.getStructure()).toString());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                EXISTING.computeIfAbsent(dimension, key -> Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>())).computeIfAbsent(ChunkPos.asLong(chunkX, chunkZ), key -> ObjectSets.synchronize(new ObjectOpenHashSet<>())).add(existingStructure);
            }
        }
    }

    public record ExistingStructure(int minY, int maxY, String existing) {}
}
