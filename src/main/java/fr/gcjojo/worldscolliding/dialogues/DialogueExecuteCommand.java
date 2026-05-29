package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.client.gui.GuiGraphics;

public class DialogueExecuteCommand extends DialogueAction {
    private String command;

    public DialogueExecuteCommand(JsonObject object){
        if(object.has("command")) {
            this.command = object.get("command").getAsString();
        }
    }

    @Override
    public void setup(DialogueScreen screen){
        super.setup(screen);
        if(this.command != null) {
            if (this.command.contains("<PLAYER>"))
                this.command = this.command.replace("<PLAYER>", screen.getMinecraft().player.getName().getString());

            ModNetwork.sendToServer(new ModNetwork.DialogueCommandPacket(command));
        }
        screen.queueAdvanceDialogue();
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
    public boolean isBlocking() {
        return true;
    }

    @Override
    public boolean isSkippable() {
        return true;
    }
}
