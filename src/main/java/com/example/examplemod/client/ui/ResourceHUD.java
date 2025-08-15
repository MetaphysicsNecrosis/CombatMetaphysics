package com.example.examplemod.client.ui;

import com.example.examplemod.core.ResourceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public class ResourceHUD {
    // Текстуры для маны
    private static final ResourceLocation MANA_BAR_BG = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/hud/mana_bar_bg.png");
    private static final ResourceLocation MANA_BAR_FILL = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/hud/mana_bar_fill.png");
    private static final ResourceLocation MANA_BAR_RESERVED = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/hud/mana_bar_reserved.png");
    
    // Текстуры для выносливости
    private static final ResourceLocation STAMINA_BAR_BG = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/hud/stamina_bar_bg.png");
    private static final ResourceLocation STAMINA_BAR_FILL = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/hud/stamina_bar_fill.png");
    
    // Размеры согласно спецификации
    private static final int BAR_BG_WIDTH = 182;
    private static final int BAR_BG_HEIGHT = 7;
    private static final int BAR_FILL_WIDTH = 180;
    private static final int BAR_FILL_HEIGHT = 5;
    
    public static void render(GuiGraphics graphics, ResourceManager resourceManager, int screenWidth, int screenHeight) {
        if (resourceManager == null) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        
        // Позиция HUD (над хотбаром)
        int centerX = screenWidth / 2;
        int bottomY = screenHeight - 39 - 10; // 10 пикселей над хотбаром
        
        // Рендер полосы маны (выше) - ИСПОЛЬЗУЕМ ВАШИ ТЕКСТУРЫ
        renderManaBar(graphics, resourceManager, centerX, bottomY - 20);
        
        // Рендер полосы выносливости (ниже) - ИСПОЛЬЗУЕМ ВАШИ ТЕКСТУРЫ  
        renderStaminaBar(graphics, resourceManager, centerX, bottomY - 10);
    }
    
    private static void renderManaBar(GuiGraphics graphics, ResourceManager resourceManager, int centerX, int y) {
        int bgX = centerX - BAR_BG_WIDTH / 2;
        int fillX = centerX - BAR_FILL_WIDTH / 2;
        int fillY = y + 1; // Смещение для центрирования заливки в фоне
        
        // Рендер фона полосы маны
        graphics.blit(RenderType::guiTextured, MANA_BAR_BG, bgX, y, 0.0f, 0.0f, BAR_BG_WIDTH, BAR_BG_HEIGHT, BAR_BG_WIDTH, BAR_BG_HEIGHT);
        
        // Рассчитываем ширину заливки основной маны
        float manaPercentage = resourceManager.getCurrentMana() / resourceManager.getMaxMana();
        int manaWidth = (int)(BAR_FILL_WIDTH * manaPercentage);
        
        // Рассчитываем ширину зарезервированной маны
        float totalManaPercentage = resourceManager.getTotalMana() / resourceManager.getMaxMana();
        int totalWidth = (int)(BAR_FILL_WIDTH * totalManaPercentage);
        
        // Рендер зарезервированной маны (сначала, как подложка)
        if (totalWidth > 0) {
            graphics.blit(RenderType::guiTextured, MANA_BAR_RESERVED, fillX, fillY, 0.0f, 0.0f, totalWidth, BAR_FILL_HEIGHT, BAR_FILL_WIDTH, BAR_FILL_HEIGHT);
        }
        
        // Рендер основной маны (поверх зарезервированной)
        if (manaWidth > 0) {
            graphics.blit(RenderType::guiTextured, MANA_BAR_FILL, fillX, fillY, 0.0f, 0.0f, manaWidth, BAR_FILL_HEIGHT, BAR_FILL_WIDTH, BAR_FILL_HEIGHT);
        }
        
        // Текст с информацией о мане
        String manaText = String.format("%.0f/%.0f", resourceManager.getTotalMana(), resourceManager.getMaxMana());
        if (resourceManager.getReservedMana() > 0) {
            manaText = String.format("%.0f+%.0f/%.0f", resourceManager.getCurrentMana(), resourceManager.getReservedMana(), resourceManager.getMaxMana());
        }
        
        int textWidth = Minecraft.getInstance().font.width(manaText);
        graphics.drawString(Minecraft.getInstance().font, manaText, 
            centerX - textWidth / 2, y - 10, 0xFF64B5F6, true); // Цвет текста маны
    }
    
    private static void renderStaminaBar(GuiGraphics graphics, ResourceManager resourceManager, int centerX, int y) {
        int bgX = centerX - BAR_BG_WIDTH / 2;
        int fillX = centerX - BAR_FILL_WIDTH / 2;
        int fillY = y + 1; // Смещение для центрирования заливки в фоне
        
        // Рендер фона полосы выносливости
        graphics.blit(RenderType::guiTextured, STAMINA_BAR_BG, bgX, y, 0.0f, 0.0f, BAR_BG_WIDTH, BAR_BG_HEIGHT, BAR_BG_WIDTH, BAR_BG_HEIGHT);
        
        // Рассчитываем ширину заливки выносливости
        float staminaPercentage = resourceManager.getStaminaPercentage();
        int staminaWidth = (int)(BAR_FILL_WIDTH * staminaPercentage);
        
        // Рендер заливки выносливости
        if (staminaWidth > 0) {
            graphics.blit(RenderType::guiTextured, STAMINA_BAR_FILL, fillX, fillY, 0.0f, 0.0f, staminaWidth, BAR_FILL_HEIGHT, BAR_FILL_WIDTH, BAR_FILL_HEIGHT);
        }
        
        // Текст с информацией о выносливости
        String staminaText = String.format("%.0f/%.0f", resourceManager.getCurrentStamina(), resourceManager.getMaxStamina());
        int textWidth = Minecraft.getInstance().font.width(staminaText);
        graphics.drawString(Minecraft.getInstance().font, staminaText, 
            centerX - textWidth / 2, y - 10, 0xFFFFC107, true); // Желтый цвет текста выносливости
    }
    
    // Fallback рендер с простыми цветными прямоугольниками
    private static void renderManaBarFallback(GuiGraphics graphics, ResourceManager resourceManager, int centerX, int y) {
        int bgX = centerX - BAR_BG_WIDTH / 2;
        int fillX = centerX - BAR_FILL_WIDTH / 2;
        int fillY = y + 1;
        
        // Фон полосы маны (темно-синий)
        graphics.fill(bgX, y, bgX + BAR_BG_WIDTH, y + BAR_BG_HEIGHT, 0xFF1A237E);
        
        // Рассчитываем ширину заливки
        float manaPercentage = resourceManager.getCurrentMana() / resourceManager.getMaxMana();
        int manaWidth = (int)(BAR_FILL_WIDTH * manaPercentage);
        
        float totalManaPercentage = resourceManager.getTotalMana() / resourceManager.getMaxMana();
        int totalWidth = (int)(BAR_FILL_WIDTH * totalManaPercentage);
        
        // Зарезервированная мана (светло-синий)
        if (totalWidth > 0) {
            graphics.fill(fillX, fillY, fillX + totalWidth, fillY + BAR_FILL_HEIGHT, 0xFF64B5F6);
        }
        
        // Основная мана (яркий синий)
        if (manaWidth > 0) {
            graphics.fill(fillX, fillY, fillX + manaWidth, fillY + BAR_FILL_HEIGHT, 0xFF2196F3);
        }
        
        // Текст
        String manaText = String.format("%.0f/%.0f", resourceManager.getTotalMana(), resourceManager.getMaxMana());
        if (resourceManager.getReservedMana() > 0) {
            manaText = String.format("%.0f+%.0f/%.0f", resourceManager.getCurrentMana(), resourceManager.getReservedMana(), resourceManager.getMaxMana());
        }
        
        int textWidth = Minecraft.getInstance().font.width(manaText);
        graphics.drawString(Minecraft.getInstance().font, manaText, 
            centerX - textWidth / 2, y - 10, 0xFF64B5F6, true);
    }
    
    private static void renderStaminaBarFallback(GuiGraphics graphics, ResourceManager resourceManager, int centerX, int y) {
        int bgX = centerX - BAR_BG_WIDTH / 2;
        int fillX = centerX - BAR_FILL_WIDTH / 2;
        int fillY = y + 1;
        
        // Фон полосы выносливости (темно-желтый)
        graphics.fill(bgX, y, bgX + BAR_BG_WIDTH, y + BAR_BG_HEIGHT, 0xFFF57F17);
        
        // Рассчитываем ширину заливки
        float staminaPercentage = resourceManager.getStaminaPercentage();
        int staminaWidth = (int)(BAR_FILL_WIDTH * staminaPercentage);
        
        // Заливка выносливости (золотой)
        if (staminaWidth > 0) {
            graphics.fill(fillX, fillY, fillX + staminaWidth, fillY + BAR_FILL_HEIGHT, 0xFFFFC107);
        }
        
        // Текст
        String staminaText = String.format("%.0f/%.0f", resourceManager.getCurrentStamina(), resourceManager.getMaxStamina());
        int textWidth = Minecraft.getInstance().font.width(staminaText);
        graphics.drawString(Minecraft.getInstance().font, staminaText, 
            centerX - textWidth / 2, y - 10, 0xFFFFC107, true);
    }
}