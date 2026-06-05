package fr.gcjojo.worldscolliding.client;

import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.stream.StreamSupport;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class ClientEventHandler {
    private static boolean isPlaying = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        AwakenedScourgeEntity activeBoss = StreamSupport.stream(mc.level.entitiesForRendering().spliterator(), false)
                .filter(e -> e instanceof AwakenedScourgeEntity)
                .map(e -> (AwakenedScourgeEntity) e)
                .filter(e -> e.getEntityData().get(AwakenedScourgeEntity.IS_PLAYING_MUSIC))
                .findFirst()
                .orElse(null);

        if (activeBoss != null && !isPlaying) {
            BossMusicPlayer.playBossMusic(activeBoss);
            isPlaying = true;
        } else if (activeBoss == null && isPlaying) {
            BossMusicPlayer.stopBossMusic();
            isPlaying = false;
        }
    }
}