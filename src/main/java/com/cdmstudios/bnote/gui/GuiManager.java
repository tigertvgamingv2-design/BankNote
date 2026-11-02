package com.cdmstudios.bnote.gui;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.managers.TemplateManager;
import com.cdmstudios.bnote.managers.TemplateManager.Template;
import com.cdmstudios.bnote.utils.Fx;
import com.cdmstudios.bnote.utils.Log;
import com.cdmstudios.bnote.utils.NoteUtils;
import com.cdmstudios.bnote.utils.Utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiManager implements Listener {

    private enum Type { MAIN, EDIT, BALANCE }

    private enum Action { CREATE, NAME, LORE_ADD, MATERIAL }

    private record Prompt(Action action, String templateId) {}

    private static class Holder implements InventoryHolder {
        final Type type;
        final String templateId;
        final List<String> ids = new ArrayList<>();
        Inventory inventory;

        Holder(Type type, String templateId) {
            this.type = type;
            this.templateId = templateId;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private static final int MAIN_BALANCE_SLOT = 53;
    private static final int BALANCE_SLOT = 13;

    private final BankNotes plugin;
    private final Map<UUID, Prompt> prompts = new ConcurrentHashMap<>();

    public GuiManager(BankNotes plugin) {
        this.plugin = plugin;
        // keeps balance displays live while a menu is open
        Bukkit.getScheduler().runTaskTimer(plugin, this::refreshBalances, 20L, 20L);
    }

    private TemplateManager templates() {
        return plugin.getTemplates();
    }

    private String balance(Player p) {
        return NoteUtils.format(plugin.getEconomy().getBalance(p));
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.setDisplayName(Utils.colorize(name));
        List<String> l = new ArrayList<>();
        for (String s : lore) l.add(Utils.colorize(s));
        meta.setLore(l);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack balanceItem(Player p) {
        return item(Material.GOLD_INGOT, "&6&lYour Balance", "&a$" + balance(p), "", "&7Updates in real time");
    }

    private void refreshBalances() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Inventory top = p.getOpenInventory().getTopInventory();
            if (!(top.getHolder() instanceof Holder h)) continue;
            if (h.type == Type.MAIN) top.setItem(MAIN_BALANCE_SLOT, balanceItem(p));
            else if (h.type == Type.BALANCE) top.setItem(BALANCE_SLOT, balanceItem(p));
        }
    }

    public void openBalance(Player p) {
        Holder h = new Holder(Type.BALANCE, null);
        Inventory inv = Bukkit.createInventory(h, 27, Utils.colorize("&6&lBalance"));
        h.inventory = inv;
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);
        inv.setItem(BALANCE_SLOT, balanceItem(p));
        inv.setItem(11, item(Material.PAPER, "&e&lWithdraw", "&7Use &f/withdraw <amount>", "&7to turn money into a note"));
        inv.setItem(15, item(Material.BARRIER, "&c&lClose"));
        p.openInventory(inv);
    }

    public void openMain(Player p) {
        Holder h = new Holder(Type.MAIN, null);
        Inventory inv = Bukkit.createInventory(h, 54, Utils.colorize("&6&lBank Notes &8- &7Editor"));
        h.inventory = inv;

        int slot = 0;
        for (Template t : templates().all()) {
            if (slot >= 45) break;
            h.ids.add(t.id);
            ItemStack it = NoteUtils.render(t, "Preview", null, 100, true);
            ItemMeta meta = it.getItemMeta();
            List<String> lore = new ArrayList<>(meta.getLore() == null ? List.of() : meta.getLore());
            lore.add(Utils.colorize("&8&m                    "));
            lore.add(Utils.colorize("&7ID: &f" + t.id));
            lore.add(Utils.colorize("&eClick to edit"));
            meta.setLore(lore);
            it.setItemMeta(meta);
            inv.setItem(slot++, it);
        }

        ItemStack pane = item(Material.BLACK_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, pane);
        inv.setItem(49, item(Material.EMERALD, "&a&lCreate Banknote", "&7Click, then type a new ID in chat"));
        inv.setItem(MAIN_BALANCE_SLOT, balanceItem(p));
        p.openInventory(inv);
    }

    public void openEdit(Player p, String id) {
        Template t = templates().get(id);
        if (t == null) {
            openMain(p);
            return;
        }
        Holder h = new Holder(Type.EDIT, id);
        Inventory inv = Bukkit.createInventory(h, 27, Utils.colorize("&6&lEditing &8- &e" + id));
        h.inventory = inv;

        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < 27; i++) inv.setItem(i, pane);

        inv.setItem(4, NoteUtils.render(t, p.getName(), p.getUniqueId(), 100, true));
        inv.setItem(10, item(Material.NAME_TAG, "&e&lName", "&7Current: &f" + t.name, "", "&eClick to change", "&8Use {amount} {player} {date}"));
        inv.setItem(11, item(Material.WRITABLE_BOOK, "&e&lLore", "&7Lines: &f" + t.lore.size(), "",
                "&eLeft-click: &7add a line", "&eRight-click: &7remove last line"));
        inv.setItem(12, item(t.material, "&e&lMaterial", "&7Current: &f" + t.material.name(), "",
                "&eClick, then type a material", "&8or type 'hand' to use your held item"));
        inv.setItem(13, item(t.glow ? Material.GLOWSTONE_DUST : Material.GUNPOWDER, "&e&lGlow",
                t.glow ? "&aEnabled" : "&cDisabled", "", "&eClick to toggle"));
        inv.setItem(15, item(Material.GOLD_NUGGET, "&a&lGive Test Note", "&7Gives you a real &a$1 &7note"));
        inv.setItem(22, item(Material.ARROW, "&7&lBack"));
        if (!TemplateManager.DEFAULT_ID.equals(id)) {
            inv.setItem(26, item(Material.TNT, "&c&lDelete", "&7Shift-click to delete"));
        }
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (!(top.getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (e.getClickedInventory() != top || !(e.getWhoClicked() instanceof Player p)) return;
        if (!p.hasPermission("banknotes.admin") && h.type != Type.BALANCE) {
            p.closeInventory();
            return;
        }

        int slot = e.getSlot();
        switch (h.type) {
            case BALANCE -> {
                if (slot == 15) p.closeInventory();
            }
            case MAIN -> {
                if (slot < h.ids.size()) {
                    openEdit(p, h.ids.get(slot));
                } else if (slot == 49) {
                    prompt(p, Action.CREATE, null, "&aType a new banknote ID (letters, numbers, - or _)");
                }
            }
            case EDIT -> handleEdit(p, h, slot, e);
        }
    }

    private void handleEdit(Player p, Holder h, int slot, InventoryClickEvent e) {
        Template t = templates().get(h.templateId);
        if (t == null) {
            openMain(p);
            return;
        }
        switch (slot) {
            case 10 -> prompt(p, Action.NAME, t.id, "&aType the new display name (supports & colors, {amount}, {player}, {date})");
            case 11 -> {
                if (e.isRightClick()) {
                    if (!t.lore.isEmpty()) t.lore.remove(t.lore.size() - 1);
                    templates().save();
                    openEdit(p, t.id);
                } else {
                    prompt(p, Action.LORE_ADD, t.id, "&aType the lore line to add");
                }
            }
            case 12 -> prompt(p, Action.MATERIAL, t.id, "&aType a material name (e.g. PAPER, MAP) or 'hand'");
            case 13 -> {
                t.glow = !t.glow;
                templates().save();
                openEdit(p, t.id);
            }
            case 15 -> {
                ItemStack note = NoteUtils.createNote(t.id, null, null, 1);
                p.getInventory().addItem(note).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
                Log.info("&e" + p.getName() + " &7took a test note from template &e" + t.id);
            }
            case 22 -> openMain(p);
            case 26 -> {
                if (e.isShiftClick() && templates().delete(t.id)) {
                    Log.warn("&e" + p.getName() + " &7deleted banknote template &e" + t.id);
                    p.sendMessage(Utils.colorize("&cDeleted banknote &f" + t.id + "&c."));
                    openMain(p);
                }
            }
            default -> { }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    private void prompt(Player p, Action action, String id, String message) {
        p.closeInventory();
        prompts.put(p.getUniqueId(), new Prompt(action, id));
        p.sendMessage(Utils.colorize(message));
        p.sendMessage(Utils.colorize("&7Type &ccancel &7to abort."));
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Prompt prompt = prompts.remove(e.getPlayer().getUniqueId());
        if (prompt == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        String msg = e.getMessage();
        Bukkit.getScheduler().runTask(plugin, () -> handlePrompt(p, prompt, msg));
    }

    private void handlePrompt(Player p, Prompt prompt, String msg) {
        if (msg.equalsIgnoreCase("cancel")) {
            reopen(p, prompt);
            return;
        }
        switch (prompt.action()) {
            case CREATE -> {
                String id = msg.toLowerCase();
                if (!id.matches("[a-z0-9_-]{1,24}")) {
                    error(p, "Invalid ID. Use 1-24 letters, numbers, - or _.");
                    openMain(p);
                } else if (templates().get(id) != null) {
                    error(p, "A banknote with that ID already exists.");
                    openMain(p);
                } else {
                    templates().create(id);
                    Log.success("&e" + p.getName() + " &7created banknote template &e" + id);
                    openEdit(p, id);
                }
            }
            case NAME -> edit(p, prompt, t -> t.name = msg);
            case LORE_ADD -> edit(p, prompt, t -> t.lore.add(msg));
            case MATERIAL -> {
                Material m = msg.equalsIgnoreCase("hand")
                        ? p.getInventory().getItemInMainHand().getType()
                        : Material.matchMaterial(msg);
                if (m == null || m.isAir() || !m.isItem()) {
                    error(p, "That's not a valid item material.");
                    reopen(p, prompt);
                } else {
                    edit(p, prompt, t -> t.material = m);
                }
            }
        }
    }

    private void edit(Player p, Prompt prompt, Consumer<Template> change) {
        Template t = templates().get(prompt.templateId());
        if (t != null) {
            change.accept(t);
            templates().save();
            Log.info("&e" + p.getName() + " &7edited banknote template &e" + t.id);
        }
        reopen(p, prompt);
    }

    private void reopen(Player p, Prompt prompt) {
        if (prompt.templateId() == null) openMain(p);
        else openEdit(p, prompt.templateId());
    }

    private void error(Player p, String msg) {
        p.sendMessage(Utils.colorize("&c" + msg));
        Fx.play(p, "error", "");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        prompts.remove(e.getPlayer().getUniqueId());
    }
}
