package com.mineposteurs;

import org.slf4j.Logger;
import com.mineposteurs.events.AnnonceHandler;
import com.mineposteurs.commands.QuizCommands;
import com.mineposteurs.quiz.Quiz;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(LesMineposteurs.MODID)
public class LesMineposteurs {
    public static final String MODID = "lesmineposteurs";
    public static final Logger LOGGER = LogUtils.getLogger();

    public LesMineposteurs(ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(new AnnonceHandler());
        NeoForge.EVENT_BUS.register(Quiz.getInstance()); // Enregistrer le gestionnaire du quiz
        NeoForge.EVENT_BUS.register(this); // Pour les événements de commandes
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        QuizCommands.register(event.getDispatcher());
        LOGGER.info("Commandes du quiz enregistrées");
    }
}