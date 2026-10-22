package net.thepir4te.playtime;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2fStack;

import java.util.function.Consumer;

@Environment(EnvType.CLIENT)
public class PlaytimeConfigScreen extends Screen {
    private final PlaytimeConfig config;
    private EditBox scaleInput;
    private EditBox hexSession;
    private EditBox hexWorld;
    private EditBox hexOverall;
    private boolean isDraggingHUD = false;
    private double dragOffsetX = 0.0;
    private double dragOffsetY = 0.0;

    public PlaytimeConfigScreen(PlaytimeConfig config) {
        super(Component.translatable("gui.playtime.editor"));
        this.config = config;
    }

    @Override
    protected void init() {
        int x = 20;
        int y = 40;

        this.addRenderableWidget(Button.builder(this.getToggleText("Session", this.config.showSession), (b) -> {
            this.config.showSession = !this.config.showSession;
            b.setMessage(this.getToggleText("Session", this.config.showSession));
        }).bounds(x, y, 100, 20).build());
        this.hexSession = this.createHexBox(x + 105, y, this.config.colorSession, (c) -> this.config.colorSession = c);

        this.addRenderableWidget(Button.builder(this.getToggleText("World", this.config.showWorld), (b) -> {
            this.config.showWorld = !this.config.showWorld;
            b.setMessage(this.getToggleText("World", this.config.showWorld));
        }).bounds(x, y + 25, 100, 20).build());
        this.hexWorld = this.createHexBox(x + 105, y + 25, this.config.colorWorld, (c) -> this.config.colorWorld = c);

        this.addRenderableWidget(Button.builder(this.getToggleText("Overall", this.config.showOverall), (b) -> {
            this.config.showOverall = !this.config.showOverall;
            b.setMessage(this.getToggleText("Overall", this.config.showOverall));
        }).bounds(x, y + 50, 100, 20).build());
        this.hexOverall = this.createHexBox(x + 105, y + 50, this.config.colorOverall, (c) -> this.config.colorOverall = c);

        this.scaleInput = new EditBox(this.font, x + 40, y + 75, 45, 20, Component.literal("Size"));
        this.scaleInput.setValue(String.valueOf(this.config.scale));
        this.scaleInput.setResponder((s) -> {
            try {
                float v = Float.parseFloat(s);
                if (v > 0.1F) {
                    this.config.scale = v;
                }
            } catch (Exception ignored) {}
        });
        this.addRenderableWidget(this.scaleInput);
    }

    private EditBox createHexBox(int x, int y, int initial, Consumer<Integer> updater) {
        EditBox box = new EditBox(this.font, x, y, 60, 20, Component.literal("Hex"));
        box.setValue(String.format("%06X", initial & 0xFFFFFF));
        box.setMaxLength(6);
        box.setTextColor(0xFFFFFFFF);
        box.setResponder((s) -> {
            try {
                updater.accept(Integer.parseInt(s, 16));
            } catch (Exception ignored) {}
        });
        this.addRenderableWidget(box);
        return box;
    }

    private Component getToggleText(String name, boolean active) {
        return Component.literal((active ? "§a✔ " : "§c✘ ") + name);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        this.renderHUDPreview(graphics);
        graphics.text(this.font, Component.translatable("gui.playtime.editor.title"), 20, 15, 0xFFFFFFFF, true);
        graphics.text(this.font, Component.translatable("gui.playtime.size"), 20, 121, 0xFFAAAAAA, true);
    }

    private void renderHUDPreview(GuiGraphicsExtractor g) {
        float scale = this.config.scale;
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate((float) this.config.hudX, (float) this.config.hudY);
        pose.scale(scale, scale);
        int y = 0;
        if (this.config.showSession) {
            g.text(this.font, "Session: " + PlaytimeManager.format(PlaytimeManager.sessionSeconds), 0, y, this.config.colorSession | 0xFF000000, true);
            y += 10;
        }
        if (this.config.showWorld) {
            g.text(this.font, "World: " + PlaytimeManager.format(PlaytimeManager.worldSeconds), 0, y, this.config.colorWorld | 0xFF000000, true);
            y += 10;
        }
        if (this.config.showOverall) {
            g.text(this.font, "Overall: " + PlaytimeManager.format(PlaytimeManager.getEffectiveOverallSeconds()), 0, y, this.config.colorOverall | 0xFF000000, true);
        }
        pose.popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int w = (int) (100.0F * this.config.scale);
        int h = (int) (30.0F * this.config.scale);
        if (mouseX >= (double)this.config.hudX && mouseX <= (double) (this.config.hudX + w)
            && mouseY >= (double)this.config.hudY && mouseY <= (double) (this.config.hudY + h)) {
            this.isDraggingHUD = true;
            this.dragOffsetX = mouseX - (double) this.config.hudX;
            this.dragOffsetY = mouseY - (double) this.config.hudY;
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (this.isDraggingHUD) {
            this.config.hudX = (int) (event.x() - this.dragOffsetX);
            this.config.hudY = (int) (event.y() - this.dragOffsetY);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.isDraggingHUD = false;
        this.config.save();
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.config.save();
        super.onClose();
    }
}
