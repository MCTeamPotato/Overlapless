package me.kall.overlapless.ported;

import net.minecraft.server.MinecraftServer;

public class ServerLifecycleHooks {
    private static MinecraftServer currentServer;

//    public static void init() {
//        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
//            currentServer = server;
//        });
//    }

    public static void setCurrentServer(MinecraftServer server) {
        currentServer = server;
    }

    public static MinecraftServer getCurrentServer() {
        return currentServer;
    }
}
