package com.example.examplemod.client.input;

import com.example.examplemod.CombatMetaphysics;
import com.example.examplemod.api.CombatController;
import com.example.examplemod.api.GothicCombatAPI;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Современный input handler для Gothic Combat System
 * Использует новый CombatController и GothicCombatAPI
 */
@EventBusSubscriber(modid = CombatMetaphysics.MODID, value = Dist.CLIENT)
public class GothicInputHandler {
    
    // === ОСНОВНЫЕ БОЕВЫЕ КЛАВИШИ ===
    
    public static final KeyMapping COMBAT_STANCE_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_combat_stance",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping ATTACK_LEFT_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_attack_left",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping ATTACK_RIGHT_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_attack_right",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping ATTACK_TOP_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_attack_top",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping ATTACK_THRUST_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_attack_thrust",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping BLOCK_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_block",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping PARRY_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_parry",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    public static final KeyMapping DODGE_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_dodge",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.gothic"
    );
    
    // === DEBUG КЛАВИШИ ===
    
    public static final KeyMapping DEBUG_INFO_KEY = new KeyMapping(
        "key.combatmetaphysics.gothic_debug_info",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.debug"
    );
    
    public static final KeyMapping SPAWN_TEST_DUMMY = new KeyMapping(
        "key.combatmetaphysics.gothic_spawn_dummy",
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        "key.categories.combatmetaphysics.debug"
    );
    
    // Состояние клавиш
    private static long blockHoldTime = 0;
    private static boolean isBlockHeld = false;
    private static final long PARRY_WINDOW = 200; // 200ms для автоматического парирования
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        // Основные клавиши
        event.register(COMBAT_STANCE_KEY);
        event.register(ATTACK_LEFT_KEY);
        event.register(ATTACK_RIGHT_KEY);
        event.register(ATTACK_TOP_KEY);
        event.register(ATTACK_THRUST_KEY);
        event.register(BLOCK_KEY);
        event.register(PARRY_KEY);
        event.register(DODGE_KEY);
        
        // Debug клавиши
        event.register(DEBUG_INFO_KEY);
        event.register(SPAWN_TEST_DUMMY);
        
        CombatMetaphysics.LOGGER.info("Gothic Combat input keys registered");
    }
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        CombatController controller = CombatController.getInstance();
        
        if (event.getAction() == GLFW.GLFW_PRESS) {
            handleKeyPress(event.getKey(), mc, controller);
        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            handleKeyRelease(event.getKey(), mc, controller);
        }
    }
    
    @SubscribeEvent
    public static void onMouseInput(InputEvent.MouseButton.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;
        
        CombatController controller = CombatController.getInstance();
        
        // Обрабатываем ПКМ для блока/парирования
        if (event.getButton() == 1) { // ПКМ
            if (event.getAction() == GLFW.GLFW_PRESS) {
                handleBlockStart(mc, controller);
            } else if (event.getAction() == GLFW.GLFW_RELEASE) {
                handleBlockEnd(mc, controller);
            }
        }
    }
    
    private static void handleKeyPress(int key, Minecraft mc, CombatController controller) {
        try {
            // Регистрируем игрока при первом использовании (только один раз)
            controller.registerPlayer(mc.player);
            // Переключение боевой стойки
            if (key == COMBAT_STANCE_KEY.getKey().getValue()) {
                controller.toggleCombatStance(mc.player);
            }
            
            // Атаки в разных направлениях
            else if (key == ATTACK_LEFT_KEY.getKey().getValue()) {
                controller.attackLeft(mc.player);
            }
            else if (key == ATTACK_RIGHT_KEY.getKey().getValue()) {
                controller.attackRight(mc.player);
            }
            else if (key == ATTACK_TOP_KEY.getKey().getValue()) {
                controller.attackTop(mc.player);
            }
            else if (key == ATTACK_THRUST_KEY.getKey().getValue()) {
                controller.attackThrust(mc.player);
            }
            
            // Защитные действия
            else if (key == PARRY_KEY.getKey().getValue()) {
                controller.parry(mc.player);
            }
            else if (key == DODGE_KEY.getKey().getValue()) {
                controller.dodge(mc.player);
            }
            
            // Debug функции
            else if (key == DEBUG_INFO_KEY.getKey().getValue()) {
                showDebugInfo(mc, controller);
            }
            else if (key == SPAWN_TEST_DUMMY.getKey().getValue()) {
                spawnTestDummy(mc);
            }
            
        } catch (Exception e) {
            CombatMetaphysics.LOGGER.error("Error in key press handler: {}", e.getMessage());
        }
    }
    
    private static void handleKeyRelease(int key, Minecraft mc, CombatController controller) {
        // В Gothic системе большинство действий мгновенные
        // Только блокирование требует удержания
    }
    
    private static void handleBlockStart(Minecraft mc, CombatController controller) {
        isBlockHeld = true;
        blockHoldTime = System.currentTimeMillis();
        
        // Проверяем, можем ли мы блокировать
        GothicCombatAPI.CombatInfo info = controller.getCombatInfo(mc.player);
        if (!controller.canPlayerPerformAction(mc.player, GothicCombatAPI.CombatAction.DEFEND)) {
            sendMessage(mc.player, "§cCannot defend right now", true);
            return;
        }
        
        // Если уже в боевой стойке - начинаем блок
        if (info.isInCombat()) {
            controller.startBlocking(mc.player);
        } else {
            // Автоматически входим в боевую стойку
            controller.toggleCombatStance(mc.player);
        }
    }
    
    private static void handleBlockEnd(Minecraft mc, CombatController controller) {
        if (!isBlockHeld) return;
        
        long holdDuration = System.currentTimeMillis() - blockHoldTime;
        isBlockHeld = false;
        
        // Если кнопка была отпущена быстро (меньше 200ms) - это парирование
        if (holdDuration < PARRY_WINDOW) {
            controller.parry(mc.player);
            sendMessage(mc.player, "§6Quick release - Parrying!", true);
        } else {
            sendMessage(mc.player, "§7Block released", true);
        }
    }
    
    private static void showDebugInfo(Minecraft mc, CombatController controller) {
        GothicCombatAPI.CombatInfo info = controller.getCombatInfo(mc.player);
        
        // Отправляем debug информацию в чат
        sendMessage(mc.player, "=== Gothic Combat Debug ===", false);
        sendMessage(mc.player, "State: " + info.getStateDescription(), false);
        sendMessage(mc.player, "Stamina: " + Math.round(info.getCurrentStamina()) + "/" + Math.round(info.getMaxStamina()) + " (" + info.getStaminaLevel() + ")", false);
        sendMessage(mc.player, "In Combat: " + (info.isInCombat() ? "YES" : "NO"), false);
        sendMessage(mc.player, "Can Move: " + (info.canMove() ? "YES" : "NO"), false);
        sendMessage(mc.player, "Vulnerable: " + (info.isVulnerable() ? "YES" : "NO"), false);
        sendMessage(mc.player, "Attacking: " + (info.isAttacking() ? "YES" : "NO"), false);
        sendMessage(mc.player, "Defending: " + (info.isDefending() ? "YES" : "NO"), false);
        sendMessage(mc.player, "Time in State: " + info.getTimeInState() + "ms", false);
        sendMessage(mc.player, "Active Players: " + controller.getActivePlayerCount(), false);
    }
    
    private static void spawnTestDummy(Minecraft mc) {
        // Простое создание тестового моба
        if (mc.level != null) {
            try {
                var playerPos = mc.player.position();
                var zombie = new net.minecraft.world.entity.monster.Zombie(
                    net.minecraft.world.entity.EntityType.ZOMBIE, mc.level);
                    
                zombie.setPos(playerPos.x + 3, playerPos.y, playerPos.z);
                zombie.setCustomName(Component.literal("§eGothic Test Dummy"));
                zombie.setCustomNameVisible(true);
                zombie.setNoAi(true); // Стоит неподвижно
                
                if (mc.level.addFreshEntity(zombie)) {
                    sendMessage(mc.player, "§aTest dummy spawned!", false);
                } else {
                    sendMessage(mc.player, "§cFailed to spawn test dummy", false);
                }
                
            } catch (Exception e) {
                sendMessage(mc.player, "§cError spawning dummy: " + e.getMessage(), false);
            }
        }
    }
    
    // === ОБРАТНАЯ СВЯЗЬ ===
    
    /**
     * Отправляет сообщение игроку
     */
    private static void sendMessage(net.minecraft.world.entity.player.Player player, String message, boolean actionBar) {
        player.displayClientMessage(Component.literal(message), actionBar);
    }
    
    /**
     * Показывает индикатор состояния боя
     */
    public static void showCombatHUD(Minecraft mc) {
        if (mc.player == null) return;
        
        CombatController controller = CombatController.getInstance();
        GothicCombatAPI.CombatInfo info = controller.getCombatInfo(mc.player);
        
        // Создаем HUD строку
        String stateColor = info.isInCombat() ? "§c" : "§a";
        String staminaColor = switch (info.getStaminaLevel()) {
            case FULL, HIGH -> "§a";
            case MEDIUM -> "§e";
            case LOW -> "§6";
            case CRITICAL, EXHAUSTED -> "§c";
        };
        
        String hudLine = String.format("%s%s §7| %sStamina: %d/%d §7| %s",
            stateColor, info.getStateDescription(),
            staminaColor, Math.round(info.getCurrentStamina()), Math.round(info.getMaxStamina()),
            info.isVulnerable() ? "§4VULNERABLE" : "");
            
        sendMessage(mc.player, hudLine, true);
    }
    
    /**
     * Обновляется каждый тик клиента
     */
    public static void clientTick(Minecraft mc) {
        if (mc.player != null) {
            // Обновляем контроллер
            CombatController.getInstance().tick();
            
            // Показываем HUD если в бою
            GothicCombatAPI.CombatInfo info = CombatController.getInstance().getCombatInfo(mc.player);
            if (info.isInCombat() || info.getTimeInState() < 1000) { // Показываем HUD 1 секунду после выхода из боя
                showCombatHUD(mc);
            }
        }
    }
    
    // === ГЕТТЕРЫ ДЛЯ UI ===
    
    public static boolean isInCombat(net.minecraft.world.entity.player.Player player) {
        return CombatController.getInstance().getCombatInfo(player).isInCombat();
    }
    
    public static float getStaminaPercentage(net.minecraft.world.entity.player.Player player) {
        return CombatController.getInstance().getStaminaBarFill(player);
    }
    
    public static int getStaminaColor(net.minecraft.world.entity.player.Player player) {
        return CombatController.getInstance().getStaminaBarColor(player);
    }
    
    public static String getCurrentStateDescription(net.minecraft.world.entity.player.Player player) {
        return CombatController.getInstance().getStateDescription(player);
    }
}