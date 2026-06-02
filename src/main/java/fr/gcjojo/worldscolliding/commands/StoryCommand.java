package fr.gcjojo.worldscolliding.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import fr.gcjojo.worldscolliding.Config;
import fr.gcjojo.worldscolliding.PlayerStoryDimensionData;
import fr.gcjojo.worldscolliding.StoryDimensionData;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.Set;

public class StoryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(Commands.literal("story").executes(StoryCommand::teleportPlayer));
    }

    private static int teleportPlayer(CommandContext<CommandSourceStack> context)
    {
        Player player = Objects.requireNonNull(context.getSource().getPlayer(), "Command must be executed via player");

        if(player.level().dimension() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            return 0;
        }

        CompoundTag playerPersistentData = player.getPersistentData();
        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        ServerLevel storyLevel = player.getServer().getLevel(ModDimensions.STORY_DIM_LEVEL_KEY);

        if(playerPersistentData.contains("StoryDimension"))
        {
            var playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));
            playerData.playerPos = playerPos;
            playerData.playerDimension = player.level().dimension().location().getPath();
            playerPersistentData.put("StoryDimension", playerData.save());

            if (playerData.scourgeDenPlaced && playerData.storyDimensionSpawnpoint != null) {
                player.teleportTo(storyLevel, playerData.storyDimensionSpawnpoint.x, playerData.storyDimensionSpawnpoint.y, playerData.storyDimensionSpawnpoint.z, Set.of(), Config.storyStructurePlayerRotation, 0.0f);
                return 1;
            }
        }
        else
        {
            String dimension = player.level().dimension().location().getPath();
            var playerData = new PlayerStoryDimensionData(Vec3.ZERO, dimension, playerPos);
            playerPersistentData.put("StoryDimension", playerData.save());
        }

        StoryDimensionData.save(player.getServer().overworld());
        player.teleportTo(storyLevel, playerPos.x, playerPos.y, playerPos.z, Set.of(), Config.storyStructurePlayerRotation, 0.0f);

        return 1;
    }
}