package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import fr.gcjojo.worldscolliding.ModEntry;
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

    private float currentFadeTime;

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
    }

    @Override
    public void step() {

    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
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

    public int getImageId()                     { return this.imageId; }
    public float getXPercentage()               { return this.xPercentage; }
    public float getYPercentage()               { return this.yPercentage; }
    public float getScale()                     { return this.scale; }
    public void setXPercentage(float newValue)  { this.xPercentage = newValue; }
    public void setYPercentage(float newValue)  { this.yPercentage = newValue; }
    public void setScale(float newValue)        { this.scale = newValue; }
}
