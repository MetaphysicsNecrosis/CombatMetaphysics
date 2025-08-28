package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Professional Gothic-style State Machine for Combat System
 * Integrates: Gothic 3-phase attacks + QTE Magic system + Defense mechanics
 * Event-driven architecture with automatic state transitions and timeouts
 */
public class PlayerStateMachine {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerStateMachine.class);
    private static final ScheduledExecutorService SCHEDULER = Executors.newScheduledThreadPool(2);
    
    // Static instances per player
    private static final Map<UUID, PlayerStateMachine> instances = new HashMap<>();
    
    // Core state management
    private final UUID playerId;
    private PlayerState currentState;
    private PlayerState previousState;
    private long stateChangeTime;
    private long stateTimeoutAt;
    private Player playerInstance;
    
    // Event system
    private final Map<PlayerState, Consumer<StateTransitionEvent>> stateHandlers;
    private final Map<String, Consumer<StateEvent>> eventHandlers;
    
    // Integrated systems
    private final StaminaManager staminaManager;
    private final GothicAttackSystem attackSystem;
    private final GothicDefenseSystem defenseSystem;
    private final ResourceManager resourceManager;
    
    // Combat activity tracking
    private long lastCombatActivity;
    private static final long COMBAT_STANCE_TIMEOUT = 5000; // 5 seconds to return to peaceful
    
    // QTE System integration
    private CompletableFuture<QTEResult> currentQTE;
    private String currentComboChain;
    private int comboStep;
    
    // Task management
    private ScheduledFuture<?> attackTickTask;
    
    private PlayerStateMachine(UUID playerId, ResourceManager resourceManager) {
        this.playerId = playerId;
        this.currentState = PlayerState.PEACEFUL;
        this.previousState = PlayerState.PEACEFUL;
        this.stateChangeTime = System.currentTimeMillis();
        this.stateTimeoutAt = Long.MAX_VALUE;
        this.lastCombatActivity = System.currentTimeMillis();
        
        // Initialize integrated systems
        this.staminaManager = new StaminaManager();
        this.attackSystem = new GothicAttackSystem();
        this.defenseSystem = new GothicDefenseSystem();
        this.resourceManager = resourceManager;
        
        // Initialize event system
        this.stateHandlers = new HashMap<>();
        this.eventHandlers = new HashMap<>();
        setupStateHandlers();
        setupEventHandlers();
        
        // Start automatic state management
        startStateManagement();
    }
    
    /**
     * Get or create state machine instance for player
     */
    public static PlayerStateMachine getInstance(UUID playerId, ResourceManager resourceManager) {
        return instances.computeIfAbsent(playerId, id -> new PlayerStateMachine(id, resourceManager));
    }
    
    /**
     * Get existing state machine instance for player
     */
    public static PlayerStateMachine getInstance(UUID playerId) {
        return instances.get(playerId);
    }
    
    /**
     * Remove state machine instance for player
     */
    public static void removeInstance(UUID playerId) {
        PlayerStateMachine instance = instances.remove(playerId);
        if (instance != null) {
            instance.cleanup();
        }
    }
    
    /**
     * Get all active instances (for system monitoring)
     */
    public static Map<UUID, PlayerStateMachine> getAllInstances() {
        return new HashMap<>(instances);
    }
    
    // ===== PROFESSIONAL STATE MACHINE API =====
    
    /**
     * Direct state transition - validates and executes state change
     */
    public StateTransitionResult transitionTo(PlayerState newState) {
        return transitionTo(newState, null, 0);
    }
    
    /**
     * State transition with reason (no timeout)
     */
    public StateTransitionResult transitionTo(PlayerState newState, String reason) {
        return transitionTo(newState, reason, 0);
    }
    
    /**
     * State transition with timeout
     */
    public StateTransitionResult transitionTo(PlayerState newState, String reason, long timeoutMs) {
        // Validate transition
        if (!canTransitionTo(newState)) {
            return StateTransitionResult.failed("Invalid transition from " + currentState + " to " + newState);
        }
        
        // Execute transition
        PlayerState oldState = currentState;
        previousState = currentState;
        currentState = newState;
        stateChangeTime = System.currentTimeMillis();
        
        // Set timeout if specified
        if (timeoutMs > 0) {
            stateTimeoutAt = stateChangeTime + timeoutMs;
        } else {
            // Use default state timeout
            long defaultTimeout = newState.getTypicalDuration();
            stateTimeoutAt = defaultTimeout == Long.MAX_VALUE ? Long.MAX_VALUE : stateChangeTime + defaultTimeout;
        }
        
        // Update combat activity
        if (newState.isCombatState()) {
            lastCombatActivity = System.currentTimeMillis();
        }
        
        // Sync movement restrictions
        PlayerMovementController.syncWithPlayerState(playerId, newState);
        
        // Fire state transition event
        StateTransitionEvent event = new StateTransitionEvent(playerId, oldState, newState, reason);
        fireStateTransitionEvent(event);
        
        LOGGER.debug("Player {} transitioned: {} -> {} ({})", playerId, oldState, newState, reason);
        return StateTransitionResult.success("Transitioned to " + newState);
    }
    
    /**
     * Force transition bypassing validation - use only for interruptions
     */
    public void forceTransition(PlayerState newState, String reason) {
        transitionTo(newState, reason, 0);
        LOGGER.warn("Player {} forced transition to {} - {}", playerId, newState, reason);
    }
    
    /**
     * Check if transition is valid
     */
    private boolean canTransitionTo(PlayerState newState) {
        return currentState.canTransitionTo(newState);
    }
    
    // ===== GOTHIC COMBAT SYSTEM INTEGRATION =====
    
    /**
     * Start Gothic attack with direction
     */
    public GothicAttackSystem.AttackResult startGothicAttack(GothicAttackSystem.AttackDirection direction) {
        if (!canTransitionTo(PlayerState.ATTACK_WINDUP)) {
            return GothicAttackSystem.AttackResult.failed("Cannot start attack from state: " + currentState);
        }
        
        // Ensure we have player instance
        if (playerInstance == null) {
            LOGGER.error("No player instance set for {}", playerId);
            return GothicAttackSystem.AttackResult.failed("No player instance");
        }
        
        // Set current player context for stamina manager
        staminaManager.setCurrentPlayer(playerId);
        
        // Check stamina
        if (!staminaManager.hasStamina(20)) {
            return GothicAttackSystem.AttackResult.failed("Insufficient stamina for attack");
        }
        
        // Transition to windup
        StateTransitionResult result = transitionTo(PlayerState.ATTACK_WINDUP, "Gothic attack: " + direction, 300);
        if (!result.isSuccess()) {
            return GothicAttackSystem.AttackResult.failed(result.getMessage());
        }
        
        // Start attack in Gothic system
        GothicAttackSystem.AttackResult attackResult = attackSystem.executeAttack(direction, staminaManager, playerInstance);
        
        // Consume stamina if attack started successfully
        if (attackResult.isSuccess()) {
            // Stamina уже потрачена через getStaminaData в GothicAttackSystem
            LOGGER.debug("Gothic attack started: {} direction {} ", playerId, direction);
        }
        
        return attackResult;
    }
    
    /**
     * Start defense action
     */
    public GothicDefenseSystem.DefenseActionResult startDefense(GothicDefenseSystem.DefenseType type) {
        PlayerState targetState = switch (type) {
            case BLOCK -> PlayerState.BLOCKING;
            case PARRY -> PlayerState.PARRYING;
            case DODGE -> PlayerState.DODGING;
        };
        
        if (!canTransitionTo(targetState)) {
            return GothicDefenseSystem.DefenseActionResult.failed("Cannot start defense from state: " + currentState);
        }
        
        // Set current player context
        staminaManager.setCurrentPlayer(playerId);
        
        // Check stamina requirements
        int staminaCost = switch (type) {
            case BLOCK -> 10;
            case PARRY -> 15;
            case DODGE -> 25;
        };
        
        if (!staminaManager.hasStamina(staminaCost)) {
            return GothicDefenseSystem.DefenseActionResult.failed("Insufficient stamina for " + type);
        }
        
        // Transition to defense state
        long timeout = switch (type) {
            case PARRY -> 150; // 100-200ms parry window
            case DODGE -> 300; // 300ms i-frames
            case BLOCK -> 1500; // 1.5s block duration
        };
        
        StateTransitionResult result = transitionTo(targetState, "Defense: " + type, timeout);
        if (!result.isSuccess()) {
            return GothicDefenseSystem.DefenseActionResult.failed(result.getMessage());
        }
        
        // Execute defense through Gothic system
        return defenseSystem.executeDefense(type, staminaManager, playerInstance);
    }
    
    // ===== QTE MAGIC SYSTEM INTEGRATION =====
    
    /**
     * Start magic spell preparation
     */
    public boolean startMagicPreparation(String spellName, int manaCost) {
        if (!canTransitionTo(PlayerState.MAGIC_PREPARING)) {
            LOGGER.debug("Cannot prepare magic from state: {}", currentState);
            return false;
        }
        
        // Check mana availability
        if (!resourceManager.canSpendMana(manaCost)) {
            LOGGER.debug("Insufficient mana for spell: {} (cost: {})", spellName, manaCost);
            return false;
        }
        
        // Reserve mana
        if (!resourceManager.tryReserveMana(manaCost, "Magic preparation: " + spellName)) {
            return false;
        }
        
        // Transition to preparation
        StateTransitionResult result = transitionTo(PlayerState.MAGIC_PREPARING, 
            "Preparing spell: " + spellName, 1000);
        
        return result.isSuccess();
    }
    
    /**
     * Start magic casting
     */
    public boolean startMagicCasting(String spellName) {
        if (!canTransitionTo(PlayerState.MAGIC_CASTING)) {
            LOGGER.debug("Cannot cast magic from state: {}", currentState);
            return false;
        }
        
        StateTransitionResult result = transitionTo(PlayerState.MAGIC_CASTING, 
            "Casting spell: " + spellName, 2000);
        
        return result.isSuccess();
    }
    
    /**
     * Start QTE transition for combo magic
     */
    public CompletableFuture<QTEResult> startQTETransition(String qteType, long windowMs) {
        if (!canTransitionTo(PlayerState.QTE_TRANSITION)) {
            return CompletableFuture.completedFuture(QTEResult.failed("Cannot start QTE from state: " + currentState));
        }
        
        // Transition to QTE state
        StateTransitionResult result = transitionTo(PlayerState.QTE_TRANSITION, 
            "QTE: " + qteType, windowMs);
        
        if (!result.isSuccess()) {
            return CompletableFuture.completedFuture(QTEResult.failed(result.getMessage()));
        }
        
        // Start QTE system
        currentQTE = new CompletableFuture<>();
        
        // Auto-timeout QTE
        SCHEDULER.schedule(() -> {
            if (!currentQTE.isDone()) {
                currentQTE.complete(QTEResult.missed("QTE timeout"));
                transitionTo(PlayerState.COMBAT_STANCE, "QTE timeout", 0);
            }
        }, windowMs, TimeUnit.MILLISECONDS);
        
        return currentQTE;
    }
    
    /**
     * Handle QTE input
     */
    public void handleQTEInput(String input, long timing) {
        if (currentState != PlayerState.QTE_TRANSITION || currentQTE == null) {
            return;
        }
        
        // Calculate QTE result based on timing
        QTEResult result = calculateQTEResult(input, timing);
        currentQTE.complete(result);
        
        // Transition based on QTE result
        if (result.isSuccess()) {
            if (result.isPerfect()) {
                transitionTo(PlayerState.MAGIC_CASTING, "Perfect QTE - continue combo", 0);
            } else {
                transitionTo(PlayerState.MAGIC_CASTING, "Good QTE - continue combo", 0);
            }
        } else {
            // Failed QTE - lose reserved mana and return to combat stance
            resourceManager.loseReservedMana("QTE failed");
            transitionTo(PlayerState.COMBAT_STANCE, "QTE failed - combo broken", 0);
        }
    }
    
    private QTEResult calculateQTEResult(String input, long timing) {
        // QTE timing calculation (OSU-style)
        long currentTime = System.currentTimeMillis();
        long targetTime = stateChangeTime + 1000; // 1 second into QTE window
        long deviation = Math.abs(currentTime - targetTime);
        
        if (deviation <= 25) {
            return QTEResult.perfect("Perfect timing!");
        } else if (deviation <= 50) {
            return QTEResult.great("Great timing!");
        } else if (deviation <= 100) {
            return QTEResult.good("Good timing");
        } else if (deviation <= 150) {
            return QTEResult.ok("OK timing");
        } else {
            return QTEResult.missed("Missed QTE");
        }
    }
    
    // ===== EVENT SYSTEM =====
    
    private void setupStateHandlers() {
        // Auto-transitions for Gothic attack phases
        stateHandlers.put(PlayerState.ATTACK_WINDUP, event -> {
            SCHEDULER.schedule(() -> {
                if (currentState == PlayerState.ATTACK_WINDUP) {
                    transitionTo(PlayerState.ATTACK_ACTIVE, "Auto: windup -> active", 200);
                }
            }, 300, TimeUnit.MILLISECONDS);
        });
        
        stateHandlers.put(PlayerState.ATTACK_ACTIVE, event -> {
            // Cancel any previous tick task
            if (attackTickTask != null && !attackTickTask.isDone()) {
                attackTickTask.cancel(false);
            }
            
            // Tick attack system during ACTIVE phase for hit detection
            attackTickTask = SCHEDULER.scheduleAtFixedRate(() -> {
                if (currentState == PlayerState.ATTACK_ACTIVE && attackSystem != null) {
                    attackSystem.tick(); // Process hit detection
                }
            }, 0, 50, TimeUnit.MILLISECONDS); // Every 50ms = 20Hz
            
            SCHEDULER.schedule(() -> {
                if (currentState == PlayerState.ATTACK_ACTIVE) {
                    // Stop ticking when transitioning out of ACTIVE
                    if (attackTickTask != null) {
                        attackTickTask.cancel(false);
                    }
                    transitionTo(PlayerState.ATTACK_RECOVERY, "Auto: active -> recovery", 400);
                }
            }, 200, TimeUnit.MILLISECONDS);
        });
        
        stateHandlers.put(PlayerState.ATTACK_RECOVERY, event -> {
            SCHEDULER.schedule(() -> {
                if (currentState == PlayerState.ATTACK_RECOVERY) {
                    transitionTo(PlayerState.COMBO_WINDOW, "Auto: recovery -> combo window", 600);
                }
            }, 400, TimeUnit.MILLISECONDS);
        });
        
        stateHandlers.put(PlayerState.COMBO_WINDOW, event -> {
            SCHEDULER.schedule(() -> {
                if (currentState == PlayerState.COMBO_WINDOW) {
                    transitionTo(PlayerState.COMBAT_STANCE, "Auto: combo window timeout", 0);
                }
            }, 600, TimeUnit.MILLISECONDS);
        });
    }
    
    private void setupEventHandlers() {
        eventHandlers.put("damage_taken", event -> {
            if (currentState.canBeInterrupted()) {
                forceTransition(PlayerState.STUNNED, "Interrupted by damage");
            }
        });
        
        eventHandlers.put("stamina_exhausted", event -> {
            if (currentState.isAttackState() || currentState.isDefensiveState()) {
                forceTransition(PlayerState.EXHAUSTED, "Stamina exhausted");
            }
        });
    }
    
    private void fireStateTransitionEvent(StateTransitionEvent event) {
        Consumer<StateTransitionEvent> handler = stateHandlers.get(event.getNewState());
        if (handler != null) {
            try {
                handler.accept(event);
            } catch (Exception e) {
                LOGGER.error("Error in state handler for {}: {}", event.getNewState(), e.getMessage());
            }
        }
    }
    
    public void fireEvent(String eventType, Object data) {
        Consumer<StateEvent> handler = eventHandlers.get(eventType);
        if (handler != null) {
            try {
                handler.accept(new StateEvent(eventType, data));
            } catch (Exception e) {
                LOGGER.error("Error in event handler for {}: {}", eventType, e.getMessage());
            }
        }
    }
    
    // ===== AUTOMATIC STATE MANAGEMENT =====
    
    private void startStateManagement() {
        // Auto-timeout states
        SCHEDULER.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            
            // Check state timeout
            if (now >= stateTimeoutAt && stateTimeoutAt != Long.MAX_VALUE) {
                handleStateTimeout();
            }
            
            // Check combat stance timeout
            if (currentState == PlayerState.COMBAT_STANCE && 
                (now - lastCombatActivity) > COMBAT_STANCE_TIMEOUT) {
                transitionTo(PlayerState.PEACEFUL, "Combat stance timeout", 0);
            }
            
        }, 50, 50, TimeUnit.MILLISECONDS);
    }
    
    private void handleStateTimeout() {
        switch (currentState) {
            case PARRYING -> transitionTo(PlayerState.COMBAT_STANCE, "Parry window ended", 0);
            case BLOCKING -> transitionTo(PlayerState.COMBAT_STANCE, "Block duration ended", 0);
            case DODGING -> transitionTo(PlayerState.COMBAT_STANCE, "Dodge completed", 0);
            case STUNNED -> transitionTo(PlayerState.COMBAT_STANCE, "Stun duration ended", 0);
            case MAGIC_PREPARING -> transitionTo(PlayerState.COMBAT_STANCE, "Magic preparation timeout", 0);
            case MAGIC_CASTING -> transitionTo(PlayerState.COMBAT_STANCE, "Magic cast completed", 0);
            case QTE_TRANSITION -> {
                if (currentQTE != null && !currentQTE.isDone()) {
                    currentQTE.complete(QTEResult.missed("QTE timeout"));
                }
                transitionTo(PlayerState.COMBAT_STANCE, "QTE timeout", 0);
            }
            default -> { /* No timeout action needed */ }
        }
        
        stateTimeoutAt = Long.MAX_VALUE;
    }
    
    // ===== COMPATIBILITY METHODS (DEPRECATED) =====
    
    @Deprecated
    public boolean startMeleePreparation(DirectionalAttackSystem.AttackDirection direction) {
        GothicAttackSystem.AttackDirection gothicDir = convertToGothicDirection(direction);
        return startGothicAttack(gothicDir).isSuccess();
    }
    
    @Deprecated
    public DirectionalAttackSystem.AttackResult executeMeleeAttack() {
        // Stub - Gothic system handles attacks automatically
        return new DirectionalAttackSystem.AttackResult(true, "Gothic attack executed", 1, 10.0f, false);
    }
    
    @Deprecated
    public boolean startDefensiveAction(DefensiveActionsManager.DefensiveType type) {
        GothicDefenseSystem.DefenseType gothicType = convertToGothicDefenseType(type);
        return startDefense(gothicType).isSuccess();
    }
    
    @Deprecated
    public boolean executeDefense(DefensiveActionsManager.DefensiveType type) {
        return startDefensiveAction(type);
    }
    
    private GothicAttackSystem.AttackDirection convertToGothicDirection(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> GothicAttackSystem.AttackDirection.LEFT;
            case RIGHT_ATTACK -> GothicAttackSystem.AttackDirection.RIGHT;
            case TOP_ATTACK -> GothicAttackSystem.AttackDirection.TOP;
            case THRUST_ATTACK -> GothicAttackSystem.AttackDirection.THRUST;
        };
    }
    
    private GothicDefenseSystem.DefenseType convertToGothicDefenseType(DefensiveActionsManager.DefensiveType type) {
        return switch (type) {
            case BLOCK -> GothicDefenseSystem.DefenseType.BLOCK;
            case PARRY -> GothicDefenseSystem.DefenseType.PARRY;
            case DODGE -> GothicDefenseSystem.DefenseType.DODGE;
        };
    }
    
    // ===== CLEANUP AND UTILITIES =====
    
    /**
     * Clean up resources when player disconnects
     */
    public void cleanup() {
        if (currentQTE != null && !currentQTE.isDone()) {
            currentQTE.complete(QTEResult.failed("Player disconnected"));
        }
        stateHandlers.clear();
        eventHandlers.clear();
        LOGGER.debug("Cleaned up state machine for player {}", playerId);
    }
    
    /**
     * Get debug information about current state
     */
    public Map<String, Object> getDebugInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("playerId", playerId.toString());
        info.put("currentState", currentState.name());
        info.put("previousState", previousState.name());
        info.put("timeInState", getTimeInCurrentState());
        info.put("timeoutIn", stateTimeoutAt == Long.MAX_VALUE ? "never" : (stateTimeoutAt - System.currentTimeMillis()));
        info.put("timeSinceLastCombatActivity", System.currentTimeMillis() - lastCombatActivity);
        
        // Gothic systems
        info.put("stamina", staminaManager.getCurrentStamina());
        info.put("maxStamina", staminaManager.getMaxStamina());
        info.put("mana", resourceManager.getCurrentMana());
        info.put("reservedMana", resourceManager.getReservedMana());
        info.put("maxMana", resourceManager.getMaxMana());
        
        // QTE info
        if (currentQTE != null) {
            info.put("qteActive", !currentQTE.isDone());
            info.put("comboChain", currentComboChain);
            info.put("comboStep", comboStep);
        }
        
        return info;
    }
    
    // ===== GETTERS =====
    
    public PlayerState getCurrentState() { return currentState; }
    public PlayerState getPreviousState() { return previousState; }
    public long getStateChangeTime() { return stateChangeTime; }
    public long getTimeInCurrentState() { return System.currentTimeMillis() - stateChangeTime; }
    public UUID getPlayerId() { return playerId; }
    public boolean isActive() { return currentState.isActive(); }
    public boolean canStartNewAction() { return currentState == PlayerState.PEACEFUL || currentState == PlayerState.COMBAT_STANCE; }
    public StaminaManager getStaminaManager() { return staminaManager; }
    public ResourceManager getResourceManager() { return resourceManager; }
    public GothicAttackSystem getAttackSystem() { return attackSystem; }
    public GothicDefenseSystem getDefenseSystem() { return defenseSystem; }
    
    // Compatibility method for UI
    public String getCurrentAction() {
        return currentState.name() + " (Gothic System)";
    }
    
    // Legacy compatibility methods
    public void setPlayerInstance(Player player) {
        this.playerInstance = player;
        // Передаем Player объект в GothicAttackSystem для коллайдеров
        GothicAttackSystem.setPlayerInstance(this.playerId, player);
    }
    
    public Player getPlayerInstance() {
        return playerInstance;
    }
    
    public boolean wasInterrupted(UUID playerId) {
        return currentState == PlayerState.INTERRUPTED;
    }
    
    public long getQteStartTime(UUID playerId) {
        return stateChangeTime; // Return state change time as QTE start approximation
    }
    
    public boolean interrupt(InterruptionSystem.InterruptionType type, String reason) {
        forceTransition(PlayerState.INTERRUPTED, "Interrupted: " + reason);
        return true;
    }
    
    // ===== INNER CLASSES =====
    
    public static class StateTransitionResult {
        private final boolean success;
        private final String message;
        
        private StateTransitionResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
        
        public static StateTransitionResult success(String message) {
            return new StateTransitionResult(true, message);
        }
        
        public static StateTransitionResult failed(String message) {
            return new StateTransitionResult(false, message);
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }
    
    public static class StateTransitionEvent {
        private final UUID playerId;
        private final PlayerState oldState;
        private final PlayerState newState;
        private final String reason;
        private final long timestamp;
        
        public StateTransitionEvent(UUID playerId, PlayerState oldState, PlayerState newState, String reason) {
            this.playerId = playerId;
            this.oldState = oldState;
            this.newState = newState;
            this.reason = reason;
            this.timestamp = System.currentTimeMillis();
        }
        
        public UUID getPlayerId() { return playerId; }
        public PlayerState getOldState() { return oldState; }
        public PlayerState getNewState() { return newState; }
        public String getReason() { return reason; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class StateEvent {
        private final String type;
        private final Object data;
        private final long timestamp;
        
        public StateEvent(String type, Object data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getType() { return type; }
        public Object getData() { return data; }
        public long getTimestamp() { return timestamp; }
    }
    
    public static class QTEResult {
        private final boolean success;
        private final boolean perfect;
        private final String message;
        private final float efficiency;
        
        private QTEResult(boolean success, boolean perfect, String message, float efficiency) {
            this.success = success;
            this.perfect = perfect;
            this.message = message;
            this.efficiency = efficiency;
        }
        
        public static QTEResult perfect(String message) {
            return new QTEResult(true, true, message, 1.0f);
        }
        
        public static QTEResult great(String message) {
            return new QTEResult(true, false, message, 0.9f);
        }
        
        public static QTEResult good(String message) {
            return new QTEResult(true, false, message, 0.7f);
        }
        
        public static QTEResult ok(String message) {
            return new QTEResult(true, false, message, 0.5f);
        }
        
        public static QTEResult missed(String message) {
            return new QTEResult(false, false, message, 0.0f);
        }
        
        public static QTEResult failed(String message) {
            return new QTEResult(false, false, message, 0.0f);
        }
        
        public boolean isSuccess() { return success; }
        public boolean isPerfect() { return perfect; }
        public String getMessage() { return message; }
        public float getEfficiency() { return efficiency; }
    }
}