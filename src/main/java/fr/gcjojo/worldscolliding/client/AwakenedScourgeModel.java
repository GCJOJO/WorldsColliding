package fr.gcjojo.worldscolliding.client;

import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AwakenedScourgeModel extends GeoModel<AwakenedScourgeEntity> {
    @Override
    public ResourceLocation getModelResource(AwakenedScourgeEntity object) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "geo/awakened_scourge.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AwakenedScourgeEntity object) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "textures/entity/awakened_scourge.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AwakenedScourgeEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath("worldscolliding", "animations/awakened_scourge.animation.json");
    }
}