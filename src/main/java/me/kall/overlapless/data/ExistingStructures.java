package me.kall.overlapless.data;

import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.event.ChunkTickEvent;
import me.kall.overlapless.Overlapless;
import me.kall.overlapless.ext.StructureHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.Set;

@Mod.EventBusSubscriber(modid = Overlapless.MOD_ID)
public class ExistingStructures {
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<ExistingStructure>>> EXISTING_STRUCTURES = Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

    private static final Object2ObjectMap<ResourceLocation, LongSet> FULL_CHUNKS = new Object2ObjectOpenHashMap<>();

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

    @SubscribeEvent
    public static void tickChunk(ChunkTickEvent.Pre event) {
        ServerLevel level = event.getLevel();
        LevelChunk chunk = event.getChunk();

        ResourceLocation dimension = level.dimension().location();
        long pos = chunk.getPos().toLong();

        if (!((StructureHolder)chunk).overlapless$isFullFilled()) {
            ((StructureHolder)chunk).overlapless$setFullFilled(true);
            FULL_CHUNKS.computeIfAbsent(dimension, key -> new LongOpenHashSet()).add(pos);
        } else {
            return;
        }

        LongSet chunks = FULL_CHUNKS.get(dimension);
        if (chunks == null || chunks.size() <= 64) return;
        
        Long2ObjectMap<Set<ExistingStructure>> record = EXISTING_STRUCTURES.get(dimension);
        if (record == null) return;
        for (long toRemove : chunks) {
            record.remove(toRemove);
        }
        
        FULL_CHUNKS.remove(dimension);
    }
}
