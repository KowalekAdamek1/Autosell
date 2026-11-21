package pl.autosell;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;
import java.util.Locale;

/** Maszyna stanów: /sklep -> klik w rudę / "sprzedaj wszystko" -> zamknięcie GUI. */
public class SellTask {
    private enum State { IDLE, WAIT_OPEN, SETTLE, ACT, WAIT_RESULT }

    private static final int MAX_STEPS = 4;
    private static final int OPEN_TIMEOUT_TICKS = 100;
    private static final int SETTLE_TICKS = 6;
    private static final int RESULT_TICKS = 8;

    private final AutoSellConfig cfg;
    private State state = State.IDLE;
    private int ticks;
    private int steps;
    private int lastContainerId = -1;
    private long nextSellAt;
    private boolean pausedBaritone;
    private long nextMineCheck;
    /** Ręczne "Sprzedaj teraz" z GUI - działa nawet gdy AutoSell jest wyłączony. */
    private boolean manual;
    /** Czy wypisywać komunikaty na czacie (ręczna sprzedaż lub debug). */
    private boolean verbose;

    public SellTask(AutoSellConfig cfg) {
        this.cfg = cfg;
        resetTimer();
    }

    public void resetTimer() {
        nextSellAt = System.currentTimeMillis() + cfg.intervalSeconds * 1000L;
    }

    public long secondsToNext() {
        return Math.max(0, (nextSellAt - System.currentTimeMillis()) / 1000L);
    }

    public boolean busy() {
        return state != State.IDLE;
    }

    /** Ręczne wywołanie z GUI. */
    public void sellNow() {
        manual = true;
        nextSellAt = 0;
    }

