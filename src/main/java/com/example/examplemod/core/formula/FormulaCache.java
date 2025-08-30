package com.example.examplemod.core.formula;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FormulaCache {
    
    private final ConcurrentHashMap<String, CompiledFormula> cache = new ConcurrentHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final int maxCacheSize;
    private final FormulaParser parser = new FormulaParser();
    private final FormulaCompiler compiler = new FormulaCompiler();
    
    public FormulaCache(int maxCacheSize) {
        this.maxCacheSize = maxCacheSize;
    }
    
    public FormulaCache() {
        this(1000);
    }
    
    public CompiledFormula getOrCompile(String expression) {
        lock.readLock().lock();
        try {
            CompiledFormula cached = cache.get(expression);
            if (cached != null) {
                return cached;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        lock.writeLock().lock();
        try {
            CompiledFormula cached = cache.get(expression);
            if (cached != null) {
                return cached;
            }
            
            if (cache.size() >= maxCacheSize) {
                evictOldest();
            }
            
            FormulaParser.ParsedFormula parsed = parser.parse(expression);
            CompiledFormula compiled = compiler.compile(parsed);
            cache.put(expression, compiled);
            
            return compiled;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public void precompile(String expression) {
        getOrCompile(expression);
    }
    
    public void clear() {
        lock.writeLock().lock();
        try {
            cache.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public int size() {
        lock.readLock().lock();
        try {
            return cache.size();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    private void evictOldest() {
        if (!cache.isEmpty()) {
            String firstKey = cache.keys().nextElement();
            cache.remove(firstKey);
        }
    }
}