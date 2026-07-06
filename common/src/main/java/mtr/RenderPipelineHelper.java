package mtr;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.architectury.injectables.annotations.ExpectPlatform;

public class RenderPipelineHelper {

	@ExpectPlatform
	public static RenderPipeline.Builder asBuilder(RenderPipeline pipeline) {
		throw new AssertionError();
	}
}
