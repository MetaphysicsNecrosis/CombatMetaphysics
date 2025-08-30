package com.example.examplemod.client.gui.components;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

public class ParameterSlider extends AbstractSliderButton {
    
    private final String paramName;
    private final float minValue;
    private final float maxValue;
    private final Consumer<Float> onValueChanged;
    private float currentValue;
    
    public ParameterSlider(int x, int y, int width, int height, 
                          String paramName, float initialValue, 
                          float minValue, float maxValue,
                          Consumer<Float> onValueChanged) {
        super(x, y, width, height, Component.literal(paramName), 
              (initialValue - minValue) / (maxValue - minValue));
        this.paramName = paramName;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.currentValue = initialValue;
        this.onValueChanged = onValueChanged;
        updateMessage();
    }
    
    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(paramName + ": " + String.format("%.1f", currentValue)));
    }
    
    @Override
    protected void applyValue() {
        currentValue = minValue + (float)value * (maxValue - minValue);
        onValueChanged.accept(currentValue);
        updateMessage();
    }
    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (isHovered()) {
            float step = (maxValue - minValue) * 0.05f;
            float newValue = currentValue + (float)deltaY * step;
            setValue(newValue);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isFocused()) {
            float step = (maxValue - minValue) * 0.01f;
            if (keyCode == 262) { // RIGHT
                setValue(currentValue + step);
                return true;
            } else if (keyCode == 263) { // LEFT
                setValue(currentValue - step);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    
    public float getCurrentValue() {
        return currentValue;
    }
    
    public void setValue(float newValue) {
        newValue = Math.max(minValue, Math.min(maxValue, newValue));
        this.currentValue = newValue;
        this.value = (newValue - minValue) / (maxValue - minValue);
        updateMessage();
    }
}