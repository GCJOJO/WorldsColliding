package fr.gcjojo.worldscolliding.client;

import net.minecraft.client.Minecraft;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;

public class BossMusicPlayer {
    private static BossMusicSound current;

    public static void playBossMusic(AwakenedScourgeEntity boss) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || boss == null) return;
        stopBossMusic();
        current = new BossMusicSound(boss);
        mc.getSoundManager().play(current);
    }

    public static void stopBossMusic() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || current == null) return;
        mc.getSoundManager().stop(current);
        current = null;
    }
}