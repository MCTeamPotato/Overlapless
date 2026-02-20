package me.kall.overlapless.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.StructureAccess;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkGenerator.class)
@SuppressWarnings({"DataFlowIssue"})
public abstract class ChunkGeneratorMixin {
    @WrapOperation(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;setStartForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V"))
    private void genStructure(StructureManager structureManager, SectionPos sectionPos, Structure structure, @NotNull StructureStart pendingStructure, StructureAccess structureAccess, Operation<Void> original) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        LevelAccessor levelAccessor = ((StructureManagerAccessor)structureManager).getLevel();
        ExistingStructure existing = ExistingStructures.getAnyExisting(pendingStructure, levelAccessor instanceof WorldGenLevel worldGenLevel ? worldGenLevel.getLevel() : (ServerLevel) levelAccessor);

        if (existing != null) {
            if (Config.logSkipStructure()) {
                int x = sectionPos.minBlockX();
                int z = sectionPos.minBlockZ();
                Overlapless.LOGGER.info("Section at [{}, {minY: {}, maxY: {}}, {}] is occupied by structure {}. Skipping the generation of {} at [{}, {minY: {}, maxY: {}}, {}]", x, existing.minY(), existing.maxY(), z, existing.existing(), Overlapless.getName(pendingStructure.getStructure()), x, pendingBox.minY(), pendingBox.maxY(), z);
            }
            return;
        }

        original.call(structureManager, sectionPos, structure, pendingStructure, structureAccess);

        if (structureManager.getStartForStructure(sectionPos, structure, structureAccess) != null) {
            ExistingStructures.afterStructureGeneration(pendingStructure, levelAccessor instanceof WorldGenLevel worldGenLevel ? worldGenLevel.getLevel() : (ServerLevel) levelAccessor);
        }
    }
}