package fr.gcjojo.worldscolliding.network;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.entity.ModEntities;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.events.ModEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1.1.6";
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
            //ctx.get().enqueueWork(() -> DialogueScreen.openForSet(msg.setName));
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

                    CompoundTag spawnpointTag = player.getPersistentData().getCompound("StoryDimension").getCompound("Spawnpoint");

                    player.teleportTo(spawnpointTag.getInt("x"), spawnpointTag.getInt("y"), spawnpointTag.getInt("z"));

                    if (msg.action != null && msg.action.startsWith("seal_")) {
                        String animNum = msg.action.replace("seal_", "");
                        String animName = "sceal" + animNum;

                        ServerLevel level = (ServerLevel) player.level();
                        AABB searchBox = player.getBoundingBox().inflate(50.0);
                        List<LivingEntity> scourges = level.getEntitiesOfClass(LivingEntity.class, searchBox, e -> e instanceof ScourgeEntity || e instanceof AwakenedScourgeEntity);

                        if (!scourges.isEmpty()) {
                            LivingEntity boss = scourges.get(0);
                            Vec3 camPos = new Vec3(boss.getX() + 13.0, boss.getY() + 2.0, boss.getZ());
                            double dx = boss.getX() - camPos.x;
                            double dy = (boss.getY() + boss.getEyeHeight()) - camPos.y;
                            double dz = boss.getZ() - camPos.z;
                            float yaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                            float pitch = (float)(-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180D / Math.PI)));

                            ModEvents.freezePlayer(player.getUUID(), camPos, yaw, pitch, 400, player.gameMode.getGameModeForPlayer(), msg.nextSet);
                            player.setGameMode(GameType.SPECTATOR);

                            if (boss instanceof software.bernie.geckolib.animatable.GeoEntity geoBoss) {
                                geoBoss.triggerAnim("seal_controller", animName);
                            }
                            return;
                        } else {
                            player.sendSystemMessage(Component.literal("Scourge introuvable pour l'animation de scellement."));
                        }
                    }

                    if (msg.nextSet != null && !msg.nextSet.isEmpty()) {
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

            });
            ctx.get().setPacketHandled(true);
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