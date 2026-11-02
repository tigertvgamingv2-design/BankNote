package com.cdmstudios.bnote.managers;

import com.cdmstudios.bnote.utils.Log;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class TemplateManager {

    public static final String DEFAULT_ID = "default";

    public static class Template {
        public final String id;
        public String name;
        public Material material = Material.PAPER;
        public List<String> lore = new ArrayList<>();
        public boolean glow = true;

        public Template(String id) {
            this.id = id;
        }
    }

    private final File file;
    private final Map<String, Template> templates = new LinkedHashMap<>();

    public TemplateManager(File dataFolder) {
        dataFolder.mkdirs();
        this.file = new File(dataFolder, "templates.yml");
        load();
    }

    public void load() {
        templates.clear();
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = yml.getConfigurationSection("templates");
        if (sec != null) {
            for (String id : sec.getKeys(false)) {
                ConfigurationSection s = sec.getConfigurationSection(id);
                if (s == null) continue;
                Template t = new Template(id);
                t.name = s.getString("name", defaultName());
                Material m = Material.matchMaterial(s.getString("material", "PAPER"));
                t.material = m != null && m.isItem() ? m : Material.PAPER;
                t.lore = new ArrayList<>(s.getStringList("lore"));
                t.glow = s.getBoolean("glow", true);
                templates.put(id, t);
            }
        }
        if (!templates.containsKey(DEFAULT_ID)) {
            templates.put(DEFAULT_ID, createDefault(DEFAULT_ID));
            save();
        }
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (Template t : templates.values()) {
            String p = "templates." + t.id + ".";
            yml.set(p + "name", t.name);
            yml.set(p + "material", t.material.name());
            yml.set(p + "lore", t.lore);
            yml.set(p + "glow", t.glow);
        }
        try {
            yml.save(file);
        } catch (IOException e) {
            Log.error("Could not save templates.yml: " + e.getMessage());
        }
    }

    public Template get(String id) {
        return id == null ? null : templates.get(id);
    }

    public Template getOrDefault(String id) {
        Template t = get(id);
        return t != null ? t : templates.get(DEFAULT_ID);
    }

    public Collection<Template> all() {
        return templates.values();
    }

    public Template create(String id) {
        Template t = createDefault(id);
        templates.put(id, t);
        save();
        return t;
    }

    public boolean delete(String id) {
        if (DEFAULT_ID.equals(id) || templates.remove(id) == null) return false;
        save();
        return true;
    }

    private static String defaultName() {
        return "&a&l\u2726 Bank Note &8\u00BB &f&l${amount} &a&l\u2726";
    }

    private static Template createDefault(String id) {
        Template t = new Template(id);
        t.name = defaultName();
        t.lore = new ArrayList<>(List.of(
                "&8&m                                ",
                "&7Issued by: &e{player}",
                "&7Value: &a${amount}",
                "&7Date: &f{date}",
                "&8&m                                ",
                "&6&oRight-click to redeem"));
        return t;
    }
}
