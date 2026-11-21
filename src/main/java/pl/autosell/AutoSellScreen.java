package pl.autosell;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class AutoSellScreen extends Screen {
    private final AutoSellConfig cfg;
    private final SellTask task;
    private Button toggleBtn, oreBtn, keyBtn, mineBtn, sellNowBtn;
    private boolean rebinding;

    public AutoSellScreen(AutoSellConfig cfg, SellTask task) {
        super(Component.literal("AutoSell"));
        this.cfg = cfg;
        this.task = task;
    }

    @Override
    protected void init() {
        int w = 200, h = 20, x = width / 2 - w / 2, y = height / 2 - 70, gap = 24;

        toggleBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            cfg.enabled = !cfg.enabled;
            if (cfg.enabled) task.resetTimer();
            cfg.save();
            refresh();
        }).bounds(x, y, w, h).build());

        oreBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            cfg.ore = cfg.ore().next().name();
            cfg.save();
            refresh();
        }).bounds(x, y + gap, w, h).build());

        keyBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            rebinding = true;
            refresh();
        }).bounds(x, y + gap * 2, w, h).build());

        mineBtn = addRenderableWidget(Button.builder(Component.empty(), b -> {
            cfg.autoMine = !cfg.autoMine;
            cfg.save();
            refresh();
        }).bounds(x, y + gap * 3, w, h).build());

        sellNowBtn = addRenderableWidget(Button.builder(Component.literal("Sprzedaj teraz"), b -> {
            task.sellNow();
            onClose();
        }).bounds(x, y + gap * 4, w, h).build());

        addRenderableWidget(Button.builder(Component.literal("Zamknij"), b -> onClose())
                .bounds(x, y + gap * 5, w, h).build());

        refresh();
    }

    private void refresh() {
        toggleBtn.setMessage(Component.literal("AutoSell: " + (cfg.enabled ? "§aWŁĄCZONY" : "§cWYŁĄCZONY")));
        oreBtn.setMessage(Component.literal("Ruda: §e" + cfg.ore().label + " §7(kliknij, aby zmienić)"));
        keyBtn.setMessage(rebinding
                ? Component.literal("> naciśnij klawisz... (Esc = anuluj) <")
                : Component.literal("Klawisz GUI: §b").append(
                        InputConstants.Type.KEYSYM.getOrCreate(cfg.guiKey).getDisplayName()));
        mineBtn.setMessage(Component.literal("Auto-kopanie (Baritone): "
                + (cfg.autoMine ? "§aTAK" : "§cNIE")
                + (BaritoneBridge.isAvailable() ? "" : " §7(brak Baritone)")));
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rebinding) {
            if (keyCode != GLFW.GLFW_KEY_ESCAPE) {
                cfg.guiKey = keyCode;
                cfg.save();
            }
            rebinding = false;
            refresh();
            return true;
        }
        if (keyCode == cfg.guiKey) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        int cx = width / 2;
        int top = height / 2 - 100;
        g.drawCenteredString(font, Component.literal("§lAutoSell §7- anarchia.gg"), cx, top, 0xFFFFFF);

        String status;
        if (!cfg.enabled) status = "Wyłączony";
        else if (task.busy()) status = "Sprzedaję...";
        else status = "Następna sprzedaż za " + task.secondsToNext() + "s (co " + cfg.intervalSeconds + "s)";
        g.drawCenteredString(font, Component.literal(status), cx, top + 14, 0xAAAAAA);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
