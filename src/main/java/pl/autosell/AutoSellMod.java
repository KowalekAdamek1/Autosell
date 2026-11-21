package pl.autosell;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoSellMod implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("AutoSell");

    private AutoSellConfig config;
    private SellTask task;
    private boolean keyWasDown;

    @Override
    public void onInitializeClient() {
        config = AutoSellConfig.load();
        task = new SellTask(config);
        ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
    }

    private void onTick(Minecraft mc) {
        // GUI pod konfigurowalnym klawiszem (domyślnie Right Shift) - reaguje na naciśnięcie
        boolean down = InputConstants.isKeyDown(mc.getWindow().getWindow(), config.guiKey);
        if (down && !keyWasDown && mc.screen == null && mc.player != null) {
            mc.setScreen(new AutoSellScreen(config, task));
        }
        keyWasDown = down;

        task.tick(mc);
    }
}
