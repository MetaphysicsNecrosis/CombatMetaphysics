package com.example.examplemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

public class SpellTestCommand {
    
    private static final Map<String, SpellTestData> playerData = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("spell")
            .then(Commands.literal("create")
                .then(Commands.argument("form", StringArgumentType.string())
                    .executes(SpellTestCommand::createSpell)))
            .then(Commands.literal("param")
                .then(Commands.argument("parameter", StringArgumentType.string())
                    .then(Commands.argument("value", FloatArgumentType.floatArg())
                        .executes(SpellTestCommand::setParameter))))
            .then(Commands.literal("element")
                .then(Commands.argument("element", StringArgumentType.string())
                    .then(Commands.argument("intensity", FloatArgumentType.floatArg(0.0f, 1.0f))
                        .executes(SpellTestCommand::setElement))))
            .then(Commands.literal("info")
                .executes(SpellTestCommand::showInfo))
            .then(Commands.literal("cast")
                .executes(SpellTestCommand::castSpell))
            .then(Commands.literal("clear")
                .executes(SpellTestCommand::clearSpell))
        );
    }
    
    private static int createSpell(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        String formType = StringArgumentType.getString(context, "form").toUpperCase();
        String[] validForms = {"PROJECTILE", "BEAM", "AREA", "BARRIER", "WAVE", "CHAIN", "INSTANT_POINT", "TOUCH", "WEAPON_ENCHANT"};
        boolean isValid = false;
        for (String valid : validForms) {
            if (valid.equals(formType) || valid.startsWith(formType)) {
                formType = valid;
                isValid = true;
                break;
            }
        }
        
        if (!isValid) {
            player.displayClientMessage(Component.literal("§cНеизвестная форма! Доступные: " + String.join(", ", validForms)), false);
            return 0;
        }
        
        SpellTestData data = new SpellTestData();
        data.formType = formType;
        playerData.put(player.getName().getString(), data);
        
        player.displayClientMessage(Component.literal("§aСоздано заклинание формы: §b" + formType), false);
        player.displayClientMessage(Component.literal("§7Используйте /spell param и /spell element для настройки"), false);
        
        return 1;
    }
    
    private static int setParameter(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        SpellTestData data = getPlayerData(player);
        if (data == null) return 0;
        
        String parameter = StringArgumentType.getString(context, "parameter");
        float value = FloatArgumentType.getFloat(context, "value");
        
        data.parameters.put(parameter, value);
        player.displayClientMessage(Component.literal("§aПараметр §b" + parameter + " §aустановлен в §b" + value), false);
        
        return 1;
    }
    
    private static int setElement(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        SpellTestData data = getPlayerData(player);
        if (data == null) return 0;
        
        String element = StringArgumentType.getString(context, "element");
        float intensity = FloatArgumentType.getFloat(context, "intensity");
        
        data.elements.put(element, intensity);
        player.displayClientMessage(Component.literal("§aЭлемент §b" + element + " §aустановлен в §b" + intensity), false);
        
        return 1;
    }
    
    private static int showInfo(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        SpellTestData data = getPlayerData(player);
        if (data == null) return 0;
        
        player.displayClientMessage(Component.literal("§e=== ИНФОРМАЦИЯ О ЗАКЛИНАНИИ ==="), false);
        player.displayClientMessage(Component.literal("§7Форма: §b" + data.formType), false);
        
        player.displayClientMessage(Component.literal("§7Параметры:"), false);
        for (Map.Entry<String, Float> entry : data.parameters.entrySet()) {
            player.displayClientMessage(Component.literal("  §b" + entry.getKey() + ": §f" + entry.getValue()), false);
        }
        
        player.displayClientMessage(Component.literal("§7Элементы:"), false);
        for (Map.Entry<String, Float> entry : data.elements.entrySet()) {
            player.displayClientMessage(Component.literal("  §b" + entry.getKey() + ": §f" + entry.getValue()), false);
        }
        
        return 1;
    }
    
    private static int castSpell(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        SpellTestData data = getPlayerData(player);
        if (data == null) return 0;
        
        player.displayClientMessage(Component.literal("§aСоздание заклинания..."), false);
        testParameterSystem(player, data);
        
        return 1;
    }
    
    private static int clearSpell(CommandContext<CommandSourceStack> context) {
        Player player = context.getSource().getPlayer();
        if (player == null) return 0;
        
        playerData.remove(player.getName().getString(), false);
        player.displayClientMessage(Component.literal("§aЗаклинание очищено"), false);
        
        return 1;
    }
    
    private static void testParameterSystem(Player player, SpellTestData data) {
        player.displayClientMessage(Component.literal("§e=== ТЕСТ СИСТЕМЫ ПАРАМЕТРОВ ==="), false);
        player.displayClientMessage(Component.literal("§71. Создается SpellComputationContext"), false);
        player.displayClientMessage(Component.literal("§72. Вызываются параметры: DamageParameter, RadiusParameter, etc."), false);
        player.displayClientMessage(Component.literal("§73. Вычисляются синергии элементов"), false);
        player.displayClientMessage(Component.literal("§74. Определяется CastMode и PersistenceType"), false);
        player.displayClientMessage(Component.literal("§75. Создается SpellEntity с результатами"), false);
        
        float damage = data.parameters.getOrDefault("damage", 10.0f);
        float radius = data.parameters.getOrDefault("radius", 3.0f);
        float fire = data.elements.getOrDefault("fire", 0.0f);
        float water = data.elements.getOrDefault("water", 0.0f);
        
        player.displayClientMessage(Component.literal("§e--- ПРИМЕРНЫЕ РАСЧЕТЫ ---"), false);
        player.displayClientMessage(Component.literal("§7Базовый урон: " + damage), false);
        
        if (fire > 0) {
            float fireBonus = fire * 0.3f;
            player.displayClientMessage(Component.literal("§7Бонус от огня: +" + (fireBonus * 100) + "%"), false);
            damage *= (1.0f + fireBonus);
        }
        
        if (fire > 0 && water > 0) {
            player.displayClientMessage(Component.literal("§cОгонь + Вода = конфликт! Создается пар"), false);
            player.displayClientMessage(Component.literal("§7Урон снижен, но добавлен эффект пара"), false);
        }
        
        player.displayClientMessage(Component.literal("§7Итоговый урон: " + String.format("%.1f", damage)), false);
        player.displayClientMessage(Component.literal("§7Радиус: " + radius + " -> Мана x" + String.format("%.1f", radius * radius * 0.8f + 1.0f)), false);
        
        String persistenceType = determinePersistenceType(data);
        player.displayClientMessage(Component.literal("§7Тип проходимости: " + persistenceType), false);
        
        String castMode = determineCastMode(data);
        player.displayClientMessage(Component.literal("§7Режим каста: " + castMode), false);
    }
    
    private static String determinePersistenceType(SpellTestData data) {
        float spirit = data.elements.getOrDefault("spirit", 0.0f);
        float lightning = data.elements.getOrDefault("lightning", 0.0f);
        
        if (spirit > 0.6f) return "GHOST";
        if (lightning > 0.8f) return "PHANTOM";
        return "PHYSICAL";
    }
    
    private static String determineCastMode(SpellTestData data) {
        if ("PROJECTILE".equals(data.formType) || "INSTANT_POINT".equals(data.formType)) {
            return "INSTANT_CAST";
        }
        
        if ("BEAM".equals(data.formType) || "AREA".equals(data.formType)) {
            return "MANA_SUSTAINED";
        }
        
        float totalPower = 0;
        for (float value : data.parameters.values()) {
            totalPower += value;
        }
        for (float value : data.elements.values()) {
            totalPower += value * 20;
        }
        
        if (totalPower > 50) {
            return "QTE_SUSTAINED";
        }
        
        return "INSTANT_CAST";
    }
    
    private static SpellTestData getPlayerData(Player player) {
        SpellTestData data = playerData.get(player.getName().getString());
        if (data == null) {
            player.displayClientMessage(Component.literal("§cСначала создайте заклинание: /spell create <форма>"), false);
        }
        return data;
    }
    
    private static class SpellTestData {
        String formType = "PROJECTILE";
        Map<String, Float> parameters = new HashMap<>();
        Map<String, Float> elements = new HashMap<>();
        
        SpellTestData() {
            parameters.put("damage", 10.0f);
            parameters.put("radius", 3.0f);
            parameters.put("speed", 1.0f);
            parameters.put("duration", 5.0f);
            parameters.put("range", 20.0f);
        }
    }
}