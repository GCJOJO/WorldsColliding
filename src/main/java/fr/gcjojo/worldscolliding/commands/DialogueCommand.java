package fr.gcjojo.worldscolliding.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import fr.gcjojo.worldscolliding.entity.ScourgeEntity;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;

@Mod.EventBusSubscriber(modid = "worldscolliding", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DialogueCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        List<String> chapters = Arrays.asList("chapter_0_set", "chapter_1_set", "chapter_1_ask", "chapter_2_set", "chapter_2_ask", "chapter_3_set", "chapter_3_ask", "chapter_4_set", "chapter_5_6_past_set", "chapter_7_prologue_set");

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
        );
    }
}