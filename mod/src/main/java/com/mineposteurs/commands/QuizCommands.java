package com.mineposteurs.commands;

import com.mineposteurs.quiz.Quiz;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import org.slf4j.Logger;
import com.mineposteurs.LesMineposteurs;

public class QuizCommands {
    private static final Logger LOGGER = LesMineposteurs.LOGGER;
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("mineposteur")
            .then(Commands.literal("quiz")
                .then(Commands.literal("start")
                    .requires(source -> source.hasPermission(2)) // Niveau OP requis
                    .executes(QuizCommands::startQuiz))
                .then(Commands.literal("end")
                    .requires(source -> source.hasPermission(2)) // Niveau OP requis
                    .executes(QuizCommands::endQuiz))
                .then(Commands.literal("status")
                    .requires(source -> source.hasPermission(2)) // Niveau OP requis
                    .executes(QuizCommands::statusQuiz))
            )
        );
    }
    
    private static int startQuiz(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            ServerLevel level = source.getLevel();
            Quiz quiz = Quiz.getInstance();
            quiz.setLevel(level);
            quiz.activateQuiz();
            
            source.sendSuccess(() -> Component.literal("§a[Quiz] Quiz activé ! Les boutons sont maintenant fonctionnels."), true);
            LOGGER.info("Quiz activé par {}", source.getTextName());
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[Quiz] Erreur lors de l'activation du quiz: " + e.getMessage()));
            LOGGER.error("Erreur lors de l'activation du quiz", e);
            return 0;
        }
    }
    
    private static int endQuiz(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            Quiz quiz = Quiz.getInstance();
            quiz.deactivateQuiz();
            
            source.sendSuccess(() -> Component.literal("§c[Quiz] Quiz désactivé ! Les boutons ne fonctionnent plus."), true);
            LOGGER.info("Quiz désactivé par {}", source.getTextName());
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[Quiz] Erreur lors de la désactivation du quiz: " + e.getMessage()));
            LOGGER.error("Erreur lors de la désactivation du quiz", e);
            return 0;
        }
    }
    
    private static int statusQuiz(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            Quiz quiz = Quiz.getInstance();
            boolean isActive = quiz.isQuizActive();
            Quiz.QuizState currentState = quiz.getCurrentState();
            int currentPlayer = quiz.getCurrentPlayerAnswering();
            int disabledCount = quiz.getDisabledPlayers().size();
            
            String status = isActive ? "§aActif" : "§cInactif";
            String stateMsg = switch (currentState) {
                case INACTIVE -> "§7Désactivé";
                case WAITING_FOR_ANSWER -> "§e⏳ En attente d'une réponse";
                case PLAYER_ANSWERED -> "§b✋ Joueur " + currentPlayer + " a répondu";
                case ROUND_FINISHED -> "§d🏁 Manche terminée";
            };
            
            source.sendSuccess(() -> Component.literal("§6=== STATUT DU QUIZ ==="), false);
            source.sendSuccess(() -> Component.literal("§6État: " + status), false);
            source.sendSuccess(() -> Component.literal("§6Phase: " + stateMsg), false);
            source.sendSuccess(() -> Component.literal("§6Joueurs désactivés: §c" + disabledCount + "§6/12"), false);
            
            if (!quiz.getDisabledPlayers().isEmpty()) {
                source.sendSuccess(() -> Component.literal("§6Joueurs éliminés: §c" + quiz.getDisabledPlayers().toString()), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§c[Quiz] Erreur lors de la récupération du statut: " + e.getMessage()));
            LOGGER.error("Erreur lors de la récupération du statut du quiz", e);
            return 0;
        }
    }

}