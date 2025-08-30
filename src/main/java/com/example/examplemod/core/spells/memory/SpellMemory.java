package com.example.examplemod.core.spells.memory;

import com.example.examplemod.core.spells.SpellDefinition;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;

import java.util.*;

/**
 * Спеллбук - хранилище созданных заклинаний для игрока
 * Работает как система макросов согласно концепту:
 * - Сохранение успешных комбинаций
 * - Быстрый вызов заготовленных заклинаний  
 * - Обмен конфигурациями между игроками
 * - Ограниченное вместилище (память игрока)
 */
public class SpellMemory {
    
    private static final int DEFAULT_CAPACITY = 20; // Базовая вместимость спеллбука
    private static final int MAX_CAPACITY = 100;    // Максимальная вместимость
    
    private final UUID ownerId;
    private final Map<String, StoredSpell> spells; // Название -> Заклинание
    private int capacity;
    private final Set<String> favorites; // Избранные заклинания
    private final Map<String, String> categories; // Название -> Категория
    
    public SpellMemory(UUID ownerId) {
        this.ownerId = ownerId;
        this.spells = new LinkedHashMap<>(); // Сохраняем порядок добавления
        this.capacity = DEFAULT_CAPACITY;
        this.favorites = new HashSet<>();
        this.categories = new HashMap<>();
    }
    
    /**
     * Сохранить заклинание в память
     */
    public boolean storeSpell(String name, SpellDefinition definition, String description) {
        if (spells.size() >= capacity && !spells.containsKey(name)) {
            return false; // Нет места
        }
        
        if (name == null || name.trim().isEmpty()) {
            return false; // Неверное имя
        }
        
        StoredSpell stored = new StoredSpell(
            name.trim(),
            definition,
            description != null ? description.trim() : "",
            System.currentTimeMillis(),
            0 // Счетчик использований
        );
        
        spells.put(name, stored);
        return true;
    }
    
    /**
     * Получить заклинание по имени
     */
    public StoredSpell getSpell(String name) {
        StoredSpell spell = spells.get(name);
        if (spell != null) {
            // Увеличиваем счетчик использований
            spell.incrementUsageCount();
        }
        return spell;
    }
    
    /**
     * Удалить заклинание
     */
    public boolean removeSpell(String name) {
        if (spells.remove(name) != null) {
            favorites.remove(name);
            categories.remove(name);
            return true;
        }
        return false;
    }
    
    /**
     * Переименовать заклинание
     */
    public boolean renameSpell(String oldName, String newName) {
        if (newName == null || newName.trim().isEmpty() || spells.containsKey(newName)) {
            return false;
        }
        
        StoredSpell spell = spells.remove(oldName);
        if (spell == null) return false;
        
        // Обновляем имя и перезаписываем
        spell.name = newName.trim();
        spells.put(newName, spell);
        
        // Обновляем связанные данные
        if (favorites.remove(oldName)) {
            favorites.add(newName);
        }
        String category = categories.remove(oldName);
        if (category != null) {
            categories.put(newName, category);
        }
        
        return true;
    }
    
    /**
     * Получить все заклинания
     */
    public Collection<StoredSpell> getAllSpells() {
        return new ArrayList<>(spells.values());
    }
    
    /**
     * Получить заклинания по категории
     */
    public List<StoredSpell> getSpellsByCategory(String category) {
        return spells.values().stream()
            .filter(spell -> category.equals(categories.get(spell.name)))
            .toList();
    }
    
    /**
     * Получить избранные заклинания
     */
    public List<StoredSpell> getFavoriteSpells() {
        return spells.values().stream()
            .filter(spell -> favorites.contains(spell.name))
            .toList();
    }
    
    /**
     * Получить самые используемые заклинания
     */
    public List<StoredSpell> getMostUsedSpells(int limit) {
        return spells.values().stream()
            .sorted((a, b) -> Integer.compare(b.usageCount, a.usageCount))
            .limit(limit)
            .toList();
    }
    
    /**
     * Добавить/убрать из избранных
     */
    public void toggleFavorite(String name) {
        if (spells.containsKey(name)) {
            if (favorites.contains(name)) {
                favorites.remove(name);
            } else {
                favorites.add(name);
            }
        }
    }
    
    /**
     * Установить категорию для заклинания
     */
    public void setSpellCategory(String spellName, String category) {
        if (spells.containsKey(spellName)) {
            if (category == null || category.trim().isEmpty()) {
                categories.remove(spellName);
            } else {
                categories.put(spellName, category.trim());
            }
        }
    }
    
    /**
     * Получить все категории
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(categories.values());
    }
    
    /**
     * Проверить, можно ли добавить заклинание
     */
    public boolean canAddSpell() {
        return spells.size() < capacity;
    }
    
    /**
     * Получить использование памяти
     */
    public int getUsedSlots() {
        return spells.size();
    }
    
    /**
     * Получить вместимость
     */
    public int getCapacity() {
        return capacity;
    }
    
    /**
     * Увеличить вместимость (через предметы/навыки)
     */
    public void increaseCapacity(int amount) {
        this.capacity = Math.min(MAX_CAPACITY, this.capacity + amount);
    }
    
