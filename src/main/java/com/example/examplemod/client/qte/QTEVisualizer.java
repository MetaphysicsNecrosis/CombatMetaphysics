package com.example.examplemod.client.qte;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * OSU-style визуализация QTE с приближающимися кругами
 * SINGLEPLAYER: Вся отрисовка локальная, никаких сетевых задержек
 */
public class QTEVisualizer {
    
    // Константы для OSU-style визуализации
    private static final long APPROACH_DURATION = 2000;    // 2 секунды приближения
    private static final int MAX_CIRCLE_SIZE = 150;        // Максимальный размер круга (как в OSU)
    private static final int HIT_CIRCLE_SIZE = 64;         // Размер цели (стандартный OSU)
    private static final int RESULT_DISPLAY_TIME = 800;    // Время показа результата (быстрее)
    
    // Цвета для различных элементов (OSU-style)
    private static final int APPROACH_CIRCLE_COLOR = 0xFFFFFFFF;  // Белый приближающийся круг
    private static final int TARGET_CIRCLE_COLOR = 0xFF00DD00;    // Зеленая цель
    private static final int HIT_CIRCLE_BORDER = 0xFF333333;      // Темная рамка
    
    // Цвета результатов (timing windows)
    private static final int PERFECT_COLOR = 0xFFFFD700;          // Золотой
    private static final int GREAT_COLOR = 0xFF00DD00;            // Зеленый
    private static final int GOOD_COLOR = 0xFFFFFF00;             // Желтый 
    private static final int OK_COLOR = 0xFFFF7F00;               // Оранжевый
    private static final int MISS_COLOR = 0xFFFF0000;             // Красный
    
    // Позиция QTE на экране (центр)
    private int centerX, centerY;
    
    public QTEVisualizer() {
        updateScreenCenter();
    }
    
    /**
     * Обновляет центральную позицию при изменении размера экрана
     */
    public void updateScreenCenter() {
        Minecraft mc = Minecraft.getInstance();
        this.centerX = mc.getWindow().getGuiScaledWidth() / 2;
        this.centerY = mc.getWindow().getGuiScaledHeight() / 2;
    }
    
    /**
     * Основной метод отрисовки QTE
     * @param graphics контекст отрисовки Minecraft
     * @param hitPoints список точек попадания для отрисовки
     * @param qteStartTime время начала QTE
     */
    public void renderQTE(GuiGraphics graphics, List<QTEHitPoint> hitPoints, long qteStartTime) {
        if (hitPoints.isEmpty()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui) return; // Не показываем если UI скрыт
        
        updateScreenCenter();
        long currentTime = System.currentTimeMillis();
        
        // Отрисовываем каждую точку попадания
        for (QTEHitPoint hitPoint : hitPoints) {
            if (hitPoint.shouldBeVisible(currentTime, APPROACH_DURATION, RESULT_DISPLAY_TIME)) {
                renderHitPoint(graphics, hitPoint, currentTime);
            }
        }
        
        // Отрисовываем общие элементы QTE
        renderQTEInfo(graphics, hitPoints, currentTime, qteStartTime);
    }
    
    /**
     * Отрисовывает одну точку попадания в OSU-style
     */
    private void renderHitPoint(GuiGraphics graphics, QTEHitPoint hitPoint, long currentTime) {
        // Вычисляем прогресс приближения (0.0 = начало, 1.0 = цель)
        float approachProgress = hitPoint.getApproachProgress(currentTime, APPROACH_DURATION);
        
        if (hitPoint.getResult() == QTEHitPoint.HitResult.WAITING) {
            // Отрисовываем приближающийся круг
            renderApproachingCircle(graphics, hitPoint, approachProgress);
            // Отрисовываем цель
            renderTargetCircle(graphics, hitPoint);
            // Отрисовываем клавишу
            renderKeyIndicator(graphics, hitPoint);
        } else {
            // Отрисовываем результат попадания
            renderHitResult(graphics, hitPoint, currentTime);
        }
    }
    
