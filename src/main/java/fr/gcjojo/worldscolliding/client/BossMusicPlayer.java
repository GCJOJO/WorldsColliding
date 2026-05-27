package fr.gcjojo.worldscolliding.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;

public class BossMusicPlayer {
    private static SimpleSoundInstance current;

    public static void playBossMusic(SoundEvent sound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || sound == null) return;
        stopBossMusic();
        current = SimpleSoundInstance.forMusic(sound);
        mc.getSoundManager().play(current);
    }

    public static void stopBossMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || current == null) return;
        mc.getSoundManager().stop(current);
        current = null;
    }
}

