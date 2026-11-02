package com.cdmstudios.bnote.commands;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.utils.Fx;
import com.cdmstudios.bnote.utils.Log;
import com.cdmstudios.bnote.utils.NoteUtils;
import com.cdmstudios.bnote.utils.Utils;
import java.util.HashMap;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WithdrawCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        if (args.length != 1) {
            player.sendMessage(Utils.colorize("&cUsage: /withdraw <amount>"));
            return true;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(Utils.colorize("&cInvalid amount."));
            return true;
        }
        if (Double.isNaN(amount) || Double.isInfinite(amount) || amount < 1) {
            player.sendMessage(Utils.colorize("&cAmount must be at least $1."));
            return true;
        }
        amount = Math.floor(amount * 100) / 100;

        Economy eco = BankNotes.getInstance().getEconomy();
        if (!eco.has(player, amount)) {
            player.sendMessage(Utils.colorize("&cYou don't have enough money."));
            Fx.play(player, "error", "");
            return true;
        }
        if (!eco.withdrawPlayer(player, amount).transactionSuccess()) {
            player.sendMessage(Utils.colorize("&cWithdrawal failed."));
            return true;
        }

        ItemStack note = NoteUtils.createNote(player.getName(), player.getUniqueId(), amount);
        HashMap<Integer, ItemStack> left = player.getInventory().addItem(note);
        left.values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));

        Log.withdraw(player.getName(), NoteUtils.format(amount));
        Fx.play(player, "withdraw", NoteUtils.format(amount));
        player.sendMessage(Utils.colorize("&aYou withdrew &f$" + NoteUtils.format(amount) + " &ainto a bank note."));
        return true;
    }
}

