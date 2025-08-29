package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.Map;

/**
 * Панель превью заклинания с визуализацией эффектов
 */
public class SpellPreviewPanel extends AbstractWidget {
    
    private String spellForm = "PROJECTILE";
    private Map<String, Float> parameters;
    private Map<String, Float> elements;
    
    private float animationTime = 0.0f;
    
    public SpellPreviewPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Preview"));
    }
    
    public void updateSpell(String form, Map<String, Float> parameters, Map<String, Float> elements) {
        this.spellForm = form;
        this.parameters = parameters;
        this.elements = elements;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        animationTime += partialTick * 0.1f;
        
        // Фон панели
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88222222);
        
        // Заголовок
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§bПревью:", getX() + 5, getY() + 5, 0xFFFFFF);
        
        // Визуализация заклинания
        renderSpellVisualization(guiGraphics);
        
        // Статистика
        renderSpellStats(guiGraphics);
    }
    
    private void renderSpellVisualization(GuiGraphics guiGraphics) {
        int centerX = getX() + width / 2;
        int centerY = getY() + 30;
        
        // Рендер формы заклинания
        switch (spellForm) {
            case "PROJECTILE" -> renderProjectilePreview(guiGraphics, centerX, centerY);
            case "BEAM" -> renderBeamPreview(guiGraphics, centerX, centerY);
            case "BARRIER" -> renderBarrierPreview(guiGraphics, centerX, centerY);
            case "AREA" -> renderAreaPreview(guiGraphics, centerX, centerY);
            case "WAVE" -> renderWavePreview(guiGraphics, centerX, centerY);
            case "CHAIN" -> renderChainPreview(guiGraphics, centerX, centerY);
            case "INSTANT_POINT" -> renderInstantPreview(guiGraphics, centerX, centerY);
            case "TOUCH" -> renderTouchPreview(guiGraphics, centerX, centerY);
            case "WEAPON_ENCHANT" -> renderEnchantPreview(guiGraphics, centerX, centerY);
        }
        
        // Элементальные эффекты
        renderElementalEffects(guiGraphics, centerX, centerY);
    }
    
    private void renderProjectilePreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        float size = parameters != null ? parameters.getOrDefault("radius", 1.0f) * 3 : 3;
        int color = getSpellColor();
        
        // Снаряд
        int projectileSize = (int)Math.max(3, size);
        guiGraphics.fill(centerX - projectileSize/2, centerY - projectileSize/2,
                        centerX + projectileSize/2, centerY + projectileSize/2, color);
        
        // Трейл
        for (int i = 1; i <= 5; i++) {
            int alpha = 255 - i * 40;
            int trailColor = (alpha << 24) | (color & 0xFFFFFF);
            int trailX = centerX - i * 4;
            int trailSize = Math.max(1, projectileSize - i);
            
            guiGraphics.fill(trailX - trailSize/2, centerY - trailSize/2,
                            trailX + trailSize/2, centerY + trailSize/2, trailColor);
        }
    }
    
    private void renderBeamPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        float thickness = parameters != null ? parameters.getOrDefault("radius", 1.0f) * 2 : 2;
        
        // Луч
        int beamThickness = (int)Math.max(2, thickness);
        guiGraphics.fill(centerX - 20, centerY - beamThickness/2,
                        centerX + 20, centerY + beamThickness/2, color);
        
        // Пульсация
        float pulse = (float)(Math.sin(animationTime * 4) * 0.3 + 0.7);
        int pulseAlpha = (int)(pulse * 128);
        int pulseColor = (pulseAlpha << 24) | 0xFFFFFF;
        
        int pulseThickness = (int)(beamThickness * 1.5f);
        guiGraphics.fill(centerX - 20, centerY - pulseThickness/2,
                        centerX + 20, centerY + pulseThickness/2, pulseColor);
    }
    
    private void renderBarrierPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        float size = parameters != null ? parameters.getOrDefault("radius", 3.0f) * 5 : 15;
        
        // Барьер (полупрозрачный купол)
        int barrierSize = (int)size;
        int barrierAlpha = 100;
        int barrierColor = (barrierAlpha << 24) | (color & 0xFFFFFF);
        
        // Рисуем как прямоугольник с рамкой
        guiGraphics.fill(centerX - barrierSize/2, centerY - barrierSize/2,
                        centerX + barrierSize/2, centerY + barrierSize/2, barrierColor);
        
        // Рамка барьера
        drawRect(guiGraphics, centerX - barrierSize/2, centerY - barrierSize/2,
                barrierSize, barrierSize, color | 0xFF000000);
    }
    
    private void renderAreaPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        float radius = parameters != null ? parameters.getOrDefault("radius", 3.0f) * 4 : 12;
        
        // Зона (круг)
        int areaRadius = (int)radius;
        int areaAlpha = 80;
        int areaColor = (areaAlpha << 24) | (color & 0xFFFFFF);
        
        // Рисуем зону как квадрат (упрощение)
        guiGraphics.fill(centerX - areaRadius, centerY - areaRadius,
                        centerX + areaRadius, centerY + areaRadius, areaColor);
        
        // Пульсирующая граница
        float pulse = (float)(Math.sin(animationTime * 3) * 0.5 + 0.5);
        int pulseRadius = (int)(areaRadius + pulse * 5);
        drawRect(guiGraphics, centerX - pulseRadius, centerY - pulseRadius,
                pulseRadius * 2, pulseRadius * 2, color | 0xFF000000);
    }
    
    private void renderWavePreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        
        // Расходящиеся волны
        for (int i = 0; i < 3; i++) {
            float offset = animationTime * 2 + i * 1.5f;
            int waveRadius = (int)(15 + Math.sin(offset) * 10);
            
            int alpha = (int)(128 - Math.abs(Math.sin(offset)) * 100);
            int waveColor = (alpha << 24) | (color & 0xFFFFFF);
            
            if (alpha > 0) {
                drawRect(guiGraphics, centerX - waveRadius, centerY - 2,
                        waveRadius * 2, 4, waveColor);
            }
        }
    }
    
    private void renderChainPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        
        // Цепные соединения между точками
        int[] targetX = {centerX, centerX + 15, centerX - 10, centerX + 5};
        int[] targetY = {centerY, centerY - 15, centerY + 10, centerY + 20};
        
        for (int i = 0; i < targetX.length - 1; i++) {
            drawLine(guiGraphics, targetX[i], targetY[i], 
                    targetX[i + 1], targetY[i + 1], color);
            
            // Точки целей
            guiGraphics.fill(targetX[i] - 2, targetY[i] - 2,
                            targetX[i] + 2, targetY[i] + 2, color);
        }
    }
    
    private void renderInstantPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        float intensity = (float)(Math.sin(animationTime * 8) * 0.5 + 0.5);
        
        // Взрыв
        int explosionSize = (int)(10 + intensity * 15);
        int alpha = (int)(255 * intensity);
        int explosionColor = (alpha << 24) | (color & 0xFFFFFF);
        
        guiGraphics.fill(centerX - explosionSize/2, centerY - explosionSize/2,
                        centerX + explosionSize/2, centerY + explosionSize/2, explosionColor);
        
        // Лучи
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int endX = centerX + (int)(Math.cos(angle) * intensity * 20);
            int endY = centerY + (int)(Math.sin(angle) * intensity * 20);
            
            drawLine(guiGraphics, centerX, centerY, endX, endY, color);
        }
    }
    
    private void renderTouchPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        
        // Рука
        guiGraphics.fill(centerX - 5, centerY, centerX + 5, centerY + 15, 0xFFFFBB88);
        
        // Эффект на руке
        float glow = (float)(Math.sin(animationTime * 6) * 0.3 + 0.7);
        int glowAlpha = (int)(glow * 200);
        int glowColor = (glowAlpha << 24) | (color & 0xFFFFFF);
        
        guiGraphics.fill(centerX - 7, centerY - 3, centerX + 7, centerY + 3, glowColor);
    }
    
    private void renderEnchantPreview(GuiGraphics guiGraphics, int centerX, int centerY) {
        int color = getSpellColor();
        
        // Меч
        guiGraphics.fill(centerX - 1, centerY - 15, centerX + 1, centerY + 5, 0xFFCCCCCC);
        guiGraphics.fill(centerX - 3, centerY + 3, centerX + 3, centerY + 8, 0xFF8B4513);
        
        // Энчант свечение
        float enchant = (float)(Math.sin(animationTime * 5) * 0.4 + 0.6);
        int enchantAlpha = (int)(enchant * 150);
        int enchantColor = (enchantAlpha << 24) | (color & 0xFFFFFF);
        
        guiGraphics.fill(centerX - 3, centerY - 17, centerX + 3, centerY + 7, enchantColor);
    }
    
    private void renderElementalEffects(GuiGraphics guiGraphics, int centerX, int centerY) {
        if (elements == null) return;
        
        // Эффекты элементов
        for (Map.Entry<String, Float> entry : elements.entrySet()) {
            String element = entry.getKey();
            float intensity = entry.getValue();
            
            if (intensity > 0) {
                renderElementEffect(guiGraphics, element, intensity, centerX, centerY);
            }
        }
    }
    
    private void renderElementEffect(GuiGraphics guiGraphics, String element, float intensity, int centerX, int centerY) {
        int effectColor = getElementEffectColor(element);
        int alpha = (int)(intensity * 100);
        int finalColor = (alpha << 24) | (effectColor & 0xFFFFFF);
        
        switch (element) {
            case "fire" -> {
                // Огненные искры
                for (int i = 0; i < 5; i++) {
                    float offset = animationTime * 3 + i;
                    int sparkX = centerX + (int)(Math.cos(offset) * 20 * intensity);
                    int sparkY = centerY + (int)(Math.sin(offset) * 20 * intensity);
                    guiGraphics.fill(sparkX, sparkY, sparkX + 1, sparkY + 1, finalColor);
                }
            }
            case "water" -> {
                // Водные капли
                float wave = (float)Math.sin(animationTime * 4);
                int waveOffset = (int)(wave * 10 * intensity);
                guiGraphics.fill(centerX - 15, centerY + waveOffset, 
                               centerX + 15, centerY + waveOffset + 2, finalColor);
            }
            case "lightning" -> {
                // Электрические разряды
                if (Math.sin(animationTime * 10) > 0.7) {
                    drawLine(guiGraphics, centerX - 20, centerY, centerX + 20, centerY + 3, finalColor);
                    drawLine(guiGraphics, centerX, centerY - 15, centerX + 2, centerY + 15, finalColor);
                }
            }
        }
    }
    
    private void renderSpellStats(GuiGraphics guiGraphics) {
        if (parameters == null) return;
        
        int statsY = getY() + height - 35;
        
        // Показываем ключевые статы
        float damage = parameters.getOrDefault("damage", 0.0f);
        float manaCost = calculatePreviewManaCost();
        
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§cУрон: §f" + String.format("%.0f", damage),
                              getX() + 5, statsY, 0xFFFFFF);
        
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§9Мана: §f" + String.format("%.0f", manaCost),
                              getX() + 5, statsY + 10, 0xFFFFFF);
        
        // Предупреждения
        if (manaCost > 100) {
            guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                                  "§c⚠ Дорого!", getX() + 80, statsY, 0xFF4444);
        }
    }
    
    private float calculatePreviewManaCost() {
        if (parameters == null) return 10.0f;
        
        float cost = 10.0f;
        cost += parameters.getOrDefault("damage", 0.0f) * 0.5f;
        float radius = parameters.getOrDefault("radius", 1.0f);
        cost += radius * radius * 0.8f;
        
        return cost;
    }
    
    private int getSpellColor() {
        // Базовый цвет формы
        int baseColor = switch (spellForm) {
            case "PROJECTILE" -> 0xFF8844;
            case "BEAM" -> 0xFFFF88;
            case "BARRIER" -> 0x8888FF;
            case "AREA" -> 0x88FF88;
            case "WAVE" -> 0x88FFFF;
            case "CHAIN" -> 0xFF88FF;
            case "INSTANT_POINT" -> 0xFFFFFF;
            case "TOUCH" -> 0xFFAA88;
            case "WEAPON_ENCHANT" -> 0xAAAAFF;
            default -> 0x888888;
        };
        
        // Модификация цвета элементами
        if (elements != null) {
            for (Map.Entry<String, Float> entry : elements.entrySet()) {
                if (entry.getValue() > 0.5f) {
                    // Доминирующий элемент меняет цвет
                    return getElementEffectColor(entry.getKey());
                }
            }
        }
        
        return baseColor;
    }
    
    private int getElementEffectColor(String element) {
        return switch (element) {
            case "fire" -> 0xFF4400;
            case "water" -> 0x0088FF;
            case "earth" -> 0x8B4513;
            case "air" -> 0xE0E0E0;
            case "ice" -> 0x87CEEB;
            case "lightning" -> 0xFFFF00;
            case "light" -> 0xFFFFDD;
            case "shadow" -> 0x444444;
            default -> 0x888888;
        };
    }
    
    private void drawLine(GuiGraphics guiGraphics, int x1, int y1, int x2, int y2, int color) {
        // Простая линия (можно улучшить)
        int dx = Math.abs(x2 - x1);
        int dy = Math.abs(y2 - y1);
        int steps = Math.max(dx, dy);
        
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / steps;
            int y = y1 + (y2 - y1) * i / steps;
            guiGraphics.fill(x, y, x + 1, y + 1, color);
        }
    }
    
    private void drawRect(GuiGraphics guiGraphics, int x, int y, int width, int height, int color) {
        // Рамка
        guiGraphics.fill(x, y, x + width, y + 1, color); // Верх
        guiGraphics.fill(x, y + height - 1, x + width, y + height, color); // Низ
        guiGraphics.fill(x, y, x + 1, y + height, color); // Лево
        guiGraphics.fill(x + width - 1, y, x + width, y + height, color); // Право
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // narrationElementOutput.add(NarrationElementOutput.Type.TITLE,
        //                          Component.literal("Spell Preview: " + spellForm));
    }
}