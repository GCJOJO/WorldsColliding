package fr.gcjojo.worldscolliding.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.ModSounds;
import java.util.stream.StreamSupport; // <-- AJOUTE CET IMPORT

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean isPlaying = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        
        boolean bossFound = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof AwakenedScourgeEntity)
                .anyMatch(e -> e.distanceTo(mc.player) < 50.0);

        if (bossFound && !isPlaying) {
            BossMusicPlayer.playBossMusic(ModSounds.THE_SCOURGE_MUSIC.get());
            isPlaying = true;
        } else if (!bossFound && isPlaying) {
            BossMusicPlayer.stopBossMusic();
            isPlaying = false;
        }
    }
}