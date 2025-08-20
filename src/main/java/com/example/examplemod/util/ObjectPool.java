package com.example.examplemod.util;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * FAWE-style Object Pool для минимизации garbage collection
 * Используется для часто создаваемых объектов типа BlockState, ItemStack, Vec3 и т.д.
 */
public class ObjectPool<T> {
    private final ConcurrentLinkedQueue<T> pool = new ConcurrentLinkedQueue<>();
    private final Supplier<T> factory;
    private final int maxSize;
    
    public ObjectPool(Supplier<T> factory, int maxSize) {
        this.factory = factory;
        this.maxSize = maxSize;
    }
    
    /**
     * Получить объект из пула (или создать новый)
     */
    public T acquire() {
        T object = pool.poll();
        return object != null ? object : factory.get();
    }
    
    /**
     * Вернуть объект в пул для переиспользования
     */
    public void release(T object) {
        if (object != null && pool.size() < maxSize) {
            pool.offer(object);
        }
    }
    
    /**
     * Очистить пул (для освобождения памяти)
     */
    public void clear() {
        pool.clear();
    }
    
    /**
     * Получить текущий размер пула
     */
    public int size() {
        return pool.size();
    }
}