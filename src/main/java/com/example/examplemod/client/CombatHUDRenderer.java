package com.example.examplemod.client;

import com.example.examplemod.client.ui.ResourceHUD;
import com.example.examplemod.core.ResourceManager;
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
        
        // SINGLEPLAYER: Создаем локальный ResourceManager для демонстрации
        if (testResourceManager == null) {
            testResourceManager = new ResourceManager(playerId, 100f, 100f);
            // Симулируем частично использованные ресурсы для демонстрации
            testResourceManager.tryReserveMana(30f, "demo");
            testResourceManager.tryUseStamina(25f, "demo");
        }
        
        // Обновляем ресурсы каждый тик
        testResourceManager.tick();
        
        // Рендерим HUD с актуальными данными
        ResourceHUD.render(graphics, testResourceManager, screenWidth, screenHeight);
        
        // Обновляем время последнего рендера
        lastUpdateTime = System.currentTimeMillis();
    }
    
    public static void resetTestResources() {
        testResourceManager = null;
    }
    
    // SINGLEPLAYER: Локальные операции без сервера
    public static void syncLocalData(UUID playerId) {
        // В SINGLEPLAYER режиме никакой синхронизации не требуется
        // Все данные хранятся локально
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