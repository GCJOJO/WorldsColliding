package fr.gcjojo.worldscolliding.client;

import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ScourgeRenderer extends GeoEntityRenderer<ScourgeEntity> {
    public ScourgeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ScourgeModel());
        this.shadowRadius = 0.5f;
    }
}