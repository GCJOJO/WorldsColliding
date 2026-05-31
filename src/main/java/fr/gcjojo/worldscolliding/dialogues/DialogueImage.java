package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;


public class DialogueImage extends DialogueAction {

    private int imageId;
    private ResourceLocation image;
    private int initialWidth;
    private int initialHeight;
    private float xPercentage = 50;
    private float yPercentage = 50;
    private float scale = 1.0f;

    private int alpha = 255;

    public DialogueImage(int imageId, String imagePath, int initialWidth, int initialHeight) {
        this.imageId = imageId;
        this.image = ResourceLocation.parse(imagePath);
        this.initialWidth = initialWidth;
        this.initialHeight = initialHeight;
    }

    public DialogueImage(JsonObject object){
        if(object.has("id"))
            this.imageId = object.get("id").getAsInt();
        if(object.has("image"))
            this.image = ResourceLocation.parse(object.get("image").getAsString());
        if(object.has("width"))
            this.initialWidth = object.get("width").getAsInt();
        if(object.has("height"))
            this.initialHeight = object.get("height").getAsInt();
        if(object.has("x"))
            this.xPercentage = object.get("x").getAsFloat();
        if(object.has("y"))
            this.yPercentage = object.get("y").getAsFloat();
        if(object.has("scale"))
            this.scale = object.get("scale").getAsFloat();
        if(object.has("alpha"))
            this.alpha = object.get("alpha").getAsInt();
    }

    @Override
    public void step() {

    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(alpha <= 8)
            return;

        if(this.alpha >= 255) this.alpha = 255;

        PoseStack pose = graphics.pose();
        pose.pushPose();
        //pose.translate(screen.width / 2f,screen.height / 2f, 0);
        float xPos = screen.width * xPercentage * 0.01f;
        float yPos = screen.height * yPercentage * 0.01f;
        pose.translate(xPos, yPos, 1);

        double imageHeight = ((double) screen.height * 0.5d) / initialHeight;
        double imageWidth = (double)initialWidth * (imageHeight / (double)initialHeight);

        //image, topPos, leftPos, uvX, uvY, width, height
        pose.scale((float) imageHeight * scale, (float) imageWidth * scale, 1);

        // Merci copislop AKA copiflop >:(
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, (float) this.alpha / 255);
        graphics.blit(image, -initialWidth / 2, -initialHeight / 2, 0, 0, initialWidth, initialHeight, initialWidth, initialHeight);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();

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

    @Override
    public boolean isSkippable() { return true; }

    public int getImageId()                     { return this.imageId; }
    public float getXPercentage()               { return this.xPercentage; }
    public float getYPercentage()               { return this.yPercentage; }
    public float getScale()                     { return this.scale; }
    public int getAlpha()                       { return this.alpha; }
    public void setXPercentage(float newValue)  { this.xPercentage = newValue; }
    public void setYPercentage(float newValue)  { this.yPercentage = newValue; }
    public void setScale(float newValue)        { this.scale = newValue; }
    public void setAlpha(int newValue)          { this.alpha = newValue; }
}
