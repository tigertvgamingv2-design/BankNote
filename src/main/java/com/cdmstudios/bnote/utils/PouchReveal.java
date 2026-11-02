package com.cdmstudios.bnote.utils;

import com.cdmstudios.bnote.BankNotes;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

// Pouch opening animation: the amount is &k scrambled in the title and reveals one character at a time.
// Right-clicking during the animation skips to the result.
public final class PouchReveal {

    private static final Map<UUID, PouchReveal> ACTIVE = new ConcurrentHashMap<>();
    private static final long STEP_TICKS = 6L;

    private final Player player;
    private final String title;
    private final String amt;
    private final BukkitRunnable task;
    private int revealed = 0;
    private boolean finished = false;

    private PouchReveal(Player player, String tierName, String amt) {
        this.player = player;
        this.title = tierName;
        this.amt = amt;
        this.task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancelAndForget();
                    return;
                }
                if (revealed >= amt.length()) {
                    finish();
                    return;
                }
                revealed++;
                // separators reveal together with the digit before them
                while (revealed < amt.length() && !Character.isDigit(amt.charAt(revealed))) revealed++;
                frame();
            }
        };
    }

    public static boolean isActive(Player p) {
        return ACTIVE.containsKey(p.getUniqueId());
    }

    public static boolean skip(Player p) {
        PouchReveal r = ACTIVE.get(p.getUniqueId());
        if (r == null) return false;
        r.finish();
        return true;
    }

    public static void start(Player player, String tierName, String amt) {
        PouchReveal old = ACTIVE.remove(player.getUniqueId());
        if (old != null) old.finish();
        PouchReveal r = new PouchReveal(player, tierName, amt);
        ACTIVE.put(player.getUniqueId(), r);
        r.frame();
        r.task.runTaskTimer(BankNotes.getInstance(), STEP_TICKS, STEP_TICKS);
    }

    private void frame() {
        String shown = amt.substring(0, revealed);
        String hidden = amt.substring(revealed);
        String sub = "&a&l$" + shown + (hidden.isEmpty() ? "" : "&k" + hidden) + "  &8(right-click to skip)";
        player.sendTitle(Utils.colorize(title), Utils.colorize(sub), 0, 15, 5);
        float pitch = 0.6f + 1.4f * ((float) revealed / Math.max(1, amt.length()));
        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, Math.min(2.0f, pitch));
    }

    private void cancelAndForget() {
        try {
            task.cancel();
        } catch (IllegalStateException ignored) {
            // task was never scheduled
        }
        ACTIVE.remove(player.getUniqueId(), this);
    }

    private void finish() {
        if (finished) return;
        finished = true;
        cancelAndForget();
        if (!player.isOnline()) return;

        player.sendTitle(Utils.colorize(title), Utils.colorize("&a&l+$" + amt), 0, 60, 15);
        Fx.playSoundOnly(player, "pouch");
        player.sendMessage(Utils.colorize("&6You opened a " + title + " &6and found &a$" + amt + "&6!"));
    }
}
