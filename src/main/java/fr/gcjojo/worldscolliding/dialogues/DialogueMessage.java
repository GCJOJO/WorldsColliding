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
    private final int speakerId;
    private final String dialogueLine;

    private int charIndex = 0;
    private int tickCount = 0;
    private int pauseTimer = 0;
    private final Random random = new Random();
    private static final Font font = Minecraft.getInstance().font;

    public DialogueMessage(int speakerId, String dialogueLine) {
        this.speakerId = speakerId;
        this.dialogueLine = dialogueLine;
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
        SoundEvent[] sounds = screen.getDialogueSpeaker(this.speakerId).getSounds();
        if(sounds.length >= 1)
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

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000);
        String speakerTranslated = Component.translatable(screen.getDialogueSpeaker(this.speakerId).getName()).getString();
        graphics.drawString(font, speakerTranslated, boxX + 10, boxY + 5, screen.getDialogueSpeaker(this.speakerId).getColor(), false);

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
            screen.queueAdvanceDialogue();
        else
            charIndex = dialogueLine.length();
    }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode != 257 && keyCode != 32)
            return;

        if(charIndex >= dialogueLine.length())
            screen.queueAdvanceDialogue();
        else
            charIndex = dialogueLine.length();
    }

    @Override
    public boolean isBlocking() { return true; }
}
