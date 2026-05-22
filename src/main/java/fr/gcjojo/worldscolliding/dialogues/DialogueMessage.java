package fr.gcjojo.worldscolliding.dialogues;

import fr.gcjojo.worldscolliding.ModEntry;
import fr.gcjojo.worldscolliding.client.gui.DialogueScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.FastColor;

import java.util.List;
import java.util.Random;

public class DialogueMessage extends DialogueAction
{
    private String speaker;
    private String dialogueLine;
    private int backgroundColor;

    private int charIndex = 0;
    private int tickCount = 0;
    private int pauseTimer = 0;
    private final Random random = new Random();
    private static final Font font = Minecraft.getInstance().font;

    public DialogueMessage(String speaker, String dialogueLine) {
        this.speaker = speaker;
        this.dialogueLine = dialogueLine;
        this.backgroundColor = FastColor.ARGB32.color(0, 0, 0, 0); // transparent
    }

    public DialogueMessage(String speaker, String dialogueLine, String backgroundColor) {
        this.speaker = speaker;
        this.dialogueLine = dialogueLine;
        this.backgroundColor = FastColor.ARGB32.color(0, 0, 0, 0);
        try{
            if(backgroundColor.length() == 8) {
                int alpha = Integer.parseInt(backgroundColor.substring(0, 2), 16);
                int red = Integer.parseInt(backgroundColor.substring(2, 4), 16);
                int green = Integer.parseInt(backgroundColor.substring(4, 6), 16);
                int blue = Integer.parseInt(backgroundColor.substring(6, 8), 16);
                this.backgroundColor = FastColor.ARGB32.color(alpha, red, green, blue);
            }
        } catch (Exception e) {
            ModEntry.getLogger().error(e.getMessage());
        }
    }

    @Override
    public void step() {
        tickCount++;

        if (pauseTimer > 0) {
            pauseTimer--;
            return;
        }

        if (charIndex >= dialogueLine.length()) return;

        charIndex++;
        SoundEvent[] sounds = DialogueUtils.getSoundForSpeaker(speaker);
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sounds[random.nextInt(sounds.length)], 1.0F));

        char currentChar = dialogueLine.charAt(charIndex - 1);
        if (currentChar == '.' || currentChar == '!' || currentChar == '?') {
            pauseTimer = 8;
        }
    }

    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int boxWidth = 300;
        int boxHeight = 80;
        int boxX = (screen.width - boxWidth) / 2;
        int boxY = screen.height - boxHeight - 20;

        if(backgroundColor != 0x00000000)
            graphics.fill(0, 0, screen.width, screen.height, backgroundColor);

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000);
        String speakerTranslated = Component.translatable(speaker).getString();
        graphics.drawString(font, speakerTranslated, boxX + 10, boxY + 5, DialogueUtils.getSpeakerColor(speaker), false);



        if (dialogueLine != null) {
            String displayedText = dialogueLine.substring(0, charIndex);
            graphics.drawWordWrap(font, Component.literal(displayedText), boxX + 10, boxY + 20, boxWidth - 20, 0xFFFFFF);

            if (charIndex >= dialogueLine.length() && (tickCount % 20 < 10)) {
                graphics.drawString(font, "▼", boxX + boxWidth - 15, boxY + boxHeight - 15, 0xFFFFFF, false);
            }
        }
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) {
        if(charIndex >= dialogueLine.length())
            screen.advanceDialogue();
        else
            charIndex = dialogueLine.length();
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if(charIndex >= dialogueLine.length())
            screen.advanceDialogue();
        else
            charIndex = dialogueLine.length();
    }
}
