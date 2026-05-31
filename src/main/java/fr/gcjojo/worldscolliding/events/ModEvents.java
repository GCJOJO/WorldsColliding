package fr.gcjojo.worldscolliding.events;

import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = ModEntry.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {

    private static class FreezeData {
        Vec3 originalPosition;
        Vec3 cameraPosition;
        float yaw;
        float pitch;
        boolean setLook;
        int ticksLeft;
        GameType previousGameMode;
        String nextDialogue;

        FreezeData(Vec3 originalPosition, Vec3 cameraPosition, float yaw, float pitch, boolean setLook, int ticksLeft, GameType previousGameMode, String nextDialogue) {
            this.originalPosition = originalPosition;
            this.cameraPosition = cameraPosition;
            this.yaw = yaw;
            this.pitch = pitch;
            this.setLook = setLook;
            this.ticksLeft = ticksLeft;
            this.previousGameMode = previousGameMode;
            this.nextDialogue = nextDialogue;
        }
    }

    private static final Map<UUID, FreezeData> frozenPlayers = new HashMap<>();
    public static final List<ItemEntity> sealItems = new ArrayList<>();

    public static void freezePlayer(UUID playerId, Vec3 position, float yaw, float pitch, int ticks, GameType prevMode, String nextDialogue) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            Vec3 orig = player.position();
            frozenPlayers.put(playerId, new FreezeData(orig, position, yaw, pitch, true, ticks, prevMode, nextDialogue));
            player.connection.teleport(position.x, position.y, position.z, yaw, pitch);
        }
    }

    public static void freezePlayer(UUID playerId, Vec3 position, int ticks, GameType prevMode, String nextDialogue) {
        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
        if (player != null) {
            Vec3 orig = player.position();
            frozenPlayers.put(playerId, new FreezeData(orig, position, 0, 0, false, ticks, prevMode, nextDialogue));
            player.connection.teleport(position.x, position.y, position.z, player.getYRot(), player.getXRot());
        }
    }

    public static void addSealItem(ItemEntity item) {
        sealItems.add(item);
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer serverPlayer) {
            FreezeData data = frozenPlayers.get(serverPlayer.getUUID());
            if (data != null) {
                serverPlayer.setDeltaMovement(0, 0, 0);
                if (data.setLook) {
                    serverPlayer.connection.teleport(data.cameraPosition.x, data.cameraPosition.y, data.cameraPosition.z, data.yaw, data.pitch);
                } else {
                    serverPlayer.connection.teleport(data.cameraPosition.x, data.cameraPosition.y, data.cameraPosition.z, serverPlayer.getYRot(), serverPlayer.getXRot());
                }
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
                        player.connection.teleport(data.originalPosition.x, data.originalPosition.y, data.originalPosition.z, player.getYRot(), player.getXRot());
                        player.setGameMode(data.previousGameMode);
                        if (!data.nextDialogue.isEmpty()) {
                            ModNetwork.sendToPlayer(new ModNetwork.OpenDialoguePacket(data.nextDialogue), player);
                        }
                    }
                    iterator.remove();
                }
            }
        }

        if (event.phase == TickEvent.Phase.END && !sealItems.isEmpty()) {
            Iterator<ItemEntity> itemIterator = sealItems.iterator();
            while (itemIterator.hasNext()) {
                ItemEntity item = itemIterator.next();

                if (!item.isAlive() || item.getItem().isEmpty()) {
                    itemIterator.remove();
                    continue;
                }

                int age = item.getPersistentData().getInt("SealAge");
                item.getPersistentData().putInt("SealAge", age + 1);

                if (age < 60) {
                    if (item.level() instanceof ServerLevel serverLevel) {
                        for (int i = 0; i < 15; i++) {
                            serverLevel.sendParticles(ParticleTypes.END_ROD,
                                    item.getX(), item.getY() + (i * 0.5), item.getZ(),
                                    1, 0.05, 0.05, 0.05, 0.0);
                        }
                    }
                    item.setDeltaMovement(0, 0.02, 0);
                }
                else if (age < 120) {
                    Player player = item.level().getNearestPlayer(item, 20.0);
                    if (player != null) {
                        Vec3 dir = player.position().add(0, 1, 0).subtract(item.position()).normalize();
                        item.setDeltaMovement(dir.scale(0.15));
                    }
                }
                else if (age == 120) {
                    item.setNoGravity(false);
                }

                if (age > 120 && item.onGround()) {
                    if (item.level() instanceof ServerLevel serverLevel) {
                        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                                item.getX(), item.getY(), item.getZ(),
                                15, 0.2, 0.2, 0.2, 0.05);
                    }
                    itemIterator.remove();
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