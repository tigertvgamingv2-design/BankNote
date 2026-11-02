package com.cdmstudios.bnote.utils;

import com.cdmstudios.bnote.BankNotes;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

// Sound and title feedback, configured under "feedback.<key>" in config.yml
public final class Fx {

    private Fx() {}

    public static void play(Player p, String key, String amount) {
        ConfigurationSection s = BankNotes.getInstance().getConfig().getConfigurationSection("feedback." + key);
        if (s == null) return;

        String sound = s.getString("sound", "");
        if (!sound.isBlank()) {
            p.playSound(p.getLocation(), sound, (float) s.getDouble("volume", 1.0), (float) s.getDouble("pitch", 1.0));
        }

        String title = s.getString("title", "");
        String sub = s.getString("subtitle", "");
        if (!title.isBlank() || !sub.isBlank()) {
            p.sendTitle(Utils.colorize(title.replace("{amount}", amount)),
                    Utils.colorize(sub.replace("{amount}", amount)), 5, 40, 10);
        }
    }
}
