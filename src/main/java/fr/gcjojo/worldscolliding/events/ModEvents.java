package fr.gcjojo.worldscolliding.events;

import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModEntry.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static class FreezeData {
        Vec3 position;
        int ticksLeft;
        GameType previousGameMode;
        String nextDialogue;

        FreezeData(Vec3 position, int ticksLeft, GameType previousGameMode, String nextDialogue) {
            this.position = position;
            this.ticksLeft = ticksLeft;
            this.previousGameMode = previousGameMode;
            this.nextDialogue = nextDialogue;
        }
    }

    private static final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();

    public static void freezePlayer(UUID playerId, Vec3 position, int ticks, GameType prevMode, String nextDialogue) {
        frozenPlayers.put(playerId, new FreezeData(position, ticks, prevMode, nextDialogue));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            FreezeData data = frozenPlayers.get(serverPlayer.getUUID());
            if (data != null) {
                serverPlayer.setDeltaMovement(0, 0, 0);
                serverPlayer.teleportTo(data.position.x, data.position.y, data.position.z);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !frozenPlayers.isEmpty()) {
            Iterator<Map.Entry<UUID, FreezeData>> iterator = frozenPlayers.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<UUID, FreezeData> entry = iterator.next();
                FreezeData data = entry.getValue();
                data.ticksLeft--;

                if (data.ticksLeft <= 0) {
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(entry.getKey());

                    if (player != null) {
                        player.setGameMode(data.previousGameMode);
                        ModNetwork.sendToPlayer(new ModNetwork.OpenDialoguePacket(data.nextDialogue), player);
                    }
                    iterator.remove();
                }
            }
        }
    }
}