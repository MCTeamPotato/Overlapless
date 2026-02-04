package me.kall.overlapless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.overlapless.Overlapless;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Overlapless.MOD_ID)
public class ExistingStructures {
    public static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> EXISTING_STRUCTURES = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    public static void record(ResourceLocation dimension, long chunk, ExistingStructure existingStructure) {
        EXISTING_STRUCTURES
                .computeIfAbsent(dimension, key -> Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>()))
                .computeIfAbsent(chunk, key -> ObjectSets.synchronize(new ObjectOpenHashSet<>()))
                .add(existingStructure);
    }

    public static Set<ExistingStructure> get(ResourceLocation dimension, long chunk) {
        return EXISTING_STRUCTURES.getOrDefault(dimension, Long2ObjectMaps.emptyMap()).getOrDefault(chunk, Collections.emptySet());
    }

    @SubscribeEvent
    public static void shutdown(LevelEvent.Save event) {
        EXISTING_STRUCTURES.clear();
    }
}
