package fr.gcjojo.worldscolliding.client;

import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;

public class AwakenedScourgeRenderer extends GeoEntityRenderer<AwakenedScourgeEntity> {
    public AwakenedScourgeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AwakenedScourgeModel());
        this.shadowRadius = 1.0f;
    }

    @Override
    protected void applyRotations(AwakenedScourgeEntity animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90f));
    }
}