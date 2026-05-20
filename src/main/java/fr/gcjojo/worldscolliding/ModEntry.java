package fr.gcjojo.worldscolliding;

import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.client.ScourgeRenderer;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.Optional;

@Mod(ModEntry.MODID)
public class ModEntry
{
    public static final String MODID = "worldscolliding";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModEntry(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        // Enregistrement des composants
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus); // AJOUTÉ : Enregistrement des sons

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Logger getLogger() { return LOGGER; }

    private static void placeScourgeDenStructure(ServerLevel level, BlockPos pos)
    {
        ResourceLocation structureID = ResourceLocation.tryParse(Config.storyStructure);
        StructureTemplateManager manager = level.getStructureManager();
        Optional<StructureTemplate> templateOpt = manager.get(structureID);

        if (templateOpt.isEmpty()) {
            System.out.println("Structure introuvable : " + structureID);
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

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SCOURGE.get(), ScourgeEntity.createAttributes().build());
    }

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

        if(event.getTo() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            LOGGER.info("Player {} joined STORY Dimension", player.getName().getString());
            PlayerStoryDimensionData data = StoryDimensionData.getPlayerData(player);
            String dimension = event.getFrom().location().getPath();
            data.playerDimension = dimension;

            if(StoryDimensionData.hasPlayer(player) && StoryDimensionData.getPlayerData(player).scourgeDenPlaced)
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
            data.scourgeDenPlaced = true;

            BlockPos blockPos = new BlockPos((int)newPlayerSpot.x, (int)newPlayerSpot.y, (int)newPlayerSpot.z);

            placeScourgeDenStructure(player.getServer().getLevel(ModDimensions.STORY_DIM_LEVEL_KEY), blockPos);

            player.teleportTo(newPlayerSpawnpoint.x, newPlayerSpawnpoint.y, newPlayerSpawnpoint.z);
            StoryDimensionData.setLastSpot(newPlayerSpot);

            StoryDimensionData.setPlayerData(player, data);
            StoryDimensionData.save(player.getServer().overworld());
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(fr.gcjojo.worldscolliding.network.ModNetwork::register);
        LOGGER.info("HELLO FROM COMMON SETUP");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SCOURGE.get(), ScourgeRenderer::new);
        }
    }
}