package com.example.examplemod.api.threads.examples;

import com.example.examplemod.api.threads.MainThreadSynchronizerAPI;
import com.example.examplemod.api.threads.IMainThreadSynchronizer;
import java.util.concurrent.CompletableFuture;

/**
 * Пример игрового модуля, демонстрирующий реальные сценарии использования
 * 
 * Показывает как типичные игровые системы (блоки, заклинания, эффекты)
 * могут использовать MainThreadSynchronizer для безопасной работы с Main Thread
 */
public class GameplayModuleExample {
    
    private final String moduleName = "GameplayModule";
    private IMainThreadSynchronizer.IModuleHandle moduleHandle;
    
    public void initialize() {
        IMainThreadSynchronizer.ModuleInfo moduleInfo = new IMainThreadSynchronizer.ModuleInfo(
            moduleName,
            "1.0.0",
            "Модуль игровых механик",
            IMainThreadSynchronizer.ModuleWeights.highPriorityFocused() // Геймплей критичен
        );
        
        moduleHandle = MainThreadSynchronizerAPI.registerModule(moduleName, moduleInfo);
        System.out.println("GameplayModuleExample initialized");
    }
    
    /**
     * Симуляция установки блока (критическая операция)
     */
    public void simulateBlockPlacement() {
        MainThreadSynchronizerAPI.executeHighPriority(moduleName + "_PlaceBlock", () -> {
            // В реальном коде здесь был бы level.setBlock()
            System.out.println("[HIGH] Установка блока в позиции (10, 64, 20)");
            
            // Симуляция быстрой операции
            try { Thread.sleep(5); } catch (InterruptedException e) {}
        });
    }
    
    /**
     * Симуляция обновления блока (обычная операция)
     */
    public void simulateBlockUpdate() {
        MainThreadSynchronizerAPI.execute(moduleName + "_UpdateBlock", () -> {
            System.out.println("[NORMAL] Обновление состояния блока");
            
            // Симуляция обновления тика блока
            try { Thread.sleep(10); } catch (InterruptedException e) {}
        });
    }
    
    /**
     * Симуляция применения заклинания (критическая операция с результатом)
     */
    public CompletableFuture<Integer> simulateSpellCasting() {
        return MainThreadSynchronizerAPI.submit(moduleName + "_CastSpell", () -> {
            System.out.println("[HIGH] Применение заклинания Fireball");
            
            // Симуляция вычисления урона
            int baseDamage = 50;
            int criticalBonus = (Math.random() > 0.8) ? 25 : 0;
            int totalDamage = baseDamage + criticalBonus;
            
            if (criticalBonus > 0) {
                System.out.println("  Критический удар! Урон: " + totalDamage);
            } else {
                System.out.println("  Обычный урон: " + totalDamage);
            }
            
            // Симуляция применения урона
            try { Thread.sleep(15); } catch (InterruptedException e) {}
            
            return totalDamage;
        });
    }
    
    /**
     * Симуляция обработки визуальных эффектов (низкий приоритет)
     */
    public void simulateParticleEffects() {
        MainThreadSynchronizerAPI.executeLowPriority(moduleName + "_ParticleEffect", () -> {
            System.out.println("[LOW] Воспроизведение частиц взрыва");
            
            // Симуляция создания частиц
            for (int i = 0; i < 5; i++) {
                System.out.println("  Частица #" + (i + 1) + " создана");
                try { Thread.sleep(2); } catch (InterruptedException e) {}
            }
        });
    }
    
    /**
     * Симуляция сохранения данных игрока (критическое действие с retry)
     */
    public void simulatePlayerDataSave() {
        String taskName = moduleName + "_SavePlayerData";
        
        CompletableFuture<Void> future = MainThreadSynchronizerAPI.getInstance()
            .executeWithRetry(taskName, () -> {
                System.out.println("[HIGH] Сохранение данных игрока...");
                
                // Симуляция возможной ошибки сохранения
                if (Math.random() < 0.3) {
                    throw new RuntimeException("Ошибка записи на диск");
                }
                
                System.out.println("  Данные игрока успешно сохранены");
                
            }, 3, IMainThreadSynchronizer.TaskPriority.HIGH);
        
        future.whenComplete((result, error) -> {
            if (error != null) {
                System.err.println("КРИТИЧЕСКАЯ ОШИБКА: Не удалось сохранить данные игрока после 3 попыток!");
                // В реальном коде здесь была бы экстренная обработка
            }
        });
    }
    
    /**
     * Симуляция периодического обновления мира (например, рост растений)
     */
    public IMainThreadSynchronizer.IScheduledTask simulateWorldTick() {
        String taskName = moduleName + "_WorldTick";
        
        return MainThreadSynchronizerAPI.getInstance().scheduleRepeating(taskName, () -> {
            System.out.println("[LOW] Тик мира: проверка роста растений");
            
            // Симуляция обновления роста
            int plantsGrown = (int) (Math.random() * 3);
            if (plantsGrown > 0) {
                System.out.println("  " + plantsGrown + " растений выросло");
            }
            
        }, 5000, IMainThreadSynchronizer.TaskPriority.LOW); // Каждые 5 секунд
    }
    
