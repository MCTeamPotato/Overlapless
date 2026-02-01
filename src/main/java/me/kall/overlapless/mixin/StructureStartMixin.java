package me.kall.overlapless.mixin;

import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(StructureStart.class)
public abstract class StructureStartMixin {
    @Inject(method = "placeInChunk", at = @At("RETURN"))
    private void onPlaceInChunk(@NotNull WorldGenLevel worldGenLevel, StructureManager structureManager, ChunkGenerator generator, RandomSource random, @NotNull BoundingBox pendingBox, ChunkPos chunkPos, CallbackInfo ci) {
        ServerLevel level = worldGenLevel.getLevel();
        ResourceLocation dimension = level.dimension().location();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());
        ExistingStructures existingStructures = ExistingStructures.get(level);

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                existingStructures.remove(dimension, ChunkPos.asLong(chunkX, chunkZ));
            }
        }

        existingStructures.setDirty();
    }
}
