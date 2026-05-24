package fr.gcjojo.worldscolliding.dialogues;

import com.eliotlash.mclib.utils.MathUtils;
import fr.gcjojo.worldscolliding.ModEntry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.FastColor;

public class DialogueFading extends DialogueAction
{
    private int fromColor;
    private int toColor;
    private float time;

    private float partialTicks = 0;

    public DialogueFading(String from, String to, float time) {
        this.fromColor = 0x00000000;
        this.toColor = 0x00000000;
        this.time = time;
        try{
            if(from.length() == 8) {
                int alpha = Integer.parseInt(from.substring(0, 2), 16);
                int red = Integer.parseInt(from.substring(2, 4), 16);
                int green = Integer.parseInt(from.substring(4, 6), 16);
                int blue = Integer.parseInt(from.substring(6, 8), 16);
                this.fromColor = FastColor.ARGB32.color(alpha, red, green, blue);
            }
            if(to.length() == 8) {
                int alpha = Integer.parseInt(to.substring(0, 2), 16);
                int red = Integer.parseInt(to.substring(2, 4), 16);
                int green = Integer.parseInt(to.substring(4, 6), 16);
                int blue = Integer.parseInt(to.substring(6, 8), 16);
                this.toColor = FastColor.ARGB32.color(alpha, red, green, blue);
            }
        } catch (Exception e) {
            ModEntry.getLogger().error(e.getMessage());
        }
    }

    @Override
    public void step() {

    }

    // partialTicks = deltaTime ?
    @Override
    public void draw(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        ModEntry.getLogger().warn(String.valueOf(partialTick));
        partialTicks += partialTick;
        float percentage = MathUtils.clamp(partialTicks / time, 0.0f, 1.0f);

        ModEntry.getLogger().warn(String.valueOf(percentage));
        int lerpColor = FastColor.ARGB32.lerp(percentage, fromColor, toColor);

        graphics.fill(0, 0, screen.width, screen.height, lerpColor);
    }

    @Override
    public void mouseClicked(double mouseX, double mouseY, int button) { }

    @Override
    public void keyPressed(int keyCode, int scanCode, int modifiers) { }

    @Override
    public boolean isBlocking() { return false; }
}
