package com.example.examplemod.core.resources;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Пул маны игрока с двумя типами:
 * - Мана Инициации (для начала заклинаний)
 * - Мана Усиления (для QTE и поддержания заклинаний)
 * 
 * Thread-safe implementation с атомарными операциями
 */
public class ManaPool {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Мана инициации
    private final AtomicReference<Float> initiationMana = new AtomicReference<>(0f);
    private final AtomicReference<Float> maxInitiationMana = new AtomicReference<>(100f);
    
    // Мана усиления  
    private final AtomicReference<Float> amplificationMana = new AtomicReference<>(0f);
    private final AtomicReference<Float> maxAmplificationMana = new AtomicReference<>(200f);
    
    // Скорости регенерации (mana per tick)
    private final AtomicReference<Float> initiationRegenRate = new AtomicReference<>(0.1f);
    private final AtomicReference<Float> amplificationRegenRate = new AtomicReference<>(0.2f);

    public ManaPool() {
        // Полный пул по умолчанию
        this.initiationMana.set(maxInitiationMana.get());
        this.amplificationMana.set(maxAmplificationMana.get());
    }

    public ManaPool(float maxInitiation, float maxAmplification) {
        this.maxInitiationMana.set(maxInitiation);
        this.maxAmplificationMana.set(maxAmplification);
        this.initiationMana.set(maxInitiation);
        this.amplificationMana.set(maxAmplification);
    }

    /**
     * Попытаться потратить ману инициации
     * @return true если мана была потрачена успешно
     */
    public boolean tryConsumeInitiation(float amount) {
        if (amount <= 0) return true;
        
        float before = initiationMana.get();
        float after = initiationMana.updateAndGet(current -> 
            current >= amount ? current - amount : current
        );
        
        return after < before; // Транзакция прошла если значение уменьшилось
    }

    /**
     * Попытаться потратить ману усиления
     * @return true если мана была потрачена успешно
     */
    public boolean tryConsumeAmplification(float amount) {
        if (amount <= 0) return true;
        
        float before = amplificationMana.get();
        float after = amplificationMana.updateAndGet(current -> 
            current >= amount ? current - amount : current
        );
        
        return after < before; // Транзакция прошла если значение уменьшилось
    }

    /**
     * Атомарное резервирование маны (для составных заклинаний)
     */
    public ManaReservation tryReserve(float initiationAmount, float amplificationAmount) {
        lock.writeLock().lock();
        try {
            float currentInitiation = initiationMana.get();
            float currentAmplification = amplificationMana.get();
            
            if (currentInitiation >= initiationAmount && currentAmplification >= amplificationAmount) {
                initiationMana.set(currentInitiation - initiationAmount);
                amplificationMana.set(currentAmplification - amplificationAmount);
                
                return new ManaReservation(this, initiationAmount, amplificationAmount, true);
            }
            
            return new ManaReservation(this, 0, 0, false);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Вернуть зарезервированную ману
     */
    void releaseReservation(float initiationAmount, float amplificationAmount) {
        if (initiationAmount > 0) {
            initiationMana.updateAndGet(current -> 
                Math.min(current + initiationAmount, maxInitiationMana.get()));
        }
        
        if (amplificationAmount > 0) {
            amplificationMana.updateAndGet(current -> 
                Math.min(current + amplificationAmount, maxAmplificationMana.get()));
        }
    }

    /**
     * Регенерация маны (вызывается каждый тик)
     */
    public void regenerate() {
        // Регенерация инициации
        initiationMana.updateAndGet(current -> 
            Math.min(current + initiationRegenRate.get(), maxInitiationMana.get()));
            
        // Регенерация усиления
        amplificationMana.updateAndGet(current -> 
            Math.min(current + amplificationRegenRate.get(), maxAmplificationMana.get()));
    }

    // Getters
    public float getInitiationMana() { return initiationMana.get(); }
    public float getMaxInitiationMana() { return maxInitiationMana.get(); }
    public float getAmplificationMana() { return amplificationMana.get(); }
    public float getMaxAmplificationMana() { return maxAmplificationMana.get(); }

    // Utility methods
    public float getInitiationPercentage() {
        return getInitiationMana() / getMaxInitiationMana();
    }

    public float getAmplificationPercentage() {
        return getAmplificationMana() / getMaxAmplificationMana();
    }

    public boolean hasInitiation(float amount) {
        return initiationMana.get() >= amount;
    }

    public boolean hasAmplification(float amount) {
        return amplificationMana.get() >= amount;
    }

    // Setters for configuration
    public void setMaxInitiationMana(float max) {
        maxInitiationMana.set(max);
    }

    public void setMaxAmplificationMana(float max) {
        maxAmplificationMana.set(max);
    }

    public void setInitiationRegenRate(float rate) {
        initiationRegenRate.set(rate);
    }

    public void setAmplificationRegenRate(float rate) {
        amplificationRegenRate.set(rate);
    }
}