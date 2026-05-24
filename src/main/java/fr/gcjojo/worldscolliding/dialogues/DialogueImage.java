package fr.gcjojo.worldscolliding.dialogues;

import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;


public class DialogueImage extends DialogueAction {

    private int imageId;
    private ResourceLocation image;
    private int initialWidth;
    private int initialHeight;

    private float currentFadeTime;

    public DialogueImage(int imageId, String imagePath, int initialWidth, int initialHeight) {
        this.imageId = imageId;
        this.image = ResourceLocation.parse(imagePath);
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
    }

    @Override
    public void step() {

    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int screenCenterX = screen.width / 2;
        int screenCenterY = screen.height / 2;

        int height = screen.height;
        int width = (int)Math.ceil(initialWidth * (height / (double)initialHeight));

        int imageX = screenCenterX - width / 2;
        int imageY = screenCenterY - height / 2;

        //image, topPos, leftPos, uvX, uvY, width, height
        graphics.blit(image, imageX, imageY, 0, 0, width, height);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {

    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {

    }

    @Override
    public boolean isBlocking() {
        return false;
    }
}
