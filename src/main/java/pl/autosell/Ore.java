package pl.autosell;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/** Rudy dostępne w GUI. Pierwszy przedmiot z listy ma najwyższy priorytet przy szukaniu w sklepie. */
public enum Ore {
    COPPER("Miedź", Items.COPPER_INGOT, Items.RAW_COPPER, Items.COPPER_ORE, Items.DEEPSLATE_COPPER_ORE, Items.COPPER_BLOCK),
    IRON("Żelazo", Items.IRON_INGOT, Items.RAW_IRON, Items.IRON_ORE, Items.DEEPSLATE_IRON_ORE, Items.IRON_BLOCK),
    GOLD("Złoto", Items.GOLD_INGOT, Items.RAW_GOLD, Items.GOLD_ORE, Items.DEEPSLATE_GOLD_ORE, Items.GOLD_BLOCK),
    DIAMOND("Diament", Items.DIAMOND, Items.DIAMOND_ORE, Items.DEEPSLATE_DIAMOND_ORE, Items.DIAMOND_BLOCK),
    EMERALD("Szmaragd", Items.EMERALD, Items.EMERALD_ORE, Items.DEEPSLATE_EMERALD_ORE, Items.EMERALD_BLOCK),
    LAPIS("Lapis", Items.LAPIS_LAZULI, Items.LAPIS_ORE, Items.DEEPSLATE_LAPIS_ORE, Items.LAPIS_BLOCK),
    REDSTONE("Redstone", Items.REDSTONE, Items.REDSTONE_ORE, Items.DEEPSLATE_REDSTONE_ORE, Items.REDSTONE_BLOCK),
    COAL("Węgiel", Items.COAL, Items.COAL_ORE, Items.DEEPSLATE_COAL_ORE, Items.COAL_BLOCK),
    QUARTZ("Kwarc", Items.QUARTZ, Items.NETHER_QUARTZ_ORE, Items.QUARTZ_BLOCK),
    NETHERITE("Netheryt", Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS, Items.NETHERITE_INGOT, Items.NETHERITE_BLOCK);

    public final String label;
    public final List<Item> items;

    Ore(String label, Item... items) {
        this.label = label;
        this.items = List.of(items);
    }

    public boolean matches(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Item item : items) {
            if (stack.is(item)) return true;
        }
        return false;
    }

    /** Priorytet: 0 = najlepszy. -1 = brak dopasowania. */
    public int priority(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        for (int i = 0; i < items.size(); i++) {
            if (stack.is(items.get(i))) return i;
        }
        return -1;
    }

    /** Bloki przekazywane do komendy Baritone "mine". */
    public String mineBlocks() {
        return switch (this) {
            case COPPER -> "copper_ore deepslate_copper_ore";
            case IRON -> "iron_ore deepslate_iron_ore";
            case GOLD -> "gold_ore deepslate_gold_ore";
            case DIAMOND -> "diamond_ore deepslate_diamond_ore";
            case EMERALD -> "emerald_ore deepslate_emerald_ore";
            case LAPIS -> "lapis_ore deepslate_lapis_ore";
            case REDSTONE -> "redstone_ore deepslate_redstone_ore";
            case COAL -> "coal_ore deepslate_coal_ore";
            case QUARTZ -> "nether_quartz_ore";
            case NETHERITE -> "ancient_debris";
        };
    }

    public Component text() {
        return Component.literal(label);
    }

    public static Ore byName(String name) {
        for (Ore o : values()) {
            if (o.name().equalsIgnoreCase(name)) return o;
        }
        return COPPER;
    }

    public Ore next() {
        Ore[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
