package fr.gcjojo.worldscolliding.client;

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.ModSounds;

public class BossMusicSound extends AbstractTickableSoundInstance {
    private AwakenedScourgeEntity boss;

    public BossMusicSound(AwakenedScourgeEntity boss) {
        super(ModSounds.THE_SCOURGE_MUSIC.get(), SoundSource.MUSIC, RandomSource.create());
        this.boss = boss;
        this.attenuation = Attenuation.NONE;
        this.looping = true;
        this.delay = 0;
        this.volume = 1.0f;
        this.x = boss.getX();
        this.y = boss.getY();
        this.z = boss.getZ();
    }

    @Override
    public void tick() {
        if (boss == null || !boss.isAlive() || boss.getEntityData().get(AwakenedScourgeEntity.IS_DYING)) {
            stop();
            return;
        }
        this.x = boss.getX();
        this.y = boss.getY();
        this.z = boss.getZ();
    }
}