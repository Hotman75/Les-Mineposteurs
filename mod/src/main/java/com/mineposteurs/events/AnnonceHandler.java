package com.mineposteurs.events;

import com.mineposteurs.Config;
import com.mineposteurs.LesMineposteurs;
import com.mineposteurs.utils.AnnonceBroadcaster;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.minecraft.server.MinecraftServer;

public class AnnonceHandler {
    private long lastTriggerTime = 0;
    private static final long COOLDOWN_MS = 1000; // 1 seconde de cooldown

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (event.getLevel().isClientSide())
            return;
        ServerLevel level = (ServerLevel) event.getLevel();

        // Cooldown
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime < COOLDOWN_MS) {
            return;
        }

        // Coordonnées depuis la config
        BlockPos lampPos = new BlockPos(Config.LAMP_X.get(), Config.LAMP_Y.get(), Config.LAMP_Z.get());
        BlockPos lecternPos = new BlockPos(Config.LECTERN_X.get(), Config.LECTERN_Y.get(), Config.LECTERN_Z.get());

        // Vérifie que la bonne lampe s’allume
        if (!event.getPos().closerThan(lampPos, 2))
            return;
        if (!level.getBlockState(lampPos).is(Blocks.REDSTONE_LAMP) ||
                !level.getBlockState(lampPos).getValue(RedstoneLampBlock.LIT))
            return;

        lastTriggerTime = currentTime;

        // Lecture du Lectern configuré
        BlockEntity be = level.getBlockEntity(lecternPos);
        if (!(be instanceof LecternBlockEntity lectern))
            return;

        ItemStack book = lectern.getBook();
        if (book.isEmpty()) {
            LesMineposteurs.LOGGER.info("Lectern vide");
            return;
        }

        // Auteur et texte
        String authorName = "Anonyme";
        StringBuilder textBuilder = new StringBuilder();

        if (book.getItem() instanceof WrittenBookItem) {
            // Livre signé - utilise les Data Components
            if (book.has(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT)) {
                var bookContent = book.get(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT);
                if (bookContent != null) {
                    authorName = bookContent.author();
                    var pages = bookContent.pages();
                    for (var page : pages) {
                        Component rawComponent = page.raw();
                        // Convertir le Component en String et nettoyer
                        String rawText = rawComponent.getString();
                        String cleanText = cleanLiteralText(rawText);
                        textBuilder.append(cleanText).append(" ");
                    }
                }
            }
        } else if (book.getItem() instanceof WritableBookItem) {
            // Livre non signé - utilise les Data Components pour les pages
            if (book.has(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT)) {
                var writableContent = book.get(net.minecraft.core.component.DataComponents.WRITABLE_BOOK_CONTENT);
                if (writableContent != null) {
                    var pages = writableContent.pages();
                    for (var page : pages) {
                        textBuilder.append(page.raw()).append(" ");
                    }
                }
            }
        }

        String text = textBuilder.toString().trim();
        String message = authorName + ": " + text;

        MinecraftServer server = level.getServer();
        if (server != null) {
            LesMineposteurs.LOGGER.info("Annonce: {}, {}", authorName, text);
            AnnonceBroadcaster.sendAnnonce(server, authorName, text);
        }
    }

    private String cleanLiteralText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Nettoyage des balises literal{} si présentes
        if (text.startsWith("literal{") && text.endsWith("}")) {
            text = text.substring(8, text.length() - 1); // Enlever "literal{" et "}"
        }

        return text;
    }

}
