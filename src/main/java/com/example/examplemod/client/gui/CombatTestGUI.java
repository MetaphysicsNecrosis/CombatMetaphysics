package com.example.examplemod.client.gui;

import com.example.examplemod.client.CombatClientManager;
import com.example.examplemod.core.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;

import java.util.UUID;

/**
 * GUI интерфейс для тестирования системы атак с коллизиями
 */
public class CombatTestGUI extends Screen {
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 25;
    
    private Button spawnMobsButton;
    private Button clearMobsButton;
    
    // Кнопки атак
    private Button horizontalSlashButton;
    private Button verticalSlashButton;
    private Button diagonalSlashButton;
    private Button thrustButton;
    
    // Кнопки тестирования коллизий
    private Button collisionTestButton;
    private Button performanceTestButton;
    
    // Информационные поля
    private String lastAttackResult = "";
    private String currentState = "IDLE";
    private int spawnedMobs = 0;
    
    public CombatTestGUI() {
        super(Component.literal("Combat System Test"));
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int startY = 40;
        int currentY = startY;
        
        // === Секция управления мобами ===
        spawnMobsButton = Button.builder(
            Component.literal("Spawn 5 Test Mobs"),
            this::spawnTestMobs
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(spawnMobsButton);
        currentY += SPACING;
        
        clearMobsButton = Button.builder(
            Component.literal("Clear All Mobs"),
            this::clearAllMobs
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(clearMobsButton);
        currentY += SPACING + 10;
        
        // === Секция атак ===
        horizontalSlashButton = Button.builder(
            Component.literal("🗡️ Horizontal Slash (LEFT/RIGHT)"),
            this::performHorizontalSlash
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(horizontalSlashButton);
        currentY += SPACING;
        
        verticalSlashButton = Button.builder(
            Component.literal("⚔️ Vertical Slash (TOP)"),
            this::performVerticalSlash
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(verticalSlashButton);
        currentY += SPACING;
        
        diagonalSlashButton = Button.builder(
            Component.literal("🔸 Diagonal Slash (COMBO)"),
            this::performDiagonalSlash
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(diagonalSlashButton);
        currentY += SPACING;
        
        thrustButton = Button.builder(
            Component.literal("🗡️ Piercing Thrust"),
            this::performThrust
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(thrustButton);
        currentY += SPACING + 10;
        
        // === Секция тестирования ===
        collisionTestButton = Button.builder(
            Component.literal("🎯 Test Collision Detection"),
            this::testCollisionDetection
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(collisionTestButton);
        currentY += SPACING;
        
        performanceTestButton = Button.builder(
            Component.literal("⚡ Performance Benchmark"),
            this::performanceBenchmark
        ).bounds(centerX - BUTTON_WIDTH/2, currentY, BUTTON_WIDTH, BUTTON_HEIGHT).build();
        addRenderableWidget(performanceTestButton);
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Рендер фона
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // Заголовок
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
        
        // Информация о текущем состоянии
        updateCurrentState();
        
        int infoY = this.height - 120;
        graphics.drawString(this.font, "Current State: " + currentState, 10, infoY, getStateColor());
        graphics.drawString(this.font, "Spawned Mobs: " + spawnedMobs, 10, infoY + 12, 0xFFAAAAAA);
        graphics.drawString(this.font, "Last Attack: " + lastAttackResult, 10, infoY + 24, 0xFFFFFFFF);
        
        // Инструкции
        graphics.drawCenteredString(this.font, "Use buttons to test combat system", this.width / 2, infoY + 50, 0xFF888888);
        graphics.drawCenteredString(this.font, "Watch top-left corner for real-time state changes", this.width / 2, infoY + 62, 0xFF888888);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    private void updateCurrentState() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(mc.player.getUUID());
            if (stateMachine != null) {
                currentState = stateMachine.getCurrentState().name();
            }
        }
    }
    
    private int getStateColor() {
        return switch (currentState) {
            case "MELEE_ATTACKING", "MELEE_PREPARING", "MELEE_CHARGING" -> 0xFFFF6B35; // Оранжевый
            case "MAGIC_CASTING", "MAGIC_PREPARING" -> 0xFF64B5F6; // Синий
            case "BLOCKING", "PARRYING", "DODGING" -> 0xFF4CAF50; // Зеленый
            case "INTERRUPTED" -> 0xFFFF0000; // Красный
            default -> 0xFFFFFFFF; // Белый
        };
    }
    
    // === Обработчики кнопок ===
    
    private void spawnTestMobs(Button button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            var playerPos = mc.player.position();
            int spawned = 0;
            
            for (int i = 0; i < 5; i++) {
                // Спавним мобов по кругу
                double angle = (2 * Math.PI * i) / 5;
                double distance = 3.0 + Math.random() * 2.0;
                
                double x = playerPos.x + Math.cos(angle) * distance;
                double y = playerPos.y;
                double z = playerPos.z + Math.sin(angle) * distance;
                
                try {
                    Zombie zombie = new Zombie(EntityType.ZOMBIE, mc.level);
                    zombie.setPos(x, y, z);
                    zombie.setCustomName(Component.literal("Test Target " + (i + 1)));
                    zombie.setCustomNameVisible(true);
                    
                    if (mc.level.addFreshEntity(zombie)) {
                        spawned++;
                    }
                } catch (Exception e) {
                    // Игнорируем ошибки спавна
                }
            }
            
            spawnedMobs += spawned;
            lastAttackResult = String.format("Spawned %d mobs", spawned);
        }
    }
    
    private void clearAllMobs(Button button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.level != null) {
            var playerPos = mc.player.position();
            int cleared = 0;
            
            // Удаляем всех мобов в радиусе 20 блоков с именем "Test Target"
            var nearbyEntities = mc.level.getEntities(mc.player, 
                mc.player.getBoundingBox().inflate(20.0));
                
            for (var entity : nearbyEntities) {
                if (entity instanceof Zombie && entity.hasCustomName() && 
                    entity.getCustomName().getString().startsWith("Test Target")) {
                    entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
                    cleared++;
                }
            }
            
            spawnedMobs = Math.max(0, spawnedMobs - cleared);
            lastAttackResult = String.format("Cleared %d mobs", cleared);
        }
    }
    
    private void performHorizontalSlash(Button button) {
        executeAttackWithPattern("Horizontal Slash", DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK, 
            "Wide horizontal arc - good for multiple enemies");
    }
    
    private void performVerticalSlash(Button button) {
        executeAttackWithPattern("Vertical Slash", DirectionalAttackSystem.AttackDirection.TOP_ATTACK,
            "Powerful overhead strike - high damage");
    }
    
    private void performDiagonalSlash(Button button) {
        // Комбо атака: сначала LEFT потом RIGHT (БЕЗ THREADING!)
        try {
            executeAttackWithPattern("Diagonal Slash (Part 1)", DirectionalAttackSystem.AttackDirection.LEFT_ATTACK,
                "Quick diagonal combo");
            
            // Небольшая задержка между частями комбо
            Thread.sleep(200);
            
            executeAttackWithPattern("Diagonal Slash (Part 2)", DirectionalAttackSystem.AttackDirection.RIGHT_ATTACK,
                "Finishing diagonal strike");
                
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            lastAttackResult = "Diagonal Combo: Interrupted";
        }
    }
    
    private void performThrust(Button button) {
        executeAttackWithPattern("Piercing Thrust", DirectionalAttackSystem.AttackDirection.THRUST_ATTACK,
            "Armor-piercing forward strike");
    }
    
    private void executeAttackWithPattern(String attackName, DirectionalAttackSystem.AttackDirection direction, String description) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            UUID playerId = mc.player.getUUID();
            PlayerStateMachine stateMachine = CombatClientManager.getInstance().getPlayerState(playerId);
            
            if (stateMachine != null) {
                try {
                    // ИСПРАВЛЕНИЕ: Принудительно очищаем ActionResolver перед началом
                    stateMachine.forceTransition(com.example.examplemod.core.PlayerState.IDLE, "GUI attack prep - clear previous");
                    
                    // Небольшая пауза для обновления системы
                    Thread.sleep(50);
                    
                    // Gothic Attack System
                    GothicAttackSystem.AttackDirection gothicDir = convertToGothicDirection(direction);
                    GothicAttackSystem.AttackResult result = stateMachine.startGothicAttack(gothicDir);
                    
                    if (result.isSuccess()) {
                        // Wait for attack to complete (Gothic system handles timing)
                        Thread.sleep(500);
                        
                        // Применяем урон через нашу систему коллизий
                        WeaponColliderSystem.SwingContext swingContext = new WeaponColliderSystem.SwingContext(
                            mc.player, direction, 1.0f);
                        WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(swingContext);
                        
                        // Apply damage through collision system
                        int actualHits = applyDamageToHits(hitResult, result.getDamage());
                        
                        // Return to peaceful state
                        stateMachine.transitionTo(PlayerState.PEACEFUL, "GUI attack completed");
                        
                        lastAttackResult = String.format("%s: %.1f dmg, %d hits%s", 
                            attackName, result.getDamage(), actualHits,
                            result.isCombo() ? " [COMBO x" + result.getComboLength() + "]" : "");
                    } else {
                        lastAttackResult = attackName + ": " + result.getMessage();
                        // Return to peaceful state on failure
                        stateMachine.transitionTo(PlayerState.PEACEFUL, "GUI attack failed");
                    }
                } catch (Exception e) {
                    lastAttackResult = attackName + ": Error - " + e.getMessage();
                    // Очищаем состояние даже при ошибке
                    try {
                        stateMachine.forceTransition(com.example.examplemod.core.PlayerState.IDLE, "GUI attack error - cleanup");
                    } catch (Exception cleanup) {
                        // Игнорируем ошибки очистки
                    }
                }
            }
        }
    }
    
    /**
     * НОВЫЙ МЕТОД: Применяет реальный урон к найденным целям
     */
    private int applyDamageToHits(WeaponColliderSystem.HitResult hitResult, float baseDamage) {
        if (!hitResult.hasHits()) {
            return 0;
        }
        
        int damaged = 0;
        Minecraft mc = Minecraft.getInstance();
        
        for (var target : hitResult.getHitTargets()) {
            if (target instanceof net.minecraft.world.entity.LivingEntity livingTarget && mc.player != null) {
                try {
                    // Рассчитываем урон с учетом collision multiplier
                    double collisionMultiplier = hitResult.getDamageMultiplier(target);
                    float finalDamage = (float) (baseDamage * collisionMultiplier);
                    
                    // Применяем урон!
                    livingTarget.hurt(livingTarget.damageSources().playerAttack(mc.player), finalDamage);
                    
                    // Добавляем knockback
                    net.minecraft.world.phys.Vec3 knockbackDirection = target.position()
                        .subtract(mc.player.position()).normalize();
                    target.setDeltaMovement(target.getDeltaMovement()
                        .add(knockbackDirection.scale(0.5)));
                    
                    damaged++;
                } catch (Exception e) {
                    // Игнорируем ошибки урона
                }
            }
        }
        
        return damaged;
    }
    
    private void testCollisionDetection(Button button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // Тест всех направлений атак
            DirectionalAttackSystem.AttackDirection[] directions = DirectionalAttackSystem.AttackDirection.values();
            int totalHits = 0;
            double totalTime = 0;
            
            for (var direction : directions) {
                WeaponColliderSystem.SwingContext swingContext = new WeaponColliderSystem.SwingContext(
                    mc.player, direction, 1.0f);
                    
                long startTime = System.nanoTime();
                WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(swingContext);
                double duration = (System.nanoTime() - startTime) / 1_000_000.0;
                
                totalHits += hitResult.getHitTargets().size();
                totalTime += duration;
            }
            
            lastAttackResult = String.format("Collision Test: %d hits, %.2fms avg", 
                totalHits, totalTime / directions.length);
        }
    }
    
    private void performanceBenchmark(Button button) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int iterations = 50;
            long totalTime = 0;
            int totalHits = 0;
            
            for (int i = 0; i < iterations; i++) {
                DirectionalAttackSystem.AttackDirection direction = 
                    DirectionalAttackSystem.AttackDirection.values()[i % 4];
                    
                WeaponColliderSystem.SwingContext swingContext = new WeaponColliderSystem.SwingContext(
                    mc.player, direction, 1.0f);
                    
                long startTime = System.nanoTime();
                WeaponColliderSystem.HitResult hitResult = WeaponColliderSystem.performCollisionSweep(swingContext);
                totalTime += System.nanoTime() - startTime;
                totalHits += hitResult.getHitTargets().size();
            }
            
            double avgTimeMs = (totalTime / iterations) / 1_000_000.0;
            double avgHits = (double) totalHits / iterations;
            
            lastAttackResult = String.format("Benchmark: %.3fms avg, %.1f hits avg", avgTimeMs, avgHits);
        }
    }
    
    private GothicAttackSystem.AttackDirection convertToGothicDirection(DirectionalAttackSystem.AttackDirection direction) {
        return switch (direction) {
            case LEFT_ATTACK -> GothicAttackSystem.AttackDirection.LEFT;
            case RIGHT_ATTACK -> GothicAttackSystem.AttackDirection.RIGHT;
            case TOP_ATTACK -> GothicAttackSystem.AttackDirection.TOP;
            case THRUST_ATTACK -> GothicAttackSystem.AttackDirection.THRUST;
        };
    }
    
    @Override
    public boolean isPauseScreen() {
        return false; // Не ставим игру на паузу
    }
}