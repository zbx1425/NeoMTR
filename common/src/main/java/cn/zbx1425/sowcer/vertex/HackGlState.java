package cn.zbx1425.sowcer.vertex;

import cn.zbx1425.sowcer.ContextCapability;
import org.lwjgl.opengl.GL33;

import java.util.Objects;

public class HackGlState {

	public Integer lightmap;
	public Integer color;

	private boolean appliedColor;
	private boolean appliedLightmap;

	public HackGlState() {
		this.lightmap = null;
		this.color = null;
	}

	public HackGlState setLightmap(int lightmap) {
		this.lightmap = lightmap;
		return this;
	}

	public HackGlState setLightmapUV(short u, short v) {
		this.lightmap = u << 16 | v;
		return this;
	}

	public HackGlState setColor(int r, int g, int b, int a) {
		this.color = r << 24 | g << 16 | b << 8 | a;
		return this;
	}

	public HackGlState setColor(int rgba) {
		this.color = rgba;
		return this;
	}

	public HackGlState copy() {
		HackGlState clone = new HackGlState();
		clone.lightmap = lightmap;
		clone.color = color;
		return clone;
	}

	public static HackGlState merge(HackGlState enqueue, HackGlState material) {
		HackGlState result = new HackGlState();
		if (material != null) {
			result.color = material.color;
			result.lightmap = material.lightmap;
		}
		if (enqueue != null) {
			if (result.color == null) result.color = enqueue.color;
			if (result.lightmap == null) result.lightmap = enqueue.lightmap;
		}
		if (result.color == null && result.lightmap == null) {
			return null;
		}
		return result;
	}

	private static HackGlState pendingState = null;

	public void applyLater() {
		pendingState = this;
	}

	public static void applyPendingState() {
		if (pendingState != null) {
			pendingState.applyInternal();
			pendingState = null;
		}
	}

	public void applyInternal() {
		if (color != null) {
			VertAttrType.COLOR.toggleAttrArray(false);
			GL33.glVertexAttrib4f(VertAttrType.COLOR.location,
					((color >>> 24) & 0xFF) / 255f, ((color >>> 16) & 0xFF) / 255f,
					((color >>> 8) & 0xFF) / 255f, (color & 0xFF) / 255f);
			appliedColor = true;
		}
		if (lightmap != null) {
			VertAttrType.UV_LIGHTMAP.toggleAttrArray(false);
			if (!ContextCapability.isGL4ES) {
				GL33.glVertexAttribI2i(VertAttrType.UV_LIGHTMAP.location,
						(short) (lightmap >>> 16), (short) (int) lightmap);
			} else {
				GL33.glVertexAttrib2f(VertAttrType.UV_LIGHTMAP.location,
						(short) (lightmap >>> 16), (short) (int) lightmap);
			}
			appliedLightmap = true;
		}
	}

	public void restore() {
		if (appliedColor) {
			VertAttrType.COLOR.toggleAttrArray(true);
			appliedColor = false;
		}
		if (appliedLightmap) {
			VertAttrType.UV_LIGHTMAP.toggleAttrArray(true);
			appliedLightmap = false;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		HackGlState that = (HackGlState) o;
		return Objects.equals(lightmap, that.lightmap) && Objects.equals(color, that.color);
	}

	@Override
	public int hashCode() {
		return Objects.hash(lightmap, color);
	}
}
