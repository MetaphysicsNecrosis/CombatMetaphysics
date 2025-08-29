package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Панель с ползунками для настройки параметров заклинания
 */
public class ParameterSliderPanel extends AbstractWidget {
    
    private static final String[] PARAMETERS = {
        "damage", "radius", "speed", "duration", "range"
    };
    
    private static final String[] PARAMETER_NAMES = {
        "Урон", "Радиус", "Скорость", "Длительность", "Дальность"
    };
    
    private static final float[] MIN_VALUES = {
        1.0f, 0.5f, 0.1f, 1.0f, 5.0f
    };
    
    private static final float[] MAX_VALUES = {
        100.0f, 10.0f, 5.0f, 30.0f, 50.0f
    };
    
    private Map<String, Float> parameters;
    private BiConsumer<String, Float> onParameterChanged;
    private int draggingSlider = -1;
    private String hoveredParameter = null;
    
    public ParameterSliderPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Parameters"));
    }
    
    public void setParameters(Map<String, Float> parameters) {
        this.parameters = parameters;
    }
    
    public void setOnParameterChanged(BiConsumer<String, Float> callback) {
        this.onParameterChanged = callback;
    }
    
    public String getHoveredParameter(int mouseX, int mouseY) {
        return hoveredParameter;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Фон панели
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88222222);
        
        // Заголовок
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              "§bПараметры:", getX() + 5, getY() + 5, 0xFFFFFF);
        
        // Рендер слайдеров
        int sliderY = getY() + 20;
        int sliderHeight = 25;
        
        hoveredParameter = null;
        
        for (int i = 0; i < PARAMETERS.length; i++) {
            String param = PARAMETERS[i];
            String displayName = PARAMETER_NAMES[i];
            float value = parameters.getOrDefault(param, 0.0f);
            float min = MIN_VALUES[i];
            float max = MAX_VALUES[i];
            
            boolean isHovered = mouseX >= getX() && mouseX <= getX() + width &&
                               mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
            
            if (isHovered) {
                hoveredParameter = param;
            }
            
            renderSlider(guiGraphics, param, displayName, value, min, max, 
                        getX() + 5, sliderY, width - 10, isHovered);
            
            sliderY += sliderHeight + 5;
        }
    }
    
    private void renderSlider(GuiGraphics guiGraphics, String param, String displayName, 
                             float value, float min, float max, int x, int y, int sliderWidth, boolean isHovered) {
        
        // Название параметра
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              displayName, x, y, 0xFFFFFF);
        
        // Фон слайдера
        int sliderY = y + 12;
        int trackColor = isHovered ? 0xFF666666 : 0xFF444444;
        guiGraphics.fill(x, sliderY, x + sliderWidth, sliderY + 6, trackColor);
        
        // Заполнение слайдера
        float ratio = (value - min) / (max - min);
        int fillWidth = (int)(sliderWidth * ratio);
        int fillColor = getParameterColor(param);
        guiGraphics.fill(x, sliderY + 1, x + fillWidth, sliderY + 5, fillColor);
        
        // Ползунок
        int knobX = (int)(x + ratio * (sliderWidth - 8));
        int knobColor = isHovered ? 0xFFFFFFFF : 0xFFCCCCCC;
        guiGraphics.fill(knobX, sliderY - 2, knobX + 8, sliderY + 8, knobColor);
        
        // Значение
        String valueText = getFormattedValue(param, value);
        int textX = x + sliderWidth - net.minecraft.client.Minecraft.getInstance().font.width(valueText);
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              valueText, textX, y, 0xAAAAAAA);
    }
    
    private int getParameterColor(String param) {
        return switch (param) {
            case "damage" -> 0xFFAA4444;
            case "radius" -> 0xFF44AA44;
            case "speed" -> 0xFF4444AA;
            case "duration" -> 0xFFAAAA44;
            case "range" -> 0xFFAA44AA;
            default -> 0xFF888888;
        };
    }
    
    private String getFormattedValue(String param, float value) {
        return switch (param) {
            case "damage" -> String.format("%.0f", value);
            case "radius" -> String.format("%.1f m", value);
            case "speed" -> String.format("%.1fx", value);
            case "duration" -> String.format("%.0f s", value);
            case "range" -> String.format("%.0f m", value);
            default -> String.format("%.1f", value);
        };
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        int relativeY = (int)(mouseY - getY() - 20);
        int sliderIndex = relativeY / 30;
        
        if (sliderIndex >= 0 && sliderIndex < PARAMETERS.length) {
            draggingSlider = sliderIndex;
            updateSliderValue(mouseX, sliderIndex);
            return true;
        }
        
        return false;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingSlider >= 0) {
            updateSliderValue(mouseX, draggingSlider);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingSlider >= 0) {
            draggingSlider = -1;
            return true;
        }
        return false;
    }
    
    private void updateSliderValue(double mouseX, int sliderIndex) {
        String param = PARAMETERS[sliderIndex];
        float min = MIN_VALUES[sliderIndex];
        float max = MAX_VALUES[sliderIndex];
        
        int sliderX = getX() + 5;
        int sliderWidth = width - 10;
        
        double relativeX = mouseX - sliderX;
        float ratio = Mth.clamp((float)(relativeX / sliderWidth), 0.0f, 1.0f);
        
        float newValue = min + ratio * (max - min);
        parameters.put(param, newValue);
        
        if (onParameterChanged != null) {
            onParameterChanged.accept(param, newValue);
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // narrationElementOutput.add(NarrationElementOutput.Type.TITLE,
        //                          Component.literal("Parameter Sliders"));
    }
}