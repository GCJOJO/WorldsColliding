package fr.gcjojo.worldscolliding.client;

import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ScourgeModel extends GeoModel<ScourgeEntity> {
    @Override
    public ResourceLocation getModelResource(ScourgeEntity object) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "geo/scourge.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(ScourgeEntity object) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "textures/entity/scourge.png");
    }

    @Override
    public ResourceLocation getAnimationResource(ScourgeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "animations/scourge.animation.json");
    }
}