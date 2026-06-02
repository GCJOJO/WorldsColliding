package fr.gcjojo.worldscolliding;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;


public class StoryDimensionData {

    private static Vec3 lastSpot = Vec3.ZERO;

    public static void setLastSpot(Vec3 newLastSpot)
    {
        lastSpot = newLastSpot;
    }

    public static Vec3 getNextAvailableSpot()
    {
        if(lastSpot == null)
            return Vec3.ZERO.add(0, 1, 0);
        return lastSpot.add(Config.storyStructureSize, 0, 0);
    }

    public static Vec3 getNextAvailableSpawnpoint()
    {
        return getNextAvailableSpot().add(Config.storyStructureSpawnpoint);
    }

    public static class StoryDimensionSavedData extends SavedData
    {
        public Vec3 savedLastSpot = new Vec3(0, 100, 0);

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

            return data;
        }

        public @NotNull CompoundTag save(CompoundTag tag) {
            CompoundTag lastSpotTag = new CompoundTag();
            lastSpotTag.putDouble("x", savedLastSpot.x);
            lastSpotTag.putDouble("y", savedLastSpot.y);
            lastSpotTag.putDouble("z", savedLastSpot.z);
            tag.put("LastSpot", lastSpotTag);

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
            data.savedLastSpot = StoryDimensionData.lastSpot;
            data.save();
        }
    }

    public static void read(LevelAccessor level) {
        if (!level.isClientSide()) {
            StoryDimensionSavedData data = StoryDimensionSavedData.getInstance(level);
            StoryDimensionData.lastSpot = data.savedLastSpot;
        }
    }
}
