package fr.gcjojo.worldscolliding.events;

import dev.architectury.event.EventResult;
import fr.gcjojo.worldscolliding.Config;
import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.PlayerStoryDimensionData;
import fr.gcjojo.worldscolliding.StoryDimensionData;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
import io.github.gcjojo.blablalib.BlablaLib;
import io.github.gcjojo.blablalib.events.BlablalibEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;

import java.util.*;

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
    public static void onPlayerCloned(PlayerEvent.Clone event){
        CompoundTag oldPersistentData = event.getOriginal().getPersistentData();
        CompoundTag newPersistentData = event.getEntity().getPersistentData();

        if(oldPersistentData.contains("StoryDimension"))
            newPersistentData.put("StoryDimension", oldPersistentData.getCompound("StoryDimension"));
        if(oldPersistentData.contains("LightEssence"))
            newPersistentData.putBoolean("LightEssence", oldPersistentData.getBoolean("LightEssence"));
        if(oldPersistentData.contains("ScourgeRespawnPosition"))
            newPersistentData.put("ScourgeRespawnPosition", oldPersistentData.getCompound("ScourgeRespawnPosition"));
        if(oldPersistentData.contains("IsInDialogue"))
            newPersistentData.putBoolean("IsInDialogue", false);
        if(oldPersistentData.contains("CurrentChapter"))
            newPersistentData.putString("CurrentChapter", oldPersistentData.getString("CurrentChapter"));
        if(oldPersistentData.contains("LastReadChapter"))
            newPersistentData.putString("LastReadChapter", oldPersistentData.getString("LastReadChapter"));
        if(oldPersistentData.contains("RespawnsScourge"))
            newPersistentData.putBoolean("RespawnsScourge", oldPersistentData.getBoolean("RespawnsScourge"));
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

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        Player player = event.getEntity();
        //LOGGER.info("Player {} changed dimension {}", player.getName().getString(), event.getTo().toString());

        if(event.getTo() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            //LOGGER.info("Player {} joined STORY Dimension", player.getName().getString());
            String dimension = event.getFrom().location().getPath();
            Vec3 playerPosition = player.position();
            CompoundTag playerPersistentData = player.getPersistentData();

            if(playerPersistentData.contains("StoryDimension")){
                PlayerStoryDimensionData playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));
                if(playerData.scourgeDenPlaced) {
                    playerPersistentData.put("StoryDimension", playerData.save());
                    player.teleportTo(playerData.storyDimensionSpawnpoint.x, playerData.storyDimensionSpawnpoint.y, playerData.storyDimensionSpawnpoint.z);

                    if(playerPersistentData.contains("RespawnsScourge") && playerPersistentData.getBoolean("RespawnsScourge"))
                    {
                        CompoundTag respawnPosTag = playerPersistentData.getCompound("ScourgeRespawnPosition");

                        int x = respawnPosTag.getInt("x");
                        int y = respawnPosTag.getInt("y");
                        int z = respawnPosTag.getInt("z");

                        BlockPos respawnPos = new BlockPos(x, y, z);

                        ScourgeEntity newScourge = new ScourgeEntity(ModEntities.SCOURGE.get(), player.level());
                        newScourge.setPos(respawnPos.getX() + 0.5d, respawnPos.getY() - 2.0d, respawnPos.getZ() + 0.5d);
                        player.level().addFreshEntity(newScourge);
                        playerPersistentData.remove("ScourgeRespawnPosition");
                        playerPersistentData.remove("RespawnsScourge");
                        playerPersistentData.putString("CurrentChapter", "new_game_plus_choice");
                    }
                    return;
                }
            }

            Vec3 newPlayerSpot = StoryDimensionData.getNextAvailableSpot();
            Vec3 newPlayerSpawnpoint = StoryDimensionData.getNextAvailableSpawnpoint();
            PlayerStoryDimensionData playerData = new PlayerStoryDimensionData(newPlayerSpawnpoint, dimension, playerPosition);
            playerData.scourgeDenPlaced = true;
            playerPersistentData.put("StoryDimension", playerData.save());

            BlockPos blockPos = new BlockPos((int)newPlayerSpot.x, (int)newPlayerSpot.y, (int)newPlayerSpot.z);

            MinecraftServer server = player.getServer();
            assert server != null;
            ServerLevel storyLevel = server.getLevel(ModDimensions.STORY_DIM_LEVEL_KEY);
            assert storyLevel != null;
            placeScourgeDenStructure(storyLevel, blockPos);

            player.teleportTo(newPlayerSpawnpoint.x, newPlayerSpawnpoint.y, newPlayerSpawnpoint.z);
            StoryDimensionData.setLastSpot(newPlayerSpot);
            StoryDimensionData.save(server.overworld());
        }

        if (event.getFrom() == ModDimensions.STORY_DIM_LEVEL_KEY){
            if(event.getEntity().level().isClientSide() && !(player instanceof ServerPlayer))
                return;

            if(player.getPersistentData().contains("ShowCredits") && player.getPersistentData().getBoolean("ShowCredits")) {
                ModNetwork.sendToPlayer(new ModNetwork.OpenDialoguePacket("credits"), (ServerPlayer) player);
                player.getPersistentData().putBoolean("ShowCredits", false);

                if(player.getPersistentData().contains("ScourgeRespawnPosition"))
                    player.getPersistentData().putBoolean("RespawnsScourge", true);
            }
        }
    }

    private static void placeScourgeDenStructure(ServerLevel level, BlockPos pos)
    {
        ResourceLocation structureID = ResourceLocation.tryParse(Config.storyStructure);
        StructureTemplateManager manager = level.getStructureManager();
        if(structureID == null) {
            ModEntry.getLogger().error("Structure ID {} is not a valid structure !", Config.storyStructure);
            return;
        }

        Optional<StructureTemplate> templateOpt = manager.get(structureID);

        if (templateOpt.isEmpty()) {
            ModEntry.getLogger().error("Unable to find structure : {}", structureID);
            return;
        }

        StructureTemplate template = templateOpt.get();

        StructurePlaceSettings settings = new StructurePlaceSettings()
                .setIgnoreEntities(false)
                .setRotation(Rotation.NONE)
                .setMirror(Mirror.NONE);

        template.placeInWorld(
                level,
                pos,
                pos,
                settings,
                level.random,
                2
        );
    }

    public static void registerBlablaLibEvents(){
        BlablalibEvents.DIALOGUE_COMPLETED.register((ServerPlayer player, String dialogue) -> {
            if(player != null) {
                if(dialogue.equals("worldscolliding:chapter_7_light_set") || dialogue.equals("worldscolliding:chapter_7_dark_set"))
                    spawnBoss(player, dialogue.contains("light"));
            }
            return EventResult.pass();
        });
    }

    private static void spawnBoss(ServerPlayer player, boolean isLight){
        Level level = player.level();

        player.getPersistentData().putBoolean("LightEssence", isLight);

        level.getEntities(player, player.getBoundingBox().inflate(15.0f), entity -> entity instanceof ScourgeEntity).forEach(scourge -> {
            Vec3 bossSpawnPos = scourge.getPosition(1.0f).add(0d, 2.0d, 0d);
            float xRot = scourge.getXRot();
            float yRot = scourge.getYRot();
            if(isLight)
                scourge.discard();

            CompoundTag scourgeRespawnPosTag = new CompoundTag();
            scourgeRespawnPosTag.putDouble("x", bossSpawnPos.x);
            scourgeRespawnPosTag.putDouble("y", bossSpawnPos.y);
            scourgeRespawnPosTag.putDouble("z", bossSpawnPos.z);

            player.getPersistentData().put("ScourgeRespawnPosition", scourgeRespawnPosTag);

            level.explode(scourge, bossSpawnPos.x, bossSpawnPos.y, bossSpawnPos.z, 10.0f, Level.ExplosionInteraction.NONE);
            Entity boss = new AwakenedScourgeEntity(ModEntities.AWAKENED_SCOURGE.get(), level);
            level.addFreshEntity(boss);
            boss.setPos(bossSpawnPos);
            boss.setXRot(xRot);
            boss.setYRot(yRot);

            boss.teleportTo((ServerLevel) level, bossSpawnPos.x, bossSpawnPos.y + 1.5, bossSpawnPos.z, Set.of(), xRot, yRot);
            scourge.getPersistentData().putBoolean("BossBattle", isLight);
            boss.getPersistentData().putUUID("Player", player.getUUID());
        });
    }
}