    /**
     * Экспортировать заклинание для обмена
     */
    public CompoundTag exportSpell(String name) {
        StoredSpell spell = spells.get(name);
        if (spell == null) return null;
        
        CompoundTag tag = new CompoundTag();
        tag.putString("name", spell.name);
        tag.putString("description", spell.description);
        tag.put("definition", spell.definition.toNBT());
        tag.putLong("created", spell.createdTime);
        tag.putString("category", categories.getOrDefault(name, ""));
        
        return tag;
    }
    
    /**
     * Импортировать заклинание от другого игрока
     */
    public boolean importSpell(CompoundTag tag, String newName) {
        if (!canAddSpell()) return false;
        
        try {
            String originalName = tag.getStringOr("name", "");
            String description = tag.getStringOr("description", "");
            SpellDefinition definition = SpellDefinition.fromNBT(tag.getCompoundOrEmpty("definition"));
            String category = tag.getStringOr("category", "");
            
            String importName = newName != null && !newName.trim().isEmpty() ? 
                newName.trim() : originalName + " (импорт)";
                
            if (storeSpell(importName, definition, description)) {
                if (!category.isEmpty()) {
                    setSpellCategory(importName, category);
                }
                return true;
            }
        } catch (Exception e) {
            // Неверный формат
        }
        
        return false;
    }
    
    /**
     * Сохранение в NBT
     */
    public CompoundTag saveToNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("owner", ownerId.toString());
        tag.putInt("capacity", capacity);
        
        ListTag spellList = new ListTag();
        for (StoredSpell spell : spells.values()) {
            CompoundTag spellTag = new CompoundTag();
            spellTag.putString("name", spell.name);
            spellTag.putString("description", spell.description);
            spellTag.put("definition", spell.definition.toNBT());
            spellTag.putLong("created", spell.createdTime);
            spellTag.putInt("usage", spell.usageCount);
            spellList.add(spellTag);
        }
        tag.put("spells", spellList);
        
        // Избранные
        ListTag favoriteList = new ListTag();
        for (String favorite : favorites) {
            CompoundTag favTag = new CompoundTag();
            favTag.putString("name", favorite);
            favoriteList.add(favTag);
        }
        tag.put("favorites", favoriteList);
        
        // Категории
        CompoundTag categoryTag = new CompoundTag();
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            categoryTag.putString(entry.getKey(), entry.getValue());
        }
        tag.put("categories", categoryTag);
        
        return tag;
    }
    
    /**
     * Загрузка из NBT
     */
    public static SpellMemory loadFromNBT(CompoundTag tag) {
        UUID ownerId = UUID.fromString(tag.getStringOr("owner", ""));
        SpellMemory memory = new SpellMemory(ownerId);
        memory.capacity = tag.getIntOr("capacity", DEFAULT_CAPACITY);
        
        // Загружаем заклинания
        ListTag spellList = tag.getListOrEmpty("spells");
        for (int i = 0; i < spellList.size(); i++) {
            if (spellList.get(i) instanceof CompoundTag spellTag) {
            try {
                String name = spellTag.getStringOr("name", "");
                String description = spellTag.getStringOr("description", "");
                SpellDefinition definition = SpellDefinition.fromNBT(spellTag.getCompoundOrEmpty("definition"));
                long created = spellTag.getLongOr("created", System.currentTimeMillis());
                int usage = spellTag.getIntOr("usage", 0);
                
                StoredSpell stored = new StoredSpell(name, definition, description, created, usage);
                memory.spells.put(name, stored);
            } catch (Exception e) {
                // Пропускаем поврежденные заклинания
            }
            }
        }
        
        // Загружаем избранные
        ListTag favoriteList = tag.getListOrEmpty("favorites");
        for (int i = 0; i < favoriteList.size(); i++) {
            if (favoriteList.get(i) instanceof CompoundTag favTag) {
            String name = favTag.getStringOr("name", "");
            if (memory.spells.containsKey(name)) {
                memory.favorites.add(name);
            }
            }
        }
        
        // Загружаем категории
        CompoundTag categoryTag = tag.getCompoundOrEmpty("categories");
        for (String key : categoryTag.keySet()) {
            if (memory.spells.containsKey(key)) {
                memory.categories.put(key, categoryTag.getStringOr(key, ""));
            }
        }
        
        return memory;
    }
    
    /**
     * Сохраненное заклинание с метаданными
     */
    public static class StoredSpell {
        public String name;
        public final SpellDefinition definition;
        public final String description;
        public final long createdTime;
        public int usageCount;
        
        public StoredSpell(String name, SpellDefinition definition, String description, long createdTime, int usageCount) {
            this.name = name;
            this.definition = definition;
            this.description = description;
            this.createdTime = createdTime;
            this.usageCount = usageCount;
        }
        
        public void incrementUsageCount() {
            this.usageCount++;
        }
        
        public boolean isRecent() {
            return System.currentTimeMillis() - createdTime < 24 * 60 * 60 * 1000; // 24 часа
        }
        
        public boolean isPopular() {
            return usageCount > 10;
        }
    }
}