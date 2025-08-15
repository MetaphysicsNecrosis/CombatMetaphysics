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
        GLFW.GLFW_KEY_R,
        CATEGORY
    );
    
    public static final KeyMapping NEXT_SPELL = new KeyMapping(
        "key.combatmetaphysics.next_spell", 
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Q,
        CATEGORY
    );
    
    // Melee combat keys - Gothic style
    public static final KeyMapping MELEE_ATTACK = new KeyMapping(
        "key.combatmetaphysics.melee_attack",
        KeyConflictContext.IN_GAME, 
        KeyModifier.NONE,
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_LEFT,
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_LEFT = new KeyMapping(
        "key.combatmetaphysics.direction_left",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_A,
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_RIGHT = new KeyMapping(
        "key.combatmetaphysics.direction_right",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_D,
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_TOP = new KeyMapping(
        "key.combatmetaphysics.direction_top",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_W,
        CATEGORY
    );
    
    public static final KeyMapping DIRECTION_THRUST = new KeyMapping(
        "key.combatmetaphysics.direction_thrust",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_S,
        CATEGORY
    );
    
    // Defensive keys
    public static final KeyMapping BLOCK = new KeyMapping(
        "key.combatmetaphysics.block",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.MOUSE,
        InputConstants.MOUSE_BUTTON_RIGHT,
        CATEGORY
    );
    
    public static final KeyMapping PARRY = new KeyMapping(
        "key.combatmetaphysics.parry",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_F,
        CATEGORY
    );
    
    public static final KeyMapping DODGE = new KeyMapping(
        "key.combatmetaphysics.dodge",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_SPACE,
        CATEGORY
    );
    
    // QTE keys
    public static final KeyMapping QTE_CONFIRM = new KeyMapping(
        "key.combatmetaphysics.qte_confirm",
        KeyConflictContext.IN_GAME,
        KeyModifier.NONE,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_E,
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