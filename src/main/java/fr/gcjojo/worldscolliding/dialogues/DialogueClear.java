package fr.gcjojo.worldscolliding.dialogues;

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
}
