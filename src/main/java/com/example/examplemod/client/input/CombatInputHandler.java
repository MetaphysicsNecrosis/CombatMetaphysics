package com.example.examplemod.client.input;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.api.CombatController;
import com.example.examplemod.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Professional Combat Input Handler for Gothic Combat + QTE Magic System
 * Handles all player input for combat actions through the new state machine API
 */
public class CombatInputHandler {
    
    // Combat state tracking
    private static boolean meleeKeyDown = false;
    private static long meleeKeyPressTime = 0;
    private static DirectionalAttackSystem.AttackDirection currentDirection = DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
    
    /**
     * Handle key press events for combat actions
     */
    public static void handleKeyPress(int key, Minecraft mc, CombatController controller) {
        if (mc.player == null) return;
        
        UUID playerId = mc.player.getUUID();
        PlayerStateMachine stateMachine = controller.getStateMachine(playerId);
        if (stateMachine == null) return;
        
        switch (key) {
            case GLFW.GLFW_KEY_Q -> { // Left mouse button alternative
                handleGothicAttackStart(playerId, stateMachine, GothicAttackSystem.AttackDirection.LEFT);
            }
            case GLFW.GLFW_KEY_E -> { // Right mouse button alternative  
                handleGothicAttackStart(playerId, stateMachine, GothicAttackSystem.AttackDirection.RIGHT);
            }
            case GLFW.GLFW_KEY_R -> { // Top attack
                handleGothicAttackStart(playerId, stateMachine, GothicAttackSystem.AttackDirection.TOP);
            }
            case GLFW.GLFW_KEY_F -> { // Thrust attack
                handleGothicAttackStart(playerId, stateMachine, GothicAttackSystem.AttackDirection.THRUST);
            }
            case GLFW.GLFW_KEY_LEFT_SHIFT -> { // Block
                handleGothicDefense(playerId, stateMachine, GothicDefenseSystem.DefenseType.BLOCK);
            }
            case GLFW.GLFW_KEY_C -> { // Parry
                handleGothicDefense(playerId, stateMachine, GothicDefenseSystem.DefenseType.PARRY);
            }
            case GLFW.GLFW_KEY_SPACE -> { // Dodge
                handleGothicDefense(playerId, stateMachine, GothicDefenseSystem.DefenseType.DODGE);
            }
            case GLFW.GLFW_KEY_1 -> { // Magic spell 1
                handleMagicCast(playerId, stateMachine, "fireball", 40);
            }
            case GLFW.GLFW_KEY_2 -> { // Magic spell 2
                handleMagicCast(playerId, stateMachine, "heal", 30);
            }
            case GLFW.GLFW_KEY_3 -> { // QTE Combo chain
                handleQTECombo(playerId, stateMachine, "fire_storm_combo");
            }
        }
    }
    
    /**
     * Handle key release events
     */
    public static void handleKeyRelease(int key, Minecraft mc, CombatController controller) {
        if (mc.player == null) return;
        
        UUID playerId = mc.player.getUUID();
        PlayerStateMachine stateMachine = controller.getStateMachine(playerId);
        if (stateMachine == null) return;
        
        // No specific release handling needed for Gothic system
        // All attacks are automatic 3-phase sequences
    }
    
    // ===== GOTHIC COMBAT HANDLERS =====
    
    /**
     * Handle Gothic attack initiation
     */
    private static void handleGothicAttackStart(UUID playerId, PlayerStateMachine stateMachine, 
                                               GothicAttackSystem.AttackDirection direction) {
        // Transition to combat stance if in peaceful state
        if (stateMachine.getCurrentState() == PlayerState.PEACEFUL) {
            stateMachine.transitionTo(PlayerState.COMBAT_STANCE, "Preparing for Gothic attack");
        }
        
        // Start Gothic attack
        GothicAttackSystem.AttackResult result = stateMachine.startGothicAttack(direction);
        
        CombatMetaphysics.LOGGER.info("Gothic attack {} initiated: {} for player {}", 
            direction, result.isSuccess() ? "SUCCESS" : "FAILED", playerId);
            
        if (result.isSuccess()) {
            showCombatMessage("Gothic Attack: " + direction + "!");
            
            if (result.isCombo()) {
                showCombatMessage("COMBO x" + result.getComboLength() + "!");
            }
        } else {
            showCombatMessage("Cannot attack: " + result.getMessage());
        }
    }
    
    /**
     * Handle Gothic defense initiation
     */
    private static void handleGothicDefense(UUID playerId, PlayerStateMachine stateMachine,
                                           GothicDefenseSystem.DefenseType defenseType) {
        // Transition to combat stance if in peaceful state
        if (stateMachine.getCurrentState() == PlayerState.PEACEFUL) {
            stateMachine.transitionTo(PlayerState.COMBAT_STANCE, "Preparing for Gothic defense");
        }
        
        // Start Gothic defense
        GothicDefenseSystem.DefenseActionResult result = stateMachine.startDefense(defenseType);
        
        CombatMetaphysics.LOGGER.info("Gothic defense {} initiated: {} for player {}", 
            defenseType, result.isSuccess() ? "SUCCESS" : "FAILED", playerId);
            
        if (result.isSuccess()) {
            String defenseName = switch (defenseType) {
                case BLOCK -> "Gothic Block";
                case PARRY -> "Gothic Parry";
                case DODGE -> "Gothic Dodge";
            };
            showCombatMessage(defenseName + " activated!");
            
            if (result.isPerfectTiming()) {
                showCombatMessage("PERFECT TIMING!");
            }
        } else {
            showCombatMessage("Cannot defend: " + result.getMessage());
        }
    }
    
