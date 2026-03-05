package me.kall.overlapless.config;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import fuzs.forgeconfigapiport.api.config.v2.ModConfigEvents;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.ported.ServerLifecycleHooks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Config {
    private static final ForgeConfigSpec CONFIG;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNSKIPPABLE_STRUCTURES_CONFIG, SKIPPABLE_FEATURES_CONFIG;
    private static final ForgeConfigSpec.BooleanValue PRINT_SKIPPING_STRUCTURE, PRINT_SKIPPING_FEATURE;
    private static final Set<ResourceLocation> UNSKIPPABLE_STRUCTURES = new ObjectOpenHashSet<>();
    private static final Set<ResourceLocation> SKIPPABLE_FEATURES = new ObjectOpenHashSet<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Overlapless");
        UNSKIPPABLE_STRUCTURES_CONFIG = builder.comment("These will always generate even if there are existing structures that occupy the chunk sections.").defineList("UnskippableStructures", Lists.newArrayList(), Predicates.alwaysTrue());
        SKIPPABLE_FEATURES_CONFIG  = builder
                .comment(
                        "If you still find overlapping generated structures in your world, they are most likely Features rather than Structures. For example, Desert Well and Amethyst Geode",
                        "You can write down the features' registry names here so they will not get overlapped with existing structures.",
                        "It's not acceptable to prevent all the features from getting overlapped with structures. Because trees/ores/flowers/etc small things are also features lol.",
                        "All the registered features: " + Arrays.toString(BuiltInRegistries.FEATURE.keySet().toArray())
                )
                .defineListAllowEmpty(Lists.newArrayList("SkippableFeatures"), Lists::newArrayList, Predicates.alwaysTrue());
        PRINT_SKIPPING_STRUCTURE = builder.define("PrintStructureSkipEventInLog", true);
        PRINT_SKIPPING_FEATURE = builder.define("PrintFeatureSkipEventInLog", true);
        builder.pop();
        CONFIG = builder.build();
    }

    public static void register(String modId) {
        ForgeConfigRegistry.INSTANCE.register(modId, ModConfig.Type.COMMON, Config.CONFIG);
        ModConfigEvents.reloading(modId).register(config -> {
            if (config.getModId().equals(Overlapless.MOD_ID)) {
                UNSKIPPABLE_STRUCTURES.clear();
                SKIPPABLE_FEATURES.clear();
            }
        });
    }

    public static boolean logSkipStructure() {
        return PRINT_SKIPPING_STRUCTURE.get();
    }

    public static boolean logSkipFeature() {
        return PRINT_SKIPPING_FEATURE.get();
    }

    public static Set<ResourceLocation> getSkippableFeatures() {
        if (SKIPPABLE_FEATURES.isEmpty()) {
            List<? extends String> list = SKIPPABLE_FEATURES_CONFIG.get();
            if (list.isEmpty()) return Collections.emptySet();

            for (String string : list) {
                ResourceLocation id = new ResourceLocation(string);
                Feature<?> feature = BuiltInRegistries.FEATURE.get(id);
                if (feature == null) {
                    Overlapless.LOGGER.error("Entry {} in SkippableFeatures config option is invalid. Failed to find corresponding feature registry element.", string);
                } else {
                    SKIPPABLE_FEATURES.add(id);
                }
            }
        }

        return SKIPPABLE_FEATURES;
    }

    public static Set<ResourceLocation> getUnskippableStructures() {
        if (UNSKIPPABLE_STRUCTURES.isEmpty()) {
            List<? extends String> list = UNSKIPPABLE_STRUCTURES_CONFIG.get();
            if (list.isEmpty()) return Collections.emptySet();

            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                for (String name : list) UNSKIPPABLE_STRUCTURES.add(new ResourceLocation(name));
                return UNSKIPPABLE_STRUCTURES;
            }

            server.registryAccess().registry(Registries.STRUCTURE).ifPresent(registry -> {
                UNSKIPPABLE_STRUCTURES.clear();
                for (String string : list) {
                    ResourceLocation id = new ResourceLocation(string);
                    Structure structure = registry.get(id);
                    if (structure == null) {
                        Overlapless.LOGGER.error("Entry {} in UnskippableStructures config option is invalid. Failed to find corresponding structure registry element.", string);
                    } else {
                        UNSKIPPABLE_STRUCTURES.add(id);
                    }
                }
            });
        }

        return UNSKIPPABLE_STRUCTURES;
    }
}
