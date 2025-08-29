package com.mineposteurs.quiz;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import com.mineposteurs.LesMineposteurs;
import org.slf4j.Logger;

import java.util.*;

public class Quiz {
    private static final Logger LOGGER = LesMineposteurs.LOGGER;

    // États du quiz
    public enum QuizState {
        INACTIVE, // Quiz désactivé
        WAITING_FOR_ANSWER, // En attente d'une réponse
        PLAYER_ANSWERED, // Un joueur a répondu
        ROUND_FINISHED // Manche terminée
    }

    // Configuration des positions (à déplacer dans Config.java si souhaité)
    private static final Map<Integer, BlockPos> PLAYER_BUTTONS = new HashMap<>();
    private static final Map<Integer, BlockPos> PLAYER_INDICATOR_BLOCKS = new HashMap<>();
    private static BlockPos PRESENTER_VALIDATE_BUTTON;
    private static BlockPos PRESENTER_INVALIDATE_BUTTON;

    // État du quiz
    private QuizState currentState = QuizState.INACTIVE;
    private int currentPlayerAnswering = -1; // ID du joueur qui a répondu
    private Set<Integer> disabledPlayers = new HashSet<>(); // Joueurs désactivés pour cette question
    private ServerLevel level;
    private boolean quizActive = false; // Contrôle global du quiz

    // Singleton
    private static Quiz instance;

    private Quiz() {
        initializePositions();
    }

    public static Quiz getInstance() {
        if (instance == null) {
            instance = new Quiz();
        }
        return instance;
    }

    /**
     * Initialise les positions des boutons et blocs indicateurs
     * TODO: Déplacer ces positions dans le fichier de configuration
     */
    private void initializePositions() {
        // Positions des boutons joueurs
        PLAYER_BUTTONS.put(1, new BlockPos(-30, 84, -427));
        PLAYER_INDICATOR_BLOCKS.put(1, new BlockPos(-30, 83, -427));

        PLAYER_BUTTONS.put(2, new BlockPos(-30, 84, -429));
        PLAYER_INDICATOR_BLOCKS.put(2, new BlockPos(-30, 83, -429));

        PLAYER_BUTTONS.put(3, new BlockPos(-30, 84, -432));
        PLAYER_INDICATOR_BLOCKS.put(3, new BlockPos(-30, 83, -432));

        PLAYER_BUTTONS.put(4, new BlockPos(-30, 84, -434));
        PLAYER_INDICATOR_BLOCKS.put(4, new BlockPos(-30, 83, -434));

        PLAYER_BUTTONS.put(5, new BlockPos(-28, 83, -427));
        PLAYER_INDICATOR_BLOCKS.put(5, new BlockPos(-28, 82, -427));

        PLAYER_BUTTONS.put(6, new BlockPos(-28, 83, -429));
        PLAYER_INDICATOR_BLOCKS.put(6, new BlockPos(-28, 82, -429));

        PLAYER_BUTTONS.put(7, new BlockPos(-28, 83, -432));
        PLAYER_INDICATOR_BLOCKS.put(7, new BlockPos(-28, 82, -432));

        PLAYER_BUTTONS.put(8, new BlockPos(-28, 83, -434));
        PLAYER_INDICATOR_BLOCKS.put(8, new BlockPos(-28, 82, -434));

        PLAYER_BUTTONS.put(9, new BlockPos(-26, 82, -427));
        PLAYER_INDICATOR_BLOCKS.put(9, new BlockPos(-26, 81, -427));

        PLAYER_BUTTONS.put(10, new BlockPos(-26, 82, -429));
        PLAYER_INDICATOR_BLOCKS.put(10, new BlockPos(-26, 81, -429));

        PLAYER_BUTTONS.put(11, new BlockPos(-26, 82, -432));
        PLAYER_INDICATOR_BLOCKS.put(11, new BlockPos(-26, 81, -432));

        PLAYER_BUTTONS.put(12, new BlockPos(-26, 82, -434));
        PLAYER_INDICATOR_BLOCKS.put(12, new BlockPos(-26, 81, -434));

        // Boutons du présentateur
        PRESENTER_VALIDATE_BUTTON = new BlockPos(-22, 83, -430);
        PRESENTER_INVALIDATE_BUTTON = new BlockPos(-22, 83, -431);
    }

