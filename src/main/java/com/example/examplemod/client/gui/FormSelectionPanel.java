package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

/**
 * Панель выбора формы заклинания с drag & drop интерфейсом
 */
public class FormSelectionPanel extends AbstractWidget {
    
    private static final String[] FORMS = {
        "PROJECTILE", "BEAM", "BARRIER", "AREA", 
        "WAVE", "CHAIN", "INSTANT_POINT", "TOUCH", "WEAPON_ENCHANT"
    };
    
    private static final String[] FORM_NAMES = {
        "Снаряд", "Луч", "Барьер", "Зона",
        "Волна", "Цепь", "Точка", "Касание", "Зачарование"
    };
    
    private String selectedForm = "PROJECTILE";
    private Consumer<String> onFormSelected;
    private int hoveredIndex = -1;
    
    public FormSelectionPanel(int x, int y, int width, int height) {
        super(x, y, width, height, Component.literal("Forms"));
    }
    
    public void setSelectedForm(String form) {
        this.selectedForm = form;
    }
    
    public void setOnFormSelected(Consumer<String> callback) {
        this.onFormSelected = callback;
    }
    
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Фон панели
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x88222222);
        
        // Заголовок
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font, 
                              "§bФорма:", getX() + 5, getY() + 5, 0xFFFFFF);
        
        // Рендер форм
        int formY = getY() + 20;
        int formHeight = 12;
        
        for (int i = 0; i < FORMS.length; i++) {
            String form = FORMS[i];
            String displayName = FORM_NAMES[i];
            
            boolean isSelected = form.equals(selectedForm);
            boolean isHovered = i == hoveredIndex;
            
            // Фон формы
            int color = isSelected ? 0x8800AA00 : (isHovered ? 0x88444444 : 0x88111111);
            guiGraphics.fill(getX() + 2, formY, getX() + width - 2, formY + formHeight, color);
            
            // Иконка формы
            renderFormIcon(guiGraphics, form, getX() + 5, formY + 2);
            
            // Название формы
            int textColor = isSelected ? 0x00FF00 : (isHovered ? 0xFFFFFF : 0xAAAAA);
            guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                                  displayName, getX() + 20, formY + 2, textColor);
            
            formY += formHeight + 2;
        }
    }
    
    private void renderFormIcon(GuiGraphics guiGraphics, String form, int x, int y) {
        // Простые иконки из символов
        String icon = switch (form) {
            case "PROJECTILE" -> "→";
            case "BEAM" -> "—";
            case "BARRIER" -> "█";
            case "AREA" -> "○";
            case "WAVE" -> "~";
            case "CHAIN" -> "⚡";
            case "INSTANT_POINT" -> "✦";
            case "TOUCH" -> "👋";
            case "WEAPON_ENCHANT" -> "⚔";
            default -> "?";
        };
        
        int iconColor = switch (form) {
            case "PROJECTILE" -> 0xFF8888;
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
        
        guiGraphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                              icon, x, y, iconColor);
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }
        
        int relativeY = (int)(mouseY - getY() - 20);
        int formIndex = relativeY / 14;
        
        if (formIndex >= 0 && formIndex < FORMS.length) {
            selectedForm = FORMS[formIndex];
            if (onFormSelected != null) {
                onFormSelected.accept(selectedForm);
            }
            return true;
        }
        
        return false;
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!isMouseOver(mouseX, mouseY)) {
            hoveredIndex = -1;
            return;
        }
        
        int relativeY = (int)(mouseY - getY() - 20);
        hoveredIndex = relativeY / 14;
        
        if (hoveredIndex < 0 || hoveredIndex >= FORMS.length) {
            hoveredIndex = -1;
        }
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
        // narrationElementOutput.add(NarrationElementOutput.Type.TITLE, 
        //                          Component.literal("Form Selection: " + selectedForm));
    }
}