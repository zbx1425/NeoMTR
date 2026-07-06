package cn.zbx1425.sowcer.vertex;

import java.util.Objects;

public class HackGlState {

	public Integer lightmap;
	public Integer color;

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
