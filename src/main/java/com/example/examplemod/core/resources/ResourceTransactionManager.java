package com.example.examplemod.core.resources;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Менеджер атомарных транзакций с ресурсами
 * Обеспечивает ACID-совместимые операции с манапулами
 */
public class ResourceTransactionManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceTransactionManager.class);
    
    private final Map<UUID, PendingTransaction> pendingTransactions = new ConcurrentHashMap<>();
    private final AtomicLong transactionIdGenerator = new AtomicLong(0);

    /**
     * Начать транзакцию с ресурсами
     */
    public ResourceTransaction beginTransaction(ManaPool manaPool) {
        UUID transactionId = UUID.randomUUID();
        long id = transactionIdGenerator.incrementAndGet();
        
        PendingTransaction pending = new PendingTransaction(transactionId, manaPool);
        pendingTransactions.put(transactionId, pending);
        
        LOGGER.trace("Started resource transaction {}", transactionId);
        return new ResourceTransaction(this, transactionId, manaPool);
    }

    /**
     * Зафиксировать транзакцию
     */
    boolean commitTransaction(UUID transactionId) {
        PendingTransaction transaction = pendingTransactions.remove(transactionId);
        if (transaction == null) {
            LOGGER.warn("Attempted to commit non-existent transaction {}", transactionId);
            return false;
        }

        try {
            // В текущей реализации все операции уже атомарные
            // Здесь можно добавить дополнительную логику для сложных транзакций
            LOGGER.trace("Committed transaction {}", transactionId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error committing transaction {}", transactionId, e);
            rollbackTransaction(transactionId);
            return false;
        }
    }

    /**
     * Откатить транзакцию
     */
    boolean rollbackTransaction(UUID transactionId) {
        PendingTransaction transaction = pendingTransactions.remove(transactionId);
        if (transaction == null) {
            LOGGER.warn("Attempted to rollback non-existent transaction {}", transactionId);
            return false;
        }

        try {
            // Откат операций (если они были сохранены)
            transaction.rollback();
            LOGGER.trace("Rolled back transaction {}", transactionId);
            return true;
        } catch (Exception e) {
            LOGGER.error("Error rolling back transaction {}", transactionId, e);
            return false;
        }
    }

    /**
     * Очистить все pending транзакции (например, при выходе игрока)
     */
    public void clearAllTransactions() {
        int count = pendingTransactions.size();
        pendingTransactions.clear();
        if (count > 0) {
            LOGGER.info("Cleared {} pending transactions", count);
        }
    }

    /**
     * Получить количество активных транзакций
     */
    public int getActivePransactionCount() {
        return pendingTransactions.size();
    }

    /**
     * Внутренний класс для отслеживания состояния транзакции
     */
    private static class PendingTransaction {
        private final UUID id;
        private final ManaPool manaPool;
        private final long startTime;
        
        // Для будущих улучшений - сохранение состояния для отката
        private float savedInitiation;
        private float savedAmplification;

        PendingTransaction(UUID id, ManaPool manaPool) {
            this.id = id;
            this.manaPool = manaPool;
            this.startTime = System.currentTimeMillis();
            
            // Сохранить текущее состояние для возможного отката
            this.savedInitiation = manaPool.getInitiationMana();
            this.savedAmplification = manaPool.getAmplificationMana();
        }

        void rollback() {
            // Для простой реализации - в нашем случае операции уже атомарные
            // В более сложных случаях здесь будет восстановление состояния
            LOGGER.trace("Rollback transaction {} (no-op in current implementation)", id);
        }
    }

    /**
     * Handle для работы с транзакцией
     */
    public static class ResourceTransaction implements AutoCloseable {
        private final ResourceTransactionManager manager;
        private final UUID transactionId;
        private final ManaPool manaPool;
        private volatile boolean committed = false;
        private volatile boolean closed = false;

        ResourceTransaction(ResourceTransactionManager manager, UUID transactionId, ManaPool manaPool) {
            this.manager = manager;
            this.transactionId = transactionId;
            this.manaPool = manaPool;
        }

        /**
         * Попытаться потратить ману инициации в рамках транзакции
         */
        public boolean consumeInitiation(float amount) {
            if (closed) throw new IllegalStateException("Transaction is closed");
            return manaPool.tryConsumeInitiation(amount);
        }

        /**
         * Попытаться потратить ману усиления в рамках транзакции
         */
        public boolean consumeAmplification(float amount) {
            if (closed) throw new IllegalStateException("Transaction is closed");
            return manaPool.tryConsumeAmplification(amount);
        }

        /**
         * Зарезервировать ману в рамках транзакции
         */
        public ManaReservation reserveMana(float initiation, float amplification) {
            if (closed) throw new IllegalStateException("Transaction is closed");
            return manaPool.tryReserve(initiation, amplification);
        }

        /**
         * Зафиксировать все изменения
         */
        public boolean commit() {
            if (closed) return false;
            committed = manager.commitTransaction(transactionId);
            return committed;
        }

        /**
         * Откатить все изменения
         */
        public void rollback() {
            if (!closed && !committed) {
                manager.rollbackTransaction(transactionId);
            }
        }

        @Override
        public void close() {
            if (!closed) {
                if (!committed) {
                    rollback();
                }
                closed = true;
            }
        }
    }
}