    // ===== QTE MAGIC SYSTEM HANDLERS =====
    
    /**
     * Handle magic spell casting
     */
    private static void handleMagicCast(UUID playerId, PlayerStateMachine stateMachine, 
                                       String spellName, int manaCost) {
        // Transition to combat stance if needed
        if (stateMachine.getCurrentState() == PlayerState.PEACEFUL) {
            stateMachine.transitionTo(PlayerState.COMBAT_STANCE, "Preparing magic");
        }
        
        // Start magic preparation
        boolean preparationSuccess = stateMachine.startMagicPreparation(spellName, manaCost);
        
        if (preparationSuccess) {
            showCombatMessage("Preparing spell: " + spellName);
            
            // Auto-transition to casting after preparation
            scheduleDelayedAction(() -> {
                if (stateMachine.getCurrentState() == PlayerState.MAGIC_PREPARING) {
                    boolean castingSuccess = stateMachine.startMagicCasting(spellName);
                    if (castingSuccess) {
                        showCombatMessage("Casting " + spellName + "...");
                    }
                }
            }, 1000);
        } else {
            showCombatMessage("Cannot cast " + spellName + " (insufficient mana or invalid state)");
        }
    }
    
    /**
     * Handle QTE combo chain initiation
     */
    private static void handleQTECombo(UUID playerId, PlayerStateMachine stateMachine, String comboName) {
        // Start QTE transition
        var qteFuture = stateMachine.startQTETransition(comboName, 1200);
        
        if (qteFuture != null) {
            showCombatMessage("QTE COMBO: " + comboName + " - Press X when prompted!");
            
            // Handle QTE completion
            qteFuture.thenAccept(result -> {
                if (result.isSuccess()) {
                    if (result.isPerfect()) {
                        showCombatMessage("PERFECT QTE! +50% damage, -20% mana cost");
                    } else {
                        showCombatMessage("QTE Success: " + result.getMessage());
                    }
                } else {
                    showCombatMessage("QTE Failed: " + result.getMessage());
                }
            });
        } else {
            showCombatMessage("Cannot start QTE combo from current state");
        }
    }
    
    /**
     * Handle QTE input (called from external QTE system)
     */
    public static void handleQTEInput(UUID playerId, String input, long timing) {
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine != null) {
            stateMachine.handleQTEInput(input, timing);
        }
    }
    
    // ===== UTILITY METHODS =====
    
    /**
     * Show combat message to player
     */
    private static void showCombatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Combat] " + message), false);
        }
    }
    
    /**
     * Schedule delayed action (utility for magic casting flow)
     */
    private static void scheduleDelayedAction(Runnable action, long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                action.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    // ===== LEGACY COMPATIBILITY (DEPRECATED) =====
    
    /**
     * @deprecated Use Gothic attack system instead
     */
    @Deprecated
    private static void handleMeleeAttackStart(UUID playerId, PlayerStateMachine stateMachine) {
        // Redirect to Gothic system with default LEFT direction
        handleGothicAttackStart(playerId, stateMachine, GothicAttackSystem.AttackDirection.LEFT);
    }
    
    /**
     * @deprecated Gothic system handles execution automatically
     */
    @Deprecated 
    private static void handleMeleeAttackExecute(UUID playerId, PlayerStateMachine stateMachine) {
        // No-op - Gothic system handles attack execution through automatic state transitions
        CombatMetaphysics.LOGGER.debug("Legacy executeMeleeAttack called - Gothic system handles this automatically");
    }
    
    /**
     * @deprecated Use Gothic defense system instead
     */
    @Deprecated
    private static void handleDefensiveAction(UUID playerId, PlayerStateMachine stateMachine, 
                                            DefensiveActionsManager.DefensiveType type) {
        // Convert and redirect to Gothic system
        GothicDefenseSystem.DefenseType gothicType = switch (type) {
            case BLOCK -> GothicDefenseSystem.DefenseType.BLOCK;
            case PARRY -> GothicDefenseSystem.DefenseType.PARRY;
            case DODGE -> GothicDefenseSystem.DefenseType.DODGE;
        };
        
        handleGothicDefense(playerId, stateMachine, gothicType);
    }
    
    // ===== COMPATIBILITY METHODS FOR UI =====
    
    public static boolean isMeleeCharging() {
        return false; // Gothic system doesn't use charging - instant attacks
    }
    
    public static DirectionalAttackSystem.AttackDirection getCurrentDirection() {
        return currentDirection; // Keep for UI compatibility
    }
    
    public static float getMeleeChargeProgress() {
        return 0.0f; // No charging in Gothic system
    }
    
    /**
     * @deprecated Direction input handled directly in Gothic attack methods
     */
    @Deprecated
    private static void handleDirectionInput(int key, UUID playerId, PlayerStateMachine stateMachine) {
        // No longer needed - Gothic attacks specify direction directly
        CombatMetaphysics.LOGGER.debug("Legacy handleDirectionInput called - Gothic system uses direct direction specification");
    }
}