    /**
     * Симуляция сложной комбинированной операции
     */
    public void simulateComplexGameplayAction() {
        System.out.println("\n=== Начинаем сложное игровое действие ===");
        
        // 1. Сначала кастуем заклинание
        simulateSpellCasting()
            .thenAccept(damage -> {
                System.out.println("Заклинание нанесло " + damage + " урона");
                
                // 2. Создаем визуальные эффекты на основе урона
                if (damage > 60) {
                    simulateParticleEffects(); // Мощный эффект
                    
                    // 3. Если критический урон, разрушаем блоки
                    MainThreadSynchronizerAPI.executeHighPriority(moduleName + "_DestroyBlocks", () -> {
                        System.out.println("[HIGH] Разрушение блоков от мощного заклинания");
                    });
                }
                
                // 4. Сохраняем статистику (может подождать)
                MainThreadSynchronizerAPI.executeLowPriority(moduleName + "_UpdateStats", () -> {
                    System.out.println("[LOW] Обновление статистики урона игрока");
                });
            })
            .exceptionally(error -> {
                System.err.println("Ошибка заклинания: " + error.getMessage());
                return null;
            });
    }
    
    /**
     * Симуляция пакетной операции для массовых изменений
     */
    public void simulateMassBlockUpdate() {
        String batchName = moduleName + "_MassBlockUpdate";
        
        IMainThreadSynchronizer.TaskBatch batch = new IMainThreadSynchronizer.TaskBatch() {
            @Override
            public void execute() throws Exception {
                System.out.println("Начинаю массовое обновление блоков (например, взрыв)");
                
                // Симуляция обновления множества блоков
                for (int x = 0; x < 5; x++) {
                    for (int z = 0; z < 5; z++) {
                        System.out.println("  Обновляю блок в (" + x + ", 64, " + z + ")");
                        Thread.sleep(1); // Симуляция обработки блока
                    }
                }
                
                System.out.println("Массовое обновление завершено");
            }
            
            @Override
            public String getDescription() {
                return "Обновление блоков в области 5x5 после взрыва";
            }
            
            @Override
            public int getEstimatedTaskCount() {
                return 25; // 5x5 блоков
            }
        };
        
        MainThreadSynchronizerAPI.getInstance()
            .executeBatch(batchName, batch, IMainThreadSynchronizer.TaskPriority.HIGH)
            .whenComplete((result, error) -> {
                if (error != null) {
                    System.err.println("Ошибка массового обновления: " + error.getMessage());
                } else {
                    System.out.println("Массовое обновление блоков успешно завершено");
                }
            });
    }
    
    /**
     * Получить статистику производительности игрового модуля
     */
    public void printGameplayPerformance() {
        System.out.println("\n=== ПРОИЗВОДИТЕЛЬНОСТЬ ИГРОВОГО МОДУЛЯ ===");
        
        if (moduleHandle != null) {
            IMainThreadSynchronizer.ModuleStats stats = moduleHandle.getStats();
            
            System.out.println("Всего игровых действий: " + stats.totalTasksSubmitted());
            System.out.println("Успешно выполнено: " + stats.totalTasksCompleted());
            System.out.println("Ошибок: " + stats.totalTasksFailed());
            System.out.println("Среднее время выполнения: " + String.format("%.2f ms", stats.averageExecutionTimeMs()));
            
            // Анализ производительности
            if (stats.averageExecutionTimeMs() > 50) {
                System.out.println("⚠️  ВНИМАНИЕ: Среднее время выполнения высокое!");
            }
            
            double failureRate = (double) stats.totalTasksFailed() / stats.totalTasksSubmitted();
            if (failureRate > 0.05) {
                System.out.println("⚠️  ВНИМАНИЕ: Высокий процент ошибок (" + String.format("%.2f%%", failureRate * 100) + ")");
            }
        }
        
        // Проверяем общее здоровье системы
        IMainThreadSynchronizer.SystemHealthReport health = MainThreadSynchronizerAPI.getInstance().getSystemHealthReport();
        System.out.println("Общее здоровье системы: " + health.status());
        
        if (health.status() != IMainThreadSynchronizer.HealthStatus.HEALTHY) {
            System.out.println("Рекомендуется снизить нагрузку или оптимизировать игровые операции");
        }
    }
    
    public void shutdown() {
        if (moduleHandle != null) {
            printGameplayPerformance();
            moduleHandle.unregister();
            moduleHandle = null;
        }
        System.out.println("GameplayModuleExample shutdown");
    }
    
    // === Демонстрационный main ===
    
    public static void main(String[] args) throws InterruptedException {
        GameplayModuleExample example = new GameplayModuleExample();
        
        example.initialize();
        
        // Симулируем различные игровые сценарии
        example.simulateBlockPlacement();
        example.simulateBlockUpdate();
        example.simulateParticleEffects();
        
        // Запускаем периодический тик мира
        IMainThreadSynchronizer.IScheduledTask worldTick = example.simulateWorldTick();
        
        // Выполняем сложное действие
        example.simulateComplexGameplayAction();
        
        // Массовое обновление
        example.simulateMassBlockUpdate();
        
        // Критичное сохранение
        example.simulatePlayerDataSave();
        
        // Даем системе поработать
        Thread.sleep(10000);
        
        // Останавливаем периодические задачи
        worldTick.cancel();
        
        // Показываем производительность
        example.printGameplayPerformance();
        
        example.shutdown();
    }
}