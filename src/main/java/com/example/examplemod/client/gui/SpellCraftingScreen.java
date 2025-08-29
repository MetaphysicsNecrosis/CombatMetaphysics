package com.example.examplemod.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Map;

/**
 * Продвинутый GUI для создания заклинаний с ползунками и перетаскиванием
 * 
 * Функции:
 * - Выбор формы заклинания (drag & drop иконки)
 * - Ползунки параметров (damage, radius, speed, etc.)
 * - Панель элементов с интенсивностью
 * - Превью заклинания в реальном времени
 * - Расчет стоимости маны
 * - Кнопка создания заклинания
 */
public class SpellCraftingScreen extends Screen {
    
    private static final ResourceLocation BACKGROUND_TEXTURE = 
        ResourceLocation.fromNamespaceAndPath("combatmetaphysics", "textures/gui/spell_crafting_table.png");
    
    private static final int GUI_WIDTH = 420;
    private static final int GUI_HEIGHT = 300;
    
    // Состояние GUI
    private String selectedForm = "PROJECTILE";
    private Map<String, Float> parameters = new HashMap<>();
    private Map<String, Float> elements = new HashMap<>();
    private boolean showAdvanced = false;
    
    // GUI компоненты
    private FormSelectionPanel formPanel;
    private ParameterSliderPanel sliderPanel; 
    private ElementPanel elementPanel;
    private SpellPreviewPanel previewPanel;
    private Button createSpellButton;
    private Button advancedToggle;
    
    public SpellCraftingScreen() {
        super(Component.literal("Магический Верстак"));
        initializeDefaultValues();
    }
    
    private void initializeDefaultValues() {
        // Базовые параметры
        parameters.put("damage", 10.0f);
        parameters.put("radius", 3.0f);
        parameters.put("speed", 1.0f);
        parameters.put("duration", 5.0f);
        parameters.put("range", 20.0f);
        parameters.put("penetration", 0.0f);
        parameters.put("crit_chance", 0.1f);
        
        // Элементы по умолчанию - все на нуле
        elements.put("fire", 0.0f);
        elements.put("water", 0.0f);
        elements.put("earth", 0.0f);
        elements.put("air", 0.0f);
        elements.put("ice", 0.0f);
        elements.put("lightning", 0.0f);
        elements.put("light", 0.0f);
        elements.put("shadow", 0.0f);
        elements.put("spirit", 0.0f);
        elements.put("void", 0.0f);
    }
    
    @Override
    protected void init() {
        super.init();
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // === ПАНЕЛЬ ВЫБОРА ФОРМ ===
        formPanel = new FormSelectionPanel(guiLeft + 10, guiTop + 25, 100, 120);
        formPanel.setSelectedForm(selectedForm);
        formPanel.setOnFormSelected(this::onFormSelected);
        
        // === ПАНЕЛЬ ПОЛЗУНКОВ ПАРАМЕТРОВ ===
        sliderPanel = new ParameterSliderPanel(guiLeft + 120, guiTop + 25, 140, 200);
        sliderPanel.setParameters(parameters);
        sliderPanel.setOnParameterChanged(this::onParameterChanged);
        
        // === ПАНЕЛЬ ЭЛЕМЕНТОВ ===
        elementPanel = new ElementPanel(guiLeft + 270, guiTop + 25, 140, 150);
        elementPanel.setElements(elements);
        elementPanel.setOnElementChanged(this::onElementChanged);
        
        // === ПРЕВЬЮ ЗАКЛИНАНИЯ ===
        previewPanel = new SpellPreviewPanel(guiLeft + 270, guiTop + 180, 140, 90);
        updatePreview();
        
        // === КНОПКИ ===
        createSpellButton = Button.builder(Component.literal("§aСоздать Заклинание"), 
                button -> createSpell())
            .bounds(guiLeft + 10, guiTop + 250, 120, 20)
            .build();
        addRenderableWidget(createSpellButton);
        
        advancedToggle = Button.builder(Component.literal("§7Расширенные"), 
                button -> toggleAdvanced())
            .bounds(guiLeft + 140, guiTop + 250, 80, 20)
            .build();
        addRenderableWidget(advancedToggle);
        
        Button resetButton = Button.builder(Component.literal("§cСброс"), 
                button -> resetAll())
            .bounds(guiLeft + 230, guiTop + 250, 60, 20)
            .build();
        addRenderableWidget(resetButton);
        
        Button helpButton = Button.builder(Component.literal("§e?"), 
                button -> showHelp())
            .bounds(guiLeft + 300, guiTop + 250, 20, 20)
            .build();
        addRenderableWidget(helpButton);
    }
    
    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Фон
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        
        int guiLeft = (this.width - GUI_WIDTH) / 2;
        int guiTop = (this.height - GUI_HEIGHT) / 2;
        
