package mtr.mappings;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

public class ModelDataWrapper {

	public final MeshDefinition modelData;
	public final PartDefinition modelPartData;
	public ModelPart modelPart;

	public ModelDataWrapper() {
		modelData = new MeshDefinition();
		modelPartData = modelData.getRoot();
	}

	public void setModelPart(int textureWidth, int textureHeight) {
		modelPart = LayerDefinition.create(modelData, textureWidth, textureHeight).bakeRoot();
	}
}
