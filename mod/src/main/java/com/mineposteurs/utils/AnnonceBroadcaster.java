package com.mineposteurs.utils;

import com.mineposteurs.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class AnnonceBroadcaster {

    public static void sendAnnonce(MinecraftServer server, String author, String text) {
        List<String> parts = splitText(text, Config.MAX_ACTIONBAR_CHARS.get());
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            
            // Envoi échelonné des segments
            sendSegmentsWithDelay(server, player, author, parts, 0);
        }
    }

    private static void sendSegmentsWithDelay(MinecraftServer server, ServerPlayer player, String author, List<String> segments, int currentIndex) {
        if (currentIndex >= segments.size()) {
            return; // Fini
        }
        
        // Vérifier que le joueur est toujours connecté
        if (player.connection == null || player.hasDisconnected()) {
            return;
        }
        
        // Envoyer le segment actuel
        String segment = segments.get(currentIndex);
        if (currentIndex == 0) {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§c" + author + ": §a" + segment)));
        }
        else {
            player.connection.send(new ClientboundSetActionBarTextPacket(Component.literal("§a" + segment)));
        }
        
        // Programmer le prochain segment
        if (currentIndex + 1 < segments.size()) {
            CompletableFuture.delayedExecutor(Config.TICKS_BETWEEN_PARTS.get() * 50L, TimeUnit.MILLISECONDS) // 50ms par tick
                .execute(() -> {
                    server.execute(() -> sendSegmentsWithDelay(server, player, author, segments, currentIndex + 1));
                });
        }
    }

    private static List<String> splitText(String text, int maxLen) {
        List<String> parts = new ArrayList<>();
        int index = 0;
        while (index < text.length()) {
            int end = Math.min(index + maxLen, text.length());
            parts.add(text.substring(index, end));
            index = end;
        }
        return parts;
    }
}