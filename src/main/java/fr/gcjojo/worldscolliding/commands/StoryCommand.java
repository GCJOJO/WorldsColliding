package fr.gcjojo.worldscolliding.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.gcjojo.worldscolliding.Config;
import fr.gcjojo.worldscolliding.PlayerStoryDimensionData;
import fr.gcjojo.worldscolliding.StoryDimensionData;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.checkerframework.checker.nullness.qual.RequiresNonNull;

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
            if(StoryDimensionData.hasPlayer(player))
            {
                PlayerStoryDimensionData playerData = StoryDimensionData.getPlayerData(player);
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
            return 1;
        }

        Vec3 playerPos = new Vec3(player.getX(), player.getY(), player.getZ());
        if(StoryDimensionData.hasPlayer(player))
        {
            var playerData = StoryDimensionData.getPlayerData(player);
            playerData.playerPos = playerPos;
            StoryDimensionData.setPlayerData(player, playerData);
        }
        else
        {
            String dimension = player.level().dimension().location().getPath();
            var playerData = new PlayerStoryDimensionData(Vec3.ZERO, dimension, playerPos);
            StoryDimensionData.setPlayerData(player, playerData);
        }
        StoryDimensionData.save(player.getServer().overworld());
        ServerLevel storyLevel = player.getServer().getLevel(ModDimensions.STORY_DIM_LEVEL_KEY);
        player.teleportTo(storyLevel, playerPos.x, playerPos.y, playerPos.z, Set.of(), Config.storyStructurePlayerRotation, 0.0f);

        return 1;
    }
}
