package com.example.examplemod.commands;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.client.qte.QTEType;
import com.example.examplemod.client.qte.QTEClientManager;
import com.example.examplemod.client.qte.OSUStyleQTEEvent;
import com.example.examplemod.client.CombatHUDRenderer;
import com.example.examplemod.commands.TestMeteorStrikeCommand;
// Старые импорты удалены - используем новую Event-Driven систему
import com.example.examplemod.server.CombatServerManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class CombatCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register meteor strike test command
        TestMeteorStrikeCommand.register(dispatcher);
        TestMeteorStrikeCommand.registerDebugCommand(dispatcher);
        
        dispatcher.register(Commands.literal("combatmetaphysics")
            .then(Commands.literal("debug")
                .then(Commands.literal("state")
                    .executes(CombatCommands::debugState)
                )
                .then(Commands.literal("resources")
                    .executes(CombatCommands::debugResources)
                )
            )
            .then(Commands.literal("test")
                .then(Commands.literal("qte")
                    .then(Commands.literal("sequence")
                        .executes(ctx -> testQTE(ctx, QTEType.SEQUENCE))
                    )
                    .then(Commands.literal("timing")
                        .executes(ctx -> testQTE(ctx, QTEType.TIMING))
                    )
                    .then(Commands.literal("rapid")
                        .executes(ctx -> testQTE(ctx, QTEType.RAPID))
                    )
                    .then(Commands.literal("precision")
                        .executes(ctx -> testQTE(ctx, QTEType.PRECISION))
                    )
                )
                .then(Commands.literal("osuqte")
                    .then(Commands.literal("sequence")
                        .executes(ctx -> testOSUQTE(ctx, OSUStyleQTEEvent.QTEType.SEQUENCE))
                    )
                    .then(Commands.literal("timing")
                        .executes(ctx -> testOSUQTE(ctx, OSUStyleQTEEvent.QTEType.TIMING))
                    )
                    .then(Commands.literal("rapid")
                        .executes(ctx -> testOSUQTE(ctx, OSUStyleQTEEvent.QTEType.RAPID))
                    )
                    .then(Commands.literal("precision")
                        .executes(ctx -> testOSUQTE(ctx, OSUStyleQTEEvent.QTEType.PRECISION))
                    )
                )
                .then(Commands.literal("mana")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                        .executes(CombatCommands::testManaReserve)
                    )
                )
                .then(Commands.literal("hud")
                    .executes(CombatCommands::testHUD)
                )
                .then(Commands.literal("reset")
                    .executes(CombatCommands::resetState)
                )
                .then(Commands.literal("magic")
                    .then(Commands.literal("prepare")
                        .then(Commands.argument("cost", IntegerArgumentType.integer(1, 100))
                            .executes(CombatCommands::testMagicPrepare)
                        )
                    )
                    .then(Commands.literal("cast")
                        .executes(CombatCommands::testMagicCast)
                    )
                )
                .then(Commands.literal("melee")
                    .then(Commands.literal("left")
                        .executes(ctx -> testMeleeAttack(ctx, "LEFT_ATTACK"))
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> testMeleeAttack(ctx, "RIGHT_ATTACK"))
                    )
                    .then(Commands.literal("top")
                        .executes(ctx -> testMeleeAttack(ctx, "TOP_ATTACK"))
                    )
                    .then(Commands.literal("thrust")
                        .executes(ctx -> testMeleeAttack(ctx, "THRUST_ATTACK"))
                    )
                )
                .then(Commands.literal("defense")
                    .then(Commands.literal("parry")
                        .executes(ctx -> testDefense(ctx, "PARRY"))
                    )
                    .then(Commands.literal("block")
                        .executes(ctx -> testDefense(ctx, "BLOCK"))
                    )
                    .then(Commands.literal("dodge")
                        .executes(ctx -> testDefense(ctx, "DODGE"))
                    )
                )
                .then(Commands.literal("interrupt")
                    .then(Commands.literal("physical")
                        .executes(ctx -> testInterrupt(ctx, "PHYSICAL_HIT"))
                    )
                    .then(Commands.literal("magical")
                        .executes(ctx -> testInterrupt(ctx, "MAGICAL_HIT"))
                    )
                    .then(Commands.literal("aoe")
                        .executes(ctx -> testInterrupt(ctx, "MASS_AOE_HIT"))
                    )
                )
            )
        );
    }
    
    private static int debugState(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Используем серверный менеджер
                CombatServerManager.getInstance().debugPrintState(playerId);
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("Debug info printed to console"), false);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int debugResources(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Используем серверный менеджер для сохранения состояния
                var resources = CombatServerManager.getInstance().getPlayerResources(playerId);
                
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Mana: %.1f/%.1f (reserved: %.1f), Stamina: %.1f/%.1f",
                        resources.getCurrentMana(), resources.getMaxMana(), resources.getReservedMana(),
                        resources.getCurrentStamina(), resources.getMaxStamina())
                ), false);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int testQTE(CommandContext<CommandSourceStack> context, QTEType type) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Создаем тестовую QTE на сервере (пока без визуализации)
                UUID qteId = UUID.randomUUID();
                List<Integer> keys = switch (type) {
                    case SEQUENCE -> Arrays.asList(87, 65, 83, 68); // W, A, S, D keycodes
                    case TIMING, PRECISION -> Arrays.asList(32); // SPACE
                    case RAPID -> Arrays.asList(69); // E
                };
                
                long duration = switch (type) {
                    case SEQUENCE -> 5000;
                    case TIMING, PRECISION -> 3000;
                    case RAPID -> 4000;
                };
                
                // Логируем создание QTE
                CombatMetaphysics.LOGGER.info("Created {} QTE for player {} with keys {} and duration {}ms", 
                    type, playerId, keys, duration);
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("Started " + type.name() + " QTE test (check console for details)"), false);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int testManaReserve(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                int amount = IntegerArgumentType.getInteger(context, "amount");
                UUID playerId = player.getUUID();
                
                // Используем серверный менеджер для сохранения состояния
                var resources = CombatServerManager.getInstance().getPlayerResources(playerId);
                boolean success = resources.tryReserveMana(amount, "test command");
                
                context.getSource().sendSuccess(() -> Component.literal(
                    success ? 
                        String.format("Reserved %.1f mana. Current: %.1f, Reserved: %.1f", 
                            (float)amount, resources.getCurrentMana(), resources.getReservedMana()) :
                        String.format("Failed to reserve %.1f mana. Available: %.1f", 
                            (float)amount, resources.getCurrentMana())
                ), false);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int testHUD(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Сбрасываем тестовые ресурсы для обновления
                CombatHUDRenderer.resetTestResources();
                
                // Принудительно синхронизируем с серверными данными
                CombatHUDRenderer.syncWithServerData(playerId);
                
                context.getSource().sendSuccess(() -> 
                    Component.literal("HUD refreshed! Resource bars should update with current values."), false);
                
                CombatMetaphysics.LOGGER.info("HUD refresh command executed for player {}", playerId);
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error: " + e.getMessage()));
        }
        return 1;
    }
    
    // === НОВЫЕ КОМАНДЫ ДЛЯ ТЕСТИРОВАНИЯ ===
    
    private static int testMagicPrepare(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("УСТАРЕВШАЯ КОМАНДА! Используйте новую Event-Driven систему: /test_meteor_strike"));
        return 0;
    }
    
    private static int testMagicCast(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal("УСТАРЕВШАЯ КОМАНДА! Используйте новую Event-Driven систему: /test_meteor_strike"));
        return 0;
    }
    
    private static int testMeleeAttack(CommandContext<CommandSourceStack> context, String direction) {
        context.getSource().sendFailure(Component.literal("УСТАРЕВШАЯ КОМАНДА! Используйте новую Event-Driven систему: /test_meteor_strike"));
        return 0;
    }
    
    private static int testDefense(CommandContext<CommandSourceStack> context, String defenseType) {
        context.getSource().sendFailure(Component.literal("УСТАРЕВШАЯ КОМАНДА! Используйте новую Event-Driven систему: /test_meteor_strike"));
        return 0;
    }
    
    private static int testInterrupt(CommandContext<CommandSourceStack> context, String interruptType) {
        context.getSource().sendFailure(Component.literal("УСТАРЕВШАЯ КОМАНДА! Используйте новую Event-Driven систему: /test_meteor_strike"));
        return 0;
    }
    
    private static int resetState(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal("Event-Driven система не требует ручного сброса состояний!"), false);
        return 1;
    }
    
    /**
     * OSU-STYLE QTE ТЕСТИРОВАНИЕ
     * SINGLEPLAYER: Локальное создание и запуск OSU-style QTE
     */
    private static int testOSUQTE(CommandContext<CommandSourceStack> context, OSUStyleQTEEvent.QTEType qteType) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Определяем имя заклинания в зависимости от типа QTE
                String spellName = switch (qteType) {
                    case SEQUENCE -> "Fireball Combo";
                    case TIMING -> "Lightning Strike";
                    case RAPID -> "Magic Missile Barrage";
                    case PRECISION -> "Arcane Orb";
                };
                
                // Создаем и запускаем OSU-style QTE через клиентский менеджер
                UUID qteId = QTEClientManager.getInstance().startOSUStyleQTE(qteType, spellName);
                
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Started OSU-style QTE: %s (%s)", 
                                qteType.name(), spellName)
                ), false);
                
                // Логируем детали QTE для отладки
                CombatMetaphysics.LOGGER.info("OSU QTE Test - Type: {}, Spell: {}, ID: {}, Player: {}", 
                    qteType, spellName, qteId, playerId);
                
                // Получаем активное QTE для дополнительной информации
                var activeQTE = QTEClientManager.getInstance().getActiveOSUQTEs().stream()
                    .filter(qte -> qte.getEventId().equals(qteId))
                    .findFirst();
                
                if (activeQTE.isPresent()) {
                    var qte = activeQTE.get();
                    CombatMetaphysics.LOGGER.info("QTE Details - Hit Points: {}, Total Duration: {}ms", 
                        qte.getHitPoints().size(), qte.getTotalDuration());
                    
                    // Показываем детали hit points в консоли
                    for (int i = 0; i < qte.getHitPoints().size(); i++) {
                        var hitPoint = qte.getHitPoints().get(i);
                        CombatMetaphysics.LOGGER.info("Hit Point {}: Key={}, Target Time={}ms from start", 
                            i + 1, hitPoint.getKeyCode(), 
                            hitPoint.getTargetTime() - qte.getStartTime());
                    }
                }
                
                // Дополнительное сообщение с инструкциями
                context.getSource().sendSuccess(() -> Component.literal(
                    "Watch the center of your screen for OSU-style circles! Press the indicated keys when circles overlap."
                ), false);
                
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("OSU QTE Error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to start OSU QTE test", e);
        }
        return 1;
    }
}