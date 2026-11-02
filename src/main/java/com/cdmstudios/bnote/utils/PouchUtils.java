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

    public static Tier getTier(String id) {
        if (id == null) return null;
        ConfigurationSection s = BankNotes.getInstance().getConfig().getConfigurationSection("pouches." + id.toLowerCase());
        if (s == null) return null;
        Material m = Material.matchMaterial(s.getString("material", "PLAYER_HEAD"));
        double min = Math.max(0, s.getDouble("min", 1));
        double max = Math.max(min, s.getDouble("max", min));
        return new Tier(id.toLowerCase(), s.getString("name", "&6Money Pouch"), m != null && m.isItem() ? m : Material.PAPER, min, max);
    }

    public static List<String> tierIds() {
        ConfigurationSection s = BankNotes.getInstance().getConfig().getConfigurationSection("pouches");
        return s == null ? List.of() : new ArrayList<>(s.getKeys(false));
    }

    public static ItemStack create(Tier t) {
        ItemStack item = new ItemStack(t.material());
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.colorize(t.name()));
        meta.setLore(Utils.colorizeAll(List.of(
                "&8&m                                ",
                "&7Contains: &a$" + NoteUtils.format(t.min()) + " &7- &a$" + NoteUtils.format(t.max()),
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
