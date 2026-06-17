package mtr.mappings;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;

public abstract class EntityRendererMapper<T extends Entity, S extends EntityRenderState> extends EntityRenderer<T, S> {

	public EntityRendererMapper(Object parameter) {
		super((EntityRendererProvider.Context) parameter);
	}
}
