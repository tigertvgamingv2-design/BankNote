package com.cdmstudios.bnote.utils;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.managers.TemplateManager;
import com.cdmstudios.bnote.managers.TemplateManager.Template;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class NoteUtils {

    private static final DecimalFormat FORMAT = new DecimalFormat("#,##0.##");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(BankNotes.getInstance(), name);
    }

    public static String format(double amount) {
        return FORMAT.format(amount);
    }

    public static ItemStack createNote(String ownerName, UUID ownerId, double amount) {
        return createNote(TemplateManager.DEFAULT_ID, ownerName, ownerId, amount);
    }

    public static ItemStack createNote(String templateId, String ownerName, UUID ownerId, double amount) {
        Template t = BankNotes.getInstance().getTemplates().getOrDefault(templateId);
        return render(t, ownerName, ownerId, amount, false);
    }

    // preview items carry no redeem data, so they can never be redeemed
    public static ItemStack render(Template t, String ownerName, UUID ownerId, double amount, boolean preview) {
        ItemStack item = new ItemStack(t.material);
        ItemMeta meta = item.getItemMeta();

        meta.setDisplayName(Utils.colorize(apply(t.name, ownerName, amount)));
        List<String> lore = new ArrayList<>();
        for (String line : t.lore) {
            if (ownerName == null && line.contains("{player}")) continue;
            lore.add(apply(line, ownerName, amount));
        }
        meta.setLore(Utils.colorizeAll(lore));
        meta.setEnchantmentGlintOverride(t.glow);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

        if (!preview) {
            PersistentDataContainer pdc = meta.getPersistentDataContainer();
            pdc.set(key("amount"), PersistentDataType.DOUBLE, amount);
            if (ownerId != null) pdc.set(key("owner"), PersistentDataType.STRING, ownerId.toString());
            if (ownerName != null) pdc.set(key("owner_name"), PersistentDataType.STRING, ownerName);
            pdc.set(key("template"), PersistentDataType.STRING, t.id);
            pdc.set(key("id"), PersistentDataType.STRING, UUID.randomUUID().toString());
        }

        item.setItemMeta(meta);
        return item;
    }

    private static String apply(String s, String ownerName, double amount) {
        return s.replace("{amount}", format(amount))
                .replace("{player}", ownerName == null ? "" : ownerName)
                .replace("{date}", java.time.LocalDate.now().toString());
    }

    public static boolean isNote(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(key("amount"), PersistentDataType.DOUBLE);
    }

    public static String getId(ItemStack item) {
        return item.getItemMeta().getPersistentDataContainer().get(key("id"), PersistentDataType.STRING);
    }

    public static double getAmount(ItemStack item) {
        Double v = item.getItemMeta().getPersistentDataContainer().get(key("amount"), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }
}
