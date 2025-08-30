package com.example.examplemod.client.gui;

import com.example.examplemod.client.gui.components.ParameterSlider;
import com.example.examplemod.core.spells.forms.SpellForms;
import com.example.examplemod.core.spells.parameters.SpellParameters;
import com.example.examplemod.core.spells.SpellDefinition;
import com.example.examplemod.core.spells.SpellCoreModule;
import com.example.examplemod.core.spells.forms.SpellFormType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI для создания заклинаний
 */
public class SpellCraftingScreen extends Screen {
    
    private static final int GUI_WIDTH = 400;
    private static final int GUI_HEIGHT = 320;
    
    private SpellForms selectedForm = SpellForms.PROJECTILE;
    private Map<String, Float> parameters = new HashMap<>();
    private List<ParameterSlider> parameterSliders = new ArrayList<>();
    
    private int leftPos;
    private int topPos;
    private Button castButton;
    private Button clearButton;
    
    public SpellCraftingScreen() {
        super(Component.translatable("gui.combatmetaphysics.spell_crafting"));
        initializeDefaults();
    }
    
    private void initializeDefaults() {
        parameters.put(SpellParameters.DAMAGE, 10.0f);
        parameters.put(SpellParameters.HEALING, 0.0f);
        parameters.put(SpellParameters.RANGE, 20.0f);
        parameters.put(SpellParameters.RADIUS, 3.0f);
        parameters.put(SpellParameters.SPEED, 1.0f);
        parameters.put(SpellParameters.DURATION, 100.0f);
        parameters.put(SpellParameters.FIRE, 0.0f);
        parameters.put(SpellParameters.ICE, 0.0f);
        parameters.put(SpellParameters.LIGHTNING, 0.0f);
        parameters.put(SpellParameters.PIERCE_COUNT, 0.0f);
        parameters.put(SpellParameters.BOUNCE_COUNT, 0.0f);
        parameters.put(SpellParameters.HOMING_STRENGTH, 0.0f);
    }
    
    @Override
    protected void init() {
        super.init();
        leftPos = (width - GUI_WIDTH) / 2;
        topPos = (height - GUI_HEIGHT) / 2;
        
        // Селектор формы
        CycleButton<SpellForms> formSelector = CycleButton.<SpellForms>builder(form -> Component.literal(form.name()))
            .withValues(SpellForms.values())
            .withInitialValue(selectedForm)
            .create(leftPos + 10, topPos + 25, 120, 20, Component.literal("Форма"),
                (button, form) -> {
                    selectedForm = form;
                    rebuildSliders();
                });
        addRenderableWidget(formSelector);
        
        // Создаем ползунки для параметров
        createParameterSliders();
        
        // Кнопка каста
        castButton = Button.builder(
            Component.literal("CAST"),
            button -> castSpell()
        ).bounds(leftPos + 10, topPos + GUI_HEIGHT - 35, 80, 25).build();
        addRenderableWidget(castButton);
        
        // Кнопка сброса
        clearButton = Button.builder(
            Component.literal("Сброс"),
            button -> resetParameters()
        ).bounds(leftPos + 100, topPos + GUI_HEIGHT - 35, 60, 25).build();
        addRenderableWidget(clearButton);
    }
    
    private void createParameterSliders() {
        parameterSliders.clear();
        
        String[] mainParams = {"damage", "healing", "range", "radius", "speed", "duration"};
        String[] elementParams = {"fire", "ice", "lightning"};
        String[] specialParams = {"pierce_count", "bounce_count", "homing_strength"};
        
        int startY = topPos + 55;
        int col1X = leftPos + 10;
        int col2X = leftPos + 205;
        
        // Основные параметры
        for (int i = 0; i < mainParams.length; i++) {
            String param = mainParams[i];
            ParameterSlider slider = new ParameterSlider(
                col1X, startY + i * 25, 180, 20,
                param, parameters.getOrDefault(param, 0.0f),
                0.0f, getMaxValue(param),
                value -> parameters.put(param, value)
            );
            parameterSliders.add(slider);
            addRenderableWidget(slider);
        }
        
        // Элементальные параметры
        for (int i = 0; i < elementParams.length; i++) {
            String param = elementParams[i];
            ParameterSlider slider = new ParameterSlider(
                col2X, startY + i * 25, 180, 20,
                param, parameters.getOrDefault(param, 0.0f),
                0.0f, 10.0f,
                value -> parameters.put(param, value)
            );
            parameterSliders.add(slider);
            addRenderableWidget(slider);
        }
        
        // Специальные параметры
        for (int i = 0; i < specialParams.length; i++) {
            String param = specialParams[i];
            ParameterSlider slider = new ParameterSlider(
                col2X, startY + (i + 3) * 25, 180, 20,
                param, parameters.getOrDefault(param, 0.0f),
                0.0f, getMaxValue(param),
                value -> parameters.put(param, value)
            );
            parameterSliders.add(slider);
            addRenderableWidget(slider);
        }
    }
    
