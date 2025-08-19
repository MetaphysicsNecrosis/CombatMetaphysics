package com.example.examplemod.client.qte;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.player.LocalPlayer;

/**
 * FALLBACK Звуковая система для QTE
 * Использует встроенные звуки Minecraft вместо кастомных
 */
public class QTESoundManager {
    
    private static QTESoundManager INSTANCE;
    private final Minecraft minecraft;
    
    private QTESoundManager() {
        this.minecraft = Minecraft.getInstance();
    }
    
    public static QTESoundManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new QTESoundManager();
        }
        return INSTANCE;
    }
    
    /**
     * Проигрывает звук начала QTE
     * FALLBACK: Звук открытия энчанта
     */
    public void playQTEStart() {
        playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 0.7f, 1.0f);
    }
    
    /**
     * Проигрывает звук приближения (тихий loop)
     * FALLBACK: Тихий звук эндер-портала
     */
    public void playQTEApproach() {
        playSound(SoundEvents.PORTAL_AMBIENT, 0.2f, 0.8f);
    }
    
    /**
     * PERFECT попадание - самый приятный звук
     * FALLBACK: Звук получения достижения + experience orb
     */
    public void playQTEPerfect() {
        // Комбинация звуков для более "эпичного" эффекта
        playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0f, 1.8f);
        scheduleDelayedSound(SoundEvents.PLAYER_LEVELUP, 0.8f, 1.5f, 100); // Через 100ms
    }
    
    /**
     * GREAT попадание - приятный звук
     * FALLBACK: Звук опыта + тихий звон
     */
    public void playQTEGreat() {
        playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.4f);
        scheduleDelayedSound(SoundEvents.ANVIL_LAND, 0.3f, 2.0f, 50);
    }
    
    /**
     * GOOD попадание - нейтральный звук
     * FALLBACK: Звук клика по дереву
     */
    public void playQTEGood() {
        playSound(SoundEvents.WOOD_PLACE, 0.6f, 1.2f);
    }
    
    /**
     * OK попадание - слабый звук
     * FALLBACK: Приглушенный клик
     */
    public void playQTEOk() {
        playSound(SoundEvents.WOOL_PLACE, 0.5f, 0.9f);
    }
    
    /**
     * MISS - звук неудачи
     * FALLBACK: Звук повреждения + грустный sound
     */
    public void playQTEMiss() {
        playSound(SoundEvents.GLASS_BREAK, 0.4f, 0.7f);
    }
    
    /**
     * Слишком рано нажал
     * FALLBACK: Короткий "pop" звук
     */
    public void playQTETooEarly() {
        playSound(SoundEvents.BUBBLE_POP, 0.5f, 1.5f);
    }
    
    /**
     * Успешное завершение всего QTE комбо
     * FALLBACK: Эпичная комбинация звуков
     */
    public void playQTEComboSuccess() {
        // Восходящая мелодия из нескольких звуков
        playSound(SoundEvents.PLAYER_LEVELUP, 1.0f, 1.0f);
        scheduleDelayedSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, 1.5f, 200);
        scheduleDelayedSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.9f, 1.8f, 400);
        scheduleDelayedSound(SoundEvents.PLAYER_LEVELUP, 1.2f, 2.0f, 600);
    }
    
    /**
     * Провал комбо - разочарование
     * FALLBACK: Нисходящая последовательность звуков
     */
    public void playQTEComboFailed() {
        playSound(SoundEvents.VILLAGER_NO, 0.8f, 1.0f);
        scheduleDelayedSound(SoundEvents.GLASS_BREAK, 0.4f, 0.6f, 300);
    }
    
    /**
     * Проигрывает звук с учетом результата QTE
     */
    public void playHitResultSound(QTEHitPoint.HitResult result) {
        switch (result) {
            case PERFECT -> playQTEPerfect();
            case GREAT -> playQTEGreat();
            case GOOD -> playQTEGood();
            case OK -> playQTEOk();
            case MISS -> playQTEMiss();
            case TOO_EARLY -> playQTETooEarly();
            default -> { /* No sound for WAITING */ }
        }
    }
    
    /**
     * Базовый метод проигрывания звука
     */
    private void playSound(net.minecraft.sounds.SoundEvent soundEvent, float volume, float pitch) {
        if (minecraft.player != null && minecraft.level != null) {
            // Проигрываем звук на позиции игрока
            minecraft.level.playLocalSound(
                minecraft.player.getX(),
                minecraft.player.getY(), 
                minecraft.player.getZ(),
                soundEvent,
                SoundSource.PLAYERS,
                volume,
                pitch,
                false
            );
        }
    }
    
    /**
     * Проигрывает звук с задержкой
     */
    private void scheduleDelayedSound(net.minecraft.sounds.SoundEvent soundEvent, 
                                    float volume, float pitch, long delayMs) {
        // Простая реализация через Thread (можно улучшить)
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                playSound(soundEvent, volume, pitch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    /**
     * Останавливает все QTE звуки (если нужно)
     */
    public void stopAllQTESounds() {
        // В простой реализации просто не проигрываем новые звуки
        // Можно расширить для более точного контроля
    }
    
    /**
     * Настройка громкости QTE звуков
     */
    private float qteVolumeMultiplier = 1.0f;
    
    public void setQTEVolume(float multiplier) {
        this.qteVolumeMultiplier = Math.max(0.0f, Math.min(2.0f, multiplier));
    }
    
    public float getQTEVolume() {
        return qteVolumeMultiplier;
    }
}