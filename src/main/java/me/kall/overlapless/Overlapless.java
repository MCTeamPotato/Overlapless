package me.kall.overlapless;

import me.kall.overlapless.config.Config;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

@Mod(Overlapless.MOD_ID)
public final class Overlapless {
    public static final String MOD_ID = "overlapless";
    public static final Logger LOGGER = LogManager.getLogger(Overlapless.class);

    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(MOD_ID, "none");

    public Overlapless(@NotNull FMLJavaModLoadingContext context) {
        Config.register(context, context.getModEventBus());
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