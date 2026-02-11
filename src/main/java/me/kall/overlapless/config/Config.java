package me.kall.overlapless.config;

import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.overlapless.Overlapless;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class Config {
    private static final ForgeConfigSpec CONFIG;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> LIST;
    private static final Set<ResourceLocation> UNSKIPPABLE_STRUCTURES = new ObjectOpenHashSet<>();

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Overlapless");
        LIST = builder.comment("These will always generate even if there are existing structures that occupy the chunk sections.").defineList("UnskippableStructures", Lists.newArrayList(), Predicates.alwaysTrue());
        builder.pop();
        CONFIG = builder.build();
    }

    public static void register(@NotNull ModLoadingContext context, @NotNull IEventBus modBus) {
        context.registerConfig(ModConfig.Type.COMMON, Config.CONFIG);
        modBus.addListener((ModConfigEvent.Reloading event) -> {
            if (event.getConfig().getModId().equals(Overlapless.MOD_ID)) {
                UNSKIPPABLE_STRUCTURES.clear();
            }
        });
    }

    public static Set<ResourceLocation> getUnskippableStructures() {
        if (UNSKIPPABLE_STRUCTURES.isEmpty()) {
            List<? extends String> list = LIST.get();
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
