package me.kall.overlapless;

import me.kall.overlapless.config.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod(Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);

    private static final ResourceLocation NONE = new ResourceLocation(MOD_ID, "none");

    public Overlapless() {
        Config.register(ModLoadingContext.get(), FMLJavaModLoadingContext.get().getModEventBus());
    }

    public static ResourceLocation getName(StructureFeature<?> structure) {
        return Optional.ofNullable(ForgeRegistries.STRUCTURE_FEATURES.getKey(structure)).orElse(NONE);
    }
}