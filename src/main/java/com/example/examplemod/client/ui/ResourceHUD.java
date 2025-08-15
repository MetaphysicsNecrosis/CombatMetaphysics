package com.example.examplemod.client.ui;

import com.example.examplemod.core.ResourceManager;
import com.example.examplemod.core.PlayerState;
import com.example.examplemod.core.PlayerStateMachine;
import com.example.examplemod.client.input.CombatInputHandler;
import com.example.examplemod.client.CombatClientManager;
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
        renderManaBar(graphics, resourceManager, centerX, bottomY - 30);
        
        // Рендер полосы выносливости (ниже) - ИСПОЛЬЗУЕМ ВАШИ ТЕКСТУРЫ  
        renderStaminaBar(graphics, resourceManager, centerX, bottomY - 20);
        
        // Рендер состояния combat системы
        renderCombatState(graphics, screenWidth, screenHeight);
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
    
    /**
     * Отображает информацию о состоянии combat системы
     */
    private static void renderCombatState(GuiGraphics graphics, int screenWidth, int screenHeight) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Получаем состояние игрока
        PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(mc.player.getUUID());
        if (stateMachine == null) return;
        
        PlayerState currentState = stateMachine.getCurrentState();
        
        // Позиция для отображения состояния (верхний левый угол)
        int x = 10;
        int y = 10;
        
        // Отображаем текущее состояние
        String stateText = getStateDisplayName(currentState);
        int stateColor = getStateColor(currentState);
        
        graphics.drawString(mc.font, "Combat State: " + stateText, x, y, stateColor, true);
        y += 12;
        
        // Отображаем дополнительную информацию в зависимости от состояния
        if (currentState.isMagicState()) {
            renderMagicStateInfo(graphics, stateMachine, x, y);
        } else if (currentState.isMeleeState()) {
            renderMeleeStateInfo(graphics, stateMachine, x, y);
        } else if (currentState.isDefensiveState()) {
            renderDefenseStateInfo(graphics, stateMachine, x, y);
        }
        
        // Отображаем прогресс заряжания атаки если активен
        if (CombatInputHandler.isMeleeCharging()) {
            renderChargeProgress(graphics, screenWidth, screenHeight);
        }
    }
    
    private static void renderMagicStateInfo(GuiGraphics graphics, PlayerStateMachine stateMachine, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        String actionText = stateMachine.getCurrentAction();
        
        if (!actionText.isEmpty()) {
            graphics.drawString(mc.font, "Action: " + actionText, x, y, 0xFF64B5F6, true);
            y += 10;
        }
        
        long timeInState = stateMachine.getTimeInCurrentState();
        graphics.drawString(mc.font, String.format("Time: %.1fs", timeInState / 1000.0f), x, y, 0xFF64B5F6, true);
    }
    
    private static void renderMeleeStateInfo(GuiGraphics graphics, PlayerStateMachine stateMachine, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        
        // Отображаем направление атаки
        if (CombatInputHandler.isMeleeCharging()) {
            String direction = CombatInputHandler.getCurrentDirection().name();
            graphics.drawString(mc.font, "Direction: " + direction, x, y, 0xFFFF6B35, true);
            y += 10;
            
            String directionHint = switch (CombatInputHandler.getCurrentDirection()) {
                case LEFT_ATTACK -> "Fast, Low Damage";
                case RIGHT_ATTACK -> "Medium Speed/Damage";
                case TOP_ATTACK -> "Slow, High Damage";
                case THRUST_ATTACK -> "Fast, Piercing";
            };
            graphics.drawString(mc.font, directionHint, x, y, 0xFFFFAA00, true);
        }
    }
    
    private static void renderDefenseStateInfo(GuiGraphics graphics, PlayerStateMachine stateMachine, int x, int y) {
        Minecraft mc = Minecraft.getInstance();
        
        var defenseData = stateMachine.getDefenseSystem().getCurrentDefense(stateMachine.getPlayerId());
        if (defenseData != null) {
            long remaining = stateMachine.getDefenseSystem().getRemainingTime(stateMachine.getPlayerId());
            graphics.drawString(mc.font, String.format("Remaining: %.1fs", remaining / 1000.0f), x, y, 0xFF4CAF50, true);
        }
    }
    
    /**
     * Отображает прогресс заряжания атаки
     */
    private static void renderChargeProgress(GuiGraphics graphics, int screenWidth, int screenHeight) {
        float progress = CombatInputHandler.getMeleeChargeProgress();
        
        // Позиция индикатора заряжания (по центру экрана)
        int barWidth = 200;
        int barHeight = 10;
        int x = (screenWidth - barWidth) / 2;
        int y = screenHeight / 2 + 50;
        
        // Фон полосы
        graphics.fill(x - 2, y - 2, x + barWidth + 2, y + barHeight + 2, 0xFF000000);
        graphics.fill(x, y, x + barWidth, y + barHeight, 0xFF333333);
        
        // Заливка прогресса
        int fillWidth = (int)(barWidth * progress);
        int color = progress >= 1.0f ? 0xFFFF0000 : 0xFFFFAA00; // Красный при полной зарядке
        graphics.fill(x, y, x + fillWidth, y + barHeight, color);
        
        // Текст прогресса
        String progressText = String.format("Charge: %.0f%%", progress * 100);
        if (progress >= 1.0f) {
            progressText += " [CHARGED]";
        }
        
        Minecraft mc = Minecraft.getInstance();
        int textWidth = mc.font.width(progressText);
        graphics.drawString(mc.font, progressText, x + (barWidth - textWidth) / 2, y - 12, 0xFFFFFFFF, true);
    }
    
    private static String getStateDisplayName(PlayerState state) {
        return switch (state) {
            case IDLE -> "Idle";
            case MAGIC_PREPARING -> "Magic Prep";
            case MAGIC_CASTING -> "Casting";
            case QTE_TRANSITION -> "QTE Active";
            case MAGIC_COOLDOWN -> "Magic Cooldown";
            case MELEE_PREPARING -> "Melee Prep";
            case MELEE_CHARGING -> "Charging";
            case MELEE_ATTACKING -> "Attacking";
            case MELEE_RECOVERY -> "Melee Recovery";
            case DEFENSIVE_ACTION -> "Defense Prep";
            case BLOCKING -> "Blocking";
            case PARRYING -> "Parrying";
            case DODGING -> "Dodging";
            case DEFENSIVE_RECOVERY -> "Defense Recovery";
            case COOLDOWN -> "Cooldown";
            case INTERRUPTED -> "INTERRUPTED";
        };
    }
    
    private static int getStateColor(PlayerState state) {
        return switch (state.getCombatType()) {
            case MAGIC -> 0xFF64B5F6;     // Синий для магии
            case MELEE -> 0xFFFF6B35;     // Оранжевый для ближнего боя
            case DEFENSIVE -> 0xFF4CAF50; // Зеленый для защиты
            case NONE -> state == PlayerState.INTERRUPTED ? 0xFFFF0000 : 0xFFFFFFFF; // Красный для прерывания, белый для остального
        };
    }
}