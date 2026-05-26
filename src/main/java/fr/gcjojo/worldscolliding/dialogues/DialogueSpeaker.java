package fr.gcjojo.worldscolliding.dialogues;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FastColor;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class DialogueSpeaker {
    private int id;
    private String name;
    private int color;
    private SoundEvent[] sounds;

    public DialogueSpeaker(JsonObject obj){
        this.color = 0xFFFFFFFF;
        String colorStr = obj.get("color").getAsString();
        try{
            if(colorStr.length() == 8) {
                int alpha = Integer.parseInt(colorStr.substring(0, 2), 16);
                int red = Integer.parseInt(colorStr.substring(2, 4), 16);
                int green = Integer.parseInt(colorStr.substring(4, 6), 16);
                int blue = Integer.parseInt(colorStr.substring(6, 8), 16);
                this.color = FastColor.ARGB32.color(alpha, red, green, blue);
            }
        } catch (Exception e) {
            ModEntry.getLogger().warn("Unable to parse color {}", colorStr);
        }

        JsonArray soundsArray = obj.get("sounds").getAsJsonArray();
        String[] soundNames = new String[soundsArray.size()];
        for(int i = 0; i <= obj.get("sounds").getAsJsonArray().size() - 1; i++)
            soundNames[i] = soundsArray.get(i).getAsString();

        this.id = obj.get("id").getAsInt();
        this.name = obj.get("name").getAsString();
        this.sounds = loadSounds(soundNames);
    }

    public DialogueSpeaker(int id, String name, int color, String[] soundNames){
        this.id = id;
        this.name = name;
        this.color = color;
        this.sounds = loadSounds(soundNames);
    }

    public DialogueSpeaker(int id, String name, int color, SoundEvent[] sounds){
        this.id = id;
        this.name = name;
        this.color = color;
        this.sounds = sounds;
    }

    public static SoundEvent[] loadSounds(String[] soundNames){
        SoundEvent[] loadedSounds = new SoundEvent[soundNames.length];
        for(int i = 0; i <= soundNames.length - 1; i++)
        {
            String soundName = soundNames[i];
            RegistryObject<SoundEvent> soundEvent = RegistryObject.create(ResourceLocation.parse(soundName), ForgeRegistries.SOUND_EVENTS);
            if(!soundEvent.isPresent()) continue;
            loadedSounds[i] = soundEvent.get();
        }
        return loadedSounds;
    }

    public int getId() { return this.id; }
    public String getName() { return this.name; }
    public int getColor() { return this.color; }
    public SoundEvent[] getSounds() { return this.sounds; }
}
