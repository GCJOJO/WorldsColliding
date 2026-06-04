package fr.gcjojo.worldscolliding.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import fr.gcjojo.worldscolliding.entity.AwakenedScourgeEntity;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.events.ModEvents;
import fr.gcjojo.worldscolliding.ModSounds;
import io.github.gcjojo.blablalib.commands.DialogueCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import software.bernie.geckolib.animatable.GeoEntity;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = "worldscolliding", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModCommands {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState from, BlockState to, Property property) {
        return to.setValue(property, from.getValue(property));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Load chapters dynamically
        List<String> chapters = Arrays.asList("\"worldscolliding:dogcheck\"", "\"worldscolliding:credits\"", "\"worldscolliding:chapter_0_set\"", "\"worldscolliding:chapter_1_set\"", "\"worldscolliding:chapter_1_ask\"", "\"worldscolliding:chapter_2_set\"", "\"worldscolliding:chapter_2_ask\"", "\"worldscolliding:chapter_3_set\"", "\"worldscolliding:chapter_3_ask", "\"worldscolliding:chapter_4_set\"", "\"worldscolliding:chapter_5_6_set", "\"worldscolliding:chapter_5_past_set", "\"worldscolliding:chapter_5_set", "\"worldscolliding:chapter_5_ask", "\"worldscolliding:chapter_6_set\"", "\"worldscolliding:chapter_6_ask\"", "\"worldscolliding:chapter_7_prologue_set", "\"worldscolliding:chapter_7_light_set\"", "\"worldscolliding:chapter_7_dark_set\"");
        List<String> animations = Arrays.asList("sceal1", "sceal2", "sceal3", "sceal4", "sceal5", "sceal6", "sceal7", "sceal8", "ascend");
        List<String> cinematics = Arrays.asList("laugh", "tp_effect");
        List<String> sealTypes = Arrays.asList("remnant", "dark", "light", "first_remnant");

        DialogueCommand.register(event.getDispatcher(), chapters, (CommandContext<CommandSourceStack> context, String dialogue) -> {
            if(!dialogue.equalsIgnoreCase("chapter_5_6_set") ||
                    !context.getSource().isPlayer() ||
                    context.getSource().getPlayer() == null)
                return dialogue;

            ServerPlayer player = context.getSource().getPlayer();

            String chapterName = "";
            if(!player.getPersistentData().contains("Chapter5") || !player.getPersistentData().getBoolean("Chapter5"))
            {
                chapterName = "chapter_5_set";
                player.getPersistentData().putBoolean("Chapter5", true);
            }
            else
            {
                chapterName = "chapter_6_set";
                player.getPersistentData().putBoolean("Chapter5", false);
            }

            return chapterName;
        });

        /*event.getDispatcher().register(Commands.literal("dialogue").requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("play")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            String currentChapter = player.getPersistentData().getString("CurrentChapter");
                            if (currentChapter.isEmpty()) {
                                currentChapter = "chapter_0_set";
                                player.getPersistentData().putString("CurrentChapter", currentChapter);
                            }
                            ModNetwork.sendToPlayer(new ModNetwork.OpenDialoguePacket(currentChapter), player);
                            return 1;
                        }))
                .then(Commands.literal("set")
                        .then(Commands.argument("chapterName", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(chapters, builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String chapterName = StringArgumentType.getString(context, "chapterName");

                                    if(chapterName.equalsIgnoreCase("chapter_5_6_set")){
                                        if(!player.getPersistentData().contains("Chapter5") || !player.getPersistentData().getBoolean("Chapter5"))
                                        {
                                            chapterName = "chapter_5_set";
                                            player.getPersistentData().putBoolean("Chapter5", true);
                                        }
                                        else
                                        {
                                            chapterName = "chapter_6_set";
                                            player.getPersistentData().putBoolean("Chapter5", false);
                                        }
                                    }

                                    player.getPersistentData().putString("CurrentChapter", chapterName);
                                    String finalChapterName = chapterName;
                                    context.getSource().sendSuccess(() -> Component.literal("Chapitre mis à jour avec succès : " + finalChapterName), true);
                                    return 1;
                                })))
        );*/

        event.getDispatcher().register(Commands.literal("scourge").requires(commandSourceStack -> commandSourceStack.hasPermission(2))
                .then(Commands.literal("cinematic")
                        .then(Commands.argument("action", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(cinematics, builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String action = StringArgumentType.getString(context, "action");
                                    ServerLevel level = player.serverLevel();

                                    switch (action) {
                                        case "laugh":
                                            level.playSound(null, player.blockPosition(), ModSounds.LAUGH.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
                                            break;
                                        case "tp_effect":
                                            level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1, player.getZ(), 100, 1.0, 2.0, 1.0, 0.05);
                                            level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
                                            break;
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("animate")
                        .then(Commands.argument("animName", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(animations, builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String animName = StringArgumentType.getString(context, "animName");

                                    AABB searchBox = player.getBoundingBox().inflate(50.0);
                                    List<LivingEntity> scourges = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e instanceof ScourgeEntity || e instanceof AwakenedScourgeEntity);

                                    if (!scourges.isEmpty()) {
                                        LivingEntity boss = scourges.get(0);

                                        if (animName.equals("ascend") && boss instanceof ScourgeEntity) {
                                            boss.getEntityData().set(ScourgeEntity.ASCENDING, true);
                                            context.getSource().sendSuccess(() -> Component.literal("Animation d'ascension lancée."), true);
                                        } else {
                                            if (animName.startsWith("sceal")) {
                                                Vec3 camPos = new Vec3(boss.getX() + 13.0, boss.getY() + 3.0, boss.getZ());
                                                double dx = boss.getX() - camPos.x;
                                                double dy = (boss.getY() + boss.getEyeHeight()) - camPos.y;
                                                double dz = boss.getZ() - camPos.z;
                                                float yaw = (float)(Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                                                float pitch = (float)(-(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180D / Math.PI)));

                                                player.sendSystemMessage(Component.literal("[DEBUG] Téléportation de la caméra en : X=" + camPos.x + " Y=" + camPos.y + " Z=" + camPos.z));

                                                ModEvents.freezePlayer(player.getUUID(), camPos, yaw, pitch, 400, player.gameMode.getGameModeForPlayer(), "");
                                                player.setGameMode(GameType.SPECTATOR);
                                            }

                                            if (boss instanceof GeoEntity geoBoss) {
                                                geoBoss.triggerAnim("seal_controller", animName);
                                                context.getSource().sendSuccess(() -> Component.literal("Animation jouée : " + animName), true);
                                            }
                                        }
                                    } else {
                                        context.getSource().sendFailure(Component.literal("Aucun Scourge trouvé à proximité."));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("sealeffect")
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(sealTypes, builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String itemType = StringArgumentType.getString(context, "itemType");

                                    AABB searchBox = player.getBoundingBox().inflate(50.0);
                                    List<LivingEntity> scourges = player.level().getEntitiesOfClass(LivingEntity.class, searchBox, e -> e instanceof ScourgeEntity || e instanceof AwakenedScourgeEntity);

                                    if (!scourges.isEmpty()) {
                                        LivingEntity scourge = scourges.get(0);
                                        Level level = scourge.level();
                                        BlockPos center = scourge.blockPosition();

                                        if (!itemType.equals("first_remnant")) {
                                            String[] prefixes = {"dark", "mana", "elf", "sunny", "blaze", "e", "lavender"};
                                            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
                                                BlockState state = level.getBlockState(pos);
                                                ResourceLocation loc = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                                                if (loc != null && loc.getNamespace().equals("botania") && loc.getPath().contains("_quartz")) {
                                                    String path = loc.getPath();
                                                    for (int i = 0; i < prefixes.length - 1; i++) {
                                                        if (path.contains(prefixes[i] + "_quartz")) {
                                                            String newPath = path.replace(prefixes[i] + "_quartz", prefixes[i+1] + "_quartz");
                                                            Block newBlock = ForgeRegistries.BLOCKS.getValue(ResourceLocation.fromNamespaceAndPath("botania", newPath));

                                                            if (newBlock != null && newBlock != Blocks.AIR) {
                                                                BlockState newState = newBlock.defaultBlockState();
                                                                for (Property<?> prop : state.getProperties()) {
                                                                    if (newState.hasProperty(prop)) {
                                                                        newState = copyProperty(state, newState, prop);
                                                                    }
                                                                }
                                                                level.setBlockAndUpdate(pos, newState);
                                                            }
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        ResourceLocation itemLoc;
                                        Item fallbackItem;
                                        if (itemType.equals("dark")) {
                                            itemLoc = ResourceLocation.fromNamespaceAndPath("kubejs", "dark_essence");
                                            fallbackItem = Items.BLACK_DYE;
                                        } else if (itemType.equals("light")) {
                                            itemLoc = ResourceLocation.fromNamespaceAndPath("kubejs", "light_essence");
                                            fallbackItem = Items.WHITE_DYE;
                                        } else {
                                            itemLoc = ResourceLocation.fromNamespaceAndPath("kubejs", "seal_remnant");
                                            fallbackItem = Items.BLUE_DYE;
                                        }

                                        Item dropItem = ForgeRegistries.ITEMS.getValue(itemLoc);
                                        if (dropItem == null || dropItem == Items.AIR) dropItem = fallbackItem;

                                        ItemEntity itemEntity = new ItemEntity(level, scourge.getX(), scourge.getY() + 2.5, scourge.getZ(), new ItemStack(dropItem));
                                        itemEntity.setNoGravity(true);
                                        itemEntity.setGlowingTag(true);
                                        itemEntity.setUnlimitedLifetime();
                                        itemEntity.getPersistentData().putInt("SealAge", 0);

                                        level.addFreshEntity(itemEntity);
                                        ModEvents.addSealItem(itemEntity);

                                    } else {
                                        context.getSource().sendFailure(Component.literal("Aucun Scourge trouvé à proximité pour jouer l'effet."));
                                    }
                                    return 1;
                                })))
        );
    }
}