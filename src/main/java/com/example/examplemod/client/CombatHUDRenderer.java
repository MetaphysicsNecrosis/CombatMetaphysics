package com.example.examplemod.client;

import com.example.examplemod.client.ui.ResourceHUD;
import com.example.examplemod.core.ResourceManager;
import com.example.examplemod.server.CombatServerManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.UUID;

public class CombatHUDRenderer {
    private static ResourceManager testResourceManager;
    private static long lastUpdateTime = 0;
    
    public static void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        
        UUID playerId = mc.player.getUUID();
        ResourceManager resourceManager = null;
        
        // Сначала пытаемся получить данные с сервера (через команды)
        try {
            resourceManager = CombatServerManager.getInstance().getPlayerResources(playerId);
        } catch (Exception e) {
            // Сервер недоступен, используем локальные тестовые данные
        }
        
        // Если серверных данных нет, создаем тестовые
        if (resourceManager == null) {
            if (testResourceManager == null) {
                testResourceManager = new ResourceManager(playerId, 100f, 100f);
                // Симулируем частично использованные ресурсы для демонстрации
                testResourceManager.tryReserveMana(30f, "demo");
                testResourceManager.tryUseStamina(25f, "demo");
            }
            resourceManager = testResourceManager;
        }
        
        // Обновляем ресурсы каждый тик
        resourceManager.tick();
        
        // Рендерим HUD с актуальными данными
        ResourceHUD.render(graphics, resourceManager, screenWidth, screenHeight);
        
        // Обновляем время последнего рендера
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public static void resetTestResources() {
        testResourceManager = null;
    }
    
    // Методы для синхронизации с сервером
    public static void syncWithServerData(UUID playerId) {
        try {
            ResourceManager serverManager = CombatServerManager.getInstance().getPlayerResources(playerId);
            if (serverManager != null && testResourceManager != null) {
                // Синхронизируем локальные данные с серверными
                testResourceManager.syncMana(serverManager.getCurrentMana(), serverManager.getReservedMana());
                testResourceManager.syncStamina(serverManager.getCurrentStamina());
            }
        } catch (Exception e) {
            // Сервер недоступен, используем локальные данные
        }
    }
    
    // Методы для тестирования через команды
    public static void testManaReservation(float amount) {
        if (testResourceManager != null) {
            testResourceManager.tryReserveMana(amount, "command test");
        }
    }
    
    public static void testStaminaUsage(float amount) {
        if (testResourceManager != null) {
            testResourceManager.tryUseStamina(amount, "command test");
        }
    }
    
    public static ResourceManager getTestResourceManager() {
        return testResourceManager;
    }
    
    public static long getLastUpdateTime() {
        return lastUpdateTime;
    }
}