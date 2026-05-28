package fr.gcjojo.worldscolliding;

import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.client.ScourgeRenderer;
import fr.gcjojo.worldscolliding.client.AwakenedScourgeRenderer;
import fr.gcjojo.worldscolliding.client.AwakenedScourgeModel;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

@Mod(ModEntry.MODID)
public class ModEntry
{
    public static final String MODID = "worldscolliding";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ModEntry(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

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
        if(structureID == null) {
            LOGGER.error("Structure ID {} is not a valid structure !", Config.storyStructure);
            return;
        }

        Optional<StructureTemplate> templateOpt = manager.get(structureID);

        if (templateOpt.isEmpty()) {
            LOGGER.error("Unable to find structure : {}", structureID);
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
        event.put(ModEntities.AWAKENED_SCOURGE.get(), AwakenedScourgeEntity.createAttributes().build());
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
        //LOGGER.info("Player {} changed dimension {}", player.getName().getString(), event.getTo().toString());

        if(event.getTo() == ModDimensions.STORY_DIM_LEVEL_KEY)
        {
            //LOGGER.info("Player {} joined STORY Dimension", player.getName().getString());
            String dimension = event.getFrom().location().getPath();
            Vec3 playerPosition = player.getPosition(1.0f);
            CompoundTag playerPersistentData = player.getPersistentData();

            if(playerPersistentData.contains("StoryDimension")){
                PlayerStoryDimensionData playerData = PlayerStoryDimensionData.load(playerPersistentData.getCompound("StoryDimension"));
                if(playerData.scourgeDenPlaced) {
                    playerData.playerDimension = dimension;
                    playerData.playerPos = playerPosition;
                    playerPersistentData.put("StoryDimension", playerData.save());
                    player.teleportTo(playerData.storyDimensionSpawnpoint.x, playerData.storyDimensionSpawnpoint.y, playerData.storyDimensionSpawnpoint.z);
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
            }
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(fr.gcjojo.worldscolliding.network.ModNetwork::register);
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
        public static void onClientSetup(FMLClientSetupEvent event) { }

        @SubscribeEvent
        public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
            event.registerEntityRenderer(ModEntities.SCOURGE.get(), ScourgeRenderer::new);
            event.registerEntityRenderer(ModEntities.AWAKENED_SCOURGE.get(), AwakenedScourgeRenderer::new);
        }
    }
}