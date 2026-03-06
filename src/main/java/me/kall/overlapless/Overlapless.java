package me.kall.overlapless;

import me.kall.overlapless.config.Config;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

@Mod(Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);

    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "none");

    public Overlapless(IEventBus modBus, Dist dist, ModContainer container) {
        Config.register(container, modBus);
    }

    public static ResourceLocation getName(Structure structure) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return NONE;
        Optional<Registry<Structure>> optionalStructures = server.registryAccess().registry(Registries.STRUCTURE);
        if (optionalStructures.isEmpty()) return NONE;
        ResourceLocation id = optionalStructures.get().getKey(structure);
        return id == null ? NONE : id;
    }
}