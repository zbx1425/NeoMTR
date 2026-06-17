package mtr;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public interface KeyMappings {

	KeyMapping LIFT_MENU = new KeyMapping("key.mtr.lift_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping TRAIN_ACCELERATE = new KeyMapping("key.mtr.train_accelerate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping TRAIN_BRAKE = new KeyMapping("key.mtr.train_brake", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping TRAIN_NEUTRAL = new KeyMapping("key.mtr.train_neutral", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping TRAIN_TOGGLE_DOORS = new KeyMapping("key.mtr.train_toggle_doors", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_1_NEGATIVE = new KeyMapping("key.mtr.debug_1_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_2_NEGATIVE = new KeyMapping("key.mtr.debug_2_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_3_NEGATIVE = new KeyMapping("key.mtr.debug_3_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_6, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_1_POSITIVE = new KeyMapping("key.mtr.debug_1_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_7, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_2_POSITIVE = new KeyMapping("key.mtr.debug_2_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_3_POSITIVE = new KeyMapping("key.mtr.debug_3_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_9, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_ROTATE_CATEGORY_NEGATIVE = new KeyMapping("key.mtr.debug_cycle_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
	KeyMapping DEBUG_ROTATE_CATEGORY_POSITIVE = new KeyMapping("key.mtr.debug_cycle_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, KeyMapping.Category.register(MTR.id("category.mtr.keybinding")));
}
