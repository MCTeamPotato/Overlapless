package me.kall.overlapless.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
@SuppressWarnings({"DataFlowIssue"})
public abstract class ChunkGeneratorMixin {
    @WrapOperation(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureFeatureManager;setStartForFeature(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/feature/ConfiguredStructureFeature;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/FeatureAccess;)V"))
    private void genStructure(StructureFeatureManager structureManager, SectionPos sectionPos, ConfiguredStructureFeature<?, ?> structure, StructureStart pendingStructure, FeatureAccess reader, Operation<Void> original) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        ResourceLocation id = Overlapless.getName(structure.feature);

        LevelAccessor levelAccessor = ((StructureManagerAccessor) structureManager).getLevel();
        ServerLevel serverLevel = levelAccessor instanceof WorldGenLevel ? ((WorldGenLevel) levelAccessor).getLevel() : (ServerLevel) levelAccessor;

        ResourceLocation dimension = serverLevel.dimension().location();

        ExistingStructures.LOCK.writeLock().lock();
        try {
            ExistingStructure existing = ExistingStructures.getAnyExisting(pendingBox.minX(), pendingBox.maxX(), pendingBox.minZ(), pendingBox.maxZ(), pendingBox.minY(), pendingBox.maxY(), id, dimension);
            if (existing != null) {
                if (Config.logSkipStructure()) {
                    int x = sectionPos.minBlockX();
                    int z = sectionPos.minBlockZ();
                    Overlapless.LOGGER.info("Section at [{}, {minY: {}, maxY: {}}, {}] is occupied by structure {}. Skipping the generation of {} at [{}, {minY: {}, maxY: {}}, {}]", x, existing.minY(), existing.maxY(), z, existing.existing(), id, x, pendingBox.minY(), pendingBox.maxY(), z);
                }
                return;
            }

            original.call(structureManager, sectionPos, structure, pendingStructure, reader);
            ExistingStructures.afterStructureGeneration(pendingBox.minX(), pendingBox.maxX(), pendingBox.minZ(), pendingBox.maxZ(), pendingBox.minY(), pendingBox.maxY(), id, dimension);
        } finally {
            ExistingStructures.LOCK.writeLock().unlock();
        }
    }
}