    private void say(Minecraft mc, String text) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§7[AutoSell] §f" + text), false);
        }
    }

    private boolean serverOk(Minecraft mc) {
        if (mc.player == null) return false;
        if (!cfg.onlyOnAnarchia) return true;
        ServerData sd = mc.getCurrentServer();
        return sd != null && sd.ip != null
                && sd.ip.toLowerCase(Locale.ROOT).contains("anarchia.gg");
    }

    private boolean hasOre(Player p) {
        Ore ore = cfg.ore();
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            if (ore.matches(p.getInventory().getItem(i))) return true;
        }
        return false;
    }

    public void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            state = State.IDLE;
            return;
        }
        boolean active = cfg.enabled || manual || state != State.IDLE;
        if (!active) return;

        if (!serverOk(mc)) {
            if (manual) {
                ServerData sd = mc.getCurrentServer();
                say(mc, "Nie jesteś na anarchia.gg (adres: " + (sd == null ? "brak - singleplayer" : sd.ip)
                        + "). Wyłącz w GUI opcję 'Tylko anarchia.gg'.");
                manual = false;
            }
            if (state != State.IDLE) abort(mc);
            return;
        }

        switch (state) {
            case IDLE -> idle(mc);
            case WAIT_OPEN -> {
                ticks++;
                if (isShop(mc)) {
                    state = State.SETTLE;
                    ticks = 0;
                } else if (ticks > OPEN_TIMEOUT_TICKS) {
                    if (verbose) say(mc, "Menu /sklep się nie otworzyło (serwer nie odpowiedział na komendę).");
                    abort(mc);
                }
            }
            case SETTLE -> {
                if (!isShop(mc)) { abort(mc); return; }
                if (++ticks >= SETTLE_TICKS) {
                    state = State.ACT;
                    ticks = 0;
                    if (cfg.debug) dump(mc);
                }
            }
            case ACT -> act(mc);
            case WAIT_RESULT -> {
                if (++ticks < RESULT_TICKS) return;
                boolean open = isShop(mc);
                int id = open ? mc.player.containerMenu.containerId : -1;
                if (open && id != lastContainerId && steps < MAX_STEPS) {
                    // otworzyło się kolejne menu (np. kategoria) - szukamy dalej
                    state = State.SETTLE;
                    ticks = 0;
                } else {
                    if (verbose) say(mc, "Gotowe (kliknięć: " + steps + ").");
                    finish(mc);
                }
            }
        }
    }

    private void idle(Minecraft mc) {
        long now = System.currentTimeMillis();

        // Pełna automatyka: Baritone sam kopie wybraną rudę
        if (cfg.autoMine && cfg.enabled && now >= nextMineCheck && mc.screen == null && BaritoneBridge.isAvailable()) {
            nextMineCheck = now + 10_000L;
            if (!BaritoneBridge.isMining()) {
                BaritoneBridge.run("mine " + cfg.ore().mineBlocks());
            }
        }

        if (now < nextSellAt) return;
        if (mc.screen != null) return; // nie przeszkadzamy, spróbujemy za chwilę

        verbose = cfg.debug || manual;
        manual = false;
        resetTimer();
        if (!hasOre(mc.player)) {
            if (verbose) say(mc, "Nie masz w ekwipunku: " + cfg.ore().label + " (szukam: " + cfg.ore().items + ").");
            return;
        }

        if (cfg.pauseBaritoneWhileSelling && BaritoneBridge.isAvailable()) {
            BaritoneBridge.run("pause");
            pausedBaritone = true;
        }
        if (verbose) say(mc, "Wpisuję /sklep...");
        mc.player.connection.sendCommand("sklep");
        state = State.WAIT_OPEN;
        ticks = 0;
        steps = 0;
        lastContainerId = -1;
    }

    private boolean isShop(Minecraft mc) {
        return mc.screen instanceof AbstractContainerScreen<?>
                && mc.player.containerMenu != null
                && mc.player.containerMenu.containerId != 0;
    }

    private void act(Minecraft mc) {
        if (!isShop(mc)) { finish(mc); return; }
        AbstractContainerMenu menu = mc.player.containerMenu;
        int shopSlots = Math.max(0, menu.slots.size() - 36); // reszta to ekwipunek gracza

        int sellAll = -1;
        int oreSlot = -1;
        int orePrio = Integer.MAX_VALUE;
        int category = -1;
        Ore ore = cfg.ore();

        for (int i = 0; i < shopSlots; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack st = slot.getItem();
            if (st.isEmpty()) continue;
            String name = st.getHoverName().getString().toLowerCase(Locale.ROOT);
            String lore = tooltip(mc, st);

            boolean isBuy = (name.contains("kup") || lore.contains("kupno")) && !(name.contains("sprzed") || lore.contains("sprzed"));
            if (isBuy) continue;

            if (name.contains("sprzedaj") && (name.contains("wszystko") || name.contains("all"))) {
                sellAll = i;
            }
            int p = ore.priority(st);
            if (p >= 0) {
                int score = p * 2 - (lore.contains("sprzed") ? 1 : 0);
                if (score < orePrio) { orePrio = score; oreSlot = i; }
            } else if (category < 0 && steps == 0
                    && (name.contains("rud") || name.contains("surowc") || name.contains("minera"))) {
                category = i;
            }
        }

        int target = sellAll >= 0 ? sellAll : oreSlot >= 0 ? oreSlot : category;
        if (target < 0) {
            if (verbose) say(mc, "Nie znalazłem w tym menu rudy ani przycisku sprzedaży. Włącz Debug w GUI i wyślij mi listę slotów.");
            finish(mc);
            return;
        }

        if (verbose) {
            say(mc, "Klikam slot #" + target + ": " + menu.slots.get(target).getItem().getHoverName().getString());
        }
        lastContainerId = menu.containerId;
        mc.gameMode.handleInventoryMouseClick(menu.containerId, target, 0, ClickType.PICKUP, mc.player);
        steps++;
        state = State.WAIT_RESULT;
        ticks = 0;
    }

    private String tooltip(Minecraft mc, ItemStack st) {
        try {
            List<Component> lines = st.getTooltipLines(Item.TooltipContext.of(mc.level), mc.player, TooltipFlag.NORMAL);
            StringBuilder sb = new StringBuilder();
            for (Component c : lines) sb.append(c.getString().toLowerCase(Locale.ROOT)).append(' ');
            return sb.toString();
        } catch (Throwable t) {
            return "";
        }
    }

    private void dump(Minecraft mc) {
        AbstractContainerMenu menu = mc.player.containerMenu;
        int shopSlots = Math.max(0, menu.slots.size() - 36);
        say(mc, "Menu: " + shopSlots + " slotów, tytuł: " + mc.screen.getTitle().getString());
        for (int i = 0; i < shopSlots; i++) {
            ItemStack st = menu.slots.get(i).getItem();
            if (st.isEmpty()) continue;
            say(mc, "#" + i + " " + st.getItem() + " | " + st.getHoverName().getString());
        }
    }

    private void finish(Minecraft mc) {
        if (mc.player != null && isShop(mc)) mc.player.closeContainer();
        cleanup();
    }

    private void abort(Minecraft mc) {
        if (mc.player != null && isShop(mc)) mc.player.closeContainer();
        cleanup();
    }

    private void cleanup() {
        state = State.IDLE;
        ticks = 0;
        if (pausedBaritone) {
            BaritoneBridge.run("resume");
            pausedBaritone = false;
        }
    }
}
