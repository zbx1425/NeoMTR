package mtr.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

public abstract class ModelDoorOverlayTopBase extends EntityModel<EntityRenderState> {

	protected ModelDoorOverlayTopBase(ModelPart root) {
		super(root);
	}

	public abstract void render(PoseStack matrices, MultiBufferSource vertexConsumers, int light, int position, float doorLeftX, float doorRightX, float doorLeftZ, float doorRightZ);
}
