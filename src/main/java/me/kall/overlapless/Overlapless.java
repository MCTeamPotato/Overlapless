package me.kall.overlapless;

import me.kall.overlapless.config.Config;
import me.kall.overlapless.ported.ServerLifecycleHooks;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public final class Overlapless implements ModInitializer {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);

    private static final ResourceLocation NONE = new ResourceLocation(MOD_ID, "none");

    @Override
    public void onInitialize() {
        Config.register(MOD_ID);
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