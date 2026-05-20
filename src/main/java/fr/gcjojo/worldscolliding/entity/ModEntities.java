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
                    .sized(1.0f, 2.0f)
                    .build("scourge"));

    public static void register(IEventBus eventBus) {
        ENTITY_TYPES.register(eventBus);
    }
}