package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import net.minecraft.client.gui.GuiGraphics;

public class DialogueClear extends DialogueAction {

    String clearedClass;

    public DialogueClear(){
        this.clearedClass = "none";
    }

    public DialogueClear(String clearedClass){
        this.clearedClass = clearedClass;
    }

    public DialogueClear(JsonObject object){
        if(object.has("cleared_actions"))
            this.clearedClass = object.get("cleared_actions").getAsString();
        else
            this.clearedClass = "none";
    }

    @Override
    public void setup(DialogueScreen screen) {
        super.setup(screen);
        if(clearedClass.equals("none"))
            screen.clearActions();
        else
            screen.clearActions(this.clearedClass);
        screen.advanceDialogue();
    }

    @Override
    public void step() {

    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {

    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public boolean isBlocking() {
        return true;
    }

    @Override
    public boolean isSkippable() { return false; }
}
