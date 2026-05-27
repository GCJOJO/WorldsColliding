package fr.gcjojo.worldscolliding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;

public class BossMusicSound extends AbstractTickableSoundInstance {
    private AwakenedScourgeEntity boss;
    private final SoundEvent soundEvent;
    private int ticksExisted = 0;

    public BossMusicSound(SoundEvent sound, AwakenedScourgeEntity boss) {
        super(sound, SoundSource.MUSIC, RandomSource.create());
        this.boss = boss;
        this.soundEvent = sound;
        this.attenuation = Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.x = boss.getX();
        this.y = boss.getY();
        this.z = boss.getZ();
        this.volume = 1.0f;
    }

    @Override
    public void tick() {
        if (boss == null || !boss.isAlive() || boss.isSilent()) {
            boss = null;
            stop();
            return;
        }
        this.x = boss.getX();
        this.y = boss.getY();
        this.z = boss.getZ();
        ticksExisted++;
        if (ticksExisted % 100 == 0) {
            Minecraft.getInstance().getMusicManager().stopPlaying();
        }
    }

    public void setBoss(AwakenedScourgeEntity boss) {
        this.boss = boss;
    }

    public AwakenedScourgeEntity getBoss() {
        return boss;
    }

    public SoundEvent getSoundEvent() {
        return soundEvent;
    }
}
