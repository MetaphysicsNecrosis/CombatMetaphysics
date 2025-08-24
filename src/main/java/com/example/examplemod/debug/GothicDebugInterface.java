package com.example.examplemod.debug;

import com.example.examplemod.api.CombatController;
import com.example.examplemod.api.GothicCombatAPI;
import com.example.examplemod.core.PlayerState;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Debug интерфейс для тестирования Gothic Combat System
 * Предоставляет команды для разработчиков и тестирования
 */
public class GothicDebugInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(GothicDebugInterface.class);
    
    /**
     * Регистрирует debug команды
     */
    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gothicdebug")
            .requires(source -> source.hasPermission(2)) // OP level 2
            
            // Информация о состоянии
            .then(Commands.literal("info")
                .executes(context -> showPlayerInfo(context)))
            
            // Принудительная смена состояния
            .then(Commands.literal("state")
                .then(Commands.argument("new_state", StringArgumentType.string())
                    .executes(context -> forceChangeState(context))))
            
            // Тестовые атаки
            .then(Commands.literal("test")
                .then(Commands.literal("attack")
                    .then(Commands.argument("direction", StringArgumentType.string())
                        .executes(context -> testAttack(context))))
                .then(Commands.literal("defense")
                    .then(Commands.argument("type", StringArgumentType.string())
                        .executes(context -> testDefense(context))))
                .then(Commands.literal("combo")
                    .executes(context -> testCombo(context)))
                .then(Commands.literal("stamina")
                    .executes(context -> testStamina(context))))
            
            // Спавн тестовых мобов
            .then(Commands.literal("spawn")
                .then(Commands.literal("dummy")
                    .executes(context -> spawnTestDummy(context)))
                .then(Commands.literal("enemies")
                    .executes(context -> spawnTestEnemies(context))))
            
            // Очистка
            .then(Commands.literal("clear")
                .executes(context -> clearTestEntities(context)))
            
            // Настройки системы
            .then(Commands.literal("config")
                .then(Commands.literal("auto_stance")
                    .executes(context -> toggleAutoStance(context)))
                .then(Commands.literal("notifications")
                    .executes(context -> toggleNotifications(context)))
                .then(Commands.literal("debug_messages")
                    .executes(context -> toggleDebugMessages(context))))
        );
    }
    
    // === КОМАНДЫ ИНФОРМАЦИИ ===
    
    private static int showPlayerInfo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Получаем информацию из API
            GothicCombatAPI.CombatInfo info = GothicCombatAPI.getCombatInfo(player);
            Map<String, Object> debugInfo = GothicCombatAPI.getDebugInfo(player);
            
            // Основная информация
            player.sendSystemMessage(Component.literal("§6=== Gothic Combat Debug Info ==="));
            player.sendSystemMessage(Component.literal("§eState: §f" + info.getCurrentState().name()));
            player.sendSystemMessage(Component.literal("§eDescription: §f" + info.getStateDescription()));
            player.sendSystemMessage(Component.literal("§eTime in State: §f" + info.getTimeInState() + "ms"));
            player.sendSystemMessage(Component.literal("§eIn Combat: §f" + (info.isInCombat() ? "§aYES" : "§cNO")));
            
            // Физическое состояние
            player.sendSystemMessage(Component.literal("§6--- Physical State ---"));
            player.sendSystemMessage(Component.literal("§eCan Move: §f" + (info.canMove() ? "§aYES" : "§cNO")));
            player.sendSystemMessage(Component.literal("§eVulnerable: §f" + (info.isVulnerable() ? "§cYES" : "§aNO")));
            player.sendSystemMessage(Component.literal("§eMovement Speed: §f" + Math.round(info.getCurrentState().getMovementSpeedMultiplier() * 100) + "%"));
            
            // Выносливость
            player.sendSystemMessage(Component.literal("§6--- Stamina ---"));
            player.sendSystemMessage(Component.literal("§eStamina: §f" + Math.round(info.getCurrentStamina()) + "/" + Math.round(info.getMaxStamina())));
            player.sendSystemMessage(Component.literal("§eLevel: §f" + info.getStaminaLevel().name()));
            player.sendSystemMessage(Component.literal("§eExhausted: §f" + (info.isExhausted() ? "§cYES" : "§aNO")));
            player.sendSystemMessage(Component.literal("§eRegen Rate: §f" + Math.round(info.getCurrentState().getStaminaRegenRate() * 100) + "%"));
            
            // Боевые системы
            player.sendSystemMessage(Component.literal("§6--- Combat Systems ---"));
            player.sendSystemMessage(Component.literal("§eAttacking: §f" + (info.isAttacking() ? "§cYES" : "§aNO")));
            player.sendSystemMessage(Component.literal("§eDefending: §f" + (info.isDefending() ? "§eYES" : "§aNO")));
            
            // Детальная информация
            if (debugInfo.containsKey("gothicAttack")) {
                Map<?, ?> attackInfo = (Map<?, ?>) debugInfo.get("gothicAttack");
                if ((Boolean) attackInfo.get("isAttacking")) {
                    player.sendSystemMessage(Component.literal("§6--- Attack Details ---"));
                    player.sendSystemMessage(Component.literal("§eWeapon: §f" + attackInfo.get("weaponType")));
                    player.sendSystemMessage(Component.literal("§eDirection: §f" + attackInfo.get("direction")));
                    player.sendSystemMessage(Component.literal("§ePhase: §f" + attackInfo.get("currentPhase")));
                    player.sendSystemMessage(Component.literal("§eCombo: §f" + (Boolean.TRUE.equals(attackInfo.get("inCombo")) ? 
                        "x" + attackInfo.get("comboLength") : "None")));
                }
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    // === КОМАНДЫ ТЕСТИРОВАНИЯ ===
    
    private static int testAttack(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String direction = StringArgumentType.getString(context, "direction").toUpperCase();
            
            GothicCombatAPI.AttackDirection attackDir = switch (direction) {
                case "LEFT" -> GothicCombatAPI.AttackDirection.LEFT;
                case "RIGHT" -> GothicCombatAPI.AttackDirection.RIGHT;
                case "TOP" -> GothicCombatAPI.AttackDirection.TOP;
                case "THRUST" -> GothicCombatAPI.AttackDirection.THRUST;
                default -> throw new IllegalArgumentException("Invalid direction: " + direction);
            };
            
            GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, attackDir);
            
            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a✓ " + result.getMessage()));
                if (result.hasExtraInfo()) {
                    player.sendSystemMessage(Component.literal("§e  " + result.getExtraInfo()));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c✗ " + result.getMessage()));
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            context.getSource().sendFailure(Component.literal("§7Valid directions: LEFT, RIGHT, TOP, THRUST"));
            return 0;
        }
    }
    
    private static int testDefense(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String type = StringArgumentType.getString(context, "type").toUpperCase();
            
            GothicCombatAPI.DefenseType defenseType = switch (type) {
                case "BLOCK" -> GothicCombatAPI.DefenseType.BLOCK;
                case "PARRY" -> GothicCombatAPI.DefenseType.PARRY;
                case "DODGE" -> GothicCombatAPI.DefenseType.DODGE;
                default -> throw new IllegalArgumentException("Invalid defense type: " + type);
            };
            
            GothicCombatAPI.CombatResult result = GothicCombatAPI.defend(player, defenseType);
            
            if (result.isSuccess()) {
                player.sendSystemMessage(Component.literal("§a✓ " + result.getMessage()));
                if (result.hasExtraInfo()) {
                    player.sendSystemMessage(Component.literal("§e  " + result.getExtraInfo()));
                }
            } else {
                player.sendSystemMessage(Component.literal("§c✗ " + result.getMessage()));
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            context.getSource().sendFailure(Component.literal("§7Valid types: BLOCK, PARRY, DODGE"));
            return 0;
        }
    }
    
    private static int testCombo(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            player.sendSystemMessage(Component.literal("§6Starting combo test sequence..."));
            
            // Последовательность атак: LEFT → RIGHT → TOP
            GothicCombatAPI.AttackDirection[] combo = {
                GothicCombatAPI.AttackDirection.LEFT,
                GothicCombatAPI.AttackDirection.RIGHT,
                GothicCombatAPI.AttackDirection.TOP
            };
            
            for (int i = 0; i < combo.length; i++) {
                GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, combo[i]);
                
                String message = String.format("§7Step %d/%d: §f%s - %s", 
                    i + 1, combo.length, combo[i].name(), 
                    result.isSuccess() ? "§a✓" : "§c✗");
                    
                player.sendSystemMessage(Component.literal(message));
                
                if (result.hasExtraInfo()) {
                    player.sendSystemMessage(Component.literal("§e  " + result.getExtraInfo()));
                }
                
                // Небольшая пауза между атаками
                try { Thread.sleep(100); } catch (InterruptedException ignored) {}
            }
            
            player.sendSystemMessage(Component.literal("§6Combo test completed!"));
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int testStamina(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            player.sendSystemMessage(Component.literal("§6Starting stamina drain test..."));
            
            // Выполняем серию атак до истощения
            int attacks = 0;
            while (attacks < 20) { // Максимум 20 атак чтобы не зациклиться
                GothicCombatAPI.CombatInfo info = GothicCombatAPI.getCombatInfo(player);
                
                if (info.isExhausted()) {
                    player.sendSystemMessage(Component.literal("§cPlayer exhausted after " + attacks + " attacks!"));
                    player.sendSystemMessage(Component.literal("§7Stamina: " + Math.round(info.getCurrentStamina()) + "/" + Math.round(info.getMaxStamina())));
                    break;
                }
                
                GothicCombatAPI.CombatResult result = GothicCombatAPI.attack(player, GothicCombatAPI.AttackDirection.LEFT);
                attacks++;
                
                player.sendSystemMessage(Component.literal(String.format("§7Attack %d: Stamina %.1f/%.1f (%s)", 
                    attacks, info.getCurrentStamina(), info.getMaxStamina(), info.getStaminaLevel())));
                
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            }
            
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    // === КОМАНДЫ УПРАВЛЕНИЯ СОСТОЯНИЕМ ===
    
    private static int forceChangeState(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String stateString = StringArgumentType.getString(context, "new_state").toUpperCase();
            
            PlayerState newState = PlayerState.valueOf(stateString);
            
            // TODO: Принудительная смена состояния через API
            // Пока просто показываем сообщение
            player.sendSystemMessage(Component.literal("§6Force state change: §f" + newState.name()));
            player.sendSystemMessage(Component.literal("§7(Not implemented yet - requires direct PlayerStateMachine access)"));
            
            return 1;
            
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("§cInvalid state: " + StringArgumentType.getString(context, "new_state")));
            context.getSource().sendFailure(Component.literal("§7Valid states: PEACEFUL, COMBAT_STANCE, ATTACK_WINDUP, etc."));
            return 0;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    // === КОМАНДЫ СПАВНА ===
    
    private static int spawnTestDummy(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Level level = player.level();
            
            Zombie dummy = new Zombie(EntityType.ZOMBIE, level);
            dummy.setPos(player.getX() + 3, player.getY(), player.getZ());
            dummy.setCustomName(Component.literal("§6Gothic Test Dummy"));
            dummy.setCustomNameVisible(true);
            dummy.setNoAi(true);
            dummy.setInvulnerable(false);
            
            if (level.addFreshEntity(dummy)) {
                player.sendSystemMessage(Component.literal("§aTest dummy spawned!"));
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("§cFailed to spawn dummy"));
                return 0;
            }
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int spawnTestEnemies(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Level level = player.level();
            
            int spawned = 0;
            for (int i = 0; i < 5; i++) {
                double angle = (2 * Math.PI * i) / 5;
                double distance = 4.0;
                
                double x = player.getX() + Math.cos(angle) * distance;
                double z = player.getZ() + Math.sin(angle) * distance;
                
                Zombie enemy = new Zombie(EntityType.ZOMBIE, level);
                enemy.setPos(x, player.getY(), z);
                enemy.setCustomName(Component.literal("§cTest Enemy " + (i + 1)));
                enemy.setCustomNameVisible(true);
                
                if (level.addFreshEntity(enemy)) {
                    spawned++;
                }
            }
            
            player.sendSystemMessage(Component.literal("§aSpawned " + spawned + " test enemies!"));
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int clearTestEntities(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            Level level = player.level();
            
            int cleared = 0;
            var nearbyEntities = level.getEntities(player, player.getBoundingBox().inflate(20.0));
            
            for (var entity : nearbyEntities) {
                if (entity instanceof Zombie && entity.hasCustomName()) {
                    String name = entity.getCustomName().getString();
                    if (name.contains("Test Dummy") || name.contains("Test Enemy")) {
                        entity.discard();
                        cleared++;
                    }
                }
            }
            
            player.sendSystemMessage(Component.literal("§7Cleared " + cleared + " test entities"));
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }
    
    // === КОМАНДЫ КОНФИГУРАЦИИ ===
    
    private static int toggleAutoStance(CommandContext<CommandSourceStack> context) {
        CombatController controller = CombatController.getInstance();
        boolean current = controller.isAutoStanceEnabled();
        controller.setAutoStanceEnabled(!current);
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§6Auto-stance: " + (!current ? "§aENABLED" : "§cDISABLED")), true);
        return 1;
    }
    
    private static int toggleNotifications(CommandContext<CommandSourceStack> context) {
        CombatController controller = CombatController.getInstance();
        boolean current = controller.isComboNotificationsEnabled();
        controller.setComboNotificationsEnabled(!current);
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§6Combo notifications: " + (!current ? "§aENABLED" : "§cDISABLED")), true);
        return 1;
    }
    
    private static int toggleDebugMessages(CommandContext<CommandSourceStack> context) {
        CombatController controller = CombatController.getInstance();
        boolean current = controller.isDebugMessagesEnabled();
        controller.setDebugMessagesEnabled(!current);
        
        context.getSource().sendSuccess(() -> Component.literal(
            "§6Debug messages: " + (!current ? "§aENABLED" : "§cDISABLED")), true);
        return 1;
    }
}