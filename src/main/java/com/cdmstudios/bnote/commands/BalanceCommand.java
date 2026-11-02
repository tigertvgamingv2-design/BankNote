package com.cdmstudios.bnote.commands;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.utils.Utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BalanceCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }
        BankNotes.getInstance().getGui().openBalance(player);
        return true;
    }
}
