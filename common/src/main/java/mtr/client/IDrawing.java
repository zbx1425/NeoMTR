package mtr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.text2speech.Narrator;
import mtr.MTR;
import mtr.data.IGui;
import mtr.mappings.MatrixStackWrapper;
import mtr.mappings.Text;
import mtr.mappings.UtilitiesClient;
import mtr.render.RenderTrains;
import mtr.screen.WidgetBetterTextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public interface IDrawing {

	static void drawStringWithFont(MatrixStackWrapper matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, float x, float y, int light, TextDrawingCallback textDrawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, IGui.HorizontalAlignment.CENTER, IGui.VerticalAlignment.CENTER, x, y, -1, -1, 1, IGui.ARGB_WHITE, true, light, null, textDrawingCallback);
	}

	static void drawStringWithFont(MatrixStackWrapper matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback, TextDrawingCallback textDrawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, horizontalAlignment, x, y, maxWidth, maxHeight, scale, textColor, shadow, light, drawingCallback, textDrawingCallback);
	}

	static void drawStringWithFont(MatrixStackWrapper matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColor, boolean shadow, int light, DrawingCallback drawingCallback, TextDrawingCallback textDrawingCallback) {
		drawStringWithFont(matrices, textRenderer, immediate, text, horizontalAlignment, verticalAlignment, xAlignment, x, y, maxWidth, maxHeight, scale, textColor, textColor, 2, shadow, light, drawingCallback, textDrawingCallback);
	}

	static void drawStringWithFont(MatrixStackWrapper matrices, Font textRenderer, MultiBufferSource.BufferSource immediate, String text, IGui.HorizontalAlignment horizontalAlignment, IGui.VerticalAlignment verticalAlignment, IGui.HorizontalAlignment xAlignment, float x, float y, float maxWidth, float maxHeight, float scale, int textColorCjk, int textColor, float fontSizeRatio, boolean shadow, int light, DrawingCallback drawingCallback, TextDrawingCallback textDrawingCallback) {
		final Style style = false && Config.useMTRFont() ? Style.EMPTY.withFont(new FontDescription.Resource(MTR.id("mtr"))) : Style.EMPTY;

		while (text.contains("||")) {
			text = text.replace("||", "|");
		}
		final String[] stringSplit = text.split("\\|");

		final List<Boolean> isCJKList = new ArrayList<>();
		final List<FormattedCharSequence> orderedTexts = new ArrayList<>();
		int totalHeight = 0, totalWidth = 0;
		for (final String stringSplitPart : stringSplit) {
			final boolean isCJK = IGui.isCjk(stringSplitPart);
			isCJKList.add(isCJK);

			final FormattedCharSequence orderedText = Text.literal(stringSplitPart).setStyle(style).getVisualOrderText();
			orderedTexts.add(orderedText);

			totalHeight += IGui.LINE_HEIGHT * (isCJK ? fontSizeRatio : 1);
			final int width = (int) Math.ceil(textRenderer.width(orderedText) * (isCJK ? fontSizeRatio : 1));
			if (width > totalWidth) {
				totalWidth = width;
			}
		}

		if (maxHeight >= 0 && totalHeight / scale > maxHeight) {
			scale = totalHeight / maxHeight;
		}

		matrices.pushPose();

		final float totalWidthScaled;
		final float scaleX;
		if (maxWidth >= 0 && totalWidth > maxWidth * scale) {
			totalWidthScaled = maxWidth * scale;
			scaleX = totalWidth / maxWidth;
		} else {
			totalWidthScaled = totalWidth;
			scaleX = scale;
		}
		matrices.scale(1 / scaleX, 1 / scale, 1 / scale);

		float offset = verticalAlignment.getOffset(y * scale, totalHeight);
		for (int i = 0; i < orderedTexts.size(); i++) {
			final boolean isCJK = isCJKList.get(i);
			final float extraScale = isCJK ? fontSizeRatio : 1;
			if (isCJK) {
				matrices.pushPose();
				matrices.scale(extraScale, extraScale, 1);
			}

			final float xOffset = horizontalAlignment.getOffset(xAlignment.getOffset(x * scaleX, totalWidth), textRenderer.width(orderedTexts.get(i)) * extraScale - totalWidth);

			final float shade = light == IGui.MAX_LIGHT_GLOWING ? 1 : Math.min(LightCoordsUtil.block(light) / 16F * 0.1F + 0.7F, 1);
			final int a = ((isCJK ? textColorCjk : textColor) >> 24) & 0xFF;
			final int r = (int) ((((isCJK ? textColorCjk : textColor) >> 16) & 0xFF) * shade);
			final int g = (int) ((((isCJK ? textColorCjk : textColor) >> 8) & 0xFF) * shade);
			final int b = (int) (((isCJK ? textColorCjk : textColor) & 0xFF) * shade);

			if (immediate != null) {
				textDrawingCallback.draw(textRenderer, orderedTexts.get(i), xOffset / extraScale, offset / extraScale, (a << 24) + (r << 16) + (g << 8) + b, shadow, matrices.last(), immediate, 0, light);
			}

			if (isCJK) {
				matrices.popPose();
			}

			offset += IGui.LINE_HEIGHT * extraScale;
		}

		matrices.popPose();

		if (drawingCallback != null) {
			final float x1 = xAlignment.getOffset(x, totalWidthScaled / scale);
			final float y1 = verticalAlignment.getOffset(y, totalHeight / scale);
			drawingCallback.drawingCallback(x1, y1, x1 + totalWidthScaled / scale, y1 + totalHeight / scale);
		}
	}

	static void drawLine(RenderTrains.RenderCallback renderCallback, float x1, float y1, float z1, float x2, float y2, float z2, int r, int g, int b) {
		renderCallback.invoke(RenderTypes.lines(), (pose, vertexConsumer) -> {
			vertexConsumer.addVertex(pose, x1, y1, z1).setLineWidth(2.0f).setColor(r, g, b, 0xFF).setNormal(pose, 0, 1, 0);
			vertexConsumer.addVertex(pose, x2, y2, z2).setLineWidth(2.0f).setColor(r, g, b, 0xFF).setNormal(pose, 0, 1, 0);
		});
	}

	static void drawRectangle(Matrix3x2fStack pose, VertexConsumer vertexConsumer, double x1, double y1, double x2, double y2, int color) {
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.addVertexWith2DPose(pose, (float)x1, (float)y1).setColor(r, g, b, a);
		vertexConsumer.addVertexWith2DPose(pose, (float)x1, (float)y2).setColor(r, g, b, a);
		vertexConsumer.addVertexWith2DPose(pose, (float)x2, (float)y2).setColor(r, g, b, a);
		vertexConsumer.addVertexWith2DPose(pose, (float)x2, (float)y1).setColor(r, g, b, a);
	}

	static void drawTexture(PoseStack.Pose matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y1, z1, x2, y2, z2, 0, 0, 1, 1, facing, color, light);
	}

	static void drawTexture(PoseStack.Pose matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, Direction facing, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, 0, 0, 1, 1, facing, -1, light);
	}

	static void drawTexture(PoseStack.Pose matrices, VertexConsumer vertexConsumer, float x, float y, float width, float height, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x, y, 0, x + width, y + height, 0, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(PoseStack.Pose matrices, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		drawTexture(matrices, vertexConsumer, x1, y2, z1, x2, y2, z2, x2, y1, z2, x1, y1, z1, u1, v1, u2, v2, facing, color, light);
	}

	static void drawTexture(PoseStack.Pose pose, VertexConsumer vertexConsumer, float x1, float y1, float z1, float x2, float y2, float z2, float x3, float y3, float z3, float x4, float y4, float z4, float u1, float v1, float u2, float v2, Direction facing, int color, int light) {
		final Vec3i vec3i = facing.getUnitVec3i();
		final int a = (color >> 24) & 0xFF;
		final int r = (color >> 16) & 0xFF;
		final int g = (color >> 8) & 0xFF;
		final int b = color & 0xFF;
		if (a == 0) {
			return;
		}
		vertexConsumer.addVertex(pose.pose(), x1, y1, z1).setColor(r, g, b, a).setUv(u1, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, vec3i.getX(), vec3i.getY(), vec3i.getZ());
		vertexConsumer.addVertex(pose.pose(), x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, vec3i.getX(), vec3i.getY(), vec3i.getZ());
		vertexConsumer.addVertex(pose.pose(), x3, y3, z3).setColor(r, g, b, a).setUv(u2, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, vec3i.getX(), vec3i.getY(), vec3i.getZ());
		vertexConsumer.addVertex(pose.pose(), x4, y4, z4).setColor(r, g, b, a).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, vec3i.getX(), vec3i.getY(), vec3i.getZ());
	}

	static void setPositionAndWidth(AbstractWidget widget, int x, int y, int widgetWidth) {
		UtilitiesClient.setWidgetX(widget, x);
		UtilitiesClient.setWidgetY(widget, y);
		widget.setWidth(Mth.clamp(widgetWidth, 0, 380 - (widget instanceof WidgetBetterTextField ? IGui.TEXT_FIELD_PADDING : 0)));
	}

	static void narrateOrAnnounce(String message) {
		String newMessage = IGui.formatStationName(message).replace("  ", " ");
		if (!newMessage.isEmpty()) {
			if (Config.useTTSAnnouncements()) {
				Narrator.getNarrator().say(newMessage, true, 1.0f);
			}
			if (Config.showAnnouncementMessages()) {
				final Player player = Minecraft.getInstance().player;
				if (player != null) {
					player.sendSystemMessage(Text.literal(newMessage));
				}
			}
		}
	}

	@FunctionalInterface
	interface DrawingCallback {
		void drawingCallback(float x1, float y1, float x2, float y2);
	}

	@FunctionalInterface
	interface TextDrawingCallback {
		void draw(Font font, FormattedCharSequence formattedCharSequence, float x, float y, int color, boolean shadow, Matrix4f matrix4f, MultiBufferSource immediate, int overlay, int light);

		class World implements TextDrawingCallback {

			@Override
			public void draw(Font font, FormattedCharSequence formattedCharSequence, float x, float y, int color, boolean shadow, Matrix4f matrix4f, MultiBufferSource immediate, int overlay, int light) {
				UtilitiesClient.drawInBatch(font, formattedCharSequence, x, y, color, shadow, matrix4f, immediate, overlay, light);
			}
		}

		class GUI implements TextDrawingCallback {
			private final GuiGraphicsExtractor guiGraphicsExtractor;

			public GUI(GuiGraphicsExtractor guiGraphicsExtractor) {
				this.guiGraphicsExtractor = guiGraphicsExtractor;
			}

			@Override
			public void draw(Font font, FormattedCharSequence formattedCharSequence, float x, float y, int color, boolean shadow, Matrix4f matrix4f, MultiBufferSource immediate, int overlay, int light) {
				guiGraphicsExtractor.text(font, formattedCharSequence, (int)x, (int)y, color, shadow);
			}
		}
	}
}
