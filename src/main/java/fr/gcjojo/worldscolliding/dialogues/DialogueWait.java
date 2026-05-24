package fr.gcjojo.worldscolliding.dialogues;

import net.minecraft.client.gui.GuiGraphics;

public class DialogueWait extends DialogueAction {

    float waitTime;
    float currentWaitTime;

    public DialogueWait(float waitTime){
        this.waitTime = waitTime;
    }

    @Override
    public void step() {
        if(currentWaitTime >= waitTime)
            screen.queueAdvanceDialogue();
    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        currentWaitTime += partialTick;
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) { }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    @Override
    public boolean isBlocking() {
        return true;
    }
}
