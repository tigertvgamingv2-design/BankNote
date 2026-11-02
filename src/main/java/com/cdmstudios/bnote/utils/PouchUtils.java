package com.cdmstudios.bnote.utils;

import com.cdmstudios.bnote.BankNotes;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

// Money pouches: right-click to receive a random amount within the tier's range (config.yml "pouches")
public final class PouchUtils {

    public record Tier(String id, String name, Material material, double min, double max) {}

    private PouchUtils() {}

    private static NamespacedKey key(String name) {
        return new NamespacedKey(BankNotes.getInstance(), name);
    }

    private static final java.util.Map<String, Object[]> DEFAULTS = java.util.Map.of(
            "t1", new Object[]{"&f&lT1 Money Pouch", Material.LEATHER, 10.0, 100.0},
            "t2", new Object[]{"&a&lT2 Money Pouch", Material.RABBIT_HIDE, 500.0, 10000.0},
            "t3", new Object[]{"&b&lT3 Money Pouch", Material.HEART_OF_THE_SEA, 25000.0, 100000.0},
            "t4", new Object[]{"&d&lT4 Money Pouch", Material.NETHER_STAR, 200000.0, 1000000.0});

    public static Tier getTier(String id) {
        if (id == null) return null;
        String tid = id.toLowerCase();
        ConfigurationSection s = BankNotes.getInstance().getConfig().getConfigurationSection("pouches." + tid);
        Object[] d = DEFAULTS.get(tid);
        if (s == null && d == null) return null;

        String name = s != null ? s.getString("name") : null;
        if (name == null) name = d != null ? (String) d[0] : "&6Money Pouch";

        Material fallback = d != null ? (Material) d[1] : Material.PAPER;
        Material m = fallback;
        if (s != null && s.isString("material")) {
            Material parsed = Material.matchMaterial(s.getString("material"));
            if (parsed != null && parsed.isItem() && !parsed.isAir()) {
                m = parsed;
            } else {
                Log.warn("Pouch &e" + tid + " &7has invalid material '" + s.getString("material") + "', using " + fallback.name());
            }
        } else {
            Log.warn("Pouch &e" + tid + " &7has no material in config.yml, using " + fallback.name());
        }

        double defMin = d != null ? (Double) d[2] : 1;
        double defMax = d != null ? (Double) d[3] : defMin;
        double min = s != null && s.isSet("min") ? s.getDouble("min") : defMin;
        double max = s != null && s.isSet("max") ? s.getDouble("max") : defMax;
        if (s == null || !s.isSet("min") || !s.isSet("max")) {
            Log.warn("Pouch &e" + tid + " &7is missing min/max in config.yml, using " + min + " - " + max);
        }
        min = Math.max(0, min);
        max = Math.max(min, max);
        return new Tier(tid, name, m, min, max);
    }

    public static List<String> tierIds() {
        java.util.Set<String> ids = new java.util.TreeSet<>(DEFAULTS.keySet());
        ConfigurationSection s = BankNotes.getInstance().getConfig().getConfigurationSection("pouches");
        if (s != null) ids.addAll(s.getKeys(false));
        return new ArrayList<>(ids);
    }

    public static ItemStack create(Tier t) {
        ItemStack item = new ItemStack(t.material());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize(t.name()));
        meta.setLore(Utils.colorizeAll(List.of(
                "&8&m                                ",
                "&7Contains: &a&k$" + NoteUtils.format(t.min()) + " &7&k-&a&k $" + NoteUtils.format(t.max()),
                "&8&m                                ",
                "&6&oRight-click to open")));
        meta.setEnchantmentGlintOverride(true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(key("pouch"), PersistentDataType.STRING, t.id());
        pdc.set(key("id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        item.setItemMeta(meta);
        return item;
    }

    public static boolean isPouch(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key("pouch"), PersistentDataType.STRING);
    }

    public static String getTierId(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(key("pouch"), PersistentDataType.STRING);
    }

    public static String getId(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(key("id"), PersistentDataType.STRING);
    }

    public static double roll(Tier t) {
        double v = t.min() == t.max() ? t.min() : ThreadLocalRandom.current().nextDouble(t.min(), t.max());
        return Math.floor(v * 100) / 100;
    }
}
