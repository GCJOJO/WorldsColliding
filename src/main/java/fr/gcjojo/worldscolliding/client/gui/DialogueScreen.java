package fr.gcjojo.worldscolliding.client.gui;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.network.ModNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DialogueScreen extends Screen {

    public record DialogueLine(String speaker, String text, String option1, String action1, String next1, String save1, String option2, String action2, String next2, String save2) {
        public DialogueLine(String speaker, String text) {
            this(speaker, text, null, null, null, null, null, null, null, null);
        }
    }

    private boolean hasOptions() {
        if (dialogues.isEmpty()) return false;
        DialogueLine lastLine = dialogues.get(dialogues.size() - 1);
        return lastLine.option1() != null && !lastLine.option1().isEmpty();
    }

    private final List<DialogueLine> dialogues;
    private int currentIndex = 0;
    private int charIndex = 0;
    private int tickCount = 0;
    private int pauseTimer = 0;
    private final Random random = new Random();

    public DialogueScreen(List<DialogueLine> dialogues) {
        super(Component.literal("Dialogue"));
        this.dialogues = dialogues;
    }

    @Override
    protected void init() {
        super.init();
        setupChoiceWidgets();
    }

    private void setupChoiceWidgets() {
        this.clearWidgets();
        if (currentIndex >= dialogues.size()) return;
        DialogueLine currentLine = dialogues.get(currentIndex);

        if ("choix".equals(currentLine.speaker())) {
            int btnWidth = 140;
            int btnHeight = 20;
            int yPos = this.height / 2 + 50;

            this.addRenderableWidget(Button.builder(Component.literal(currentLine.option1()), b -> {
                handleChoiceSelection(currentLine.next1(), currentLine.save1(), currentLine.action1());
            }).bounds(this.width / 4 - btnWidth / 2, yPos, btnWidth, btnHeight).build());

            this.addRenderableWidget(Button.builder(Component.literal(currentLine.option2()), b -> {
                handleChoiceSelection(currentLine.next2(), currentLine.save2(), currentLine.action2());
            }).bounds(3 * this.width / 4 - btnWidth / 2, yPos, btnWidth, btnHeight).build());
        }
    }

    private void handleChoiceSelection(String nextSet, String saveSet, String action) {
        ModNetwork.sendToServer(new ModNetwork.ChoiceSelectedPacket(nextSet, saveSet, action));

        if (action != null && action.startsWith("seal_")) {
            this.onClose();
            return;
        }

        try {
            ResourceLocation res = new ResourceLocation("worldscolliding", "dialogues.json");
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resourceOpt.isPresent()) {
                JsonObject root = new Gson().fromJson(new InputStreamReader(resourceOpt.get().open()), JsonObject.class);
                if (root.has(nextSet)) {
                    this.dialogues.clear();
                    root.getAsJsonArray(nextSet).forEach(element -> {
                        JsonObject obj = element.getAsJsonObject();
                        String speaker = obj.get("speaker").getAsString();
                        if ("choix".equals(speaker)) {
                            this.dialogues.add(new DialogueLine(
                                    speaker, null,
                                    obj.get("option1").getAsString(), obj.get("action1").getAsString(), obj.get("next1").getAsString(), obj.get("save1").getAsString(),
                                    obj.get("option2").getAsString(), obj.get("action2").getAsString(), obj.get("next2").getAsString(), obj.get("save2").getAsString()
                            ));
                        } else {
                            this.dialogues.add(new DialogueLine(speaker, obj.get("text").getAsString()));
                        }
                    });
                    this.currentIndex = 0;
                    this.charIndex = 0;
                    this.tickCount = 0;
                    this.pauseTimer = 0;
                    setupChoiceWidgets();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.onClose();
    }

    public static void openForSet(String setName) {
        try {
            ResourceLocation res = new ResourceLocation("worldscolliding", "dialogues.json");
            var resourceOpt = Minecraft.getInstance().getResourceManager().getResource(res);
            if (resourceOpt.isPresent()) {
                JsonObject root = new Gson().fromJson(new InputStreamReader(resourceOpt.get().open()), JsonObject.class);
                if (root.has(setName)) {
                    List<DialogueLine> lines = new ArrayList<>();
                    root.getAsJsonArray(setName).forEach(element -> {
                        JsonObject obj = element.getAsJsonObject();
                        String speaker = obj.get("speaker").getAsString();
                        if ("choix".equals(speaker)) {
                            lines.add(new DialogueLine(
                                    speaker, null,
                                    obj.get("option1").getAsString(), obj.get("action1").getAsString(), obj.get("next1").getAsString(), obj.get("save1").getAsString(),
                                    obj.get("option2").getAsString(), obj.get("action2").getAsString(), obj.get("next2").getAsString(), obj.get("save2").getAsString()
                            ));
                        } else {
                            lines.add(new DialogueLine(speaker, obj.get("text").getAsString()));
                        }
                    });
                    Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new DialogueScreen(lines)));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void tick() {
        tickCount++;

        if (pauseTimer > 0) {
            pauseTimer--;
            return;
        }

        if (currentIndex >= dialogues.size()) return;
        DialogueLine currentLine = dialogues.get(currentIndex);
        if ("choix".equals(currentLine.speaker())) return;

        if (charIndex < currentLine.text().length()) {
            charIndex++;
            playSoundForSpeaker(currentLine.speaker());

            char currentChar = currentLine.text().charAt(charIndex - 1);
            if (currentChar == '.' || currentChar == '!' || currentChar == '?') {
                pauseTimer = 8;
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);

        if (currentIndex >= dialogues.size()) return;
        DialogueLine currentLine = dialogues.get(currentIndex);

        if ("choix".equals(currentLine.speaker())) {
            String opt1 = currentLine.option1();
            graphics.drawCenteredString(this.font, opt1, this.width / 4, this.height / 2, 0xFFFFFF);

            String opt2 = currentLine.option2();
            graphics.drawCenteredString(this.font, opt2, 3 * this.width / 4, this.height / 2, 0xFFFFFF);
        } else {
            int boxWidth = 300;
            int boxHeight = 80;
            int boxX = (this.width - boxWidth) / 2;
            int boxY = this.height - boxHeight - 20;

            graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x80000000);
            String speaker = currentLine.speaker();
            graphics.drawString(this.font, speaker, boxX + 10, boxY + 5, getSpeakerColor(speaker), false);

            if (currentLine.text() != null) {
                String displayedText = currentLine.text().substring(0, charIndex);
                graphics.drawWordWrap(this.font, Component.literal(displayedText), boxX + 10, boxY + 20, boxWidth - 20, 0xFFFFFF);

                if (charIndex >= currentLine.text().length() && (tickCount % 20 < 10)) {
                    graphics.drawString(this.font, "▼", boxX + boxWidth - 15, boxY + boxHeight - 15, 0xFFFFFF, false);
                }
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentIndex < dialogues.size() && "choix".equals(dialogues.get(currentIndex).speaker())) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        advanceDialogue();
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (currentIndex < dialogues.size() && "choix".equals(dialogues.get(currentIndex).speaker())) {
            return super.keyPressed(keyCode, scanCode, modifiers);
        }
        if (keyCode == 257 || keyCode == 32) {
            advanceDialogue();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void advanceDialogue() {
        if (dialogues.isEmpty() || currentIndex >= dialogues.size()) return;
        DialogueLine currentLine = dialogues.get(currentIndex);

        if (charIndex < currentLine.text().length()) {
            charIndex = currentLine.text().length();
        } else {
            if (currentIndex >= dialogues.size() - 1) {
                if (!hasOptions()) {
                    String currentSet = Minecraft.getInstance().player.getPersistentData().getString("CurrentChapter");
                    if (currentSet.endsWith("_set") && !currentSet.contains("_ask")) {
                        String askSet = currentSet.replace("_set", "_ask");
                        ModNetwork.CHANNEL.sendToServer(new ModNetwork.ChoiceSelectedPacket(askSet, askSet, null));
                        this.onClose();
                        return;
                    }
                }
                this.onClose();
            } else {
                currentIndex++;
                charIndex = 0;
                setupChoiceWidgets();
            }
        }
    }

    private int getSpeakerColor(String speaker) {
        return switch (speaker) {
            case "The One" -> 0xFFD700;
            case "The Scourge" -> 0x555555;
            case "The Voice" -> 0xFF0000;
            default -> 0xFFFFFF;
        };
    }

    private void playSoundForSpeaker(String speaker) {
        SoundEvent[] sounds = switch (speaker) {
            case "The One" -> new SoundEvent[]{SoundEvents.NOTE_BLOCK_BELL.value(), SoundEvents.NOTE_BLOCK_CHIME.value()};
            case "The Scourge" -> new SoundEvent[]{SoundEvents.NOTE_BLOCK_BASS.value()};
            case "The Voice" -> new SoundEvent[]{SoundEvents.NOTE_BLOCK_BIT.value()};
            default -> new SoundEvent[]{SoundEvents.NOTE_BLOCK_HARP.value()};
        };
        if (sounds.length > 0) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sounds[random.nextInt(sounds.length)], 1.0F));
        }
    }
}