    /**
     * Отрисовывает приближающийся белый круг (главная OSU фишка)
     */
    private void renderApproachingCircle(GuiGraphics graphics, QTEHitPoint hitPoint, float progress) {
        if (progress >= 1.0f) return; // Уже достиг цели
        
        // Размер круга уменьшается от MAX до HIT_CIRCLE_SIZE
        float currentSize = Mth.lerp(progress, MAX_CIRCLE_SIZE, HIT_CIRCLE_SIZE);
        int radius = (int)(currentSize / 2);
        
        // Прозрачность увеличивается по мере приближения
        int alpha = (int)(180 + 75 * progress); // От 180 до 255
        int color = (alpha << 24) | (APPROACH_CIRCLE_COLOR & 0xFFFFFF);
        
        // Отрисовываем кольцо (не заполненный круг)
        renderCircleOutline(graphics, centerX, centerY, radius, color, 3);
    }
    
    /**
     * Отрисовывает целевой круг (зеленый, всегда в центре)
     */
    private void renderTargetCircle(GuiGraphics graphics, QTEHitPoint hitPoint) {
        int targetRadius = HIT_CIRCLE_SIZE / 2;
        
        // Заполненный зеленый круг
        renderCircleFilled(graphics, centerX, centerY, targetRadius, TARGET_CIRCLE_COLOR);
        
        // Темная рамка вокруг цели
        renderCircleOutline(graphics, centerX, centerY, targetRadius, HIT_CIRCLE_BORDER, 2);
    }
    
    /**
     * Отрисовывает индикатор клавиши в центре цели
     */
    private void renderKeyIndicator(GuiGraphics graphics, QTEHitPoint hitPoint) {
        String keyName = getKeyName(hitPoint.getKeyCode());
        
        // Фон для текста клавиши
        int textWidth = Minecraft.getInstance().font.width(keyName);
        int textHeight = 9; // Высота шрифта
        
        int bgX = centerX - textWidth / 2 - 2;
        int bgY = centerY - textHeight / 2 - 1;
        int bgWidth = textWidth + 4;
        int bgHeight = textHeight + 2;
        
        // Полупрозрачный темный фон
        graphics.fill(bgX, bgY, bgX + bgWidth, bgY + bgHeight, 0x80000000);
        
        // Белый текст клавиши
        graphics.drawCenteredString(Minecraft.getInstance().font, keyName, 
                                  centerX, centerY - 4, 0xFFFFFFFF);
    }
    
    /**
     * Отрисовывает результат попадания с анимацией
     */
    private void renderHitResult(GuiGraphics graphics, QTEHitPoint hitPoint, long currentTime) {
        // Время с момента получения результата
        long timeSinceHit = currentTime - hitPoint.getActualHitTime();
        if (timeSinceHit > RESULT_DISPLAY_TIME) return;
        
        // Анимация появления и исчезновения
        float animProgress = (float)timeSinceHit / RESULT_DISPLAY_TIME;
        float scale = hitPoint.getResultScale();
        
        // Эффект "всплывания" результата
        if (animProgress < 0.3f) {
            // Быстрое увеличение в первые 30%
            scale *= (1 + animProgress * 2);
        } else if (animProgress > 0.7f) {
            // Постепенное исчезновение в последние 30%
            float fadeProgress = (animProgress - 0.7f) / 0.3f;
            scale *= (1 - fadeProgress * 0.5f);
        }
        
        int radius = (int)(HIT_CIRCLE_SIZE / 2 * scale);
        int color = hitPoint.getResultColor();
        
        // Прозрачность для fade-out эффекта
        int alpha = animProgress > 0.7f ? 
                   (int)(255 * (1 - (animProgress - 0.7f) / 0.3f)) : 255;
        color = (alpha << 24) | (color & 0xFFFFFF);
        
        // Отрисовываем результат
        renderCircleFilled(graphics, centerX, centerY, radius, color);
        
        // Текст результата
        String resultText = hitPoint.getResultText();
        graphics.drawCenteredString(Minecraft.getInstance().font, resultText,
                                  centerX, centerY - radius - 15, color);
        
        // Процент точности для PERFECT/GREAT/GOOD
        if (hitPoint.isSuccessfulHit()) {
            String scoreText = hitPoint.getScorePercentage() + "%";
            graphics.drawCenteredString(Minecraft.getInstance().font, scoreText,
                                      centerX, centerY + radius + 5, 0xFFFFFFFF);
        }
    }
    
