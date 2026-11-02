package com.cdmstudios.bnote.hooks;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.utils.NoteUtils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

// Only loaded when PlaceholderAPI is present
public class BankNotesExpansion extends PlaceholderExpansion {

    private final BankNotes plugin;

    public BankNotesExpansion(BankNotes plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "banknotes";
    }

    @Override
    public String getAuthor() {
        return "CDM Studios";
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        return switch (params.toLowerCase()) {
            case "balance" -> player == null ? "" : NoteUtils.format(plugin.getEconomy().getBalance(player));
            case "balance_raw" -> player == null ? "" : String.valueOf(plugin.getEconomy().getBalance(player));
            case "redeemed_total" -> String.valueOf(plugin.getRedeemedStore().size());
            case "templates" -> String.valueOf(plugin.getTemplates().all().size());
            default -> null;
        };
    }
}
