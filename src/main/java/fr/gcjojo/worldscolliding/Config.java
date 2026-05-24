package fr.gcjojo.worldscolliding;

import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ModEntry.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.ConfigValue<String> STORY_STRUCTURE = BUILDER
            .define("story_structure", "worldscolliding:scourge_den");

    private static final ForgeConfigSpec.IntValue STORY_STRUCTURE_SIZE = BUILDER
            .comment("What the size of the structure in the story dimension is. This value is used to layout the grid of structures for each player.")
            .defineInRange("story_structure_size", 61, 0, Integer.MAX_VALUE);

    private static final ForgeConfigSpec.DoubleValue STORY_STRUCTURE_PLAYER_SPAWNPOINT_X = BUILDER
            .comment("The x position of the spawnpoint of players in the structure relatives to the structure's origin")
            .defineInRange("story_structure_player_spawnpoint_x", 55.5d, -128.0d, 128.0);

    private static final ForgeConfigSpec.DoubleValue STORY_STRUCTURE_PLAYER_SPAWNPOINT_Y = BUILDER
            .comment("The y position of the spawnpoint of players in the structure relatives to the structure's origin")
            .defineInRange("story_structure_player_spawnpoint_y", 6.0d, -128.0d, 128.0d);

    private static final ForgeConfigSpec.DoubleValue STORY_STRUCTURE_PLAYER_SPAWNPOINT_Z = BUILDER
            .comment("The z position of the spawnpoint of players in the structure relatives to the structure's origin")
            .defineInRange("story_structure_player_spawnpoint_z", 30.5d, -128.0d, 128.0d);

    private static final ForgeConfigSpec.DoubleValue STORY_STRUCTURE_PLAYER_ROTATION = BUILDER
            .comment("The angle of rotation of the player when teleporting to Story Dimension")
            .defineInRange("story_structure_player_rotation", 90.0d, 0.0d, 360.0d);

    // a list of strings that are treated as resource locations for items
    /*private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);*/

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static String storyStructure;
    public static int storyStructureSize;
    public static Vec3 storyStructureSpawnpoint;
    public static float storyStructurePlayerRotation;

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        storyStructure = STORY_STRUCTURE.get();
        storyStructureSize = STORY_STRUCTURE_SIZE.get();

        double spawnpointX = STORY_STRUCTURE_PLAYER_SPAWNPOINT_X.get();
        double spawnpointY = STORY_STRUCTURE_PLAYER_SPAWNPOINT_Y.get();
        double spawnpointZ = STORY_STRUCTURE_PLAYER_SPAWNPOINT_Z.get();
        storyStructureSpawnpoint = new Vec3(spawnpointX, spawnpointY, spawnpointZ);

        storyStructurePlayerRotation = (float)(double)STORY_STRUCTURE_PLAYER_ROTATION.get();
        // convert the list of strings into a set of items
        /*items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .collect(Collectors.toSet());*/
    }
}
