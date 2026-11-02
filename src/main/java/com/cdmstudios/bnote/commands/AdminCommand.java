package com.cdmstudios.bnote.commands;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.managers.TemplateManager;
import com.cdmstudios.bnote.utils.Log;
import com.cdmstudios.bnote.utils.NoteUtils;
import com.cdmstudios.bnote.utils.PouchUtils;
import com.cdmstudios.bnote.utils.Utils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class AdminCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "banknotes.admin";

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(PERM)) {
            sender.sendMessage(Utils.colorize("&cYou don't have permission."));
            return true;
        }
        if (args.length == 0) {
            help(sender, label);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> {
                // /bnadmin create <amount> [player] - note with no issuer name
                if (args.length < 2) {
                    help(sender, label);
                    return true;
                }
                Double amount = parseAmount(sender, args[1]);
                if (amount == null) return true;
                Player target = resolveTarget(sender, args.length > 2 ? args[2] : null);
                if (target == null) return true;
                String templateId = args.length > 3 ? args[3].toLowerCase() : TemplateManager.DEFAULT_ID;
                if (BankNotes.getInstance().getTemplates().get(templateId) == null) {
                    sender.sendMessage(Utils.colorize("&cUnknown banknote template: &f" + templateId));
                    return true;
                }
                give(sender, target, NoteUtils.createNote(templateId, null, null, amount), amount, "nameless");
            }
            case "pouch" -> {
                // /bnadmin pouch <tier> [player] [count]
                if (args.length < 2) {
                    help(sender, label);
                    return true;
                }
                PouchUtils.Tier tier = PouchUtils.getTier(args[1]);
                if (tier == null) {
                    sender.sendMessage(Utils.colorize("&cUnknown tier. Available: &f" + String.join(", ", PouchUtils.tierIds())));
                    return true;
                }
                Player target = resolveTarget(sender, args.length > 2 ? args[2] : null);
                if (target == null) return true;
                int count = 1;
                if (args.length > 3) {
                    try {
                        count = Math.max(1, Math.min(64, Integer.parseInt(args[3])));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(Utils.colorize("&cInvalid count."));
                        return true;
                    }
                }
                for (int i = 0; i < count; i++) {
                    target.getInventory().addItem(PouchUtils.create(tier)).values()
                            .forEach(it -> target.getWorld().dropItemNaturally(target.getLocation(), it));
                }
                sender.sendMessage(Utils.colorize("&aGave &f" + target.getName() + " &f" + count + "x " + tier.name() + "&a."));
                if (sender != target) target.sendMessage(Utils.colorize("&aYou received " + count + "x " + tier.name() + "&a."));
                Log.info("&e" + sender.getName() + " &7gave &e" + count + "x " + tier.id().toUpperCase() + " &7pouch to &e" + target.getName());
            }
            case "editor" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return true;
                }
                BankNotes.getInstance().getGui().openMain(p);
            }
            case "give" -> {
                // /bnadmin give <player> <amount> - note issued under the target's name
                if (args.length < 3) {
                    help(sender, label);
                    return true;
                }
                Player target = resolveTarget(sender, args[1]);
                if (target == null) return true;
                Double amount = parseAmount(sender, args[2]);
                if (amount == null) return true;
                give(sender, target, NoteUtils.createNote(target.getName(), target.getUniqueId(), amount), amount, "named");
            }
            case "check" -> {
                if (!(sender instanceof Player p)) {
                    sender.sendMessage("Only players can use this.");
                    return true;
                }
                ItemStack item = p.getInventory().getItemInMainHand();
                if (!NoteUtils.isNote(item)) {
                    p.sendMessage(Utils.colorize("&cHold a bank note in your main hand."));
                } else {
                    p.sendMessage(Utils.colorize("&6Bank note worth &a$" + NoteUtils.format(NoteUtils.getAmount(item))
                            + " &7x" + item.getAmount()));
                }
            }
            case "reload" -> {
                BankNotes.getInstance().reloadConfig();
                sender.sendMessage(Utils.colorize("&aConfig reloaded."));
                Log.info("Config reloaded by &e" + sender.getName());
            }
            default -> help(sender, label);
        }
        return true;
    }

    private void give(CommandSender sender, Player target, ItemStack note, double amount, String kind) {
        target.getInventory().addItem(note).values()
                .forEach(i -> target.getWorld().dropItemNaturally(target.getLocation(), i));
        String amt = NoteUtils.format(amount);
        sender.sendMessage(Utils.colorize("&aGave &f" + target.getName() + " &aa " + kind + " bank note worth &f$" + amt + "&a."));
        if (sender != target) {
            target.sendMessage(Utils.colorize("&aYou received a bank note worth &f$" + amt + "&a."));
        }
        Log.info("&e" + sender.getName() + " &7created a " + kind + " &a$" + amt + " &7note for &e" + target.getName());
    }

    private Double parseAmount(CommandSender sender, String s) {
        try {
            double v = Double.parseDouble(s);
            if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0) throw new NumberFormatException();
            return Math.floor(v * 100) / 100;
        } catch (NumberFormatException e) {
            sender.sendMessage(Utils.colorize("&cInvalid amount."));
            return null;
        }
    }

    private Player resolveTarget(CommandSender sender, String name) {
        if (name == null) {
            if (sender instanceof Player p) return p;
            sender.sendMessage("Specify a player.");
            return null;
        }
        Player p = Bukkit.getPlayerExact(name);
        if (p == null) sender.sendMessage(Utils.colorize("&cPlayer not found."));
        return p;
    }

    private void help(CommandSender s, String label) {
        s.sendMessage(Utils.colorize("&8&m      &6 Bank Notes Admin &8&m      "));
        s.sendMessage(Utils.colorize("&e/" + label + " create <amount> [player] [template] &7- nameless note"));
        s.sendMessage(Utils.colorize("&e/" + label + " give <player> <amount> &7- named note"));
        s.sendMessage(Utils.colorize("&e/" + label + " pouch <tier> [player] [count] &7- money pouch (t1-t4)"));
        s.sendMessage(Utils.colorize("&e/" + label + " editor &7- open the banknote editor"));
        s.sendMessage(Utils.colorize("&e/" + label + " check &7- inspect held note"));
        s.sendMessage(Utils.colorize("&e/" + label + " reload &7- reload config"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission(PERM)) return List.of();
        if (args.length == 1) return List.of("create", "give", "pouch", "check", "editor", "reload");
        if (args[0].equalsIgnoreCase("pouch")) {
            if (args.length == 2) return PouchUtils.tierIds();
            if (args.length == 3) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            return List.of();
        }
        if (args[0].equalsIgnoreCase("create") && args.length == 4) {
            return BankNotes.getInstance().getTemplates().all().stream().map(t -> t.id).toList();
        }
        boolean playerArg = (args[0].equalsIgnoreCase("give") && args.length == 2)
                || (args[0].equalsIgnoreCase("create") && args.length == 3);
        if (playerArg) return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        return List.of();
    }
}
