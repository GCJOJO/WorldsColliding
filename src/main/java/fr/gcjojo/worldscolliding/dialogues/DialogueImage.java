package fr.gcjojo.worldscolliding.dialogues;

import com.mojang.blaze3d.vertex.PoseStack;
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
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(screen.width / 2f,screen.height / 2f, 0);

        double imageHeight = ((double) screen.height * 0.5d) / initialHeight;
        double imageWidth = (double)initialWidth * (imageHeight / (double)initialHeight);

        //image, topPos, leftPos, uvX, uvY, width, height
        pose.scale((float) imageHeight, (float) imageWidth, 1);
        graphics.blit(image, -initialWidth / 2, -initialHeight / 2, 0, 0, initialWidth, initialHeight, initialWidth, initialHeight);
        pose.popPose();
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
