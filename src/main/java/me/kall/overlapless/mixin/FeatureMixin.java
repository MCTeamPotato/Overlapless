package me.kall.overlapless.mixin;

import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;
import java.util.Set;

@Mixin(ConfiguredFeature.class)
public abstract class FeatureMixin<FC extends FeatureConfiguration, F extends Feature<FC>> {
    @Shadow @Final public F feature;

    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void skipFeature(WorldGenLevel reader, ChunkGenerator chunkGenerator, Random random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.FEATURES.getKey(this.feature);
        if (id == null) return;
        if (!Config.getSkippableFeatures().contains(id)) return;

        ExistingStructures.LOCK.readLock().lock();
        try {
            Set<ExistingStructure> structures = ExistingStructures.getInChunk(reader.getLevel().dimension().location(), ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4));
            if (structures == null) return;
            int y = pos.getY();
            for (ExistingStructure existingStructure : structures) {
                if (y >= existingStructure.minY() && y <= existingStructure.maxY()) {
                    cir.setReturnValue(false);
                    if (Config.logSkipFeature()) Overlapless.LOGGER.info("Section at [{}] is occupied by structure {}. Skipping the generation of feature {}.", pos.toShortString(), existingStructure.existing(), id.toString());
                    break;
                }
            }
        } finally {
            ExistingStructures.LOCK.readLock().unlock();
        }
    }
}