    /**
     * Отрисовывает общую информацию о QTE
     */
    private void renderQTEInfo(GuiGraphics graphics, List<QTEHitPoint> hitPoints, 
                              long currentTime, long qteStartTime) {
        // Счетчик прогресса
        int completed = 0;
        int total = hitPoints.size();
        
        for (QTEHitPoint hitPoint : hitPoints) {
            if (hitPoint.isProcessed()) {
                completed++;
            }
        }
        
        String progressText = completed + "/" + total;
        graphics.drawString(Minecraft.getInstance().font, progressText,
                          centerX - 50, centerY - 100, 0xFFFFFFFF, false);
        
        // Общее время QTE
        long elapsedTime = currentTime - qteStartTime;
        String timeText = String.format("%.1fs", elapsedTime / 1000f);
        graphics.drawString(Minecraft.getInstance().font, timeText,
                          centerX + 30, centerY - 100, 0xFFFFFFFF, false);
    }
    
    /**
     * Вспомогательный метод: отрисовка заполненного круга
     */
    private void renderCircleFilled(GuiGraphics graphics, int centerX, int centerY, 
                                   int radius, int color) {
        // Простая реализация через квадраты (можно улучшить)
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                if (x * x + y * y <= radius * radius) {
                    graphics.fill(centerX + x, centerY + y, 
                                centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }
    
    /**
     * Вспомогательный метод: отрисовка контура круга
     */
    private void renderCircleOutline(GuiGraphics graphics, int centerX, int centerY,
                                   int radius, int color, int thickness) {
        // Простая реализация через кольцо
        int outerRadiusSquared = (radius + thickness) * (radius + thickness);
        int innerRadiusSquared = radius * radius;
        
        for (int x = -radius - thickness; x <= radius + thickness; x++) {
            for (int y = -radius - thickness; y <= radius + thickness; y++) {
                int distanceSquared = x * x + y * y;
                if (distanceSquared <= outerRadiusSquared && distanceSquared >= innerRadiusSquared) {
                    graphics.fill(centerX + x, centerY + y,
                                centerX + x + 1, centerY + y + 1, color);
                }
            }
        }
    }
    
    /**
     * Преобразует GLFW код клавиши в читаемое имя
     */
    private String getKeyName(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
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
            case GLFW.GLFW_KEY_1 -> "1";
            case GLFW.GLFW_KEY_2 -> "2";
            case GLFW.GLFW_KEY_3 -> "3";
            case GLFW.GLFW_KEY_4 -> "4";
            default -> String.valueOf((char)keyCode);
        };
    }
    
    /**
     * Проверяет, активно ли QTE в данный момент
     */
    public boolean isQTEActive(List<QTEHitPoint> hitPoints, long currentTime) {
        for (QTEHitPoint hitPoint : hitPoints) {
            if (hitPoint.shouldBeVisible(currentTime, APPROACH_DURATION, RESULT_DISPLAY_TIME)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Возвращает следующую ожидающую точку попадания
     */
    public QTEHitPoint getNextWaitingHitPoint(List<QTEHitPoint> hitPoints, long currentTime) {
        for (QTEHitPoint hitPoint : hitPoints) {
            if (hitPoint.getResult() == QTEHitPoint.HitResult.WAITING &&
                currentTime >= hitPoint.getTargetTime() - APPROACH_DURATION) {
                return hitPoint;
            }
        }
        return null;
    }
}