package fr.gcjojo.worldscolliding.dialogues;

import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import net.minecraft.client.gui.GuiGraphics;

public class DialogueNext extends DialogueAction {

    String nextSet;

    public DialogueNext(String nextSet) {
        this.nextSet = nextSet;
    }

    @Override
    public void setup(DialogueScreen screen) {
        super.setup(screen);
        screen.changeSet(nextSet);
    }

    @Override
    public void step() { }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) { }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    @Override
    public boolean isBlocking() { return true; }
}
