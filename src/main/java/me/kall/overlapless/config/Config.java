package me.kall.overlapless.config;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.overlapless.Overlapless;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Config {
    private static final ModConfigSpec CONFIG;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> UNSKIPPABLE_STRUCTURES_CONFIG, SKIPPABLE_FEATURES_CONFIG;
    private static final ModConfigSpec.BooleanValue PRINT_SKIPPING_STRUCTURE, PRINT_SKIPPING_FEATURE;
    private static final Set<ResourceLocation> UNSKIPPABLE_STRUCTURES = new ObjectOpenHashSet<>();
    private static final Set<ResourceLocation> SKIPPABLE_FEATURES = new ObjectOpenHashSet<>();

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("Overlapless");
        UNSKIPPABLE_STRUCTURES_CONFIG = builder.comment("These will always generate even if there are existing structures that occupy the chunk sections.").defineListAllowEmpty("UnskippableStructures", Lists.newArrayList(), () -> "namespace:path", Predicates.alwaysTrue());
        SKIPPABLE_FEATURES_CONFIG  = builder
                .comment(
                        "If you still find overlapping generated structures in your world, they are most likely Features rather than Structures. For example, Desert Well and Amethyst Geode",
                        "You can write down the features' registry names here so they will not get overlapped with existing structures.",
                        "It's not acceptable to prevent all the features from getting overlapped with structures. Because trees/ores/flowers/etc small things are also features lol.",
                        "All the registered features: " + Arrays.toString(BuiltInRegistries.FEATURE.keySet().toArray())
                )
                .defineListAllowEmpty("SkippableFeatures", Lists.newArrayList(), () -> "namespace:path", Predicates.alwaysTrue());
        PRINT_SKIPPING_STRUCTURE = builder.define("PrintStructureSkipEventInLog", true);
        PRINT_SKIPPING_FEATURE = builder.define("PrintFeatureSkipEventInLog", true);
        builder.pop();
        CONFIG = builder.build();
    }

    public static void register(@NotNull ModContainer container, @NotNull IEventBus modBus) {
        container.registerConfig(ModConfig.Type.COMMON, Config.CONFIG);
        modBus.addListener((ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getModId().equals(Overlapless.MOD_ID)) {
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
                ResourceLocation id = ResourceLocation.parse(string);
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
                for (String name : list) UNSKIPPABLE_STRUCTURES.add(ResourceLocation.parse(name));
                return UNSKIPPABLE_STRUCTURES;
            }

            server.registryAccess().registry(Registries.STRUCTURE).ifPresent(registry -> {
                UNSKIPPABLE_STRUCTURES.clear();
                for (String string : list) {
                    ResourceLocation id = ResourceLocation.parse(string);
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
