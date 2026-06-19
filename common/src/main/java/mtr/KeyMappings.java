package mtr;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public interface KeyMappings {
	
	static KeyMapping.Category MTR_CATEGORY = KeyMapping.Category.register(MTR.id("category.mtr.keybinding"));

	KeyMapping LIFT_MENU = new KeyMapping("key.mtr.lift_menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, MTR_CATEGORY);
	KeyMapping TRAIN_ACCELERATE = new KeyMapping("key.mtr.train_accelerate", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UP, MTR_CATEGORY);
	KeyMapping TRAIN_BRAKE = new KeyMapping("key.mtr.train_brake", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_DOWN, MTR_CATEGORY);
	KeyMapping TRAIN_NEUTRAL = new KeyMapping("key.mtr.train_neutral", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT, MTR_CATEGORY);
	KeyMapping TRAIN_TOGGLE_DOORS = new KeyMapping("key.mtr.train_toggle_doors", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT, MTR_CATEGORY);
	KeyMapping DEBUG_1_NEGATIVE = new KeyMapping("key.mtr.debug_1_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_4, MTR_CATEGORY);
	KeyMapping DEBUG_2_NEGATIVE = new KeyMapping("key.mtr.debug_2_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_5, MTR_CATEGORY);
	KeyMapping DEBUG_3_NEGATIVE = new KeyMapping("key.mtr.debug_3_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_6, MTR_CATEGORY);
	KeyMapping DEBUG_1_POSITIVE = new KeyMapping("key.mtr.debug_1_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_7, MTR_CATEGORY);
	KeyMapping DEBUG_2_POSITIVE = new KeyMapping("key.mtr.debug_2_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_8, MTR_CATEGORY);
	KeyMapping DEBUG_3_POSITIVE = new KeyMapping("key.mtr.debug_3_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_9, MTR_CATEGORY);
	KeyMapping DEBUG_ROTATE_CATEGORY_NEGATIVE = new KeyMapping("key.mtr.debug_cycle_negative", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_SUBTRACT, MTR_CATEGORY);
	KeyMapping DEBUG_ROTATE_CATEGORY_POSITIVE = new KeyMapping("key.mtr.debug_cycle_positive", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_KP_ADD, MTR_CATEGORY);
}
