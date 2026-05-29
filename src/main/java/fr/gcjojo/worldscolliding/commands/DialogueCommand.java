package fr.gcjojo.worldscolliding.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import fr.gcjojo.worldscolliding.events.ModEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.core.BlockPos;
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

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = "worldscolliding", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DialogueCommand {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState copyProperty(BlockState from, BlockState to, Property property) {
        return to.setValue(property, from.getValue(property));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        // Load chapters dynamically
        List<String> chapters = Arrays.asList("dogcheck", "credits", "chapter_0_set", "chapter_1_set", "chapter_1_ask", "chapter_2_set", "chapter_2_ask", "chapter_3_set", "chapter_3_ask", "chapter_4_set", "chapter_5_6_past_set", "chapter_5_6_seal_set", "chapter_5_6_seal_ask", "chapter_7_prologue_set", "chapter_7_light_set", "chapter_7_dark_set");

        event.getDispatcher().register(Commands.literal("dialogue")
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
                                    player.getPersistentData().putString("CurrentChapter", chapterName);
                                    context.getSource().sendSuccess(() -> Component.literal("Chapitre mis à jour avec succès : " + chapterName), true);
                                    return 1;
                                })))
        );

        event.getDispatcher().register(Commands.literal("scourge")
                .then(Commands.literal("animate")
                        .then(Commands.argument("animName", StringArgumentType.string())
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String animName = StringArgumentType.getString(context, "animName");

                                    AABB searchBox = player.getBoundingBox().inflate(50.0);
                                    List<ScourgeEntity> scourges = player.level().getEntitiesOfClass(ScourgeEntity.class, searchBox);

                                    if (!scourges.isEmpty()) {
                                        scourges.get(0).triggerAnim("seal_controller", animName);
                                        context.getSource().sendSuccess(() -> Component.literal("Animation jouée : " + animName), true);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("Aucun Scourge trouvé à proximité."));
                                    }
                                    return 1;
                                })))
                .then(Commands.literal("sealeffect")
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(Arrays.asList("remnant", "dark", "light"), builder))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    String itemType = StringArgumentType.getString(context, "itemType");

                                    AABB searchBox = player.getBoundingBox().inflate(50.0);
                                    List<ScourgeEntity> scourges = player.level().getEntitiesOfClass(ScourgeEntity.class, searchBox);

                                    if (!scourges.isEmpty()) {
                                        ScourgeEntity scourge = scourges.get(0);
                                        Level level = scourge.level();
                                        BlockPos center = scourge.blockPosition();

                                        String[] prefixes = {"dark", "mana", "elf", "sunny", "blaze", "e", "lavender"};
                                        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, -3, -3), center.offset(3, 3, 3))) {
                                            BlockState state = level.getBlockState(pos);
                                            ResourceLocation loc = ForgeRegistries.BLOCKS.getKey(state.getBlock());

                                            if (loc != null && loc.getNamespace().equals("botania") && loc.getPath().contains("_quartz")) {
                                                String path = loc.getPath();
                                                for (int i = 0; i < prefixes.length - 1; i++) {
                                                    if (path.startsWith(prefixes[i] + "_quartz")) {
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

                                        context.getSource().sendSuccess(() -> Component.literal("Effet de sceau joué : Blocs modifiés et item apparu !"), true);
                                    } else {
                                        context.getSource().sendFailure(Component.literal("Aucun Scourge trouvé à proximité pour jouer l'effet."));
                                    }
                                    return 1;
                                })))
        );
    }
}