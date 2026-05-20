package fr.gcjojo.worldscolliding;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class StoryDimensionData {

    private static Vec3 lastSpot = Vec3.ZERO;
    // List of players' uuid to know who came into this dimension before
    private static HashMap<String, PlayerStoryDimensionData> playersData = new HashMap<>();

    public static boolean hasPlayer(Player player)
    {
        if(playersData == null)
            return false;

        String uuid = player.getStringUUID();
        return playersData.containsKey(uuid);
    }

    public static void addPlayer(Player player, PlayerStoryDimensionData data)
    {
        if(playersData == null)
            playersData = new HashMap<>();

        String uuid = player.getStringUUID();
        playersData.put(uuid, data);
    }

    public static void setPlayerData(Player player, PlayerStoryDimensionData data)
    {
        if(!hasPlayer(player))
            addPlayer(player, data);
        else
            playersData.replace(player.getStringUUID(), data);
    }

    public static PlayerStoryDimensionData getPlayerData(Player player)
    {
        if(!hasPlayer(player))
            return new PlayerStoryDimensionData(Vec3.ZERO, "overworld", Vec3.ZERO);

        String uuid = player.getStringUUID();
        return playersData.get(uuid);
    }

    public static void setLastSpot(Vec3 newLastSpot)
    {
        lastSpot = newLastSpot;
    }

    public static Vec3 getNextAvailableSpot()
    {
        if(lastSpot == null)
            return Vec3.ZERO;
        return lastSpot.add(Config.storyStructureSize, 0, 0);
    }

    public static Vec3 getNextAvailableSpawnpoint()
    {
        return getNextAvailableSpot().add(Config.storyStructureSpawnpoint);
    }

    public static class StoryDimensionSavedData extends SavedData
    {
        public Vec3 savedLastSpot;
        public HashMap<String, PlayerStoryDimensionData> savedPlayersData;

        private static StoryDimensionSavedData create() {
            return new StoryDimensionSavedData();
        }

        private static StoryDimensionSavedData load(CompoundTag tag) {
            StoryDimensionSavedData data = create();

            CompoundTag lastSpotTag = tag.getCompound("LastSpot");
            double lastSpotX = lastSpotTag.getDouble("x");
            double lastSpotY = lastSpotTag.getDouble("y");
            double lastSpotZ = lastSpotTag.getDouble("z");
            data.savedLastSpot = new Vec3(lastSpotX, lastSpotY, lastSpotZ);

            CompoundTag playersDataMapTag = tag.getCompound("PlayersData");
            Set<String> playerUuids =  playersDataMapTag.getAllKeys();

            data.savedPlayersData.clear();
            playerUuids.forEach((String uuid) ->
            {
                CompoundTag playerDataTag = tag.getCompound(uuid);
                PlayerStoryDimensionData playerData = PlayerStoryDimensionData.load(playerDataTag);
                data.savedPlayersData.put(uuid, playerData);
            });

            return data;
        }

        public CompoundTag save(CompoundTag tag) {
            CompoundTag lastSpotTag = new CompoundTag();
            lastSpotTag.putDouble("x", savedLastSpot.x);
            lastSpotTag.putDouble("y", savedLastSpot.y);
            lastSpotTag.putDouble("z", savedLastSpot.z);
            tag.put("LastSpot", lastSpotTag);

            CompoundTag playersDataMapTag = new CompoundTag();
            savedPlayersData.forEach((String playerUuid, PlayerStoryDimensionData playerData) ->
            {
                playersDataMapTag.put(playerUuid, playerData.save(new CompoundTag()));
            });
            tag.put("PlayersData", playersDataMapTag);
            return tag;
        }

        /*
         * #server-side ONLY
         * Gets current instance of the saved data
         * Return new instance when on client
         */
        public static StoryDimensionSavedData getInstance(LevelAccessor level) {
            MinecraftServer server = level.getServer();
            if (server == null) {
                return create();
            }
            return server.overworld().getDataStorage().computeIfAbsent(StoryDimensionSavedData::load, StoryDimensionSavedData::create, "story-dimension-data");
        }

        public void save() {
            this.setDirty();
        }

    }

    public static void save(LevelAccessor level) {
        if (!level.isClientSide()) {
            StoryDimensionSavedData data = StoryDimensionSavedData.getInstance(level);
            data.savedPlayersData = StoryDimensionData.playersData;
            data.savedLastSpot = StoryDimensionData.lastSpot;
            data.save();
        }
    }

    public static void read(LevelAccessor level) {
        if (!level.isClientSide()) {
            StoryDimensionSavedData data = StoryDimensionSavedData.getInstance(level);
            StoryDimensionData.playersData = data.savedPlayersData;
        }
    }
}
