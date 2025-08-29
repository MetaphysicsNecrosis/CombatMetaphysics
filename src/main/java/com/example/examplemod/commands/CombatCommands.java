package com.example.examplemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * Команды для тестирования системы боевой магии
 */
public class CombatCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("combat")
                .then(Commands.literal("test")
                    .executes(CombatCommands::testCommand))
                .then(Commands.literal("mana")
                    .executes(CombatCommands::checkMana))
                .then(Commands.literal("cast")
                    .then(Commands.argument("spellname", StringArgumentType.word())
                        .executes(CombatCommands::castSpell)))
        );
    }

    private static int testCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            source.sendSuccess(() -> Component.literal("CombatMetaphysics система активна!"), false);
            
            if (source.getEntity() instanceof Player player) {
                source.sendSuccess(() -> Component.literal("Игрок: " + player.getName().getString()), false);
                
                // Тестовая информация о системе
                source.sendSuccess(() -> Component.literal("Core Modules загружены:"), false);
                source.sendSuccess(() -> Component.literal("✓ SpellCore Module"), false);
                source.sendSuccess(() -> Component.literal("✓ ResourceCore Module"), false);
                source.sendSuccess(() -> Component.literal("✓ GeometryCore Module"), false);
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Ошибка тестирования: " + e.getMessage()));
            return 0;
        }
    }

    private static int checkMana(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Команда доступна только игрокам"));
            return 0;
        }

        try {
            var system = com.example.examplemod.core.CombatMagicSystem.getInstance();
            if (!system.isInitialized()) {
                source.sendFailure(Component.literal("Система магии не инициализирована"));
                return 0;
            }
            
            var manaPool = system.getPlayerManaPool(player);
            source.sendSuccess(() -> Component.literal(String.format("Мана инициации: %.1f/%.1f", 
                manaPool.getInitiationMana(), manaPool.getMaxInitiationMana())), false);
            source.sendSuccess(() -> Component.literal(String.format("Мана усиления: %.1f/%.1f", 
                manaPool.getAmplificationMana(), manaPool.getMaxAmplificationMana())), false);
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Ошибка проверки маны: " + e.getMessage()));
            return 0;
        }
    }

    private static int castSpell(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String spellName = StringArgumentType.getString(context, "spellname");
        
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.literal("Команда доступна только игрокам"));
            return 0;
        }

        try {
            var system = com.example.examplemod.core.CombatMagicSystem.getInstance();
            if (!system.isInitialized()) {
                source.sendFailure(Component.literal("Система магии не инициализирована"));
                return 0;
            }
            
            source.sendSuccess(() -> Component.literal("Пытаюсь применить заклинание: " + spellName), false);
            
            boolean success = system.castSpell(player, spellName);
            if (success) {
                source.sendSuccess(() -> Component.literal("Заклинание '" + spellName + "' успешно применено!"), false);
                
                // Показать статистику системы
                var stats = system.getStats();
                source.sendSuccess(() -> Component.literal(String.format("Активных заклинаний: %d", stats.activeSpells())), false);
            } else {
                source.sendFailure(Component.literal("Не удалось применить заклинание '" + spellName + "'"));
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(Component.literal("Ошибка каста заклинания: " + e.getMessage()));
            return 0;
        }
    }
}