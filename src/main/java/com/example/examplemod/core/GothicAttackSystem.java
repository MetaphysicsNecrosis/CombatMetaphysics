package com.example.examplemod.core;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gothic-style трехфазная система атак с направленными ударами и коллайдерами оружия
 * Фазы: Замах -> Активная фаза -> Восстановление
 */
public class GothicAttackSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(GothicAttackSystem.class);
    
    // Активные атаки по игрокам
    private final Map<UUID, AttackData> activeAttacks = new ConcurrentHashMap<>();
    
    // Комбо цепочки
    private final Map<UUID, ComboChain> activeCombos = new ConcurrentHashMap<>();
    
    // Тайминги для разных типов оружия (в миллисекундах)
    public enum WeaponType {
        SWORD(300, 200, 400),       // Меч: быстрый и сбалансированный
        AXE(500, 150, 600),         // Топор: медленный но мощный
        SPEAR(250, 300, 300),       // Копье: быстрый замах, длинная активная фаза
        HAMMER(600, 100, 700),      // Молот: очень медленный, короткая активная фаза
        DAGGER(150, 100, 200),      // Кинжал: очень быстрый
        STAFF(400, 250, 350);       // Посох: магическое оружие
        
        public final long windupDuration;
        public final long activeDuration; 
        public final long recoveryDuration;
        
        WeaponType(long windup, long active, long recovery) {
            this.windupDuration = windup;
            this.activeDuration = active;
            this.recoveryDuration = recovery;
        }
        
        public long getTotalDuration() {
            return windupDuration + activeDuration + recoveryDuration;
        }
    }
    
    public enum AttackDirection {
        LEFT,       // Удар слева
        RIGHT,      // Удар справа
        TOP,        // Удар сверху (overhead)
        THRUST      // Выпад вперед
    }
    
    /**
     * Данные об активной атаке
     */
    public static class AttackData {
        private final UUID playerId;
        private final WeaponType weaponType;
        private final AttackDirection direction;
        private final long startTime;
        private final float baseDamage;
        private final float staminaCost;
        
        // Текущее состояние атаки
        private AttackPhase currentPhase;
        private long phaseStartTime;
        private Set<UUID> hitEntities; // Предотвращает множественные попадания
        private boolean hitDetectionPerformed; // Предотвращает спам коллайдеров
        
        public AttackData(UUID playerId, WeaponType weaponType, AttackDirection direction, 
                         float baseDamage, float staminaCost) {
            this.playerId = playerId;
            this.weaponType = weaponType;
            this.direction = direction;
            this.startTime = System.currentTimeMillis();
            this.baseDamage = baseDamage;
            this.staminaCost = staminaCost;
            this.currentPhase = AttackPhase.WINDUP;
            this.phaseStartTime = startTime;
            this.hitEntities = new HashSet<>();
            this.hitDetectionPerformed = false;
        }
        
        // Геттеры
        public UUID getPlayerId() { return playerId; }
        public WeaponType getWeaponType() { return weaponType; }
        public AttackDirection getDirection() { return direction; }
        public long getStartTime() { return startTime; }
        public float getBaseDamage() { return baseDamage; }
        public float getStaminaCost() { return staminaCost; }
        public AttackPhase getCurrentPhase() { return currentPhase; }
        public long getPhaseStartTime() { return phaseStartTime; }
        public long getTimeInCurrentPhase() { return System.currentTimeMillis() - phaseStartTime; }
        public long getTotalTime() { return System.currentTimeMillis() - startTime; }
        public Set<UUID> getHitEntities() { return hitEntities; }
        
        public void advanceToPhase(AttackPhase newPhase) {
            this.currentPhase = newPhase;
            this.phaseStartTime = System.currentTimeMillis();
            // Сбрасываем флаг hit detection при переходе в ACTIVE
            if (newPhase == AttackPhase.ACTIVE) {
                this.hitDetectionPerformed = false;
            }
        }
        
        public boolean hasHitEntity(UUID entityId) {
            return hitEntities.contains(entityId);
        }
        
        public void addHitEntity(UUID entityId) {
            hitEntities.add(entityId);
        }
        
        public boolean isHitDetectionPerformed() {
            return hitDetectionPerformed;
        }
        
        public void markHitDetectionPerformed() {
            this.hitDetectionPerformed = true;
        }
    }
    
    public enum AttackPhase {
        WINDUP,     // Замах - телеграфирование
        ACTIVE,     // Активная фаза - нанесение урона
        RECOVERY    // Восстановление - возврат в позицию
    }
    
    /**
     * Комбо цепочка атак
     */
    public static class ComboChain {
        private final UUID playerId;
        private final List<AttackDirection> sequence;
        private int currentStep;
        private long lastAttackTime;
        private int comboMultiplier;
        
        public ComboChain(UUID playerId) {
            this.playerId = playerId;
            this.sequence = new ArrayList<>();
            this.currentStep = 0;
            this.lastAttackTime = System.currentTimeMillis();
            this.comboMultiplier = 1;
        }
        
        public void addAttack(AttackDirection direction) {
            sequence.add(direction);
            currentStep++;
            lastAttackTime = System.currentTimeMillis();
            comboMultiplier = Math.min(comboMultiplier + 1, 5); // Максимум x5 комбо
        }
        
        public boolean isExpired() {
            return System.currentTimeMillis() - lastAttackTime > 1500; // 1.5s таймаут комбо
        }
        
        public float getComboMultiplier() {
            return 1.0f + (comboMultiplier - 1) * 0.2f; // +20% за каждый удар в комбо
        }
        
        public int getComboLength() { return comboMultiplier; }
        public List<AttackDirection> getSequence() { return new ArrayList<>(sequence); }
    }
    
    /**
     * Результат атаки
     */
    public static class AttackResult {
        private final boolean success;
        private final String message;
        private final int entitiesHit;
        private final float totalDamage;
        private final boolean isCombo;
        private final int comboLength;
        
        public AttackResult(boolean success, String message, int entitiesHit, 
                           float totalDamage, boolean isCombo, int comboLength) {
            this.success = success;
            this.message = message;
            this.entitiesHit = entitiesHit;
            this.totalDamage = totalDamage;
            this.isCombo = isCombo;
            this.comboLength = comboLength;
        }
        
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public int getEntitiesHit() { return entitiesHit; }
        public float getTotalDamage() { return totalDamage; }
        public boolean isCombo() { return isCombo; }
        public int getComboLength() { return comboLength; }
        public float getDamage() { return totalDamage; }
        
        // Factory methods
        public static AttackResult failed(String message) {
            return new AttackResult(false, message, 0, 0.0f, false, 0);
        }
        
        public static AttackResult success(String message, int hits, float damage, boolean isCombo, int comboLength) {
            return new AttackResult(true, message, hits, damage, isCombo, comboLength);
        }
    }
    
    /**
     * Начинает новую атаку
     */
    public AttackResult startAttack(UUID playerId, Player player, AttackDirection direction) {
        // Проверяем, нет ли уже активной атаки
        if (activeAttacks.containsKey(playerId)) {
            AttackData current = activeAttacks.get(playerId);
            
            // Если игрок в окне комбо - позволяем продолжить
            if (current.getCurrentPhase() == AttackPhase.RECOVERY) {
                long timeInRecovery = current.getTimeInCurrentPhase();
                WeaponType weapon = current.getWeaponType();
                
                // Окно комбо в последние 200ms фазы восстановления
                long comboWindow = weapon.recoveryDuration - 200;
                if (timeInRecovery >= comboWindow) {
                    return continueCombo(playerId, player, direction);
                }
            }
            
            return new AttackResult(false, "Attack already in progress", 0, 0, false, 0);
        }
        
        // Определяем тип оружия
        WeaponType weaponType = getWeaponType(player.getMainHandItem());
        
        // Рассчитываем базовый урон и стоимость выносливости
        float baseDamage = calculateBaseDamage(player, weaponType, direction);
        float staminaCost = calculateStaminaCost(weaponType, direction);
        
        // Тратим стамину через StaminaManager (если он был передан через executeAttack)
        // Стамина тратится в executeAttack методе
        
        // Создаем данные атаки
        AttackData attackData = new AttackData(playerId, weaponType, direction, baseDamage, staminaCost);
        activeAttacks.put(playerId, attackData);
        
        LOGGER.debug("Player {} started {} attack with {} (windup: {}ms, stamina cost: {})", 
                    playerId, direction, weaponType, weaponType.windupDuration, staminaCost);
        
        return new AttackResult(true, "Attack started", 0, 0, false, 0);
    }
    
    /**
     * Продолжает комбо
     */
    private AttackResult continueCombo(UUID playerId, Player player, AttackDirection direction) {
        AttackData previousAttack = activeAttacks.get(playerId);
        
        // Получаем или создаем комбо цепочку
        ComboChain combo = activeCombos.computeIfAbsent(playerId, ComboChain::new);
        combo.addAttack(direction);
        
        // Завершаем предыдущую атаку
        activeAttacks.remove(playerId);
        
        // Начинаем новую атаку с комбо бонусом
        WeaponType weaponType = previousAttack.getWeaponType();
        float baseDamage = calculateBaseDamage(player, weaponType, direction) * combo.getComboMultiplier();
        float staminaCost = calculateStaminaCost(weaponType, direction) * 0.8f; // Сниженная стоимость в комбо
        
        AttackData attackData = new AttackData(playerId, weaponType, direction, baseDamage, staminaCost);
        activeAttacks.put(playerId, attackData);
        
        LOGGER.debug("Player {} continued combo: {} hits, multiplier: {}", 
                    playerId, combo.getComboLength(), combo.getComboMultiplier());
        
        return new AttackResult(true, "Combo continued", 0, 0, true, combo.getComboLength());
    }
    
    /**
     * Обновляет все активные атаки
     */
    public void tick() {
        Iterator<Map.Entry<UUID, AttackData>> iterator = activeAttacks.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, AttackData> entry = iterator.next();
            UUID playerId = entry.getKey();
            AttackData attack = entry.getValue();
            
            boolean shouldRemove = updateAttackPhase(attack);
            if (shouldRemove) {
                iterator.remove();
                LOGGER.debug("Attack completed for player {}", playerId);
            }
        }
        
        // Очищаем истекшие комбо
        activeCombos.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * Обновляет фазу атаки
     */
    private boolean updateAttackPhase(AttackData attack) {
        WeaponType weapon = attack.getWeaponType();
        long timeInPhase = attack.getTimeInCurrentPhase();
        
        switch (attack.getCurrentPhase()) {
            case WINDUP -> {
                if (timeInPhase >= weapon.windupDuration) {
                    attack.advanceToPhase(AttackPhase.ACTIVE);
                    LOGGER.debug("Attack {} entered ACTIVE phase", attack.getPlayerId());
                }
            }
            case ACTIVE -> {
                if (timeInPhase >= weapon.activeDuration) {
                    attack.advanceToPhase(AttackPhase.RECOVERY);
                    LOGGER.debug("Attack {} entered RECOVERY phase", attack.getPlayerId());
                } else {
                    // Во время активной фазы проверяем попадания
                    performHitDetection(attack);
                }
            }
            case RECOVERY -> {
                if (timeInPhase >= weapon.recoveryDuration) {
                    // Атака завершена
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Выполняет проверку попаданий во время активной фазы
     */
    private void performHitDetection(AttackData attack) {
        UUID playerId = attack.getPlayerId();
        
        // Проверяем что hit detection еще не был выполнен для этой фазы
        if (attack.isHitDetectionPerformed()) {
            return; // Уже выполнен
        }
        
        // Получаем Player объект из кэша или из мира
        Player player = getPlayerById(playerId);
        if (player == null) {
            // Пытаемся найти игрока в мире
            player = findPlayerInWorld(playerId);
            if (player == null) {
                LOGGER.warn("Cannot perform hit detection - player not found: {}", playerId);
                return;
            }
            // Кэшируем найденного игрока
            playerInstances.put(playerId, player);
        }
        
        // Помечаем что hit detection выполняется
        attack.markHitDetectionPerformed();
        
        // Конвертируем направление для WeaponColliderSystem
        DirectionalAttackSystem.AttackDirection legacyDirection = convertToLegacyDirection(attack.getDirection());
        
        // Создаем контекст для системы коллизий
        WeaponColliderSystem.SwingContext context = new WeaponColliderSystem.SwingContext(
            player, legacyDirection, 1.0f); // TODO: Получить реальный charge multiplier
        
        // Выполняем проверку коллизий
        WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(context);
        
        if (hitResult.hasHits()) {
            processHits(attack, hitResult, player);
        } else {
            LOGGER.debug("No hits detected for attack {} direction {}", playerId, attack.getDirection());
        }
    }
    
    /**
     * Обрабатывает попадания по целям
     */
    private void processHits(AttackData attack, WeaponColliderSystem.HitResult hitResult, Player attacker) {
        for (Entity target : hitResult.getHitTargets()) {
            UUID targetId = target.getUUID();
            
            // Проверяем, не попадали ли мы уже по этой цели
            if (attack.hasHitEntity(targetId)) {
                continue;
            }
            
            // Добавляем цель в список пораженных
            attack.addHitEntity(targetId);
            
            // Рассчитываем урон
            double damageMultiplier = hitResult.getDamageMultiplier(target);
            float finalDamage = attack.getBaseDamage() * (float) damageMultiplier;
            
            // Применяем урон
            applyDamageToTarget(target, finalDamage, attacker, attack);
            
            LOGGER.debug("Hit applied to {}: damage={}, multiplier={}", 
                        target.getType().getDescription().getString(), finalDamage, damageMultiplier);
        }
    }
    
    /**
     * Применяет урон к цели
     */
    private void applyDamageToTarget(Entity target, float damage, Player attacker, AttackData attack) {
        if (!(target instanceof LivingEntity livingTarget)) {
            return;
        }
        
        // ВАЖНО: Урон должен применяться в главном потоке игры!
        // Так как мы вызываемся из ScheduledExecutorService, нужно синхронизировать с главным потоком
        if (!attacker.level().isClientSide()) {
            // Серверная сторона - планируем выполнение в главном потоке
            net.minecraft.server.MinecraftServer server = attacker.getServer();
            if (server != null) {
                server.execute(() -> {
                    // Проверяем что цель еще жива
                    if (livingTarget.isDeadOrDying()) {
                        return;
                    }
                    
                    // Применяем knockback
                    Vec3 knockback = calculateKnockback(attacker, target, attack.getDirection());
                    livingTarget.knockback(
                        knockback.length(), 
                        knockback.x, 
                        knockback.z
                    );
                    
                    // Применяем урон в главном потоке согласно NeoForge документации
                    float healthBefore = livingTarget.getHealth();
                    
                    // Создаем правильный DamageSource для игрока
                    var damageSource = attacker.level().damageSources().playerAttack(attacker);
                    
                    // Отладочная информация
                    LOGGER.debug("Applying damage: attacker={}, target={}, damage={}, health_before={}", 
                        attacker.getName().getString(), 
                        target.getType().getDescription().getString(),
                        damage,
                        healthBefore);
                    
                    // Применяем урон напрямую через метод hurt
                    livingTarget.hurt(damageSource, damage);
                    
                    float healthAfter = livingTarget.getHealth();
                    float actualDamage = healthBefore - healthAfter;
                    
                    LOGGER.info("Gothic attack damage: {} -> {} (dealt: {}/{} damage, {} direction)", 
                               attacker.getName().getString(), 
                               target.getType().getDescription().getString(), 
                               String.format("%.1f", actualDamage),
                               String.format("%.1f", damage),
                               attack.getDirection());
                    
                    // Звуковые эффекты при реальном уроне
                    if (actualDamage > 0) {
                        playHitSound(attacker, target, attack.getDirection());
                    }
                });
            } else {
                LOGGER.warn("Cannot apply damage - no server instance");
            }
        }
    }
    
    /**
     * Определяет тип урона по направлению атаки
     */
    private com.example.examplemod.core.defense.DamageType getDamageTypeForDirection(AttackDirection direction) {
        return switch (direction) {
            case LEFT, RIGHT -> com.example.examplemod.core.defense.DamageType.SLASHING;
            case TOP -> com.example.examplemod.core.defense.DamageType.BLUDGEONING;
            case THRUST -> com.example.examplemod.core.defense.DamageType.PIERCING;
        };
    }
    
    /**
     * Рассчитывает knockback в зависимости от направления атаки
     */
    private Vec3 calculateKnockback(Player attacker, Entity target, AttackDirection direction) {
        Vec3 attackVector = target.position().subtract(attacker.position()).normalize();
        
        float knockbackStrength = switch (direction) {
            case LEFT, RIGHT -> 0.4f;  // Средний knockback для боковых ударов
            case TOP -> 0.6f;           // Сильный knockback для удара сверху
            case THRUST -> 0.3f;        // Слабый knockback для выпада
        };
        
        return attackVector.scale(knockbackStrength);
    }
    
    /**
     * Воспроизводит звук попадания
     */
    private void playHitSound(Player attacker, Entity target, AttackDirection direction) {
        // TODO: Интегрировать с CombatSoundManager
        // Пока используем стандартные звуки Minecraft
        Level level = attacker.level();
        if (!level.isClientSide()) {
            level.playSound(null, target.blockPosition(),
                net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,
                net.minecraft.sounds.SoundSource.PLAYERS,
                1.0f, 1.0f);
        }
    }
    
    /**
     * Конвертирует направление атаки для совместимости с WeaponColliderSystem
     */
    private DirectionalAttackSystem.AttackDirection convertToLegacyDirection(AttackDirection direction) {
        return switch (direction) {
            case LEFT -> DirectionalAttackSystem.AttackDirection.LEFT_ATTACK;
            case RIGHT -> DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK;
            case TOP -> DirectionalAttackSystem.AttackDirection.TOP_ATTACK;
            case THRUST -> DirectionalAttackSystem.AttackDirection.THRUST_ATTACK;
        };
    }
    
    // Статическая карта для хранения Player объектов
    private static final Map<UUID, Player> playerInstances = new HashMap<>();
    
    /**
     * Execute attack through state machine integration
     */
    public AttackResult executeAttack(AttackDirection direction, StaminaManager staminaManager, Player player) {
        if (player == null) {
            return AttackResult.failed("No player instance");
        }
        
        UUID playerId = player.getUUID();
        
        // Определяем тип оружия для расчета стоимости стамины
        WeaponType weaponType = getWeaponType(player.getMainHandItem());
        float staminaCost = calculateStaminaCost(weaponType, direction);
        
        // Проверяем и тратим стамину
        if (staminaManager != null) {
            StaminaManager.StaminaData staminaData = staminaManager.getStaminaData(playerId);
            if (staminaData != null && !staminaData.tryConsume(staminaCost, "Gothic attack: " + direction)) {
                return AttackResult.failed("Not enough stamina");
            }
        }
        
        return startAttack(playerId, player, direction);
    }
    
    /**
     * Устанавливает Player объект для использования в hit detection
     */
    public static void setPlayerInstance(UUID playerId, Player player) {
        playerInstances.put(playerId, player);
    }
    
    /**
     * Check if player has active attack
     */
    public boolean hasActiveAttack(UUID playerId) {
        return activeAttacks.containsKey(playerId);
    }
    
    /**
     * Получает Player объект по ID
     */
    private static Player getPlayerById(UUID playerId) {
        return playerInstances.get(playerId);
    }
    
    /**
     * Ищет игрока в мире по UUID
     */
    private static Player findPlayerInWorld(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return null;
        }
        
        for (ServerLevel level : server.getAllLevels()) {
            Player player = level.getPlayerByUUID(playerId);
            if (player != null) {
                return player;
            }
        }
        return null;
    }
    
    /**
     * Определяет тип оружия по предмету
     */
    private WeaponType getWeaponType(ItemStack item) {
        if (item.isEmpty()) {
            return WeaponType.DAGGER; // Кулаки = кинжал
        }
        
        String itemName = item.getItem().toString().toLowerCase();
        
        if (itemName.contains("sword")) return WeaponType.SWORD;
        if (itemName.contains("axe")) return WeaponType.AXE;
        if (itemName.contains("spear") || itemName.contains("trident")) return WeaponType.SPEAR;
        if (itemName.contains("hammer") || itemName.contains("mace")) return WeaponType.HAMMER;
        if (itemName.contains("dagger") || itemName.contains("knife")) return WeaponType.DAGGER;
        if (itemName.contains("staff") || itemName.contains("wand")) return WeaponType.STAFF;
        
        return WeaponType.SWORD; // По умолчанию
    }
    
    /**
     * Рассчитывает базовый урон
     */
    private float calculateBaseDamage(Player player, WeaponType weaponType, AttackDirection direction) {
        ItemStack weapon = player.getMainHandItem();
        float baseDamage = weapon.isEmpty() ? 4.0f : 12.0f; // Увеличенный базовый урон для тестирования
        
        // Модификаторы по типу оружия
        float weaponMultiplier = switch (weaponType) {
            case DAGGER -> 0.7f;
            case SWORD -> 1.0f;
            case AXE -> 1.4f;
            case HAMMER -> 1.8f;
            case SPEAR -> 1.1f;
            case STAFF -> 0.9f;
        };
        
        // Модификаторы по направлению
        float directionMultiplier = switch (direction) {
            case LEFT, RIGHT -> 0.9f;  // Боковые удары слабее
            case TOP -> 1.2f;          // Удар сверху сильнее
            case THRUST -> 1.1f;       // Выпад средний
        };
        
        return baseDamage * weaponMultiplier * directionMultiplier;
    }
    
    /**
     * Рассчитывает стоимость выносливости
     */
    private float calculateStaminaCost(WeaponType weaponType, AttackDirection direction) {
        float baseCost = switch (weaponType) {
            case DAGGER -> 10f;
            case SWORD -> 15f;
            case AXE -> 25f;
            case HAMMER -> 30f;
            case SPEAR -> 12f;
            case STAFF -> 8f;
        };
        
        float directionMultiplier = switch (direction) {
            case LEFT, RIGHT -> 1.0f;
            case TOP -> 1.3f;  // Удар сверху дороже
            case THRUST -> 0.9f; // Выпад дешевле
        };
        
        return baseCost * directionMultiplier;
    }
    
    // === ГЕТТЕРЫ И СЛУЖЕБНЫЕ МЕТОДЫ ===
    
    public boolean isAttacking(UUID playerId) {
        return activeAttacks.containsKey(playerId);
    }
    
    public AttackData getCurrentAttack(UUID playerId) {
        return activeAttacks.get(playerId);
    }
    
    public boolean isInCombo(UUID playerId) {
        ComboChain combo = activeCombos.get(playerId);
        return combo != null && !combo.isExpired();
    }
    
    public ComboChain getCurrentCombo(UUID playerId) {
        return activeCombos.get(playerId);
    }
    
    /**
     * Принудительно отменяет атаку (при прерывании)
     */
    public void cancelAttack(UUID playerId, String reason) {
        AttackData removed = activeAttacks.remove(playerId);
        if (removed != null) {
            LOGGER.debug("Attack cancelled for player {}: {}", playerId, reason);
        }
        
        // Также очищаем комбо
        activeCombos.remove(playerId);
    }
    
    /**
     * Очищает все атаки игрока
     */
    public void clearPlayerAttacks(UUID playerId) {
        activeAttacks.remove(playerId);
        activeCombos.remove(playerId);
    }
    
    // === МЕТОДЫ ДЛЯ СОВМЕСТИМОСТИ СО СТАРОЙ СИСТЕМОЙ ===
    
    /**
     * Завершает атаку (совместимость)
     */
    public void completeAttack(UUID playerId) {
        cancelAttack(playerId, "Attack completed");
    }
    
    /**
     * Проверяет, заряжается ли атака (совместимость)
     */
    public boolean isCharging(UUID playerId) {
        AttackData attack = activeAttacks.get(playerId);
        return attack != null && attack.getCurrentPhase() == AttackPhase.WINDUP;
    }
    
    /**
     * Отменяет зарядку (совместимость) 
     */
    public void cancelCharging(UUID playerId) {
        cancelAttack(playerId, "Charging cancelled");
    }
    
    /**
     * Получает статистику для отладки
     */
    public Map<String, Object> getDebugInfo(UUID playerId) {
        Map<String, Object> info = new HashMap<>();
        
        AttackData attack = activeAttacks.get(playerId);
        if (attack != null) {
            info.put("isAttacking", true);
            info.put("weaponType", attack.getWeaponType().name());
            info.put("direction", attack.getDirection().name());
            info.put("currentPhase", attack.getCurrentPhase().name());
            info.put("timeInPhase", attack.getTimeInCurrentPhase());
            info.put("totalTime", attack.getTotalTime());
            info.put("baseDamage", attack.getBaseDamage());
            info.put("staminaCost", attack.getStaminaCost());
        } else {
            info.put("isAttacking", false);
        }
        
        ComboChain combo = activeCombos.get(playerId);
        if (combo != null && !combo.isExpired()) {
            info.put("inCombo", true);
            info.put("comboLength", combo.getComboLength());
            info.put("comboMultiplier", combo.getComboMultiplier());
            info.put("comboSequence", combo.getSequence());
        } else {
            info.put("inCombo", false);
        }
        
        return info;
    }
}