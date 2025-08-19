package com.example.examplemod.commands;

import com.example.examplemod.core.actions.ActionRegistry;
import com.example.examplemod.core.pipeline.*;
import com.example.examplemod.core.state.PlayerStateComposition;
import com.example.examplemod.core.state.StateCapability;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;

/**
 * Команда для тестирования meteor_strike Composite Action
 */
public class TestMeteorStrikeCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("test_meteor_strike")
                .then(Commands.argument("range", FloatArgumentType.floatArg(1.0f, 20.0f))
                    .then(Commands.argument("damage", FloatArgumentType.floatArg(1.0f, 100.0f))
                        .executes(TestMeteorStrikeCommand::executeMeteorStrike)
                    )
                    .executes(ctx -> executeMeteorStrike(ctx, 30.0f)) // Урон по умолчанию
                )
                .executes(ctx -> executeMeteorStrike(ctx, 8.0f, 30.0f)) // Дефолтные значения
        );
    }
    
    private static int executeMeteorStrike(CommandContext<CommandSourceStack> context) {
        float range = FloatArgumentType.getFloat(context, "range");
        float damage = FloatArgumentType.getFloat(context, "damage");
        return executeMeteorStrike(context, range, damage);
    }
    
    private static int executeMeteorStrike(CommandContext<CommandSourceStack> context, float damage) {
        float range = FloatArgumentType.getFloat(context, "range");
        return executeMeteorStrike(context, range, damage);
    }
    
    private static int executeMeteorStrike(CommandContext<CommandSourceStack> context, float range, float damage) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by players"));
            return 0;
        }
        
        try {
            // Создаем событие meteor_strike
            BlockPos targetPos = player.blockPosition();
            ActionEvent event = new ActionEvent.Builder("meteor_strike", player)
                    .position(targetPos)
                    .range(range)
                    .damage(damage)
                    .parameter("blockDestructionPower", 3.5f)
                    .source(ActionEvent.EventSource.SERVER_COMMAND)
                    .build();
            
            // Создаем состояние игрока (для тестирования)
            PlayerStateComposition playerState = new PlayerStateComposition();
            playerState.addCapability(StateCapability.CASTING);
            
            // Создаем pipeline и выполняем действие
            ActionPipeline pipeline = new ActionPipeline();
            PipelineResult result = pipeline.process(event, playerState, player);
            
            // Выводим результат
            if (result.isSuccess()) {
                source.sendSuccess(() -> Component.literal("✅ Meteor Strike executed successfully!"), true);
                
                // Дополнительная информация о результате
                if (result.getResult() != null) {
                    source.sendSuccess(() -> Component.literal("Result: " + result.getResult().toString()), false);
                }
                
                return 1;
            } else if (result.isSuspended()) {
                source.sendSuccess(() -> Component.literal("⏳ Meteor Strike suspended (QTE or async action)"), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("❌ Meteor Strike failed: " + result.getErrorMessage()));
                return 0;
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("❌ Error executing Meteor Strike: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }
    
    /**
     * Команда для отладки ActionRegistry
     */
    public static void registerDebugCommand(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("debug_actions")
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    ActionRegistry registry = ActionRegistry.getInstance();
                    
                    var debugInfo = registry.getDebugInfo();
                    source.sendSuccess(() -> Component.literal("=== Action Registry Debug ==="), false);
                    source.sendSuccess(() -> Component.literal("Total actions: " + debugInfo.get("totalActions")), false);
                    source.sendSuccess(() -> Component.literal("Core actions: " + debugInfo.get("coreActions")), false);
                    source.sendSuccess(() -> Component.literal("Composite actions: " + debugInfo.get("compositeActions")), false);
                    source.sendSuccess(() -> Component.literal("Script actions: " + debugInfo.get("scriptActions")), false);
                    
                    // Выводим список всех действий
                    @SuppressWarnings("unchecked")
                    var actionLevels = (java.util.Map<String, ActionRegistry.ActionLevel>) debugInfo.get("actionLevels");
                    source.sendSuccess(() -> Component.literal("=== Available Actions ==="), false);
                    
                    actionLevels.forEach((actionType, level) -> {
                        source.sendSuccess(() -> Component.literal(actionType + " (" + level + ")"), false);
                    });
                    
                    return 1;
                })
        );
    }
}