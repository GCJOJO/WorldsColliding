package fr.gcjojo.worldscolliding;

import com.mojang.logging.LogUtils;
import fr.gcjojo.worldscolliding.blocks.ModBlocks;
import fr.gcjojo.worldscolliding.client.AwakenedScourgeRenderer;
import fr.gcjojo.worldscolliding.client.ScourgeRenderer;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.items.ModItems;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.worldgen.dimension.ModDimensions;
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
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
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

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModEntities.register(modEventBus);
        ModSounds.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerAttributes);

        MinecraftForge.EVENT_BUS.register(this);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    public static Logger getLogger() { return LOGGER; }

    private void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SCOURGE.get(), ScourgeEntity.createAttributes().build());
        event.put(ModEntities.AWAKENED_SCOURGE.get(), AwakenedScourgeEntity.createAttributes().build());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event)
    {
        StoryDimensionData.read(event.getServer().overworld());
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