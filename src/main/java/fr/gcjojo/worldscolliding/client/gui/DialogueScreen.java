package fr.gcjojo.worldscolliding.client.gui;

import fr.gcjojo.worldscolliding.ModEntry;
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
    private List<DialogueSpeaker> dialogueSpeakers;

    private String currentSet;

    private List<DialogueAction> currentActions = new ArrayList<>();

    private boolean advanceDialogueAtTickEnd = false;

    public DialogueScreen(String setName) {
        super(Component.literal("Dialogue"));
        this.currentSet = setName;
        var actions = loadSet(setName);
        if(actions == null || actions.isEmpty())
        {
            this.onClose();
            return;
        }
        this.dialogueActions = actions;

        var speakers = loadSpeakers(setName);
        if(speakers == null || speakers.isEmpty())
        {
            this.onClose();
            return;
        }
        this.dialogueSpeakers = speakers;
    }

    public DialogueScreen(String setName, List<DialogueAction> actions, List<DialogueSpeaker> speakers) {
        super(Component.literal("Dialogue"));
        this.currentSet = setName;
        this.dialogueActions = actions;
        this.dialogueSpeakers = speakers;
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
        List<DialogueSpeaker> newSpeakers = loadSpeakers(setName);
        if(newSpeakers == null) {
            this.onClose();
            return;
        }

        currentSet = setName;
        actionIndex = -1;
        dialogueActions = newActions;
        dialogueSpeakers = newSpeakers;
        advanceDialogue();
    }

    public static List<DialogueAction> loadSet(String setPath) {
        try {
            String namespace = ModEntry.MODID;
            String setName = setPath;
            if(setPath.contains(":"))
            {
                namespace = setPath.split(":")[0];
                setName = setPath.split(":")[1];
            }

            ResourceLocation res = ResourceLocation.fromNamespaceAndPath(namespace, "dialogues.json");
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
                            case "clear" -> actions.add(new DialogueClear(obj));
                            case "wait" -> actions.add(new DialogueWait(obj));
                            case "choice" -> actions.add(new DialogueChoice(obj));
                            case "change_set" -> actions.add(new DialogueNext(obj.get("set").getAsString()));
                            case "fade" -> actions.add(new DialogueFading(obj));
                            case "message" -> actions.add(new DialogueMessage(obj.get("speaker").getAsInt(), Component.translatable(obj.get("text").getAsString()).getString()));
                            case "image" -> actions.add(new DialogueImage(obj));
                            case "credit" -> actions.add(new DialogueCredit(obj));
                            case "image_move" -> actions.add(new DialogueMoveImage(obj));
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

    public static List<DialogueSpeaker> loadSpeakers(String setPath){
        try {
            String namespace = ModEntry.MODID;
            String setName = setPath;
            if (setPath.contains(":")) {
                namespace = setPath.split(":")[0];
                setName = setPath.split(":")[1];
            }

            ResourceLocation res = ResourceLocation.fromNamespaceAndPath(namespace, "dialogues.json");
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resourceOpt.isPresent()) {
                JsonObject root = new Gson().fromJson(new InputStreamReader(resourceOpt.get().open()), JsonObject.class);
                List<DialogueSpeaker> speakers = new ArrayList<>();
                if(!root.has("speakers"))
                    return speakers;

                root.getAsJsonArray("speakers").forEach(element -> {
                    JsonObject obj = element.getAsJsonObject();
                    speakers.add(new DialogueSpeaker(obj));
                });

                return speakers;
            }
        } catch (Exception e) {
            ModEntry.getLogger().warn("Oopsie cannot load speakers !");
        }

        return null;
    }

    public static void openForSet(String setName) {
        List<DialogueAction> actions = loadSet(setName);
        List<DialogueSpeaker> speakers = loadSpeakers(setName);
        if(actions != null && !actions.isEmpty() && speakers != null)
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new DialogueScreen(setName, actions, speakers)));
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
        if(actionIndex >= dialogueActions.size())
            return;
        if(!currentSet.contains("credits"))
            //graphics.fill(0, 0, this.width, this.height, 0x44000000);
            graphics.fillGradient(0, 0, this.width, this.height, 0x11000000, 0xDD000000);

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
        if(keyCode == 256) {
            endDialogue();
            return true;
        }

        if(actionIndex < dialogueActions.size()) {
            currentActions.forEach(dialogueAction -> dialogueAction.keyPressed(keyCode, scanCode, modifiers));
        }

//        if (keyCode == 257 || keyCode == 32) {
//            advanceDialogue();
//            return true;
//        }

        return false;
    }

    public void queueAdvanceDialogue() { this.advanceDialogueAtTickEnd = true; }

    public void advanceDialogue() {
        actionIndex++;
        if (dialogueActions.isEmpty() || actionIndex >= dialogueActions.size()) {
            endDialogue();
            return;
        }

        currentActions.removeIf(DialogueAction::isBlocking);

        for(int i = actionIndex; i <= dialogueActions.size(); i++)
        {
            DialogueAction currentAction = dialogueActions.get(i);
            currentActions.add(currentAction);
            currentAction.setup(this);
            if(currentAction.isBlocking())
            {
                actionIndex = i;
                break;
            }
        }
    }

    public List<DialogueSpeaker> getDialogueSpeakers() { return this.dialogueSpeakers; }

    public DialogueSpeaker getDialogueSpeaker(int id){
        if (dialogueSpeakers.isEmpty())
            return null;
        for(var speaker : this.dialogueSpeakers)
            if(speaker.getId() == id) return speaker;
        return null;
    }

    public void clearActions() { currentActions.clear(); }

    public void clearActions(String clearedClass){
        Class<? extends DialogueAction> classToRemove = null;

        switch (clearedClass)
        {
            case "message" -> classToRemove = DialogueMessage.class;
            case "credit" -> classToRemove = DialogueCredit.class;
        }
        if(classToRemove != null)
            currentActions.removeIf(classToRemove::isInstance);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    public void endDialogue(){
        clearActions();
        ModNetwork.sendToServer(new ModNetwork.DialogueCompletedPacket(currentSet));
        this.onClose();
    }

    public List<DialogueAction> getCurrentActions() {
        return this.currentActions;
    }
}