    /**
     * Active le quiz (commande admin)
     */
    public void activateQuiz() {
        quizActive = true;
        currentState = QuizState.WAITING_FOR_ANSWER;
        disabledPlayers.clear();

        // Réactiver tous les boutons et remettre les blocs par défaut
        resetAllIndicatorBlocks();

        LOGGER.info("Quiz activé par un administrateur");
    }

    /**
     * Désactive le quiz (commande admin)
     */
    public void deactivateQuiz() {
        quizActive = false;
        currentState = QuizState.INACTIVE;
        currentPlayerAnswering = -1;
        disabledPlayers.clear();

        // Remettre tous les blocs par défaut
        resetAllIndicatorBlocks();

        LOGGER.info("Quiz désactivé par un administrateur");
    }

    /**
     * Vérifie si le quiz est actif
     */
    public boolean isQuizActive() {
        return quizActive;
    }

    /**
     * Définit le niveau du serveur pour les opérations sur les blocs
     */
    public void setLevel(ServerLevel level) {
        this.level = level;
    }

    /**
     * Gère l'appui sur un bouton joueur
     */
    public boolean handlePlayerButtonPress(BlockPos buttonPos, ServerLevel level) {
        if (!quizActive || currentState != QuizState.WAITING_FOR_ANSWER) {
            return false; // Quiz inactif ou pas en attente de réponse
        }

        this.level = level;

        // Trouver quel joueur a appuyé
        int playerId = getPlayerIdFromButtonPos(buttonPos);
        if (playerId == -1 || disabledPlayers.contains(playerId)) {
            return false; // Bouton non reconnu ou joueur désactivé
        }

        // Un joueur a répondu
        currentPlayerAnswering = playerId;
        currentState = QuizState.PLAYER_ANSWERED;

        // Allumer le bloc indicateur du joueur (sea lantern)
        setPlayerIndicatorBlock(playerId, Blocks.SEA_LANTERN.defaultBlockState());

        // Désactiver tous les autres boutons
        disableOtherPlayerButtons(playerId);

        LOGGER.info("Le joueur {} a appuyé sur son bouton", playerId);
        return true;
    }

    /**
     * Gère l'appui sur le bouton de validation du présentateur
     */
    public boolean handlePresenterValidateButton(BlockPos buttonPos) {
        if (!quizActive || !PRESENTER_VALIDATE_BUTTON.equals(buttonPos) || currentState != QuizState.PLAYER_ANSWERED) {
            return false;
        }

        LOGGER.info("Présentateur a validé la réponse du joueur {}", currentPlayerAnswering);

        // Bonne réponse - nouvelle question
        currentPlayerAnswering = -1;
        disabledPlayers.clear(); // Tous les joueurs sont réactivés
        currentState = QuizState.WAITING_FOR_ANSWER;

        // Remettre tous les blocs indicateurs à l'état d'origine
        for (int playerId : PLAYER_INDICATOR_BLOCKS.keySet()) {
            setPlayerIndicatorBlock(playerId, Blocks.BLACK_CONCRETE.defaultBlockState());
        }

        // Les joueurs peuvent de nouveau appuyer sur leurs boutons
        enableAllPlayerButtons();

        return true;
    }

    /**
     * Gère l'appui sur le bouton d'invalidation du présentateur
     */
    public boolean handlePresenterInvalidateButton(BlockPos buttonPos) {
        if (!quizActive || !PRESENTER_INVALIDATE_BUTTON.equals(buttonPos)
                || currentState != QuizState.PLAYER_ANSWERED) {
            return false;
        }

        LOGGER.info("Présentateur a invalidé la réponse du joueur {}", currentPlayerAnswering);

        // Mauvaise réponse
        // Mettre le bloc du joueur en rouge (redstone block)
        setPlayerIndicatorBlock(currentPlayerAnswering, Blocks.REDSTONE_BLOCK.defaultBlockState());

        // Désactiver définitivement ce joueur pour cette question
        disabledPlayers.add(currentPlayerAnswering);

        // Remettre l'état en attente et réactiver les autres boutons
        currentState = QuizState.WAITING_FOR_ANSWER;
        currentPlayerAnswering = -1;
        enableAllPlayerButtons();

        return true;
    }

