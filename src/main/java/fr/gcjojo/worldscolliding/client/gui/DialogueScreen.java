package fr.gcjojo.worldscolliding.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.dialogues.DialogueAction;
import fr.gcjojo.worldscolliding.dialogues.DialogueChoice;
import fr.gcjojo.worldscolliding.dialogues.DialogueFading;
import fr.gcjojo.worldscolliding.dialogues.DialogueMessage;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class DialogueScreen extends Screen {

    /*public record DialogueLine(String speaker, String text, String option1, String action1, String next1, String save1, String option2, String action2, String next2, String save2) {
        public DialogueLine(String speaker, String text) {
            this(speaker, text, null, null, null, null, null, null, null, null);
        }
    }*/

    /*private boolean hasOptions() {
        if (dialogues.isEmpty()) return false;
        DialogueLine lastLine = dialogues.get(dialogues.size() - 1);
        return lastLine.option1() != null && !lastLine.option1().isEmpty();
    }*/

    private List<DialogueAction> actions;
    private int actionIndex = -1;

    public DialogueScreen(List<DialogueAction> actions) {
        super(Component.literal("Dialogue"));
        this.actions = actions;
    }

    @Override
    protected void init() {
        super.init();
        advanceDialogue();
    }

    public void drawButton(Button button)
    {
        this.addRenderableWidget(button);
    }

    public void handleChoiceSelection(String nextSet, String saveSet, String action) {
        ModNetwork.sendToServer(new ModNetwork.ChoiceSelectedPacket(nextSet, saveSet, action));

        if (action != null && action.startsWith("seal_")) {
            this.onClose();
            return;
        }

        List<DialogueAction> newActions = loadSet(nextSet);
        if(newActions == null) {
            this.onClose();
            return;
        }

        actionIndex = -1;
        actions = newActions;
        advanceDialogue();
    }

    public static List<DialogueAction> loadSet(String setName) {
        try {
            ResourceLocation res = new ResourceLocation("worldscolliding", "dialogues.json");
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resourceOpt.isPresent()) {
                JsonObject root = new Gson().fromJson(new InputStreamReader(resourceOpt.get().open()), JsonObject.class);
                if (root.has(setName)) {
                    List<DialogueAction> actions = new ArrayList<>();
                    root.getAsJsonArray(setName).forEach(element -> {
                        JsonObject obj = element.getAsJsonObject();
                        String speaker = obj.get("speaker").getAsString();

                        switch(speaker)
                        {
                            case "choix" -> actions.add(new DialogueChoice(
                                obj.get("option1").getAsString(), obj.get("next1").getAsString(), obj.get("save1").getAsString(), obj.get("action1").getAsString(),
                                obj.get("option2").getAsString(), obj.get("next2").getAsString(), obj.get("save2").getAsString(), obj.get("action2").getAsString()
                            ));
                            case "fading" -> actions.add(new DialogueFading(obj.get("from").getAsString(), obj.get("to").getAsString(), obj.get("time").getAsFloat()));
                            default -> actions.add(new DialogueMessage(speaker, Component.translatable(obj.get("text").getAsString()).getString(), obj.has("background_color") ? obj.get("background_color").getAsString() : "00000000"));
                        }
                    });
                    return actions;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    public static void openForSet(String setName) {
        List<DialogueAction> actions = loadSet(setName);
        if(actions != null)
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new DialogueScreen(actions)));
    }

    @Override
    public void tick() {
        if(actionIndex >= actions.size())
            return;

        DialogueAction currentAction = actions.get(actionIndex);
        if(currentAction == null)
            return;

        currentAction.step();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        if(actionIndex >= actions.size())
            return;

        DialogueAction currentAction = actions.get(actionIndex);
        if(currentAction == null)
            return;

        currentAction.draw(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(actionIndex < actions.size()) {
            DialogueAction currentAction = actions.get(actionIndex);
            if(currentAction != null)
                currentAction.mouseClicked(mouseX, mouseY, button);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(actionIndex < actions.size()) {
            DialogueAction currentAction = actions.get(actionIndex);
            if(currentAction != null)
                currentAction.mouseClicked(keyCode, scanCode, modifiers);
        }

        if (keyCode == 257 || keyCode == 32) {
            advanceDialogue();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void advanceDialogue() {
        actionIndex++;
        if (actions.isEmpty() || actionIndex >= actions.size()) {
            this.onClose();
            return;
        }
        DialogueAction currentAction = actions.get(actionIndex);

        currentAction.setup(this);

        /*if (!hasOptions()) {
            String currentSet = Minecraft.getInstance().player.getPersistentData().getString("CurrentChapter");
            if (currentSet.endsWith("_set") && !currentSet.contains("_ask")) {
                String askSet = currentSet.replace("_set", "_ask");
                ModNetwork.CHANNEL.sendToServer(new ModNetwork.ChoiceSelectedPacket(askSet, askSet, null));
                this.onClose();
                return;
            }
        }*/
    }
}