package cn.zbx1425.sowcer.batch;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;

public enum BatchType {

	REGULAR(DefaultVertexFormat.ENTITY),
	INSTANCED(DefaultVertexFormat.ENTITY); // TODO: Workarounds for baked mesh. Cannot to determine runtime instanced state.

	private final VertexFormat format;

	BatchType(VertexFormat format) {
		this.format = format;
	}

	public VertexFormat getFormat() {
		return format;
	}
}
