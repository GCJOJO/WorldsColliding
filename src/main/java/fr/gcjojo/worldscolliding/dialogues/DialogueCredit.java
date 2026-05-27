package fr.gcjojo.worldscolliding.dialogues;

import com.eliotlash.mclib.utils.MathUtils;
import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.Vec3i;
import net.minecraftforge.common.util.TextTable;

public class DialogueCredit extends DialogueAction {
    enum DisplayState
    {
        FADE_IN,
        HOLD,
        FADE_OUT,
        TRANSPARENT
    }

    enum TextAlignment{
        LEFT,
        CENTER,
        RIGHT
    }

    private String text;
    private float fadeInTime;
    private float holdTime;
    private float fadeOutTime;
    private float xPercentage;
    private float yPercentage;
    private float scale;
    private Vec3i color;
    private TextAlignment alignment;

    private DisplayState state;
    private static final Font font = Minecraft.getInstance().font;

    private int alpha = 0;
    private float currentTime = 0.0f;

    public DialogueCredit(String text, float fadeInTime, float holdTime, float fadeOutTime, float xPercentage, float yPercentage, float scale, String color){
        this.text = text;
        this.fadeInTime = fadeInTime;
        this.holdTime = holdTime;
        this.fadeOutTime = fadeOutTime;
        this.xPercentage = xPercentage;
        this.yPercentage = yPercentage;
        this.scale = scale;

        try {
            if(color.length() == 6) {
                int red = Integer.parseInt(color.substring(0, 2), 16);
                int green = Integer.parseInt(color.substring(2, 4), 16);
                int blue = Integer.parseInt(color.substring(4, 6), 16);
                this.color = new Vec3i(red, green, blue);
            }
        } catch (Exception e) { ModEntry.getLogger().error(e.getMessage()); }

        this.alignment = TextAlignment.CENTER;
        this.alpha = 0;
        state = DisplayState.FADE_IN;
    }

    public DialogueCredit(JsonObject object){
        this.text = "";
        this.fadeInTime = 0.0f;
        this.holdTime = 1.0f;
        this.fadeOutTime = 0.0f;
        this.xPercentage = 0.0f;
        this.yPercentage = 0.0f;
        this.scale = 1.0f;
        this.color = new Vec3i(255, 255, 255);
        this.alignment = TextAlignment.CENTER;

        this.alpha = 0;
        this.state = DisplayState.FADE_IN;

        if(object.has("text"))
            this.text = object.get("text").getAsString();
        if(object.has("fade_in_time"))
            this.fadeInTime = object.get("fade_in_time").getAsFloat();
        if(object.has("hold_time"))
            this.holdTime = object.get("hold_time").getAsFloat();
        if(object.has("fade_out_time"))
            this.fadeOutTime = object.get("fade_out_time").getAsFloat();
        if(object.has("x"))
            this.xPercentage = object.get("x").getAsFloat();
        if(object.has("y"))
            this.yPercentage = object.get("y").getAsFloat();
        if(object.has("scale"))
            this.scale = object.get("scale").getAsFloat();
        if(object.has("color")) {
            String color = object.get("color").getAsString();
            try {
                if(color.length() == 6) {
                    int red = Integer.parseInt(color.substring(0, 2), 16);
                    int green = Integer.parseInt(color.substring(2, 4), 16);
                    int blue = Integer.parseInt(color.substring(4, 6), 16);
                    this.color = new Vec3i(red, green, blue);
                }
            } catch (Exception e) { ModEntry.getLogger().error(e.getMessage()); }
        }

        if(object.has("alignment"))
        {
            switch(object.get("alignment").getAsString())
            {
                case "center" -> this.alignment = TextAlignment.CENTER;
                case "right" -> this.alignment = TextAlignment.RIGHT;
                default -> this.alignment = TextAlignment.LEFT;
            }
        }
    }

    @Override
    public void step() {
    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if(state == DisplayState.TRANSPARENT)
            return;

        this.alpha = 0;
        PoseStack pose = graphics.pose();
        pose.pushPose();
        switch (state)
        {
            case FADE_IN -> {
                float percentage = MathUtils.clamp(currentTime / fadeInTime, 0.00f, 1.0f);
                this.alpha = (int)(percentage * 255);
                if(currentTime >= fadeInTime) {
                    currentTime = 0.0f;
                    state = DisplayState.HOLD;
                }
            }
            case HOLD -> {
                this.alpha = 255;
                if(currentTime >= holdTime) {
                    currentTime = 0.0f;
                    state = DisplayState.FADE_OUT;
                }
            }
            case FADE_OUT -> {
                float percentage = MathUtils.clamp(currentTime / fadeOutTime, 0.0f, 1.0f);
                this.alpha = 255 - (int)(percentage * 255);
                if (currentTime >= fadeOutTime)
                {
                    currentTime = 0.0f;
                    state = DisplayState.TRANSPARENT;
                    this.alpha = 0;
                }
            }
        }

        MutableComponent textComponent = Component.translatable(this.text);

        int color = FastColor.ARGB32.color(this.alpha, this.color.getX(), this.color.getY(), this.color.getZ());

        //ModEntry.getLogger().info("Alpha : %d, Red : %d, Green : %d, Blue : %d".formatted(this.alpha, this.color.getX(), this.color.getY(), this.color.getZ()));

        int xPos;
        switch(this.alignment)
        {
            case LEFT       -> xPos = (int)(screen.width * (this.xPercentage * 0.01f));
            case RIGHT      -> xPos = (int)(screen.width * (this.xPercentage * 0.01f) - (font.width(textComponent) * scale));
            default         -> xPos = (int)(screen.width * (this.xPercentage * 0.01f) - (font.width(textComponent) * scale * 0.5f));    // CENTER ALIGNMENT
        }

        int yPos = (int)(screen.height * (this.yPercentage * 0.01f));

        pose.translate(xPos, yPos, 1.0f);
        pose.scale(this.scale, this.scale, 1.0f);
        graphics.drawString(font, textComponent, 0, 0, color);
        pose.popPose();

        currentTime += partialTick;
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
