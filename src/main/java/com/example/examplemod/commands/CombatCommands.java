package com.example.examplemod.commands;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.client.qte.QTEType;
import com.example.examplemod.client.qte.QTEClientManager;
import com.example.examplemod.client.qte.OSUStyleQTEEvent;
import com.example.examplemod.client.CombatHUDRenderer;
import com.example.examplemod.commands.TestMeteorStrikeCommand;
import com.example.examplemod.util.BlockProtectionRegistry;
// Старые импорты удалены - используем новую Event-Driven систему
// SINGLEPLAYER: Removed server imports
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

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
            .then(Commands.literal("reload")
                .then(Commands.literal("spells")
                    .executes(CombatCommands::reloadSpells)
                )
            )
            .then(Commands.literal("recalc")
                .then(Commands.literal("light")
                    .executes(CombatCommands::recalculateLight)
                )
                .then(Commands.literal("physics")
                    .executes(CombatCommands::updatePhysics)
                )
            )
            .then(Commands.literal("protection")
                .then(Commands.literal("status")
                    .executes(CombatCommands::showProtectionStatus)
                )
                .then(Commands.literal("valuable")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(CombatCommands::setProtectValuable)
                    )
                )
                .then(Commands.literal("reinforced")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(CombatCommands::setProtectReinforced)
                    )
                )
                .then(Commands.literal("infrastructure")
                    .then(Commands.argument("enabled", BoolArgumentType.bool())
                        .executes(CombatCommands::setProtectInfrastructure)
                    )
                )
                .then(Commands.literal("threshold")
                    .then(Commands.argument("power", FloatArgumentType.floatArg(1.0f, 100.0f))
                        .executes(CombatCommands::setReinforcedThreshold)
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
                
                // SINGLEPLAYER: Debug локального состояния
                CombatMetaphysics.LOGGER.info("DEBUG STATE for player {}: SINGLEPLAYER mode", playerId);
                
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
                
                // SINGLEPLAYER: Используем локальные ресурсы из HUD
                var resources = CombatHUDRenderer.getTestResourceManager();
                
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
                
                // SINGLEPLAYER: Используем локальные ресурсы из HUD
                var resources = CombatHUDRenderer.getTestResourceManager();
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
                
                // SINGLEPLAYER: Синхронизируем локальные данные
                CombatHUDRenderer.syncLocalData(playerId);
                
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
    
    /**
     * Принудительное пересчёт освещения (рекомендация от Gemini)
     */
    private static int recalculateLight(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                
                if (context.getSource().getLevel() instanceof ServerLevel serverLevel) {
                    BlockPos playerPos = player.blockPosition();
                    int radius = 5; // Пересчёт в радиусе 5 чанков
                    
                    int recalculatedChunks = 0;
                    long startTime = System.currentTimeMillis();
                    
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            ChunkPos chunkPos = new ChunkPos(
                                (playerPos.getX() >> 4) + x,
                                (playerPos.getZ() >> 4) + z
                            );
                            
                            try {
                                // Принудительное обновление освещения по чанку
                                serverLevel.getChunkSource().getLightEngine().checkBlock(
                                    chunkPos.getWorldPosition()
                                );
                                recalculatedChunks++;
                            } catch (Exception e) {
                                CombatMetaphysics.LOGGER.warn("Failed to recalculate light for chunk {}: {}", 
                                    chunkPos, e.getMessage());
                            }
                        }
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    // Делаем переменные effectively final для lambda
                    final int finalRecalculatedChunks = recalculatedChunks;
                    final long finalDuration = duration;
                    final int finalRadius = radius;
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("Light recalculated for %d chunks in %dms (radius: %d)", 
                            finalRecalculatedChunks, finalDuration, finalRadius)
                    ), false);
                    
                    CombatMetaphysics.LOGGER.info("Light recalculation completed: {} chunks in {}ms", 
                        finalRecalculatedChunks, finalDuration);
                } else {
                    context.getSource().sendFailure(Component.literal("Light recalculation only available on server!"));
                }
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Light recalc error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to recalculate light", e);
        }
        return 1;
    }
    
    /**
     * Принудительное обновление физики (рекомендация от Gemini)
     */
    private static int updatePhysics(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                
                if (context.getSource().getLevel() instanceof ServerLevel serverLevel) {
                    BlockPos playerPos = player.blockPosition();
                    int radius = 3; // Обновление физики в радиусе 3 чанков
                    
                    int updatedChunks = 0;
                    long startTime = System.currentTimeMillis();
                    
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            ChunkPos chunkPos = new ChunkPos(
                                (playerPos.getX() >> 4) + x,
                                (playerPos.getZ() >> 4) + z
                            );
                            
                            try {
                                var chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                                
                                // Помечаем чанк для обновления физики и рендера
                                chunk.markUnsaved();
                                
                                // Принудительно обновляем соседние чанки
                                for (int dx = -1; dx <= 1; dx++) {
                                    for (int dz = -1; dz <= 1; dz++) {
                                        if (dx == 0 && dz == 0) continue;
                                        try {
                                            var neighborChunk = serverLevel.getChunk(chunkPos.x + dx, chunkPos.z + dz);
                                            neighborChunk.markUnsaved();
                                        } catch (Exception ignored) {
                                            // Игнорируем ошибки соседних чанков
                                        }
                                    }
                                }
                                
                                updatedChunks++;
                            } catch (Exception e) {
                                CombatMetaphysics.LOGGER.warn("Failed to update physics for chunk {}: {}", 
                                    chunkPos, e.getMessage());
                            }
                        }
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    // Делаем переменные effectively final для lambda
                    final int finalUpdatedChunks = updatedChunks;
                    final long finalDuration = duration;
                    final int finalRadius = radius;
                    
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("Physics updated for %d chunks in %dms (radius: %d)", 
                            finalUpdatedChunks, finalDuration, finalRadius)
                    ), false);
                    
                    CombatMetaphysics.LOGGER.info("Physics update completed: {} chunks in {}ms", 
                        finalUpdatedChunks, finalDuration);
                } else {
                    context.getSource().sendFailure(Component.literal("Physics update only available on server!"));
                }
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Physics update error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to update physics", e);
        }
        return 1;
    }
    
    /**
     * Hot reload пользовательских заклинаний (согласно CLAUDE.md)
     */
    private static int reloadSpells(CommandContext<CommandSourceStack> context) {
        try {
            // TODO: Реализовать когда будет Custom Spell Loader
            context.getSource().sendSuccess(() -> Component.literal(
                "Spell hot reload will be implemented with Custom Spell Loader system"
            ), false);
            
            CombatMetaphysics.LOGGER.info("Spell reload command executed (not yet implemented)");
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Spell reload error: " + e.getMessage()));
        }
        return 1;
    }
    
    /**
     * НОВЫЕ КОМАНДЫ ДЛЯ УПРАВЛЕНИЯ ЗАЩИТОЙ БЛОКОВ
     */
    
    private static int showProtectionStatus(CommandContext<CommandSourceStack> context) {
        try {
            var settings = BlockProtectionRegistry.getSettings();
            
            context.getSource().sendSuccess(() -> Component.literal("=== Block Protection Settings ==="), false);
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                "Absolute: %s | Valuable: %s | Reinforced: %s | Infrastructure: %s",
                settings.protectAbsolute ? "ON" : "OFF",
                settings.protectValuable ? "ON" : "OFF", 
                settings.protectReinforced ? "ON" : "OFF",
                settings.protectInfrastructure ? "ON" : "OFF"
            )), false);
            context.getSource().sendSuccess(() -> Component.literal(String.format(
                "Reinforced Power Threshold: %.1f", settings.reinforcedPowerThreshold
            )), false);
            
            // Логируем детальную статистику
            BlockProtectionRegistry.logProtectionStats();
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Protection status error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int setProtectValuable(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            var settings = BlockProtectionRegistry.getSettings();
            settings.protectValuable = enabled;
            
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("Valuable block protection: %s", enabled ? "ENABLED" : "DISABLED")
            ), false);
            
            CombatMetaphysics.LOGGER.info("Valuable block protection set to: {}", enabled);
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Set valuable protection error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int setProtectReinforced(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            var settings = BlockProtectionRegistry.getSettings();
            settings.protectReinforced = enabled;
            
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("Reinforced block protection: %s", enabled ? "ENABLED" : "DISABLED")
            ), false);
            
            CombatMetaphysics.LOGGER.info("Reinforced block protection set to: {}", enabled);
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Set reinforced protection error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int setProtectInfrastructure(CommandContext<CommandSourceStack> context) {
        try {
            boolean enabled = BoolArgumentType.getBool(context, "enabled");
            var settings = BlockProtectionRegistry.getSettings();
            settings.protectInfrastructure = enabled;
            
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("Infrastructure block protection: %s", enabled ? "ENABLED" : "DISABLED")
            ), false);
            
            CombatMetaphysics.LOGGER.info("Infrastructure block protection set to: {}", enabled);
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Set infrastructure protection error: " + e.getMessage()));
        }
        return 1;
    }
    
    private static int setReinforcedThreshold(CommandContext<CommandSourceStack> context) {
        try {
            float threshold = FloatArgumentType.getFloat(context, "power");
            var settings = BlockProtectionRegistry.getSettings();
            settings.reinforcedPowerThreshold = threshold;
            
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("Reinforced power threshold set to: %.1f", threshold)
            ), false);
            
            CombatMetaphysics.LOGGER.info("Reinforced power threshold set to: {}", threshold);
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Set threshold error: " + e.getMessage()));
        }
        return 1;
    }
}