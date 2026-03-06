package me.kall.overlapless.mixin;

import me.kall.overlapless.Overlapless;
import me.kall.overlapless.config.Config;
import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(Feature.class)
public abstract class FeatureMixin<FC extends FeatureConfiguration> {
    @Inject(method = "place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z", at = @At("HEAD"), cancellable = true)
    private void skipFeature(FC config, WorldGenLevel reader, ChunkGenerator chunkGenerator, RandomSource random, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ResourceLocation id = ForgeRegistries.FEATURES.getKey((Feature<?>) (Object) this);
        if (id == null) return;
        if (Config.getSkippableFeatures().contains(id)) {
            synchronized (ExistingStructures.EXISTING_STRUCTURES) {
                Set<ExistingStructure> structures = ExistingStructures.getInChunk(reader.getLevel().dimension().location(), ChunkPos.asLong(pos));
                if (structures == null) return;
                int y = pos.getY();
                for (ExistingStructure existingStructure : structures) {
                    if (y >= existingStructure.minY() && y <= existingStructure.maxY()) {
                        cir.setReturnValue(false);
                        if (Config.logSkipFeature()) Overlapless.LOGGER.info("Section at [{}] is occupied by structure {}. Skipping the generation of feature {}.", pos.toShortString(), existingStructure.existing(), id.toString());
                        break;
                    }
                }
            }
        }
    }
}
