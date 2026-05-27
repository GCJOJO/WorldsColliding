package fr.gcjojo.worldscolliding;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, "worldscolliding");

    public static final RegistryObject<SoundEvent> SWORD_DRAW = registerSound("sword_draw");
    public static final RegistryObject<SoundEvent> PROTOSS_ELECTRIC = registerSound("protoss_electric");
    public static final RegistryObject<SoundEvent> MASTER_SWORD = registerSound("zelda_master_sword");
    public static final RegistryObject<SoundEvent> THE_SCOURGE_MUSIC = registerSound("the_scourge_music");

    public static final RegistryObject<SoundEvent> ENTER_GANONDORF = registerSound("enterganondorf");
    public static final RegistryObject<SoundEvent> LAUGH = registerSound("laugh");
    public static final RegistryObject<SoundEvent> SWITCH_CLICK = registerSound("switchclick");

    private static RegistryObject<SoundEvent> registerSound(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("worldscolliding", name)));
    }

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}