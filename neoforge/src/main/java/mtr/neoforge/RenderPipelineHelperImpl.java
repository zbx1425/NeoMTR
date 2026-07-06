package mtr.neoforge;

import com.mojang.blaze3d.pipeline.RenderPipeline;

public class RenderPipelineHelperImpl {

	public static RenderPipeline.Builder asBuilder(RenderPipeline pipeline) {
		return pipeline.toBuilder();
	}
}
