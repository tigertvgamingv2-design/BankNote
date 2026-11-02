package com.cdmstudios.bnote.utils;

import org.bukkit.Bukkit;

public final class Log {

    private static final String PREFIX = "&8[&6&lBank&e&lNotes&8] ";

    private Log() {}

    private static void send(String msg) {
        Bukkit.getConsoleSender().sendMessage(Utils.colorize(PREFIX + msg));
    }

    public static void info(String msg) {
        send("&b\u2139 &7" + msg);
    }

    public static void success(String msg) {
        send("&a\u2714 &f" + msg);
    }

    public static void warn(String msg) {
        send("&e\u26A0 &e" + msg);
    }

    public static void error(String msg) {
        send("&c\u2716 &c" + msg);
    }

    public static void withdraw(String player, String amount) {
        send("&6\u279C &e" + player + " &7withdrew &a$" + amount + " &7into a bank note");
    }

    public static void redeem(String player, String amount) {
        send("&d\u279C &e" + player + " &7redeemed a bank note for &a$" + amount);
    }

    public static void banner(String version) {
        Bukkit.getConsoleSender().sendMessage(Utils.colorize("&8&m                                                  "));
        Bukkit.getConsoleSender().sendMessage(Utils.colorize("&6&l  \u2726 BANK NOTES &8- &7v" + version));
        Bukkit.getConsoleSender().sendMessage(Utils.colorize("&7  Author: &eCDM Studios"));
        Bukkit.getConsoleSender().sendMessage(Utils.colorize("&8&m                                                  "));
    }
}
