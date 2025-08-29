package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Панель элементов с интенсивностью и визуальными индикаторами
 */
public class ElementPanel extends AbstractWidget {
    
    private static final String[] ELEMENTS = {
        "fire", "water", "earth", "air", "ice", 
        "lightning", "light", "shadow"
    };
    
    private static final String[] ELEMENT_NAMES = {
        "Огонь", "Вода", "Земля", "Воздух", "Лёд",
        "Молния", "Свет", "Тень"
    };
    
    private static final String[] ELEMENT_SYMBOLS = {
        "🔥", "💧", "🌍", "💨", "❄",
        "⚡", "☀", "🌙"
    };
    
    private Map<String, Float> elements;
    private BiConsumer<String, Float> onElementChanged;
    private int draggingElement = -1;
    private String hoveredElement = null;
    
    public ElementPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Elements"));
    }
    
    public void setElements(Map<String, Float> elements) {
        this.elements = elements;
    }
    
    public void setOnElementChanged(BiConsumer<String, Float> callback) {
        this.onElementChanged = callback;
    }
    
    public String getHoveredElement(int mouseX, int mouseY) {
        return hoveredElement;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Фон панели
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88222222);
        
        // Заголовок
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§bЭлементы:", getX() + 5, getY() + 5, 0xFFFFFF);
        
        // Рендер элементов в сетке 2x4
        int elementSize = 30;
        int padding = 5;
        int startX = getX() + 10;
        int startY = getY() + 20;
        
        hoveredElement = null;
        
        for (int i = 0; i < ELEMENTS.length; i++) {
            String element = ELEMENTS[i];
            String displayName = ELEMENT_NAMES[i];
            String symbol = ELEMENT_SYMBOLS[i];
            float intensity = elements.getOrDefault(element, 0.0f);
            
            int row = i / 2;
            int col = i % 2;
            int elementX = startX + col * (elementSize + padding);
            int elementY = startY + row * (elementSize + padding + 10);
            
            boolean isHovered = mouseX >= elementX && mouseX <= elementX + elementSize &&
                               mouseY >= elementY && mouseY <= elementY + elementSize + 20;
            
            if (isHovered) {
                hoveredElement = element;
            }
            
            renderElement(guiGraphics, element, displayName, symbol, intensity,
                         elementX, elementY, elementSize, isHovered);
        }
        
        // Индикатор общей элементальной силы
        renderElementalPowerIndicator(guiGraphics);
    }
    
    private void renderElement(GuiGraphics guiGraphics, String element, String displayName, 
                              String symbol, float intensity, int x, int y, int size, boolean isHovered) {
        
        // Фон элемента
        int bgColor = getElementColor(element);
        int alpha = (int)(128 + intensity * 127); // От полупрозрачного до непрозрачного
        int finalColor = (alpha << 24) | (bgColor & 0xFFFFFF);
        
        if (isHovered) {
            finalColor = (Math.min(255, alpha + 50) << 24) | (bgColor & 0xFFFFFF);
        }
        
        guiGraphics.fill(x, y, x + size, y + size, finalColor);
        
        // Рамка
        int borderColor = intensity > 0 ? 0xFFFFFFFF : 0xFF666666;
        if (isHovered) {
            borderColor = 0xFFFFFF00; // Жёлтая рамка при наведении
        }
        
        // Рамка (1 пиксель)
        guiGraphics.fill(x - 1, y - 1, x + size + 1, y, borderColor); // Верх
        guiGraphics.fill(x - 1, y + size, x + size + 1, y + size + 1, borderColor); // Низ
        guiGraphics.fill(x - 1, y - 1, x, y + size + 1, borderColor); // Лево
        guiGraphics.fill(x + size, y - 1, x + size + 1, y + size + 1, borderColor); // Право
        
        // Символ элемента
        guiGraphics.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font,
                                      symbol, x + size/2, y + size/2 - 4, 0xFFFFFF);
        
        // Название элемента
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              displayName, x, y + size + 2, 0xFFFFFF);
        
        // Интенсивность
        String intensityText = String.format("%.0f%%", intensity * 100);
        int textColor = intensity > 0.7f ? 0xFFAA44 : (intensity > 0.3f ? 0xFFFF88 : 0xCCCCCC);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              intensityText, x, y + size + 12, textColor);
    }
    
    private void renderElementalPowerIndicator(GuiGraphics guiGraphics) {
        float totalPower = 0;
        int activeElements = 0;
        
        for (float intensity : elements.values()) {
            if (intensity > 0) {
                totalPower += intensity;
                activeElements++;
            }
        }
        
        int indicatorX = getX() + 5;
        int indicatorY = getY() + height - 25;
        
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§7Сила: §b" + String.format("%.1f", totalPower), 
                              indicatorX, indicatorY, 0xFFFFFF);
        
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§7Элементов: §b" + activeElements,
                              indicatorX, indicatorY + 10, 0xFFFFFF);
    }
    
    private int getElementColor(String element) {
        return switch (element) {
            case "fire" -> 0xFF4400;
            case "water" -> 0x0088FF;
            case "earth" -> 0x8B4513;
            case "air" -> 0xE0E0E0;
            case "ice" -> 0x87CEEB;
            case "lightning" -> 0xFFFF00;
            case "light" -> 0xFFFFDD;
            case "shadow" -> 0x333333;
            default -> 0x888888;
        };
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        // Определяем какой элемент был нажат
        int elementSize = 30;
        int padding = 5;
        int startX = getX() + 10;
        int startY = getY() + 20;
        
        for (int i = 0; i < ELEMENTS.length; i++) {
            int row = i / 2;
            int col = i % 2;
            int elementX = startX + col * (elementSize + padding);
            int elementY = startY + row * (elementSize + padding + 10);
            
            if (mouseX >= elementX && mouseX <= elementX + elementSize &&
                mouseY >= elementY && mouseY <= elementY + elementSize) {
                
                draggingElement = i;
                updateElementIntensity(mouseY, i);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingElement >= 0) {
            updateElementIntensity(mouseY, draggingElement);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingElement >= 0) {
            draggingElement = -1;
            return true;
        }
        return false;
    }
    
    private void updateElementIntensity(double mouseY, int elementIndex) {
        String element = ELEMENTS[elementIndex];
        
        int elementSize = 30;
        int padding = 5;
        int startY = getY() + 20;
        int row = elementIndex / 2;
        int elementY = startY + row * (elementSize + padding + 10);
        
        // Вертикальная позиция в пределах элемента определяет интенсивность
        double relativeY = mouseY - elementY;
        float intensity = 1.0f - Mth.clamp((float)(relativeY / elementSize), 0.0f, 1.0f);
        
        // Квантование к красивым значениям
        intensity = Math.round(intensity * 10.0f) / 10.0f;
        
        elements.put(element, intensity);
        
        if (onElementChanged != null) {
            onElementChanged.accept(element, intensity);
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // narrationElementOutput.add(NarrationElementOutput.Type.TITLE,
        //                          Component.literal("Element Panel"));
    }
}