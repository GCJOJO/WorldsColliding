package fr.gcjojo.worldscolliding.dialogues;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.util.Random;

public class DialogueUtils
{
    public static int getSpeakerColor(String speaker) {
        return switch (speaker) {
            case "worldscolliding.speaker.the_one" -> 0xFFD700;
            case "worldscolliding.speaker.scourge" -> 0x555555;
            case "worldscolliding.speaker.the_voice" -> 0xFF0000;
            default -> 0xFFFFFF;
        };
    }

    public static SoundEvent[] getSoundForSpeaker(String speaker) {
        return switch (speaker) {
            case "worldscolliding.speaker.the_one" -> new SoundEvent[]{ SoundEvents.NOTE_BLOCK_BELL.value(), SoundEvents.NOTE_BLOCK_CHIME.value() };
            case "worldscolliding.speaker.scourge" -> new SoundEvent[]{ SoundEvents.NOTE_BLOCK_BASS.value() };
            case "worldscolliding.speaker.the_voice" -> new SoundEvent[]{ SoundEvents.NOTE_BLOCK_BIT.value() };
            default -> new SoundEvent[] { SoundEvents.NOTE_BLOCK_HARP.value() };
        };
    }
}
