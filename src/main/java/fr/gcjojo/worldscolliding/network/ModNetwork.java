package fr.gcjojo.worldscolliding.network;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.PlayerStoryDimensionData;
import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.events.ModEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.function.Supplier;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(ResourceLocation.fromNamespaceAndPath(ModEntry.MODID, "main"))
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static int packetId = 0;

    public static void register() {
        CHANNEL.messageBuilder(OpenDialoguePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenDialoguePacket::encode)
                .decoder(OpenDialoguePacket::new)
                .consumerMainThread(OpenDialoguePacket::handle)
                .add();

        CHANNEL.messageBuilder(ChoiceSelectedPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ChoiceSelectedPacket::encode)
                .decoder(ChoiceSelectedPacket::new)
                .consumerMainThread(ChoiceSelectedPacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueCompletedPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueCompletedPacket::encode)
                .decoder(DialogueCompletedPacket::new)
                .consumerMainThread(DialogueCompletedPacket::handle)
                .add();

        CHANNEL.messageBuilder(DialogueCommandPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DialogueCommandPacket::encode)
                .decoder(DialogueCommandPacket::new)
                .consumerMainThread(DialogueCommandPacket::handle)
                .add();
    }

    public static void sendToPlayer(Object packet, ServerPlayer player) {
        CHANNEL.sendTo(packet, player.connection.connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }

    public static class OpenDialoguePacket {
        private final String setName;

        public OpenDialoguePacket(String setName) {
            this.setName = setName;
        }

        public OpenDialoguePacket(FriendlyByteBuf buf) {
            this.setName = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(this.setName);
        }

        public static void handle(OpenDialoguePacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> DialogueScreen.openForSet(msg.setName));
            ctx.get().setPacketHandled(true);
        }
    }

    public static class ChoiceSelectedPacket {
        private final String nextSet;
        private final String saveSet;
        private final String action;

        public ChoiceSelectedPacket(String nextSet, String saveSet, String action) {
            this.nextSet = nextSet;
            this.saveSet = saveSet;
            this.action = action;
        }

        public ChoiceSelectedPacket(FriendlyByteBuf buf) {
            this.nextSet = buf.readUtf();
            this.saveSet = buf.readUtf();
            this.action = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(this.nextSet);
            buf.writeUtf(this.saveSet);
            buf.writeUtf(this.action);
        }

        public static void handle(ChoiceSelectedPacket msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if (player != null) {
                    player.getPersistentData().putString("CurrentChapter", msg.saveSet);
                    PlayerStoryDimensionData data = PlayerStoryDimensionData.load(player.getPersistentData().getCompound("StoryDimension"));
                    player.teleportTo(data.storyDimensionSpawnpoint.x, data.storyDimensionSpawnpoint.y, data.storyDimensionSpawnpoint.z);

                    if (msg.action != null && msg.action.startsWith("seal_")) {
                        String animName = "sceal" + msg.action.split("_")[1];

                        AABB searchBox = player.getBoundingBox().inflate(50.0);
                        List<ScourgeEntity> scourges = player.level().getEntitiesOfClass(ScourgeEntity.class, searchBox);

                        if (!scourges.isEmpty()) {
                            ScourgeEntity nearestScourge = scourges.get(0);
                            nearestScourge.triggerAnim("seal_controller", animName);
                        }

                        GameType previousGameMode = player.gameMode.getGameModeForPlayer();
                        player.setGameMode(GameType.SPECTATOR);

                        ModEvents.freezePlayer(player.getUUID(), player.position(), 400, previousGameMode, msg.nextSet);
                    }
                    else if (msg.nextSet != null && !msg.nextSet.isEmpty()) {
                        ModNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                                new OpenDialoguePacket(msg.nextSet));
                    }
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }

    public static class DialogueCompletedPacket {
        private final String setName;

        public DialogueCompletedPacket(String setName) {
            this.setName = setName;
        }

        public DialogueCompletedPacket(FriendlyByteBuf buf) {
            this.setName = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(this.setName);
        }

        public static void handle(DialogueCompletedPacket msg, Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if(player != null) {
                    player.getPersistentData().putBoolean("IsInDialogue", false);
                    player.getPersistentData().putString("LastReadChapter", msg.setName);

                    if(msg.setName.equals("chapter_7_light_set") || msg.setName.equals("chapter_7_dark_set"))
                        spawnBoss(player, msg.setName.contains("light"));
                }
            });
            ctx.get().setPacketHandled(true);
        }

        public static void spawnBoss(ServerPlayer player, boolean isLight){
            Level level = player.level();
            level.getEntities(player, player.getBoundingBox().inflate(15.0f), entity -> entity instanceof ScourgeEntity).forEach(scourge -> {
                Vec3 bossSpawnPos = scourge.getPosition(1.0f).add(0d, 2.0d, 0d);
                if(isLight){
                    scourge.discard();

                    CompoundTag scourgeRespawnPosTag = new CompoundTag();
                    scourgeRespawnPosTag.putDouble("x", bossSpawnPos.x);
                    scourgeRespawnPosTag.putDouble("y", bossSpawnPos.y);
                    scourgeRespawnPosTag.putDouble("z", bossSpawnPos.z);

                    player.getPersistentData().put("ScourgeRespawnPosition", scourgeRespawnPosTag);
                }

                level.explode(scourge, bossSpawnPos.x, bossSpawnPos.y, bossSpawnPos.z, 10.0f, Level.ExplosionInteraction.NONE);
                Entity boss = new AwakenedScourgeEntity(ModEntities.AWAKENED_SCOURGE.get(), level);
                level.addFreshEntity(boss);
                boss.setPos(bossSpawnPos);
                scourge.getPersistentData().putBoolean("BossBattle", isLight);
                boss.getPersistentData().putUUID("Player", player.getUUID());
            });
        }
    }

    public static class DialogueCommandPacket {
        private String command;

        public DialogueCommandPacket(String command) {
            this.command = command;
        }

        public DialogueCommandPacket(FriendlyByteBuf buf) {
            this.command = buf.readUtf();
        }

        public void encode(FriendlyByteBuf buf) {
            buf.writeUtf(this.command);
        }

        public static void handle(DialogueCommandPacket msg, Supplier<NetworkEvent.Context> ctx){
            ctx.get().enqueueWork(() -> {
                ServerPlayer player = ctx.get().getSender();
                if(player == null) return;
                MinecraftServer server = player.getServer();
                if(server == null) return;

                String formattedCommand = "execute positioned as %s run %s".formatted(player.getName().getString(), msg.command);
                try {
                    int success = server.getCommands().getDispatcher().execute(formattedCommand, server.createCommandSourceStack().withEntity(player).withLevel((ServerLevel) player.level()));
                    ModEntry.getLogger().warn(String.valueOf(success));
                } catch (CommandSyntaxException e) {
                    throw new RuntimeException(e);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}