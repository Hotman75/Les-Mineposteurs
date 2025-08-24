package com.mineposteurs;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.WritableBookItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

@Mod(LesMineposteurs.MODID)
public class LesMineposteurs {
    public static final String MODID = "lesmineposteurs";
    public static final Logger LOGGER = LogUtils.getLogger();
    
    // Cache pour éviter les déclenchements multiples
    private long lastTriggerTime = 0;
    private static final long COOLDOWN_MS = 1000; // 1 seconde de cooldown

    public LesMineposteurs(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {

        if (event.getLevel().isClientSide())
            return;

        ServerLevel level = (ServerLevel) event.getLevel();
        
        // Cooldown pour éviter les déclenchements multiples
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastTriggerTime < COOLDOWN_MS) {
            return;
        }

        // Coordonnées depuis la config
        BlockPos lampPos = new BlockPos(
                Config.LAMP_X.get(),
                Config.LAMP_Y.get(),
                Config.LAMP_Z.get());
        BlockPos lecternPos = new BlockPos(
                Config.LECTERN_X.get(),
                Config.LECTERN_Y.get(),
                Config.LECTERN_Z.get());

        // Ne déclenche que si le bloc notifié est adjacent à la lampe configurée
        if (!event.getPos().closerThan(lampPos, 2)) {
            return;
        }
        if (!level.getBlockState(lampPos).is(Blocks.REDSTONE_LAMP)
                || !level.getBlockState(lampPos).getValue(RedstoneLampBlock.LIT)) {
            return;
        }
        
        // Mettre à jour le temps du dernier déclenchement
        lastTriggerTime = currentTime;

        // Lecture du Lectern configuré
        BlockEntity be = level.getBlockEntity(lecternPos);
        if (!(be instanceof LecternBlockEntity lectern)) {
            // LOGGER.info("pas lectern position");
            return;
        }

        ItemStack book = lectern.getBook();
        if (book.isEmpty()) {
            LOGGER.info("Lectern vide");
            return;
        }

        // Vérifie si c'est un livre signé ou un livre écrivable
        if (!(book.getItem() instanceof WrittenBookItem
                || book.getItem() instanceof WritableBookItem)) {
            LOGGER.info("Pas un livre valide");
            return;
        }

        // Récupération de l'auteur et du texte du livre
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
                        textBuilder.append(page.raw()).append(" ");
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

        try {
            // Création d'un CommandSourceStack approprié
            CommandSourceStack commandSource = new CommandSourceStack(
                    level.getServer(), // CommandSource
                    Vec3.atCenterOf(lampPos), // Position
                    Vec2.ZERO, // Rotation
                    level, // ServerLevel
                    4, // Permission level
                    "Mineposteur", // Name
                    Component.literal("Mineposteur"), // Display name
                    level.getServer(), // Server
                    null // Entity
            );

            String message = authorName.replace("\"", "\\\"") + ": &b" + text.replace("\"", "\\\"").replace("literal{", "").replace("}", "");

            LOGGER.info("Annonce Mineposteurs : {}", message);

            // Actionbar
            level.getServer().getCommands().performPrefixedCommand(
                    commandSource,
                    "paradigm actionbar &a" + message);

        } catch (Exception e) {
            LOGGER.error("Erreur lors de l'exécution de la commande Paradigm", e);
        }
    }
}