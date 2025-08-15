package com.example.examplemod.commands;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.client.qte.QTEType;
import com.example.examplemod.client.CombatHUDRenderer;
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
                .then(Commands.literal("mana")
                    .then(Commands.argument("amount", IntegerArgumentType.integer(1, 100))
                        .executes(CombatCommands::testManaReserve)
                    )
                )
                .then(Commands.literal("hud")
                    .executes(CombatCommands::testHUD)
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
}