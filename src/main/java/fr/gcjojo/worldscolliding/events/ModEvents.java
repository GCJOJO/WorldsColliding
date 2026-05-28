package fr.gcjojo.worldscolliding.events;

import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

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

    private static class PlayerTeleportCredit{
        public Player player;
        public int tickDelay;

        PlayerTeleportCredit(Player player, int tickDelay){
            this.player = player;
            this.tickDelay = tickDelay;
        }
    }

    private static final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();

    private static final List<PlayerTeleportCredit> teleportCreditPlayers = new ArrayList<>();

    public static void freezePlayer(UUID playerId, Vec3 position, int ticks, GameType prevMode, String nextDialogue) {
        frozenPlayers.put(playerId, new FreezeData(position, ticks, prevMode, nextDialogue));
    }

    public static void teleportPlayerToCredits(Player player, int tickDelay){
        teleportCreditPlayers.add(new PlayerTeleportCredit(player, tickDelay));
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
        if (event.phase == TickEvent.Phase.END) {
            if(!frozenPlayers.isEmpty()){
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

            if(!teleportCreditPlayers.isEmpty()){
                Iterator<PlayerTeleportCredit> playerTeleportCreditIterator = teleportCreditPlayers.iterator();
                while(playerTeleportCreditIterator.hasNext()){
                    PlayerTeleportCredit playerTeleportCredit = playerTeleportCreditIterator.next();
                    if(playerTeleportCredit.tickDelay <= 0)
                    {
                        ServerPlayer player = (ServerPlayer) playerTeleportCredit.player;
                        if(player == null) continue;

                        CompoundTag playerPersistentData = player.getPersistentData();
                        BlockPos pos = player.getRespawnPosition();
                        player.teleportTo(player.server.getLevel(player.getRespawnDimension()), pos.getX(), pos.getY(), pos.getZ(), Set.of(), 0.0f, 0.0f);
                        playerTeleportCreditIterator.remove();
                    }

                    playerTeleportCredit.tickDelay--;
                }
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event){
        if(event.getEntity() instanceof Player && ((Player)event.getEntity()).isCreative())
            return;

        if(event.getEntity().level().dimension() == ModDimensions.STORY_DIM_LEVEL_KEY)
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event){
        if(event.getPlayer().level().dimension() == ModDimensions.STORY_DIM_LEVEL_KEY && !event.getPlayer().isCreative())
            event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent event){
        if(event.getLevel().dimension() == ModDimensions.STORY_DIM_LEVEL_KEY)
            event.setCanceled(true);
    }
}