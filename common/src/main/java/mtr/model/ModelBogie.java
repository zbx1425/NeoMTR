package mtr.model;

import com.mojang.blaze3d.vertex.PoseStack;
import mtr.mappings.ModelDataWrapper;
import mtr.mappings.ModelMapper;
import mtr.render.MoreRenderLayers;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Map;

public class ModelBogie extends EntityModel<EntityRenderState> {

	private final ModelMapper bogie;
	private final Identifier texture = Identifier.parse("mtr:textures/vehicle/bogie_1.png");

	public ModelBogie() {
		super(new ModelPart(List.of(), Map.of()));
        final int textureWidth = 186;
		final int textureHeight = 77;

		final ModelDataWrapper modelDataWrapper = new ModelDataWrapper();

		bogie = new ModelMapper(modelDataWrapper);
		bogie.setPos(0, 24, 0);
		bogie.texOffs(0, 0).addBox(-14, 1, -32, 1, 13, 64, 0, false);
		bogie.texOffs(0, 0).addBox(-13, 1.5F, 16, 1, 14, 14, 0, false);
		bogie.texOffs(0, 0).addBox(-13, 1.5F, -30, 1, 14, 14, 0, false);
		bogie.texOffs(66, 0).addBox(-13, 1, -23.5F, 13, 8, 47, 0, false);

		modelDataWrapper.setModelPart(textureWidth, textureHeight);
		bogie.setModelPart();
	}

	public void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int position) {
		ModelTrainBase.renderMirror(bogie, matrices, vertexConsumers.getBuffer(MoreRenderLayers.getExterior(texture)), light, position);
	}
}
