package com.example.examplemod.client.input;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.client.CombatClientManager;
import com.example.examplemod.core.DirectionalAttackSystem;
import com.example.examplemod.core.DefensiveActionsManager;
import com.example.examplemod.core.PlayerStateMachine;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

import java.util.UUID;

/**
 * Клиентский обработчик ввода для combat системы
 * Согласно CLAUDE.md: Gothic-style направленные атаки с удержанием кнопки
 */
@EventBusSubscriber(modid = CombatMetaphysics.MODID, value = Dist.CLIENT)
public class CombatInputHandler {
    
    // Кнопки для combat действий
    public static final KeyMapping MAGIC_CAST_KEY = new KeyMapping(
        "key.combatmetaphysics.magic_cast",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_R, // R для магии
        "key.categories.combatmetaphysics"
    );
    
    public static final KeyMapping MELEE_ATTACK_KEY = new KeyMapping(
        "key.combatmetaphysics.melee_attack",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F, // F для ближнего боя
        "key.categories.combatmetaphysics"
    );
    
    public static final KeyMapping PARRY_KEY = new KeyMapping(
        "key.combatmetaphysics.parry",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_G, // G для парирования
        "key.categories.combatmetaphysics"
    );
    
    public static final KeyMapping BLOCK_KEY = new KeyMapping(
        "key.combatmetaphysics.block",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_V, // V для блокирования
        "key.categories.combatmetaphysics"
    );
    
    public static final KeyMapping DODGE_KEY = new KeyMapping(
        "key.combatmetaphysics.dodge",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_C, // C для уклонения
        "key.categories.combatmetaphysics"
    );
    
    // Состояние кнопок
    private static boolean meleeKeyDown = false;
    private static long meleeKeyPressTime = 0;
    private static DirectionalAttackSystem.AttackDirection currentDirection = DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(MAGIC_CAST_KEY);
        event.register(MELEE_ATTACK_KEY);
        event.register(PARRY_KEY);
        event.register(BLOCK_KEY);
        event.register(DODGE_KEY);
        
        CombatMetaphysics.LOGGER.info("Combat input keys registered");
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        UUID playerId = mc.player.getUUID();
        PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(playerId);
        
