package fr.gcjojo.worldscolliding;

import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public class PlayerStoryDimensionData
{
    //String uuid;
    public boolean scourgeDenPlaced = false;
    public Vec3 storyDimensionSpawnpoint;
    public String playerDimension;
    public Vec3 playerPos;

    private PlayerStoryDimensionData()
    {

    }

    public PlayerStoryDimensionData(/*String uuid,*/ Vec3 storySpawnpoint, String dimension, Vec3 pos)
    {
        //this.uuid = uuid;
        this.storyDimensionSpawnpoint = storySpawnpoint;
        this.playerDimension = dimension;
        this.playerPos = pos;
    }

    public static PlayerStoryDimensionData load(CompoundTag nbt)
    {
        PlayerStoryDimensionData data = new PlayerStoryDimensionData();
        data.scourgeDenPlaced = nbt.getBoolean("ScourgeDenPlaced");
        //data.uuid = nbt.getString("uuid");
        CompoundTag spawnpointTag = nbt.getCompound("Spawnpoint");
        double spawnpointX = spawnpointTag.getDouble("x");
        double spawnpointY = spawnpointTag.getDouble("y");
        double spawnpointZ = spawnpointTag.getDouble("z");
        data.storyDimensionSpawnpoint = new Vec3(spawnpointX, spawnpointY, spawnpointZ);

        data.playerDimension = nbt.getString("Dimension");

        CompoundTag posTag = nbt.getCompound("Position");
        double posX = posTag.getDouble("x");
        double posY = posTag.getDouble("y");
        double posZ = posTag.getDouble("z");
        data.playerPos = new Vec3(posX, posY, posZ);

        return data;
    }

    public CompoundTag save(CompoundTag nbt)
    {
        //nbt.putString("uuid", this.uuid);
        nbt.putBoolean("ScourgeDenPlaced", this.scourgeDenPlaced);

        CompoundTag spawnpointTag = new CompoundTag();
        spawnpointTag.putDouble("x", this.storyDimensionSpawnpoint.x);
        spawnpointTag.putDouble("y", this.storyDimensionSpawnpoint.y);
        spawnpointTag.putDouble("z", this.storyDimensionSpawnpoint.z);
        nbt.put("Spawnpoint", spawnpointTag);

        nbt.putString("Dimension", this.playerDimension);

        CompoundTag posTag = new CompoundTag();
        posTag.putDouble("x", this.playerPos.x);
        posTag.putDouble("y", this.playerPos.y);
        posTag.putDouble("z", this.playerPos.z);
        nbt.put("Position", posTag);

        return nbt;
    }
}
