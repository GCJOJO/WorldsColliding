package fr.gcjojo.worldscolliding.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.dialogues.*;
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
    private List<DialogueAction> dialogueActions;
    private int actionIndex = -1;

    private List<DialogueAction> currentActions = new ArrayList<>();

    private boolean advanceDialogueAtTickEnd = false;

    public DialogueScreen(List<DialogueAction> actions) {
        super(Component.literal("Dialogue"));
        this.dialogueActions = actions;
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

        changeSet(nextSet);
    }

    public void changeSet(String setName) {
        List<DialogueAction> newActions = loadSet(setName);
        if(newActions == null) {
            this.onClose();
            return;
        }

        actionIndex = -1;
        dialogueActions = newActions;
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
                        String action = obj.get("action").getAsString();
                        switch(action)
                        {
                            case "clear" -> actions.add(new DialogueClear());
                            case "wait" -> actions.add(new DialogueWait(obj.get("time").getAsFloat()));
                            case "choice" -> actions.add(new DialogueChoice(
                                obj.get("option1").getAsString(), obj.get("next1").getAsString(), obj.get("save1").getAsString(), obj.get("action1").getAsString(),
                                obj.get("option2").getAsString(), obj.get("next2").getAsString(), obj.get("save2").getAsString(), obj.get("action2").getAsString()
                            ));
                            case "change_set" -> actions.add(new DialogueNext(obj.get("set").getAsString()));
                            case "fade" -> actions.add(new DialogueFading(obj.get("from").getAsString(), obj.get("to").getAsString(), obj.get("time").getAsFloat()));
                            case "message" -> actions.add(new DialogueMessage(obj.get("speaker").getAsString(), Component.translatable(obj.get("text").getAsString()).getString()));
                            case "image" -> actions.add(new DialogueImage(obj.get("id").getAsInt(), obj.get("image").getAsString(), obj.get("width").getAsInt(), obj.get("height").getAsInt()));
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
        if(actionIndex >= dialogueActions.size())
            return;

        currentActions.forEach(DialogueAction::step);

        if(advanceDialogueAtTickEnd) {
            this.advanceDialogueAtTickEnd = false;
            this.advanceDialogue();
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        //draw images : graphics.blit(BACKGROUND_LOCATION, 0, 0, 0, 0.0F, 0.0F, this.width, this.height, 32, 32);
        graphics.fill(0, 0, this.width, this.height, 0x44000000);

        if(actionIndex >= dialogueActions.size())
            return;

        currentActions.forEach(action -> action.draw(graphics, mouseX, mouseY, partialTick));

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if(actionIndex < dialogueActions.size()) {
            currentActions.forEach(dialogueAction -> dialogueAction.mouseClicked(mouseX, mouseY, button));
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if(actionIndex < dialogueActions.size()) {
            currentActions.forEach(dialogueAction -> dialogueAction.mouseClicked(keyCode, scanCode, modifiers));
        }

        if (keyCode == 257 || keyCode == 32) {
            advanceDialogue();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    public void queueAdvanceDialogue() { this.advanceDialogueAtTickEnd = true; }

    public void advanceDialogue() {
        if (dialogueActions.isEmpty() || actionIndex >= dialogueActions.size() - 1) {
            this.onClose();
            return;
        }
        currentActions.removeIf(DialogueAction::isBlocking);

        for(int i = actionIndex; i <= dialogueActions.size() - 1; i++)
        {
            actionIndex++;
            DialogueAction currentAction = dialogueActions.get(actionIndex);
            currentActions.add(currentAction);
            currentAction.setup(this);
            if(currentAction.isBlocking())
                break;
        }
    }

    public void clearActions() {
        currentActions.clear();
    }
}