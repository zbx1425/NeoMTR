package cn.zbx1425.mtrsteamloco.render.block;

import cn.zbx1425.mtrsteamloco.MainClient;
import cn.zbx1425.mtrsteamloco.block.BlockEyeCandy;
import cn.zbx1425.mtrsteamloco.data.EyeCandyProperties;
import cn.zbx1425.mtrsteamloco.data.EyeCandyRegistry;
import cn.zbx1425.mtrsteamloco.render.ShadersModHandler;
import cn.zbx1425.mtrsteamloco.render.rail.RailRenderDispatcher;
import cn.zbx1425.mtrsteamloco.render.scripting.eyecandy.EyeCandyScriptContext;
import cn.zbx1425.sowcer.math.Matrix4f;
import cn.zbx1425.sowcer.math.PoseStackUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import mtr.RegistryObject;
import mtr.block.IBlock;
import mtr.mappings.BlockEntityRendererMapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityEyeCandyRenderer extends BlockEntityRendererMapper<BlockEyeCandy.BlockEntityEyeCandy, BlockEntityEyeCandyRenderer.EyecandyRenderState> {

    private static final RegistryObject<ItemStack> BRUSH_ITEM_STACK = new RegistryObject<>(() -> new ItemStack(mtr.Items.BRUSH.get(), 1));
    private static final RegistryObject<ItemStack> BARRIER_ITEM_STACK = new RegistryObject<>(() -> new ItemStack(net.minecraft.world.item.Items.BARRIER, 1));

    public BlockEntityEyeCandyRenderer(BlockEntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void submit(EyecandyRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        Matrix4f candyPose = new Matrix4f(poseStack.last().pose()).copy();

        if (state.prop == null || RailRenderDispatcher.isHoldingBrush) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.5f, 0.5f);
            PoseStackUtil.rotY(poseStack, (float) ((System.currentTimeMillis() % 1000) * (Math.PI * 2 / 1000)));
            state.itemBlockRenderState.submit(poseStack, submitNodeCollector, state.lightToUse, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.prop == null) return;

        // RenderLevelStageEvent is NOT fired during shadow pass.
        // Enqueueing geometry when submit is called during shadow pass will result in them being drawn in later pass
        // instead, and with a wrong transform.
        // Need to investigate how to actually support drawing things in Iris's shadow pass.
        if (ShadersModHandler.isRenderingShadowPass()) return;

        candyPose.translate(0.5f, 0f, 0.5f);
        candyPose.translate(state.translateX, state.translateY, state.translateZ);
        candyPose.rotateY(-(float)Math.toRadians(state.blockYRotation) + (float)(Math.PI));
        candyPose.rotateX(state.rotateX);
        candyPose.rotateY(state.rotateY);
        candyPose.rotateZ(state.rotateZ);
        if (state.prop.model != null) {
            MainClient.drawScheduler.enqueue(state.prop.model, candyPose, state.lightToUse);
        }
        if (state.prop.script != null) {
            synchronized (state.scriptContext) {
                state.scriptContext.scriptResult.commit(MainClient.drawScheduler, candyPose, state.lightToUse);
            }
        }
    }

    @Override
    public EyecandyRenderState createRenderState() {
        return new EyecandyRenderState();
    }

    @Override
    public void extractRenderState(BlockEyeCandy.BlockEntityEyeCandy blockEntity, EyecandyRenderState state, final float partialTicks, final Vec3 cameraPosition, final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.fullLight = blockEntity.fullLight;
        state.prefabId = blockEntity.prefabId;
        state.prop = EyeCandyRegistry.getProperty(state.prefabId);
        state.translateX = blockEntity.translateX;
        state.translateY = blockEntity.translateY;
        state.translateZ = blockEntity.translateZ;
        state.rotateX = blockEntity.rotateX;
        state.rotateY = blockEntity.rotateY;
        state.rotateZ = blockEntity.rotateZ;
        state.scriptContext = blockEntity.scriptContext;
        state.blockYRotation = IBlock.getStatePropertySafe(blockEntity.getBlockState(), BlockEyeCandy.FACING).toYRot();
        state.lightToUse = blockEntity.fullLight ? LightCoordsUtil.pack(15, 15) : state.lightCoords;

        ItemStack itemToShow = blockEntity.prefabId != null && state.prop == null ? BARRIER_ITEM_STACK.get() : BRUSH_ITEM_STACK.get();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.itemBlockRenderState, itemToShow, ItemDisplayContext.GROUND, blockEntity.getLevel(), null, 0);

        if (state.prop != null && state.prop.script != null) {
            state.prop.script.tryCallRenderFunctionAsync(state.scriptContext);
        }

        // TODO: Temp code for custom PSD
        blockEntity.tickDoor();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public boolean shouldRender(BlockEyeCandy.BlockEntityEyeCandy blockEntity, Vec3 vec3) {
        return true;
    }

    public static class EyecandyRenderState extends BlockEntityRenderState {
        ItemStackRenderState itemBlockRenderState = new ItemStackRenderState();
        String prefabId;
        EyeCandyProperties prop;
        EyeCandyScriptContext scriptContext;
        boolean fullLight;
        int lightToUse;
        float blockYRotation;
        float translateX;
        float translateY;
        float translateZ;
        float rotateX;
        float rotateY;
        float rotateZ;
    }
}
