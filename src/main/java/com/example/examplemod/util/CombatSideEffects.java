package com.example.examplemod.util;

/**
 * WorldEdit-style система контроля побочных эффектов для массовых операций с блоками
 * Позволяет отключать/откладывать дорогие операции (lighting, neighbor updates, physics)
 */
public class CombatSideEffects {
    
    // Битовые флаги для side effects
    public static final int NONE = 0;
    public static final int UPDATE_CLIENTS = 1;      // Визуальное обновление клиента
    public static final int UPDATE_NEIGHBORS = 2;    // Уведомление соседних блоков
    public static final int UPDATE_LIGHTING = 4;     // Обновление освещения
    public static final int SPAWN_DROPS = 8;         // Спавн дропов
    public static final int UPDATE_PHYSICS = 16;     // Физические обновления (вода, лава, песок)
    public static final int SEND_PACKETS = 32;       // Отправка пакетов клиенту
    
    // Предустановленные комбинации
    public static final int ALL = UPDATE_CLIENTS | UPDATE_NEIGHBORS | UPDATE_LIGHTING | 
                                  SPAWN_DROPS | UPDATE_PHYSICS | SEND_PACKETS;
    
    // Для массового разрушения - ТОЛЬКО визуальные изменения
    public static final int MASS_DESTROY_VISUAL = UPDATE_CLIENTS | SEND_PACKETS;
    
    // Для deferred обновления после массовой операции
    public static final int MASS_DESTROY_DEFERRED = UPDATE_NEIGHBORS | UPDATE_LIGHTING | UPDATE_PHYSICS;
    
    // Полное отключение всех эффектов (максимальная производительность)
    public static final int SILENT = NONE;
    
    private final int flags;
    
    public CombatSideEffects(int flags) {
        this.flags = flags;
    }
    
    public boolean shouldUpdateClients() {
        return (flags & UPDATE_CLIENTS) != 0;
    }
    
    public boolean shouldUpdateNeighbors() {
        return (flags & UPDATE_NEIGHBORS) != 0;
    }
    
    public boolean shouldUpdateLighting() {
        return (flags & UPDATE_LIGHTING) != 0;
    }
    
    public boolean shouldSpawnDrops() {
        return (flags & SPAWN_DROPS) != 0;
    }
    
    public boolean shouldUpdatePhysics() {
        return (flags & UPDATE_PHYSICS) != 0;
    }
    
    public boolean shouldSendPackets() {
        return (flags & SEND_PACKETS) != 0;
    }
    
    /**
     * Создает новый SideEffects с добавленными флагами
     */
    public CombatSideEffects with(int additionalFlags) {
        return new CombatSideEffects(this.flags | additionalFlags);
    }
    
    /**
     * Создает новый SideEffects без указанных флагов
     */
    public CombatSideEffects without(int flagsToRemove) {
        return new CombatSideEffects(this.flags & ~flagsToRemove);
    }
    
    /**
     * Конвертирует в стандартные Minecraft флаги для setBlock()
     */
    public int toMinecraftFlags() {
        int minecraftFlags = 0;
        
        // Minecraft Block.UPDATE_NEIGHBORS = 1
        if (shouldUpdateNeighbors()) {
            minecraftFlags |= 1;
        }
        
        // Minecraft Block.UPDATE_CLIENTS = 2  
        if (shouldUpdateClients()) {
            minecraftFlags |= 2;
        }
        
        // Minecraft Block.UPDATE_INVISIBLE = 4 (не используем)
        // Minecraft Block.UPDATE_IMMEDIATE = 8 (не используем)
        // Minecraft Block.UPDATE_KNOWN_SHAPE = 16 (не используем)
        // Minecraft Block.UPDATE_SUPPRESS_DROPS = 32
        if (!shouldSpawnDrops()) {
            minecraftFlags |= 32;
        }
        
        // Minecraft Block.UPDATE_MOVE_BY_PISTON = 64 (не используем)
        // Minecraft Block.UPDATE_SUPPRESS_LIGHT = 128
        if (!shouldUpdateLighting()) {
            minecraftFlags |= 128;
        }
        
        return minecraftFlags;
    }
    
    /**
     * Создает предустановленные конфигурации
     */
    public static class Presets {
        public static final CombatSideEffects VISUAL_ONLY = new CombatSideEffects(MASS_DESTROY_VISUAL);
        public static final CombatSideEffects DEFERRED_PHYSICS = new CombatSideEffects(MASS_DESTROY_DEFERRED);
        public static final CombatSideEffects ALL_EFFECTS = new CombatSideEffects(ALL);
        public static final CombatSideEffects NO_EFFECTS = new CombatSideEffects(NONE);
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SideEffects[");
        if (shouldUpdateClients()) sb.append("CLIENTS,");
        if (shouldUpdateNeighbors()) sb.append("NEIGHBORS,");
        if (shouldUpdateLighting()) sb.append("LIGHTING,");
        if (shouldSpawnDrops()) sb.append("DROPS,");
        if (shouldUpdatePhysics()) sb.append("PHYSICS,");
        if (shouldSendPackets()) sb.append("PACKETS,");
        
        if (sb.length() > 12) {
            sb.setLength(sb.length() - 1); // Убираем последнюю запятую
        }
        sb.append("]");
        return sb.toString();
    }
}