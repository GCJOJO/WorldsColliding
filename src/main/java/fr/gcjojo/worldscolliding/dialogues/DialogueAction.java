package fr.gcjojo.worldscolliding.dialogues;

import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import net.minecraft.client.gui.GuiGraphics;

public abstract class DialogueAction
{
    protected DialogueScreen screen;

    public void setup(DialogueScreen screen)
    {
        this.screen = screen;
    }

    public abstract void step();
    public abstract void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    public abstract void mouseClicked(double mouseX, double mouseY, int button);
    public abstract void keyPressed(int keyCode, int scanCode, int modifiers);

    public abstract boolean isBlocking();
}
