package me.kall.overlapless.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.data.ExistingStructure;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "tryGenerateStructure", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/StructureManager;setStartForStructure(Lnet/minecraft/core/SectionPos;Lnet/minecraft/world/level/levelgen/structure/Structure;Lnet/minecraft/world/level/levelgen/structure/StructureStart;Lnet/minecraft/world/level/chunk/StructureAccess;)V"), cancellable = true)
    private void beforeStructureGeneration(
            StructureSet.StructureSelectionEntry structureSelectionEntry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState random,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            CallbackInfoReturnable<Boolean> cir,
            @Local @NotNull StructureStart pendingStructure
    ) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        ExistingStructure existing = Overlapless.getAnyExisting(pendingStructure, (ServerLevel) ((StructureManagerAccessor)structureManager).getLevel());

        if (existing != null) {
            cir.setReturnValue(false);
            int x = chunkPos.getMinBlockX();
            int z = chunkPos.getMinBlockZ();
            Overlapless.LOGGER.info("Section at [{}, {minY: {}, maxY: {}}, {}] is occupied by structure {}, skipping the generation of {} at [{}, {minY: {}, maxY: {}}, {}]", x, existing.minY(), existing.maxY(), z, existing.existing(), Overlapless.getName(pendingStructure.getStructure()), x, pendingBox.minY(), pendingBox.maxY(), z);
        }
    }

    @Inject(method = "tryGenerateStructure", at = @At(value = "RETURN", ordinal = 0))
    private void afterStructureGeneration(
            StructureSet.StructureSelectionEntry structureSelectionEntry,
            StructureManager structureManager,
            RegistryAccess registryAccess,
            RandomState random,
            StructureTemplateManager structureTemplateManager,
            long seed,
            ChunkAccess chunk,
            ChunkPos chunkPos,
            SectionPos sectionPos,
            @NotNull CallbackInfoReturnable<Boolean> cir,
            @Local @NotNull StructureStart pendingStructure
    ) {
        if (cir.getReturnValue()) Overlapless.afterStructureGeneration(pendingStructure, (ServerLevel) ((StructureManagerAccessor)structureManager).getLevel());
    }
}
