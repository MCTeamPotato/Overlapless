package me.kall.overlapless;

import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

@Mod(Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "none");

    public static ResourceLocation getName(Structure structure) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return NONE;
        ResourceLocation id = server.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id == null ? NONE : id;
    }

    private static boolean overlap(int minY1, int maxY1, int minY2, int maxY2) {
        return Math.max(minY1, minY2) <= Math.min(maxY1, maxY2);
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
                    Set<ExistingStructure> structures = ExistingStructures.get(level.dimension().location(), existingChunk);
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

    public static void afterStructureGeneration(@NotNull StructureStart pendingStructure, @NotNull ServerLevel level) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());

        ResourceLocation dimension = level.dimension().location();

        ExistingStructure existingStructure = new ExistingStructure(pendingBox.minY(), pendingBox.maxY(), Overlapless.getName(pendingStructure.getStructure()).toString());

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                ExistingStructures.record(dimension, ChunkPos.asLong(chunkX, chunkZ), existingStructure);
            }
        }
    }
}
