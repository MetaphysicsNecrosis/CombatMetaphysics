package com.example.examplemod.core.resources;

/**
 * Резервирование маны для составных заклинаний
 * Реализует паттерн RAII для автоматического освобождения ресурсов
 */
public class ManaReservation implements AutoCloseable {
    private final ManaPool pool;
    private final float initiationAmount;
    private final float amplificationAmount;
    private final boolean successful;
    private volatile boolean released = false;

    ManaReservation(ManaPool pool, float initiationAmount, float amplificationAmount, boolean successful) {
        this.pool = pool;
        this.initiationAmount = initiationAmount;
        this.amplificationAmount = amplificationAmount;
        this.successful = successful;
    }

    /**
     * Проверить, было ли резервирование успешным
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * Получить количество зарезервированной маны инициации
     */
    public float getInitiationAmount() {
        return successful ? initiationAmount : 0;
    }

    /**
     * Получить количество зарезервированной маны усиления
     */
    public float getAmplificationAmount() {
        return successful ? amplificationAmount : 0;
    }

    /**
     * Освободить зарезервированную ману
     */
    public void release() {
        if (successful && !released) {
            pool.releaseReservation(initiationAmount, amplificationAmount);
            released = true;
        }
    }

    /**
     * Частично использовать зарезервированную ману
     * @param useInitiation количество маны инициации к использованию
     * @param useAmplification количество маны усиления к использованию
     * @return новое резервирование с оставшейся маной
     */
    public ManaReservation partialUse(float useInitiation, float useAmplification) {
        if (!successful) {
            throw new IllegalStateException("Cannot use mana from unsuccessful reservation");
        }

        if (useInitiation > initiationAmount || useAmplification > amplificationAmount) {
            throw new IllegalArgumentException("Trying to use more mana than reserved");
        }

        float remainingInitiation = initiationAmount - useInitiation;
        float remainingAmplification = amplificationAmount - useAmplification;

        // Создаём новое резервирование с оставшейся маной
        ManaReservation remaining = new ManaReservation(pool, remainingInitiation, remainingAmplification, true);
        
        // Помечаем текущее как использованное
        released = true;
        
        return remaining;
    }

    @Override
    public void close() {
        release();
    }

    // Deprecated finalize() удален - используйте try-with-resources или явный вызов close()
}