package com.example.examplemod.client.ui;

import com.example.examplemod.client.qte.QTEEvent;
import com.example.examplemod.client.qte.QTEType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

public class QTEHUD {
    private static final int QTE_PANEL_WIDTH = 200;
    private static final int QTE_PANEL_HEIGHT = 80;
    private static final int PROGRESS_BAR_WIDTH = 180;
    private static final int PROGRESS_BAR_HEIGHT = 8;
    
    private static final int BACKGROUND_COLOR = 0xAA000000;
    private static final int BORDER_COLOR = 0xFFFFFFFF;
    private static final int PROGRESS_COLOR = 0xFF00FF00;
    private static final int FAIL_COLOR = 0xFFFF0000;
    private static final int SUCCESS_COLOR = 0xFF00FF00;
    
    public static void render(GuiGraphics graphics, QTEEvent qteEvent, int screenWidth, int screenHeight) {
        if (qteEvent == null || !qteEvent.isActive()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return;
        
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int panelX = centerX - QTE_PANEL_WIDTH / 2;
        int panelY = centerY - QTE_PANEL_HEIGHT / 2;
        
        // Фон панели
        graphics.fill(panelX, panelY, panelX + QTE_PANEL_WIDTH, panelY + QTE_PANEL_HEIGHT, BACKGROUND_COLOR);
        
        // Рамка
        graphics.fill(panelX - 1, panelY - 1, panelX + QTE_PANEL_WIDTH + 1, panelY, BORDER_COLOR); // Верх
        graphics.fill(panelX - 1, panelY + QTE_PANEL_HEIGHT, panelX + QTE_PANEL_WIDTH + 1, panelY + QTE_PANEL_HEIGHT + 1, BORDER_COLOR); // Низ
        graphics.fill(panelX - 1, panelY, panelX, panelY + QTE_PANEL_HEIGHT, BORDER_COLOR); // Лево
        graphics.fill(panelX + QTE_PANEL_WIDTH, panelY, panelX + QTE_PANEL_WIDTH + 1, panelY + QTE_PANEL_HEIGHT, BORDER_COLOR); // Право
        
        // Заголовок
        String title = getQTETitle(qteEvent.getType());
        int titleWidth = mc.font.width(title);
        graphics.drawString(mc.font, title, centerX - titleWidth / 2, panelY + 5, 0xFFFFFFFF, false);
        
        // Инструкции
        String instruction = getQTEInstruction(qteEvent);
        int instructionWidth = mc.font.width(instruction);
        graphics.drawString(mc.font, instruction, centerX - instructionWidth / 2, panelY + 20, 0xFFCCCCCC, false);
        
        // Прогресс-бар
        renderProgressBar(graphics, qteEvent, centerX, panelY + 35);
        
        // Клавиши
        renderKeys(graphics, qteEvent, centerX, panelY + 50);
        
        // Счет (если QTE завершен)
        if (qteEvent.isCompleted()) {
            String scoreText = String.format("Score: %.1f%%", qteEvent.getScore());
            int scoreWidth = mc.font.width(scoreText);
            int scoreColor = qteEvent.getScore() >= 50 ? SUCCESS_COLOR : FAIL_COLOR;
            graphics.drawString(mc.font, scoreText, centerX - scoreWidth / 2, panelY + 65, scoreColor, false);
        }
    }
    
    private static void renderProgressBar(GuiGraphics graphics, QTEEvent qteEvent, int centerX, int y) {
        int barX = centerX - PROGRESS_BAR_WIDTH / 2;
        
        // Фон прогресс-бара
        graphics.fill(barX - 1, y - 1, barX + PROGRESS_BAR_WIDTH + 1, y + PROGRESS_BAR_HEIGHT + 1, BORDER_COLOR);
        graphics.fill(barX, y, barX + PROGRESS_BAR_WIDTH, y + PROGRESS_BAR_HEIGHT, BACKGROUND_COLOR);
        
        // Прогресс
        float progress = qteEvent.getProgress();
        int progressWidth = (int)(PROGRESS_BAR_WIDTH * progress);
        
        if (progressWidth > 0) {
            int progressColor = PROGRESS_COLOR;
            if (qteEvent.getType() == QTEType.TIMING || qteEvent.getType() == QTEType.PRECISION) {
                // Для timing и precision показываем зоны
                progressColor = getTimingColor(progress);
            }
            graphics.fill(barX, y, barX + progressWidth, y + PROGRESS_BAR_HEIGHT, progressColor);
        }
        
        // Для timing/precision QTE показываем зону успеха
        if (qteEvent.getType() == QTEType.TIMING) {
            int zoneStart = (int)(PROGRESS_BAR_WIDTH * 0.6f);
            int zoneEnd = (int)(PROGRESS_BAR_WIDTH * 0.9f);
            graphics.fill(barX + zoneStart, y - 2, barX + zoneEnd, y - 1, SUCCESS_COLOR);
        } else if (qteEvent.getType() == QTEType.PRECISION) {
            int zoneStart = (int)(PROGRESS_BAR_WIDTH * 0.35f);
            int zoneEnd = (int)(PROGRESS_BAR_WIDTH * 0.65f);
            graphics.fill(barX + zoneStart, y - 2, barX + zoneEnd, y - 1, SUCCESS_COLOR);
        }
    }
    
    private static void renderKeys(GuiGraphics graphics, QTEEvent qteEvent, int centerX, int y) {
        var expectedKeys = qteEvent.getExpectedKeys();
        if (expectedKeys.isEmpty()) return;
        
        int totalWidth = expectedKeys.size() * 20 + (expectedKeys.size() - 1) * 5;
        int startX = centerX - totalWidth / 2;
        
        for (int i = 0; i < expectedKeys.size(); i++) {
            int keyX = startX + i * 25;
            int keyCode = expectedKeys.get(i);
            String keyName = getKeyName(keyCode);
            
            // Цвет клавиши
            int keyColor = 0xFF666666;
            if (qteEvent.getType() == QTEType.SEQUENCE) {
                if (i < qteEvent.getCurrentKeyIndex()) {
                    keyColor = SUCCESS_COLOR; // Уже нажата
                } else if (i == qteEvent.getCurrentKeyIndex()) {
                    keyColor = 0xFFFFFF00; // Текущая
                }
            } else if (qteEvent.getType() == QTEType.RAPID) {
                keyColor = qteEvent.getCurrentKeyIndex() > 0 ? SUCCESS_COLOR : 0xFFFFFF00;
            }
            
            // Рамка клавиши
            graphics.fill(keyX - 1, y - 1, keyX + 16, y + 11, BORDER_COLOR);
            graphics.fill(keyX, y, keyX + 15, y + 10, keyColor);
            
            // Текст клавиши
            int textWidth = Minecraft.getInstance().font.width(keyName);
            int textX = keyX + 7 - textWidth / 2;
            graphics.drawString(Minecraft.getInstance().font, keyName, textX, y + 2, 0xFF000000, false);
        }
    }
    
    private static String getQTETitle(QTEType type) {
        return switch (type) {
            case SEQUENCE -> "Sequence QTE";
            case TIMING -> "Timing QTE";
            case RAPID -> "Rapid QTE";
            case PRECISION -> "Precision QTE";
        };
    }
    
    private static String getQTEInstruction(QTEEvent qteEvent) {
        return switch (qteEvent.getType()) {
            case SEQUENCE -> "Press keys in order";
            case TIMING -> "Press at the right moment";
            case RAPID -> String.format("Press rapidly (%d more)", 
                Math.max(0, (int)(qteEvent.getDifficultyMultiplier() * 5) - qteEvent.getCurrentKeyIndex()));
            case PRECISION -> "Press in the green zone";
        };
    }
    
    private static int getTimingColor(float progress) {
        // Зеленый в нужной зоне, красный вне зоны
        if (progress >= 0.6f && progress <= 0.9f) {
            return SUCCESS_COLOR;
        } else if (progress >= 0.35f && progress <= 0.65f) {
            return SUCCESS_COLOR; // для precision
        } else {
            return FAIL_COLOR;
        }
    }
    
    private static String getKeyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPC";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "ALT";
            case GLFW.GLFW_KEY_W -> "W";
            case GLFW.GLFW_KEY_A -> "A";
            case GLFW.GLFW_KEY_S -> "S";
            case GLFW.GLFW_KEY_D -> "D";
            case GLFW.GLFW_KEY_Q -> "Q";
            case GLFW.GLFW_KEY_E -> "E";
            case GLFW.GLFW_KEY_R -> "R";
            case GLFW.GLFW_KEY_F -> "F";
            default -> String.valueOf((char)keyCode);
        };
    }
}