        // Обработка нажатий клавиш
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleKeyPress(event.getKey(), playerId, stateMachine);
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            handleKeyRelease(event.getKey(), playerId, stateMachine);
        }
    }
    
    private static void handleKeyPress(int key, UUID playerId, PlayerStateMachine stateMachine) {
        try {
            // Магическое заклинание
            if (key == MAGIC_CAST_KEY.getKey().getValue()) {
                handleMagicCast(playerId, stateMachine);
            }
            
            // Начало заряжания атаки ближнего боя (Gothic-style)
            else if (key == MELEE_ATTACK_KEY.getKey().getValue()) {
                handleMeleeAttackStart(playerId, stateMachine);
            }
            
            // Парирование
            else if (key == PARRY_KEY.getKey().getValue()) {
                handleDefensiveAction(playerId, stateMachine, DefensiveActionsManager.DefensiveType.PARRY);
            }
            
            // Блокирование
            else if (key == BLOCK_KEY.getKey().getValue()) {
                handleDefensiveAction(playerId, stateMachine, DefensiveActionsManager.DefensiveType.BLOCK);
            }
            
            // Уклонение
            else if (key == DODGE_KEY.getKey().getValue()) {
                handleDefensiveAction(playerId, stateMachine, DefensiveActionsManager.DefensiveType.DODGE);
            }
            
            // Направления атак (при удержании кнопки атаки)
            else if (meleeKeyDown) {
                handleDirectionInput(key, playerId, stateMachine);
            }
            
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Error handling key press: {}", e.getMessage());
        }
    }
    
    private static void handleKeyRelease(int key, UUID playerId, PlayerStateMachine stateMachine) {
        try {
            // Отпускание кнопки атаки ближнего боя - выполнение атаки
            if (key == MELEE_ATTACK_KEY.getKey().getValue() && meleeKeyDown) {
                handleMeleeAttackExecute(playerId, stateMachine);
            }
            
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Error handling key release: {}", e.getMessage());
        }
    }
    
    private static void handleMagicCast(UUID playerId, PlayerStateMachine stateMachine) {
        if (stateMachine.getCurrentState().canTransitionTo(com.example.examplemod.core.PlayerState.MAGIC_PREPARING)) {
            boolean success = stateMachine.startMagicPreparation("client_spell", 25f);
            
            CombatMetaphysics.LOGGER.info("Magic cast initiated: {} for player {}", 
                success ? "SUCCESS" : "FAILED", playerId);
                
            if (success) {
                // Можно добавить звуковые эффекты или визуальные индикаторы
                showCombatMessage("Подготовка заклинания...");
            } else {
                showCombatMessage("Невозможно применить магию в данный момент");
            }
        }
    }
    
    private static void handleMeleeAttackStart(UUID playerId, PlayerStateMachine stateMachine) {
        if (stateMachine.getCurrentState().canTransitionTo(com.example.examplemod.core.PlayerState.MELEE_PREPARING)) {
            meleeKeyDown = true;
            meleeKeyPressTime = System.currentTimeMillis();
            currentDirection = DirectionalAttackSystem.AttackDirection.LEFT_ATTACK; // По умолчанию
            
            boolean success = stateMachine.startMeleePreparation(currentDirection);
            
            CombatMetaphysics.LOGGER.info("Melee charge started: {} for player {}", 
                success ? "SUCCESS" : "FAILED", playerId);
                
            if (success) {
                showCombatMessage("Заряжание атаки... (используйте WASD для выбора направления)");
            } else {
                meleeKeyDown = false;
                showCombatMessage("Невозможно атаковать в данный момент");
            }
        }
    }
    
    private static void handleMeleeAttackExecute(UUID playerId, PlayerStateMachine stateMachine) {
        meleeKeyDown = false;
        long chargeTime = System.currentTimeMillis() - meleeKeyPressTime;
        
        if (stateMachine.getCurrentState().isMeleeState()) {
            var result = stateMachine.executeMeleeAttack();
            
            CombatMetaphysics.LOGGER.info("Melee attack executed: {} (damage: {}, charge time: {}ms) for player {}", 
                result.isSuccess() ? "SUCCESS" : "FAILED", result.getDamage(), chargeTime, playerId);
                
            if (result.isSuccess()) {
                String chargeIndicator = chargeTime > 2000 ? " [ЗАРЯЖЕН]" : "";
                showCombatMessage(String.format("Атака выполнена! Урон: %.1f%s", result.getDamage(), chargeIndicator));
            } else {
                showCombatMessage("Атака не удалась: " + result.getMessage());
            }
        }
    }
    
    private static void handleDefensiveAction(UUID playerId, PlayerStateMachine stateMachine, 
                                            DefensiveActionsManager.DefensiveType type) {
        if (stateMachine.getCurrentState().canTransitionTo(com.example.examplemod.core.PlayerState.DEFENSIVE_ACTION)) {
            boolean success = stateMachine.startDefensiveAction(type);
            
            CombatMetaphysics.LOGGER.info("Defensive action ({}): {} for player {}", 
                type, success ? "SUCCESS" : "FAILED", playerId);
                
            if (success) {
                String actionName = switch (type) {
                    case PARRY -> "Парирование";
                    case BLOCK -> "Блокирование";
                    case DODGE -> "Уклонение";
                };
                showCombatMessage(actionName + " активировано!");
            } else {
                showCombatMessage("Невозможно использовать защиту в данный момент");
            }
        }
    }
    
    private static void handleDirectionInput(int key, UUID playerId, PlayerStateMachine stateMachine) {
        DirectionalAttackSystem.AttackDirection newDirection = null;
        
        // WASD для выбора направления атаки
        if (key == GLFW.GLFW_KEY_W) {
            newDirection = DirectionalAttackSystem.AttackDirection.TOP_ATTACK;
        } else if (key == GLFW.GLFW_KEY_A) {
            newDirection = DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
        } else if (key == GLFW.GLFW_KEY_S) {
            newDirection = DirectionalAttackSystem.AttackDirection.THRUST_ATTACK;
        } else if (key == GLFW.GLFW_KEY_D) {
            newDirection = DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK;
        }
        
        if (newDirection != null && newDirection != currentDirection) {
            currentDirection = newDirection;
            
            // Обновляем направление в системе атак
            if (stateMachine.getCurrentState().isMeleeState()) {
                // Можно отменить текущую атаку и начать новую с другим направлением
                stateMachine.getAttackSystem().cancelCharging(playerId);
                stateMachine.getAttackSystem().startCharging(playerId, newDirection);
                
                String directionName = switch (newDirection) {
                    case LEFT_ATTACK -> "Левая атака (быстрая)";
                    case RIGHT_ATTACK -> "Правая атака (средняя)";
                    case TOP_ATTACK -> "Верхняя атака (мощная)";
                    case THRUST_ATTACK -> "Колющая атака (пробивающая)";
                };
                
                showCombatMessage("Направление: " + directionName);
            }
        }
    }
    
    /**
     * Отображает сообщение о combat действии в чате
     */
    private static void showCombatMessage(String message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(
                net.minecraft.network.chat.Component.literal("[Combat] " + message), 
                true // Action bar
            );
        }
    }
    
    /**
     * Получает прогресс заряжания текущей атаки (0.0 - 1.0)
     */
    public static float getMeleeChargeProgress() {
        if (!meleeKeyDown || meleeKeyPressTime == 0) return 0.0f;
        
        long elapsed = System.currentTimeMillis() - meleeKeyPressTime;
        return Math.min(1.0f, elapsed / 2000.0f); // 2 секунды для полной зарядки
    }
    
    /**
     * Проверяет, заряжается ли атака в данный момент
     */
    public static boolean isMeleeCharging() {
        return meleeKeyDown;
    }
    
    /**
     * Получает текущее направление атаки
     */
    public static DirectionalAttackSystem.AttackDirection getCurrentDirection() {
        return currentDirection;
    }
}