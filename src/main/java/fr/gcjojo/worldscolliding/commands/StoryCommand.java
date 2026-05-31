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

        CompoundTag playerPersistentData = player.getPersistentData();
        if(player.level().dimension() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            return 0;
            /*if(playerPersistentData.contains("StoryDimension"))
            {
                PlayerStoryDimensionData playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));
                ServerLevel toLevel;

                switch(playerData.playerDimension)
                {
                    case "the_end":
                        toLevel = player.getServer().getLevel(Level.END);
                        break;
                    case "nether":
                        toLevel = player.getServer().getLevel(Level.NETHER);
                        break;
                    case "overworld":
                    default:
                        toLevel = player.getServer().getLevel(Level.OVERWORLD);
                        break;
                }

                player.teleportTo(toLevel, playerData.playerPos.x, playerData.playerPos.y, playerData.playerPos.z, Set.of(), 0.0f, 0.0f);
                return 1;
            }
            player.teleportTo(player.getServer().overworld(), player.getX(), player.getY(), player.getZ(), Set.of(), 0.0f, 0.0f);
            return 1;*/
        }

        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        if(playerPersistentData.contains("StoryDimension"))
        {
            var playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));
            playerData.playerPos = playerPos;
            playerPersistentData.put("StoryDimension", playerData.save());
        }
        else
        {
            String dimension = player.level().dimension().location().getPath();
            var playerData = new PlayerStoryDimensionData(Vec3.ZERO, dimension, playerPos);
            playerPersistentData.put("StoryDimension", playerData.save());
        }
        StoryDimensionData.save(player.getServer().overworld());
        ServerLevel storyLevel = player.getServer().getLevel(ModDimensions.STORY_DIM_LEVEL_KEY);
        player.teleportTo(storyLevel, playerPos.x, playerPos.y, playerPos.z, Set.of(), Config.storyStructurePlayerRotation, 0.0f);

        return 1;
    }
}
