package com.example.examplemod.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

public class CombatKeyBindings {
    public static final String CATEGORY = "key.categories.combatmetaphysics";
    
    // Magic combat keys
    public static final KeyMapping CAST_SPELL = new KeyMapping(
        "key.combatmetaphysics.cast_spell",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping NEXT_SPELL = new KeyMapping(
        "key.combatmetaphysics.next_spell", 
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    // Melee combat keys - Gothic style
    public static final KeyMapping MELEE_ATTACK = new KeyMapping(
        "key.combatmetaphysics.melee_attack",
        KeyConflictContext.IN_GAME, 
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_LEFT = new KeyMapping(
        "key.combatmetaphysics.direction_left",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_RIGHT = new KeyMapping(
        "key.combatmetaphysics.direction_right",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_TOP = new KeyMapping(
        "key.combatmetaphysics.direction_top",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_THRUST = new KeyMapping(
        "key.combatmetaphysics.direction_thrust",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    // Defensive keys
    public static final KeyMapping BLOCK = new KeyMapping(
        "key.combatmetaphysics.legacy_block",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping PARRY = new KeyMapping(
        "key.combatmetaphysics.legacy_parry",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping DODGE = new KeyMapping(
        "key.combatmetaphysics.legacy_dodge",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    // QTE keys
    public static final KeyMapping QTE_CONFIRM = new KeyMapping(
        "key.combatmetaphysics.qte_confirm",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    // НОВЫЕ КЛАВИШИ ТЕСТИРОВАНИЯ АТАК
    public static final KeyMapping TEST_HORIZONTAL_SLASH = new KeyMapping(
        "key.combatmetaphysics.test_horizontal",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping TEST_VERTICAL_SLASH = new KeyMapping(
        "key.combatmetaphysics.test_vertical",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping TEST_DIAGONAL_COMBO = new KeyMapping(
        "key.combatmetaphysics.test_diagonal",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping TEST_THRUST_ATTACK = new KeyMapping(
        "key.combatmetaphysics.test_thrust",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping SPAWN_TEST_MOBS = new KeyMapping(
        "key.combatmetaphysics.spawn_mobs",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    public static final KeyMapping CLEAR_TEST_MOBS = new KeyMapping(
        "key.combatmetaphysics.clear_mobs",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        InputConstants.UNKNOWN.getValue(),
        CATEGORY
    );
    
    // Utility methods
    public static boolean isAttackHeld() {
        return MELEE_ATTACK.isDown();
    }
    
    public static boolean isBlockHeld() {
        return BLOCK.isDown();
    }
    
    public static boolean isCastSpellPressed() {
        return CAST_SPELL.consumeClick();
    }
    
    public static boolean isParryPressed() {
        return PARRY.consumeClick();
    }
    
    public static boolean isDodgePressed() {
        return DODGE.consumeClick();
    }
    
    public static boolean isQteConfirmPressed() {
        return QTE_CONFIRM.consumeClick();
    }
    
    // НОВЫЕ методы для тестовых клавиш
    public static boolean isTestHorizontalPressed() {
        return TEST_HORIZONTAL_SLASH.consumeClick();
    }
    
    public static boolean isTestVerticalPressed() {
        return TEST_VERTICAL_SLASH.consumeClick();
    }
    
    public static boolean isTestDiagonalPressed() {
        return TEST_DIAGONAL_COMBO.consumeClick();
    }
    
    public static boolean isTestThrustPressed() {
        return TEST_THRUST_ATTACK.consumeClick();
    }
    
    public static boolean isSpawnMobsPressed() {
        return SPAWN_TEST_MOBS.consumeClick();
    }
    
    public static boolean isClearMobsPressed() {
        return CLEAR_TEST_MOBS.consumeClick();
    }
    
    public static AttackDirection getCurrentAttackDirection() {
        if (DIRECTION_LEFT.isDown()) return AttackDirection.LEFT;
        if (DIRECTION_RIGHT.isDown()) return AttackDirection.RIGHT;
        if (DIRECTION_TOP.isDown()) return AttackDirection.TOP;
        if (DIRECTION_THRUST.isDown()) return AttackDirection.THRUST;
        return AttackDirection.NONE;
    }
    
    public enum AttackDirection {
        NONE,
        LEFT,    // Быстрая, низкий урон, сложно парировать
        RIGHT,   // Средняя скорость, средний урон
        TOP,     // Медленная, высокий урон, пробивает блок
        THRUST   // Быстрая, колющая, игнорирует броню
    }
}