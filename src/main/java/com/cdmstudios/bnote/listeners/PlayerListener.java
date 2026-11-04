package com.cdmstudios.bnote.listeners;

import com.cdmstudios.bnote.BankNotes;
import com.cdmstudios.bnote.utils.Fx;
import com.cdmstudios.bnote.utils.Log;
import com.cdmstudios.bnote.utils.NoteUtils;
import com.cdmstudios.bnote.utils.PouchReveal;
import com.cdmstudios.bnote.utils.PouchUtils;
import com.cdmstudios.bnote.utils.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerListener implements Listener {

    private boolean deposit(Player player, double amount) {
        try {
            return BankNotes.getInstance().getEconomy().depositPlayer(player, amount).transactionSuccess();
        } catch (Exception e) {
            Log.error("Deposit threw for " + player.getName() + ": " + e.getMessage());
            return false;
        }
    }

    // Deposit failed: make the id redeemable again and give the item back
    private void rollback(Player player, String id, ItemStack item) {
        try {
            BankNotes.getInstance().getRedeemedStore().unredeem(id);
        } catch (java.io.IOException e) {
            Log.error("Failed to un-redeem " + id + " after failed deposit: " + e.getMessage());
        }
        ItemStack back = item.clone();
        back.setAmount(1);
        player.getInventory().addItem(back).values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
    }

    private void openPouch(Player player, EquipmentSlot hand, ItemStack item) {
        String id = PouchUtils.getId(item);
        PouchUtils.Tier tier = PouchUtils.getTier(PouchUtils.getTierId(item));
        if (tier == null) {
            player.sendMessage(Utils.colorize("&cThis pouch's tier no longer exists."));
            return;
        }

        ItemStack back = item.clone();
        back.setAmount(1);
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItem(hand, item.getAmount() <= 0 ? null : item);

        boolean first;
        try {
            first = id != null && BankNotes.getInstance().getRedeemedStore().redeem(id);
        } catch (java.io.IOException e) {
            player.getInventory().addItem(back).values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            Log.error("Failed to save opened pouch " + id + ": " + e.getMessage());
            player.sendMessage(Utils.colorize("&cCould not open right now. Try again."));
            return;
        }
        if (!first) {
            Log.warn("&e" + player.getName() + " &7tried to open an already-used/invalid pouch &8(" + id + ")");
            player.sendMessage(Utils.colorize("&cThis pouch has already been opened or is invalid."));
            Fx.play(player, "error", "");
            return;
        }

        double amount = PouchUtils.roll(tier);
        if (!deposit(player, amount)) {
            rollback(player, id, back);
            player.sendMessage(Utils.colorize("&cThe deposit failed, so your pouch was returned. Try again."));
            Fx.play(player, "error", "");
            return;
        }
        String amt = NoteUtils.format(amount);
        Log.info("&e" + player.getName() + " &7opened a &e" + tier.id().toUpperCase() + " &7pouch and received &a$" + amt);
        PouchReveal.start(player, tier.name(), amt);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (PouchReveal.skip(player)) {
            event.setCancelled(true);
            return;
        }
        EquipmentSlot hand = event.getHand();
        if (hand == null) return;
        ItemStack item = player.getInventory().getItem(hand);
        if (PouchUtils.isPouch(item)) {
            event.setUseItemInHand(Event.Result.DENY);
            event.setUseInteractedBlock(Event.Result.DENY);
            event.setCancelled(true);
            openPouch(player, hand, item);
            return;
        }
        if (!NoteUtils.isNote(item)) return;

        event.setUseItemInHand(Event.Result.DENY);
        event.setUseInteractedBlock(Event.Result.DENY);
        event.setCancelled(true);

        double amount = NoteUtils.getAmount(item);
        String id = NoteUtils.getId(item);
        item.setAmount(item.getAmount() - 1);
        player.getInventory().setItem(hand, item.getAmount() <= 0 ? null : item);

        boolean first;
        try {
            first = id != null && BankNotes.getInstance().getRedeemedStore().redeem(id);
        } catch (java.io.IOException e) {
            // Could not persist; refund the note rather than risk a double redeem
            ItemStack back = item.clone();
            back.setAmount(1);
            player.getInventory().addItem(back).values().forEach(i -> player.getWorld().dropItemNaturally(player.getLocation(), i));
            Log.error("Failed to save redeemed note " + id + ": " + e.getMessage());
            player.sendMessage(Utils.colorize("&cCould not redeem right now. Try again."));
            return;
        }
        if (!first) {
            Log.warn("&e" + player.getName() + " &7tried to redeem an already-used/invalid note &8(" + id + ")");
            player.sendMessage(Utils.colorize("&cThis bank note has already been redeemed or is invalid."));
            Fx.play(player, "error", "");
            return;
        }

        if (!deposit(player, amount)) {
            rollback(player, id, item);
            player.sendMessage(Utils.colorize("&cThe deposit failed, so your note was returned. Try again."));
            Fx.play(player, "error", "");
            return;
        }
        Log.redeem(player.getName(), NoteUtils.format(amount));
        Fx.play(player, "redeem", NoteUtils.format(amount));
        player.sendMessage(Utils.colorize("&aYou redeemed a bank note for &f$" + NoteUtils.format(amount) + "&a."));
    }
}