        // GUI фон (простой фон вместо текстуры)
        guiGraphics.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, 0xFF333333);
        
        // Заголовок
        guiGraphics.drawString(this.font, "§6§lМагический Верстак", 
                              guiLeft + 10, guiTop + 8, 0xFFFFFF, true);
        
        // Рендер всех панелей
        if (formPanel != null) {
            formPanel.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (sliderPanel != null) {
            sliderPanel.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (elementPanel != null) {
            elementPanel.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        if (previewPanel != null) {
            previewPanel.render(guiGraphics, mouseX, mouseY, partialTick);
        }
        
        // Показать расширенные параметры если включено
        if (showAdvanced) {
            renderAdvancedOptions(guiGraphics, guiLeft, guiTop);
        }
        
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        
        // Тултипы
        renderTooltips(guiGraphics, mouseX, mouseY);
    }
    
    private void renderAdvancedOptions(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        // Расширенная панель
        guiGraphics.fill(guiLeft + 350, guiTop + 25, guiLeft + 500, guiTop + 200, 0x88000000);
        guiGraphics.drawString(this.font, "§bРасширенные", guiLeft + 355, guiTop + 30, 0xFFFFFF);
        
        // Дополнительные ползунки
        int y = guiTop + 45;
        renderAdvancedSlider(guiGraphics, "Пробитие", parameters.get("penetration"), 
                           guiLeft + 355, y, 0.0f, 5.0f);
        
        y += 25;
        renderAdvancedSlider(guiGraphics, "Критшанс", parameters.get("crit_chance"), 
                           guiLeft + 355, y, 0.0f, 1.0f);
        
        y += 25;
        guiGraphics.drawString(this.font, "§7Персистентность:", guiLeft + 355, y, 0xFFFFFF);
        y += 12;
        guiGraphics.drawString(this.font, "§fPhysical", guiLeft + 355, y, 0xFFFFFF);
    }
    
    private void renderAdvancedSlider(GuiGraphics guiGraphics, String name, float value, 
                                    int x, int y, float min, float max) {
        guiGraphics.drawString(this.font, name, x, y, 0xFFFFFF);
        
        // Слайдер
        int sliderWidth = 100;
        int sliderX = x;
        int sliderY = y + 12;
        
        // Фон слайдера
        guiGraphics.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 4, 0xFF444444);
        
        // Позиция ползунка
        float ratio = (value - min) / (max - min);
        int knobX = (int)(sliderX + ratio * (sliderWidth - 4));
        guiGraphics.fill(knobX, sliderY - 1, knobX + 4, sliderY + 5, 0xFFFFFFFF);
        
        // Значение
        guiGraphics.drawString(this.font, String.format("%.1f", value), 
                              sliderX + sliderWidth + 5, y, 0xFFFFFF);
    }
    
    private void renderTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Тултипы для элементов
        if (elementPanel != null && elementPanel.isMouseOver(mouseX, mouseY)) {
            String hoveredElement = elementPanel.getHoveredElement(mouseX, mouseY);
            if (hoveredElement != null) {
                renderElementTooltip(guiGraphics, hoveredElement, mouseX, mouseY);
            }
        }
        
        // Тултипы для параметров
        if (sliderPanel != null && sliderPanel.isMouseOver(mouseX, mouseY)) {
            String hoveredParam = sliderPanel.getHoveredParameter(mouseX, mouseY);
            if (hoveredParam != null) {
                renderParameterTooltip(guiGraphics, hoveredParam, mouseX, mouseY);
            }
        }
    }
    
    private void renderElementTooltip(GuiGraphics guiGraphics, String element, int mouseX, int mouseY) {
        String[] tooltipLines = getElementTooltip(element);
        if (tooltipLines.length > 0) {
            guiGraphics.renderComponentTooltip(this.font, 
                java.util.List.of(tooltipLines).stream()
                    .map(Component::literal)
                    .collect(java.util.stream.Collectors.toList()),
                mouseX, mouseY);
        }
    }
    
    private void renderParameterTooltip(GuiGraphics guiGraphics, String parameter, int mouseX, int mouseY) {
        String[] tooltipLines = getParameterTooltip(parameter);
        if (tooltipLines.length > 0) {
            guiGraphics.renderComponentTooltip(this.font,
                java.util.List.of(tooltipLines).stream()
                    .map(Component::literal)
                    .collect(java.util.stream.Collectors.toList()),
                mouseX, mouseY);
        }
    }
    
    private String[] getElementTooltip(String element) {
        return switch (element) {
            case "fire" -> new String[]{"§cОгонь", "§7+30% урона", "§7Поджигает цели", "§4Конфликтует с водой"};
            case "water" -> new String[]{"§9Вода", "§7+20% исцеления", "§7Тушит огонь", "§4Конфликтует с огнем"};
            case "lightning" -> new String[]{"§eМолния", "§7+80% пробития", "§7Цепная реакция", "§7Игнорирует броню"};
            case "earth" -> new String[]{"§2Земля", "§7+40% пробития", "§7Увеличивает прочность", "§7Замедляет цели"};
            case "spirit" -> new String[]{"§dДух", "§7Ghost форма при 60%+", "§7Игнорирует физические барьеры", "§7Действует только на живое"};
            default -> new String[]{"§7" + element};
        };
    }
    
    private String[] getParameterTooltip(String parameter) {
        return switch (parameter) {
            case "damage" -> new String[]{"§cУрон", "§7Базовый урон заклинания", "§7Влияет на расход маны"};
            case "radius" -> new String[]{"§aРадиус", "§7Размер зоны действия", "§7Квадратично влияет на ману"};
            case "speed" -> new String[]{"§bСкорость", "§7Скорость движения снаряда", "§7Влияет на точность попадания"};
            case "duration" -> new String[]{"§eDлительность", "§7Время существования", "§7Для барьеров и зон"};
            case "range" -> new String[]{"§6Дальность", "§7Максимальная дистанция", "§7Для снарядов и лучей"};
            default -> new String[]{"§7" + parameter};
        };
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Делегируем клики панелям
        if (formPanel != null && formPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (sliderPanel != null && sliderPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        if (elementPanel != null && elementPanel.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        
        return super.mouseClicked(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        // Делегируем перетаскивание панелям
        if (sliderPanel != null && sliderPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            updatePreview();
            return true;
        }
        
        if (elementPanel != null && elementPanel.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            updatePreview();
            return true;
        }
        
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
    
    // === CALLBACKS ===
    
    private void onFormSelected(String form) {
        this.selectedForm = form;
        updatePreview();
    }
    
    private void onParameterChanged(String parameter, float value) {
        this.parameters.put(parameter, value);
        updatePreview();
    }
    
    private void onElementChanged(String element, float intensity) {
        this.elements.put(element, intensity);
        updatePreview();
    }
    
    private void updatePreview() {
        if (previewPanel != null) {
            previewPanel.updateSpell(selectedForm, parameters, elements);
        }
        
        // Обновляем кнопку создания заклинания
        if (createSpellButton != null) {
            float manaCost = calculateManaCost();
            createSpellButton.setMessage(Component.literal(
                String.format("§aСоздать (%.0f маны)", manaCost)));
        }
    }
    
    private float calculateManaCost() {
        float baseCost = 10.0f;
        
        // Стоимость от параметров
        baseCost += parameters.get("damage") * 0.5f;
        baseCost += parameters.get("radius") * parameters.get("radius") * 0.8f;
        baseCost += parameters.get("duration") * 1.2f;
        baseCost += parameters.get("range") * 0.3f;
        baseCost += parameters.get("penetration") * 5.0f;
        
        // Модификатор от элементов
        for (float intensity : elements.values()) {
            if (intensity > 0) {
                baseCost += intensity * 8.0f;
            }
        }
        
        // Модификатор формы
        baseCost *= switch (selectedForm) {
            case "PROJECTILE" -> 1.0f;
            case "BEAM" -> 1.5f;
            case "BARRIER" -> 2.0f;
            case "AREA" -> 1.8f;
            case "WAVE" -> 1.3f;
            case "CHAIN" -> 1.4f;
            case "INSTANT_POINT" -> 0.8f;
            case "TOUCH" -> 0.6f;
            case "WEAPON_ENCHANT" -> 1.1f;
            default -> 1.0f;
        };
        
        return Math.max(5.0f, baseCost);
    }
    
    private void createSpell() {
        // Создание заклинания
        if (minecraft != null && minecraft.player != null) {
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§aСоздано заклинание: " + selectedForm));
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§7Мана: " + String.format("%.0f", calculateManaCost())));
            
            // Здесь будет вызов команды создания заклинания
            // TODO: Интеграция с системой параметров
            
            this.onClose();
        }
    }
    
    private void toggleAdvanced() {
        showAdvanced = !showAdvanced;
        advancedToggle.setMessage(Component.literal(showAdvanced ? "§bСкрыть" : "§7Расширенные"));
    }
    
    private void resetAll() {
        initializeDefaultValues();
        selectedForm = "PROJECTILE";
        
        if (formPanel != null) formPanel.setSelectedForm(selectedForm);
        if (sliderPanel != null) sliderPanel.setParameters(parameters);
        if (elementPanel != null) elementPanel.setElements(elements);
        
        updatePreview();
    }
    
    private void showHelp() {
        if (minecraft != null && minecraft.player != null) {
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§e=== ПОМОЩЬ ==="));
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§7Левая панель: выбор формы заклинания"));
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§7Средняя панель: настройка параметров"));
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§7Правая панель: элементы и превью"));
            // TODO: Отправить команду создания заклинания на сервер
            // minecraft.player.sendSystemMessage(Component.literal("§7Наводите мышь для подсказок!"));
        }
    }
    
    @Override
    public boolean isPauseScreen() {
        return false;
    }
}