    /**
     * Trouve l'ID du joueur à partir de la position du bouton
     */
    private int getPlayerIdFromButtonPos(BlockPos buttonPos) {
        for (Map.Entry<Integer, BlockPos> entry : PLAYER_BUTTONS.entrySet()) {
            if (entry.getValue().equals(buttonPos)) {
                return entry.getKey();
            }
        }
        return -1;
    }

    /**
     * Active tous les boutons joueurs (sauf ceux désactivés)
     */
    private void enableAllPlayerButtons() {
        if (level == null)
            return;

        for (int playerId : PLAYER_BUTTONS.keySet()) {
            if (!disabledPlayers.contains(playerId)) {
                BlockPos buttonPos = PLAYER_BUTTONS.get(playerId);
                // Les boutons restent physiquement présents, la logique de désactivation
                // se fait via l'état du quiz
            }
        }
    }

    /**
     * Désactive tous les boutons sauf celui du joueur spécifié
     */
    private void disableOtherPlayerButtons(int activePlayerId) {
        // La désactivation est gérée par l'état du quiz
        // Les autres joueurs ne pourront plus appuyer tant que l'état n'est pas
        // WAITING_FOR_ANSWER
    }

    /**
     * Remet tous les blocs indicateurs à leur état par défaut
     */
    private void resetAllIndicatorBlocks() {
        if (level == null)
            return;

        for (int playerId : PLAYER_BUTTONS.keySet()) {
            if (!disabledPlayers.contains(playerId)) {
                setPlayerIndicatorBlock(playerId, Blocks.BLACK_CONCRETE.defaultBlockState());
            }
        }
    }

    /**
     * Définit l'état du bloc indicateur d'un joueur
     */
    private void setPlayerIndicatorBlock(int playerId, BlockState blockState) {
        if (level == null)
            return;

        BlockPos indicatorPos = PLAYER_INDICATOR_BLOCKS.get(playerId);
        if (indicatorPos != null) {
            level.setBlock(indicatorPos, blockState, 3);
        }
    }

    /**
     * Vérifie si une position correspond à un bouton de joueur
     */
    public boolean isPlayerButton(BlockPos pos) {
        return PLAYER_BUTTONS.containsValue(pos);
    }

    /**
     * Vérifie si une position correspond à un bouton du présentateur
     */
    public boolean isPresenterButton(BlockPos pos) {
        return PRESENTER_VALIDATE_BUTTON.equals(pos) || PRESENTER_INVALIDATE_BUTTON.equals(pos);
    }

    // Getters pour l'état du quiz
    public QuizState getCurrentState() {
        return currentState;
    }

    public int getCurrentPlayerAnswering() {
        return currentPlayerAnswering;
    }

    public Set<Integer> getDisabledPlayers() {
        return new HashSet<>(disabledPlayers);
    }

    /**
     * Event handler pour les interactions avec les boutons
     */
    @SubscribeEvent
    public void onBlockRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos clickedPos = event.getPos();

        // Vérifier si c'est un bouton joueur
        if (isPlayerButton(clickedPos)) {
            if (handlePlayerButtonPress(clickedPos, serverLevel)) {
                event.setCanceled(true); // Empêcher d'autres interactions
            }
            return;
        }

        // Vérifier si c'est un bouton présentateur
        if (isPresenterButton(clickedPos)) {
            boolean handled = false;
            if (PRESENTER_VALIDATE_BUTTON.equals(clickedPos)) {
                handled = handlePresenterValidateButton(clickedPos);
            } else if (PRESENTER_INVALIDATE_BUTTON.equals(clickedPos)) {
                handled = handlePresenterInvalidateButton(clickedPos);
            }

            if (handled) {
                event.setCanceled(true);
            }
        }
    }
}