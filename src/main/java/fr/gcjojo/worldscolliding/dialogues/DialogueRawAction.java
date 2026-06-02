package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;

public class DialogueRawAction extends DialogueAction {

    private String action;

    public DialogueRawAction(JsonObject object){
        this.action = object.get("action").getAsString();
    }

    public String getRawAction() { return this.action; }

    @Override
    public void step() { }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) { }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) { }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    @Override
    public boolean isBlocking() { return false; }

    @Override
    public boolean isSkippable() { return true; }
}
