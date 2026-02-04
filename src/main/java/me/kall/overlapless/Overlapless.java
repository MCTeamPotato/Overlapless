package me.kall.overlapless;

import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Lists;
import me.kall.overlapless.data.ExistingStructure;
import me.kall.overlapless.data.ExistingStructures;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "none");

    private static final ForgeConfigSpec CONFIG;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> UNSKIPPABLE_STRUCTURES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("Overlapless");
        UNSKIPPABLE_STRUCTURES = builder
                .comment("These will always generate even if there are existing structures that occupy the chunk sections.")
                .defineList("UnskippableStructures", Lists.newArrayList(), Predicates.alwaysTrue());
        builder.pop();
        CONFIG = builder.build();
    }

    private static final Supplier<Set<ResourceLocation>> STRUCTURES = Suppliers.memoize(() -> UNSKIPPABLE_STRUCTURES.get().stream().map(ResourceLocation::parse).collect(Collectors.toSet()));

    public Overlapless(@NotNull FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, CONFIG);
    }

    public static ResourceLocation getName(Structure structure) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return NONE;
        ResourceLocation id = server.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(structure);
        return id == null ? NONE : id;
    }

    private static boolean overlap(int minY1, int maxY1, int minY2, int maxY2) {
        return Math.max(minY1, minY2) <= Math.min(maxY1, maxY2);
    }

    public static @Nullable ExistingStructure getAnyExisting(@NotNull StructureStart pendingStructure, ServerLevel level) {
        if (STRUCTURES.get().contains(getName(pendingStructure.getStructure()))) return null;

        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        ResourceLocation dimension = level.dimension().location();

        int pendingMinY = pendingBox.minY();
        int pendingMaxY = pendingBox.maxY();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());
        ExistingStructure existing = null;

        checking: {
            synchronized (ExistingStructures.EXISTING_STRUCTURES) {
                for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                    for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                        long existingChunk = ChunkPos.asLong(chunkX, chunkZ);
                        Set<ExistingStructure> structures = ExistingStructures.get(dimension, existingChunk);
                        if (structures == null) continue;
                        for (ExistingStructure existingStructure : structures) {
                            if (Overlapless.overlap(existingStructure.minY(), existingStructure.maxY(), pendingMinY, pendingMaxY)) {
                                existing = existingStructure;
                                break checking;
                            }
                        }
                    }
                }
            }
        }

        return existing;
    }

    public static void afterStructureGeneration(@NotNull StructureStart pendingStructure, @NotNull ServerLevel level) {
        BoundingBox pendingBox = pendingStructure.getBoundingBox();
        int minChunkX = SectionPos.blockToSectionCoord(pendingBox.minX());
        int maxChunkX = SectionPos.blockToSectionCoord(pendingBox.maxX());
        int minChunkZ = SectionPos.blockToSectionCoord(pendingBox.minZ());
        int maxChunkZ = SectionPos.blockToSectionCoord(pendingBox.maxZ());

        ResourceLocation dimension = level.dimension().location();

        ExistingStructure existingStructure = new ExistingStructure(pendingBox.minY(), pendingBox.maxY(), Overlapless.getName(pendingStructure.getStructure()).toString());

        synchronized (ExistingStructures.EXISTING_STRUCTURES) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    ExistingStructures.record(dimension, ChunkPos.asLong(chunkX, chunkZ), existingStructure);
                }
            }
        }
    }
}
