package fr.gcjojo.worldscolliding.entity;

import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ModEntry.MODID);

    public static final RegistryObject<EntityType<ScourgeEntity>> SCOURGE =
            ENTITY_TYPES.register("scourge", () -> EntityType.Builder.of(ScourgeEntity::new, MobCategory.MONSTER)
                    .sized(3.0f, 2.0f)
                    .build("scourge"));

    public static final RegistryObject<EntityType<AwakenedScourgeEntity>> AWAKENED_SCOURGE =
            ENTITY_TYPES.register("awakened_scourge", () -> EntityType.Builder.of(AwakenedScourgeEntity::new, MobCategory.MONSTER)
                    .sized(2.0f, 5.0f)
                    .clientTrackingRange(64)
                    .build("awakened_scourge"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}