    private float getMaxValue(String param) {
        return switch (param) {
            case "damage", "healing" -> 100.0f;
            case "range" -> 50.0f;
            case "radius" -> 20.0f;
            case "speed" -> 10.0f;
            case "duration" -> 600.0f;
            case "pierce_count", "bounce_count" -> 10.0f;
            case "homing_strength" -> 5.0f;
            default -> 10.0f;
        };
    }
    
    private void rebuildSliders() {
        parameterSliders.forEach(this::removeWidget);
        createParameterSliders();
    }
    
    private void castSpell() {
        if (minecraft != null && minecraft.player != null) {
            try {
                // Создаем SpellDefinition напрямую из GUI параметров
                SpellDefinition definition = createSpellDefinition();
                
                // Кастуем через SpellCoreModule напрямую
                SpellCoreModule.getInstance().castSpell(
                    definition, 
                    definition.baseParameters(),
                    minecraft.player.level(),
                    minecraft.player
                );
                
                minecraft.player.displayClientMessage(
                    Component.literal("§aЗаклинание " + selectedForm.name() + " применено!"), 
                    false
                );
                
            } catch (Exception e) {
                minecraft.player.displayClientMessage(
                    Component.literal("§cОшибка каста: " + e.getMessage()), 
                    false
                );
            }
        }
    }
    
    private SpellDefinition createSpellDefinition() {
        // Создаем SpellParameters из текущих настроек GUI
        SpellParameters spellParams = new SpellParameters();
        
        for (Map.Entry<String, Float> entry : parameters.entrySet()) {
            spellParams.setParameter(entry.getKey(), entry.getValue());
        }
        
        // Конвертируем SpellForms в SpellFormType
        SpellFormType formType = convertToFormType(selectedForm);
        
        // Создаем определение заклинания
        return new SpellDefinition(
            "gui_spell_" + System.currentTimeMillis(),
            "GUI Заклинание",
            "custom",
            formType,
            spellParams,
            calculateManaCost(),
            calculateManaCost() * 0.1f,
            60,
            false
        );
    }
    
    private SpellFormType convertToFormType(SpellForms form) {
        return switch (form) {
            case PROJECTILE -> SpellFormType.PROJECTILE;
            case BEAM -> SpellFormType.BEAM;
            case BARRIER -> SpellFormType.BARRIER;
            case AREA -> SpellFormType.AREA;
            case WAVE -> SpellFormType.WAVE;
            case TOUCH -> SpellFormType.TOUCH;
            case WEAPON_ENCHANT -> SpellFormType.WEAPON_ENCHANT;
            case INSTANT_POINT -> SpellFormType.INSTANT_POINT;
            case CHAIN -> SpellFormType.CHAIN;
        };
    }
    
    private void resetParameters() {
        parameters.clear();
        initializeDefaults();
        rebuildSliders();
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(leftPos, topPos, leftPos + GUI_WIDTH, topPos + GUI_HEIGHT, 0xC0101010);
        guiGraphics.fill(leftPos + 1, topPos + 1, leftPos + GUI_WIDTH - 1, topPos + GUI_HEIGHT - 1, 0x80000000);
        
        // Заголовок
        guiGraphics.drawString(font, "Магический верстак", leftPos + 10, topPos + 8, 0xFFFFFF);
        
        // Линия разделения
        guiGraphics.fill(leftPos + 10, topPos + 20, leftPos + GUI_WIDTH - 10, topPos + 21, 0xFF444444);
        
        // Текущая форма
        guiGraphics.drawString(font, "Выбранная форма: " + selectedForm.name(), 
                              leftPos + 140, topPos + 30, 0xFFFF00);
        
        // Предпросмотр стоимости маны
        float totalManaCost = calculateManaCost();
        guiGraphics.drawString(font, "Стоимость маны: " + String.format("%.0f", totalManaCost), 
                              leftPos + 250, topPos + GUI_HEIGHT - 60, 0x00FF00);
        
        // Разделители колонок
        guiGraphics.fill(leftPos + 200, topPos + 50, leftPos + 201, topPos + GUI_HEIGHT - 50, 0xFF444444);
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }
    
    private float calculateManaCost() {
        float cost = parameters.getOrDefault(SpellParameters.DAMAGE, 0.0f) * 2.0f;
        cost += parameters.getOrDefault(SpellParameters.RANGE, 0.0f) * 0.5f;
        cost += parameters.getOrDefault(SpellParameters.RADIUS, 0.0f) * 1.5f;
        cost += (parameters.getOrDefault(SpellParameters.FIRE, 0.0f) +
                 parameters.getOrDefault(SpellParameters.ICE, 0.0f) +
                 parameters.getOrDefault(SpellParameters.LIGHTNING, 0.0f)) * 3.0f;
        return Math.max(1.0f, cost);
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}