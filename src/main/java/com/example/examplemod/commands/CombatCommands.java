package com.example.examplemod.commands;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.client.qte.QTEType;
import com.example.examplemod.client.qte.QTEClientManager;
import com.example.examplemod.client.qte.OSUStyleQTEEvent;
import com.example.examplemod.client.CombatClientManager;
import com.example.examplemod.client.CombatHUDRenderer;
import com.example.examplemod.commands.TestMeteorStrikeCommand;
import com.example.examplemod.util.BlockProtectionRegistry;
import com.example.examplemod.core.*;
import com.example.examplemod.core.GothicAttackSystem.AttackDirection;
import com.example.examplemod.core.GothicAttackSystem.AttackResult;
import com.example.examplemod.client.gui.CombatTestGUI;
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
                .then(Commands.literal("state_display")
                    .executes(CombatCommands::debugStateDisplay)
                )
            )
            .then(Commands.literal("gothic")
                .then(Commands.literal("attack")
                    .then(Commands.literal("left")
                        .executes(ctx -> testGothicAttack(ctx, AttackDirection.LEFT))
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> testGothicAttack(ctx, AttackDirection.RIGHT))
                    )
                    .then(Commands.literal("top")
                        .executes(ctx -> testGothicAttack(ctx, AttackDirection.TOP))
                    )
                    .then(Commands.literal("thrust")
                        .executes(ctx -> testGothicAttack(ctx, AttackDirection.THRUST))
                    )
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
                        .executes(ctx -> testMeleeAttackNew(ctx, DirectionalAttackSystem.AttackDirection.LEFT_ATTACK))
                    )
                    .then(Commands.literal("right")
                        .executes(ctx -> testMeleeAttackNew(ctx, DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK))
                    )
                    .then(Commands.literal("top")
                        .executes(ctx -> testMeleeAttackNew(ctx, DirectionalAttackSystem.AttackDirection.TOP_ATTACK))
                    )
                    .then(Commands.literal("thrust")
                        .executes(ctx -> testMeleeAttackNew(ctx, DirectionalAttackSystem.AttackDirection.THRUST_ATTACK))
                    )
                )
                .then(Commands.literal("collision")
                    .then(Commands.literal("test")
                        .then(Commands.argument("direction", IntegerArgumentType.integer(0, 3))
                            .executes(CombatCommands::testWeaponCollision)
                        )
                    )
                    .then(Commands.literal("spawn_mobs")
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 20))
                            .executes(CombatCommands::spawnTestMobs)
                        )
                    )
                    .then(Commands.literal("performance")
                        .executes(CombatCommands::testCollisionPerformance)
                    )
                    .then(Commands.literal("gui")
                        .executes(CombatCommands::openTestGUI)
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
    
    /**
     * НОВЫЙ метод тестирования ближнего боя с реальными коллизиями
     */
    private static int testMeleeAttackNew(CommandContext<CommandSourceStack> context, DirectionalAttackSystem.AttackDirection direction) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Получаем PlayerStateMachine для SINGLEPLAYER
                PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(playerId);
                if (stateMachine == null) {
                    context.getSource().sendFailure(Component.literal("Combat state machine not initialized!"));
                    return 0;
                }
                
                // Gothic Attack System - direct execution
                AttackDirection gothicDir = convertToGothicDirection(direction);
                AttackResult result = stateMachine.startGothicAttack(gothicDir);
                
                if (!result.isSuccess()) {
                    context.getSource().sendFailure(Component.literal("Gothic attack failed: " + result.getMessage()));
                    return 0;
                }
                
                // Wait for attack sequence to complete
                try {
                    Thread.sleep(800); // Gothic 3-phase attack duration
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                // Return to peaceful state after attack
                stateMachine.transitionTo(PlayerState.PEACEFUL, "command test completed");
                
                if (result.isSuccess()) {
                    String comboText = result.isCombo() ? " [COMBO x" + result.getComboLength() + "]" : "";
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("✓ Gothic %s attack: %.1f damage%s (%s)", 
                            gothicDir.name(), result.getDamage(), comboText, result.getMessage())
                    ), false);
                    
                    // Log details for debugging
                    CombatMetaphysics.LOGGER.info("Gothic attack test - Direction: {}, Damage: {}, Message: {}", 
                        gothicDir, result.getDamage(), result.getMessage());
                } else {
                    context.getSource().sendFailure(Component.literal(
                        "Gothic attack failed: " + result.getMessage()
                    ));
                }
                
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Melee test error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to test melee attack", e);
        }
        return 0;
    }
    
    /**
     * Тестирование системы коллизий оружия
     */
    private static int testWeaponCollision(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                int directionInt = IntegerArgumentType.getInteger(context, "direction");
                
                DirectionalAttackSystem.AttackDirection direction = switch (directionInt) {
                    case 0 -> DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
                    case 1 -> DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK;
                    case 2 -> DirectionalAttackSystem.AttackDirection.TOP_ATTACK;
                    case 3 -> DirectionalAttackSystem.AttackDirection.THRUST_ATTACK;
                    default -> DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
                };
                
                // Создаем контекст для collision test
                WeaponColliderSystem.SwingContext swingContext = new WeaponColliderSystem.SwingContext(
                    player, direction, 1.0f // без charge multiplier для теста
                );
                
                long startTime = System.nanoTime();
                
                // Выполняем collision sweep
                WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(swingContext);
                
                long duration = System.nanoTime() - startTime;
                double durationMs = duration / 1_000_000.0;
                
                if (hitResult.hasHits()) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("✓ Collision test: %d hits found in %.2fms", 
                            hitResult.getHitTargets().size(), durationMs)
                    ), false);
                    
                    // Показываем детали каждого попадания
                    for (int i = 0; i < hitResult.getHitTargets().size(); i++) {
                        var target = hitResult.getHitTargets().get(i);
                        double distance = hitResult.getDistance(target);
                        double damageMultiplier = hitResult.getDamageMultiplier(target);
                        
                        // Делаем переменные effectively final для lambda
                        final int hitNumber = i + 1;
                        final String targetName = target.getName().getString();
                        final double finalDistance = distance;
                        final double finalDamageMultiplier = damageMultiplier;
                        
                        context.getSource().sendSuccess(() -> Component.literal(
                            String.format("  Hit %d: %s (dist: %.1f, dmg: %.2fx)", 
                                hitNumber, targetName, finalDistance, finalDamageMultiplier)
                        ), false);
                    }
                } else {
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("○ Collision test: No hits (%.2fms)", durationMs)
                    ), false);
                }
                
                // Логируем производительность
                CombatMetaphysics.LOGGER.info("Collision test - Direction: {}, Hits: {}, Duration: {:.2f}ms", 
                    direction, hitResult.getHitTargets().size(), durationMs);
                
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Collision test error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to test weapon collision", e);
        }
        return 0;
    }
    
    /**
     * Создание мобов для тестирования коллизий
     */
    private static int spawnTestMobs(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                var level = context.getSource().getLevel();
                int count = IntegerArgumentType.getInteger(context, "count");
                
                int spawned = 0;
                var playerPos = player.position();
                
                for (int i = 0; i < count; i++) {
                    // Спавним зомби по кругу вокруг игрока
                    double angle = (2 * Math.PI * i) / count;
                    double distance = 3.0 + Math.random() * 2.0; // 3-5 блоков от игрока
                    
                    double x = playerPos.x + Math.cos(angle) * distance;
                    double y = playerPos.y;
                    double z = playerPos.z + Math.sin(angle) * distance;
                    
                    // Ищем подходящую высоту
                    var blockPos = new BlockPos((int)x, (int)y, (int)z);
                    while (level.getBlockState(blockPos).isSolid() && blockPos.getY() < 256) {
                        blockPos = blockPos.above();
                        y++;
                    }
                    
                    try {
                        // Создаем зомби
                        var zombie = new net.minecraft.world.entity.monster.Zombie(
                            net.minecraft.world.entity.EntityType.ZOMBIE, level
                        );
                        
                        zombie.setPos(x, y, z);
                        zombie.setCustomName(Component.literal("Test Target " + (i + 1)));
                        zombie.setCustomNameVisible(true);
                        
                        if (level.addFreshEntity(zombie)) {
                            spawned++;
                        }
                    } catch (Exception e) {
                        CombatMetaphysics.LOGGER.warn("Failed to spawn test mob {}: {}", i, e.getMessage());
                    }
                }
                
                // Делаем переменную effectively final для lambda
                final int finalSpawned = spawned;
                
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Spawned %d test mobs around you for collision testing", finalSpawned)
                ), false);
                
                CombatMetaphysics.LOGGER.info("Spawned {} test mobs for collision testing", finalSpawned);
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Mob spawn error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to spawn test mobs", e);
        }
        return 0;
    }
    
    private static AttackDirection convertToGothicDirection(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> AttackDirection.LEFT;
            case RIGHT_ATTACK -> AttackDirection.RIGHT;
            case TOP_ATTACK -> AttackDirection.TOP;
            case THRUST_ATTACK -> AttackDirection.THRUST;
        };
    }
    
    /**
     * Test Gothic attack command
     */
    private static int testGothicAttack(CommandContext<CommandSourceStack> context, AttackDirection direction) {
        if (!context.getSource().isPlayer()) {
            context.getSource().sendFailure(Component.literal("This command can only be executed by a player"));
            return 0;
        }
        
        var player = context.getSource().getPlayer();
        var playerId = player.getUUID();
        
        // Register player if not already registered
        var controller = com.example.examplemod.api.CombatController.getInstance();
        controller.registerPlayer(player);
        
        // Get state machine
        PlayerStateMachine stateMachine = PlayerStateMachine.getInstance(playerId);
        if (stateMachine == null) {
            context.getSource().sendFailure(Component.literal("Failed to get state machine"));
            return 0;
        }
        
        // Ensure in combat stance
        if (stateMachine.getCurrentState() == PlayerState.PEACEFUL) {
            stateMachine.transitionTo(PlayerState.COMBAT_STANCE, "Test command");
        }
        
        // Execute Gothic attack
        AttackResult result = stateMachine.startGothicAttack(direction);
        
        if (result.isSuccess()) {
            context.getSource().sendSuccess(() -> Component.literal(
                String.format("✓ Gothic %s attack started! Damage: %.1f", 
                    direction.name(), result.getDamage())
            ), true);
            
            // Log for debugging
            CombatMetaphysics.LOGGER.info("Gothic attack test - Direction: {}, Success: {}", 
                direction, result.isSuccess());
        } else {
            context.getSource().sendFailure(Component.literal(
                "Gothic attack failed: " + result.getMessage()
            ));
        }
        
        return 1;
    }
    
    /**
     * Тест производительности системы коллизий
     */
    private static int testCollisionPerformance(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                
                context.getSource().sendSuccess(() -> Component.literal("Starting collision performance test..."), false);
                
                int iterations = 100;
                long totalTime = 0;
                int totalHits = 0;
                
                // Тестируем все направления атак
                DirectionalAttackSystem.AttackDirection[] directions = DirectionalAttackSystem.AttackDirection.values();
                
                for (int i = 0; i < iterations; i++) {
                    DirectionalAttackSystem.AttackDirection direction = directions[i % directions.length];
                    
                    WeaponColliderSystem.SwingContext swingContext = new WeaponColliderSystem.SwingContext(
                        player, direction, 1.0f + (float)Math.random() // random charge
                    );
                    
                    long startTime = System.nanoTime();
                    WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(swingContext);
                    long duration = System.nanoTime() - startTime;
                    
                    totalTime += duration;
                    totalHits += hitResult.getHitTargets().size();
                }
                
                double avgTimeMs = (totalTime / iterations) / 1_000_000.0;
                double avgHits = (double) totalHits / iterations;
                
                // Получаем статистику системы коллизий
                var performanceStats = WeaponColliderSystem.getPerformanceStats();
                
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Performance test completed (%d iterations):", iterations)
                ), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("  Average time: %.3fms per collision check", avgTimeMs)
                ), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("  Average hits: %.1f targets per check", avgHits)
                ), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("  Cache stats: %s", performanceStats.toString())
                ), false);
                
                CombatMetaphysics.LOGGER.info("Collision performance test - {} iterations, avg {:.3f}ms, avg {:.1f} hits", 
                    iterations, avgTimeMs, avgHits);
                
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Performance test error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to run collision performance test", e);
        }
        return 0;
    }
    
    private static int testDefense(CommandContext<CommandSourceStack> context, String defenseType) {
        context.getSource().sendFailure(Component.literal("Defense testing will be implemented next"));
        return 0;
    }
    
    private static int testInterrupt(CommandContext<CommandSourceStack> context, String interruptType) {
        context.getSource().sendFailure(Component.literal("Interrupt testing will be implemented next"));
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
    
    /**
     * Debug команда для проверки отображения состояний в HUD
     */
    private static int debugStateDisplay(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                var player = context.getSource().getPlayerOrException();
                UUID playerId = player.getUUID();
                
                // Получаем или создаем PlayerStateMachine
                PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(playerId);
                if (stateMachine == null) {
                    context.getSource().sendFailure(Component.literal("State machine not found! Creating new one..."));
                    
                    // Принудительно создаем state machine
                    var resourceManager = CombatClientManager.getInstance().getPlayerResources(playerId);
                    stateMachine = PlayerStateMachine.getInstance(playerId, resourceManager);
                    
                    if (stateMachine == null) {
                        context.getSource().sendFailure(Component.literal("Failed to create state machine!"));
                        return 0;
                    }
                }
                
                // Получаем текущее состояние
                PlayerState currentState = stateMachine.getCurrentState();
                long timeInState = stateMachine.getTimeInCurrentState();
                String currentAction = stateMachine.getCurrentAction();
                
                // Отчет о состоянии
                context.getSource().sendSuccess(() -> Component.literal("=== State Display Debug ==="), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Current State: %s (color: 0x%08X)", 
                        currentState.name(), getStateColor(currentState))
                ), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Time in State: %.1fs", timeInState / 1000.0f)
                ), false);
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Current Action: %s", currentAction.isEmpty() ? "NONE" : currentAction)
                ), false);
                
                // Информация о ресурсах
                var resourceManager = stateMachine.getResourceManager();
                if (resourceManager != null) {
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("Mana: %.0f/%.0f (reserved: %.0f)",
                            resourceManager.getCurrentMana(), resourceManager.getMaxMana(),
                            resourceManager.getReservedMana())
                    ), false);
                    context.getSource().sendSuccess(() -> Component.literal(
                        String.format("Stamina: %.0f/%.0f",
                            resourceManager.getCurrentStamina(), resourceManager.getMaxStamina())
                    ), false);
                } else {
                    context.getSource().sendSuccess(() -> Component.literal("Resource Manager: NULL"), false);
                }
                
                // Collision система stats
                var perfStats = WeaponColliderSystem.getPerformanceStats();
                context.getSource().sendSuccess(() -> Component.literal(
                    String.format("Collision Cache: %s profiles, %s frames",
                        perfStats.getOrDefault("cachedProfiles", 0),
                        perfStats.getOrDefault("totalFrames", 0))
                ), false);
                
                // Инструкции
                context.getSource().sendSuccess(() -> Component.literal(""), false);
                context.getSource().sendSuccess(() -> Component.literal("✓ State display should now be visible in top-left corner of screen"), false);
                context.getSource().sendSuccess(() -> Component.literal("  Use F3 to toggle debug info if needed"), false);
                context.getSource().sendSuccess(() -> Component.literal("  Try different states with melee/collision test commands"), false);
                
                CombatMetaphysics.LOGGER.info("State display debug - State: {}, Time: {}ms, Action: {}", 
                    currentState, timeInState, currentAction);
                
                return 1;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("State display debug error: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to debug state display", e);
        }
        return 0;
    }
    
    /**
     * Открывает GUI интерфейс для тестирования боевой системы
     */
    private static int openTestGUI(CommandContext<CommandSourceStack> context) {
        try {
            if (context.getSource().isPlayer()) {
                // Открываем GUI на клиентской стороне
                net.minecraft.client.Minecraft.getInstance().setScreen(new CombatTestGUI());
                
                context.getSource().sendSuccess(() -> Component.literal(
                    "✓ Combat Test GUI opened! Use the interface to test attacks with collisions."
                ), false);
                
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("GUI can only be opened by players!"));
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to open GUI: " + e.getMessage()));
            CombatMetaphysics.LOGGER.error("Failed to open Combat Test GUI", e);
        }
        return 0;
    }
    
    /**
     * Вспомогательный метод для получения цвета состояния
     */
    private static int getStateColor(PlayerState state) {
        return switch (state.getCombatType()) {
            case MAGIC -> 0xFF64B5F6;     // Синий для магии
            case MELEE -> 0xFFFF6B35;     // Оранжевый для ближнего боя
            case DEFENSIVE -> 0xFF4CAF50; // Зеленый для защиты
            case NONE -> state == PlayerState.INTERRUPTED ? 0xFFFF0000 : 0xFFFFFFFF; // Красный для прерывания, белый для остального
        };
    }
}