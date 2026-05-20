package fr.gcjojo.worldscolliding;

import com.mojang.logging.LogUtils;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.telemetry.events.WorldLoadEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.profiling.jfr.event.WorldLoadFinishedEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(ModEntry.MODID)
public class ModEntry
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "worldscolliding";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModEntry(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);

        // Register our mod's ForgeConfigSpec so that Forge can create and load the config file for us
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Logger getLogger() { return LOGGER; }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        StoryDimensionData.read(event.getServer().overworld());
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event)
    {
        Player player = event.getEntity();
        LOGGER.info("Player {} changed dimension {}", player.getName().getString(), event.getTo().toString());
        //event.getEntity().changeDimension()
        if(event.getTo() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            LOGGER.info("Player {} joined STORY Dimension", player.getName().getString());
            PlayerStoryDimensionData data = StoryDimensionData.getPlayerData(player);
            String dimension = event.getFrom().location().getPath();
            data.playerDimension = dimension;

            if(StoryDimensionData.hasPlayer(player))
            {
                PlayerStoryDimensionData playerData = StoryDimensionData.getPlayerData(player);
                player.teleportTo(playerData.storyDimensionSpawnpoint.x, playerData.storyDimensionSpawnpoint.y, playerData.storyDimensionSpawnpoint.z);
                StoryDimensionData.setPlayerData(player, data);
                StoryDimensionData.save(player.getServer().overworld());
                return;
            }

            Vec3 newPlayerSpot = StoryDimensionData.getNextAvailableSpot();
            Vec3 newPlayerSpawnpoint = StoryDimensionData.getNextAvailableSpawnpoint();

            data.storyDimensionSpawnpoint = newPlayerSpawnpoint;

            //Spawn structure
            player.teleportTo(newPlayerSpawnpoint.x, newPlayerSpawnpoint.y, newPlayerSpawnpoint.z);
            StoryDimensionData.setLastSpot(newPlayerSpot);
            //player.level().setBlock(player.getOnPos(), Blocks.STONE.defaultBlockState(), 0);

            StoryDimensionData.setPlayerData(player, data);
            StoryDimensionData.save(player.getServer().overworld());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        // Some common setup code
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        // Do something when the server starts
        LOGGER.info("HELLO from server starting");
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Some client setup code
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }
}
