package com.haroldstudios.thirstbar.utility;

import com.cryptomorin.xseries.XBiome;
import com.cryptomorin.xseries.XMaterial;
import com.haroldstudios.thirstbar.ThirstBar;
import org.bukkit.block.Biome;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/*
Utility class for config
 */
public class ConfigValue {

    private static final Map<ItemStack, Double> itemList = new HashMap<>();
    // Default values here strictly in case initialization fails.
    private static boolean requireFullThirstToRegen = true;
    private static List<String> blacklistedWorlds = new ArrayList<>();
    private static boolean disabledHurtAnimation = true;
    private static double timeForThirstToDecreaseInSeconds = 60;
    private static double thirstRemovedPerTimerTick = 1.0;
    private static double maxThirstBarValue = 20.0; // 20 = one row of hearts
    private static double maxHealthValue = 20.0; // 20 = one row of hearts
    private static double noThirstBarDamagePerTimerTick = 1.0;
    private static double consumableThirstGain = 2.5;
    private static double globalMultiplier = 1.0;
    private static HashMap<Biome, Double> biomesSpecific;
    private static HashMap<String, HashMap<Biome, Double>> worldBiomesSpecific;
    private static boolean biomesEnabled = false;
    private static boolean afkEnabled = false;
    private static int timeOut = 300;
    private static boolean afkChatReset = false;
    private static boolean afkMoveReset = false;
    private static boolean afkInteractReset = false;
    private static boolean afkOnlyInWater = false;

    private ConfigValue() {
    }

    public static void initialize(ThirstBar plugin) {
        biomesSpecific = new HashMap<>();
        worldBiomesSpecific = new HashMap<>();
        FileConfiguration config = plugin.getConfig();
        requireFullThirstToRegen = config.getBoolean("requireFullThirstToRegen");
        disabledHurtAnimation = config.getBoolean("disabledHurtAnimation");
        timeForThirstToDecreaseInSeconds = roundToHalf(config.getDouble("timeForThirstToDecreaseInSeconds"));
        thirstRemovedPerTimerTick = roundToHalf(config.getDouble("thirstRemovedPerTimerTick"));
        maxThirstBarValue = roundToHalf(config.getDouble("maxThirstBarValue"));
        maxHealthValue = roundToHalf(config.getDouble("maxHealthValue"));
        noThirstBarDamagePerTimerTick = roundToHalf(config.getDouble("noThirstBarDamagePerTimerTick"));

        blacklistedWorlds = config.getStringList("blackListedWorlds");

        if (!new File(plugin.getDataFolder() + "/afk.yml").exists()) {
            plugin.saveResource("afk.yml", false);
        }

        File afkFile = new File(plugin.getDataFolder() + "/afk.yml");
        YamlConfiguration afkConfig = YamlConfiguration.loadConfiguration(afkFile);

        afkEnabled = afkConfig.getBoolean("enabled", false);
        if (afkEnabled) {
            timeOut = afkConfig.getInt("timeout", 300);
            afkOnlyInWater = afkConfig.getBoolean("stop-only-in-water", false);
            afkChatReset = afkConfig.getBoolean("modes.chat", true);
            afkMoveReset = afkConfig.getBoolean("modes.move", true);
            afkInteractReset = afkConfig.getBoolean("modes.interact", true);
        }

        itemList.clear();

        if (!new File(plugin.getDataFolder() + "/consumables.yml").exists()) {
            plugin.saveResource("consumables.yml", false);
        }
        if (!new File(plugin.getDataFolder() + "/biomes.yml").exists()) {
            plugin.saveResource("biomes.yml", false);
        }

        File langFile = new File(plugin.getDataFolder() + "/consumables.yml");
        YamlConfiguration externalYamlConfig = YamlConfiguration.loadConfiguration(langFile);
        consumableThirstGain = roundToHalf(externalYamlConfig.getDouble("bottles"));
        ConfigurationSection section = externalYamlConfig.getConfigurationSection("consumables");
        for (String key : section.getKeys(false)) {
            String id = externalYamlConfig.getString("consumables." + key + ".id");
            String[] split = id.split(":");
            Optional<XMaterial> it;
            try {
                int first = Integer.parseInt(split[0]);
                int second = Integer.parseInt(split[1]);
                it = XMaterial.matchXMaterial(first, (byte) second);
            } catch (NumberFormatException nfe) {
                it = XMaterial.matchXMaterial(id);
            }
            ItemStack item = null;
            if (it.isPresent()) {
                item = it.get().parseItem();
            }
            if (item == null) continue;
            itemList.put(item, roundToHalf(externalYamlConfig.getDouble("consumables." + key + ".replenishes")));
        }

        File biomesFile = new File(plugin.getDataFolder() + "/biomes.yml");
        YamlConfiguration biomesConfig = YamlConfiguration.loadConfiguration(biomesFile);
        globalMultiplier = biomesConfig.getDouble("global", 1.0);
        biomesEnabled = biomesConfig.getBoolean("enabled", true);
        ConfigurationSection biomes = biomesConfig.getConfigurationSection("global-biomes");
        for (String key : biomes.getKeys(false)) {
            Optional<XBiome> optionalBiome = XBiome.matchXBiome(key);
            if (!optionalBiome.isPresent()) {
                continue;
            }

            Biome biome = optionalBiome.get().getBiome();
            biomesSpecific.put(biome, biomes.getDouble(key, 1.0));
        }
        ConfigurationSection worldBiomes = biomesConfig.getConfigurationSection("world-biomes");
        for (String key : worldBiomes.getKeys(false)) {
            HashMap<Biome, Double> innerBiomes = new HashMap<>();
            ConfigurationSection specificWorldBiomes = worldBiomes.getConfigurationSection(key);
            for (String biomeKey : specificWorldBiomes.getKeys(false)) {
                Optional<XBiome> optionalBiome = XBiome.matchXBiome(biomeKey);
                if (!optionalBiome.isPresent()) {
                    continue;
                }

                Biome biome = optionalBiome.get().getBiome();
                innerBiomes.put(biome, specificWorldBiomes.getDouble(biomeKey, 1.0));
            }
            if (!innerBiomes.isEmpty()) {
                worldBiomesSpecific.put(key, innerBiomes);
            }
        }
    }

    /**
     * Rounds double value to nearest 0.5
     *
     * @param d Double to round
     * @return Double rounded to the nearest 0.5
     */
    private static double roundToHalf(double d) {
        return Math.round(d * 2) / 2.0;
    }

    private static double calculateThirstMultiplier(Player player) {
        Biome biome = player.getWorld().getBiome(player.getLocation());
        String world = player.getWorld().getName();

        if (worldBiomesSpecific.containsKey(world)) {
            HashMap<Biome, Double> biomes = worldBiomesSpecific.get(world);
            if (biomes.containsKey(biome)) {
                return biomes.get(biome);
            }
        }

        if (biomesSpecific.containsKey(biome)) {
            return biomesSpecific.get(biome);
        }
        return globalMultiplier;
    }

    public static double calculateThirst(Player player) {
        if (!biomesEnabled) {
            return getThirstRemovedPerTimerTick();
        }

        return getThirstRemovedPerTimerTick() * calculateThirstMultiplier(player);
    }

    /*
            GETTERS
     */

    public static boolean doesRequireFullThirstToRegen() {
        return requireFullThirstToRegen;
    }

    public static List<String> getBlacklistedWorlds() {
        return blacklistedWorlds;
    }

    public static boolean isHurtAnimationDisabled() {
        return disabledHurtAnimation;
    }

    public static double getConsumableThirstGain() {
        return consumableThirstGain;
    }

    public static double getMaxHealthValue() {
        return maxHealthValue;
    }

    public static double getMaxThirstBarValue() {
        return maxThirstBarValue;
    }

    public static double getNoThirstBarDamagePerTimerTick() {
        return noThirstBarDamagePerTimerTick;
    }

    public static double getThirstRemovedPerTimerTick() {
        return thirstRemovedPerTimerTick;
    }

    public static double getTimeForThirstToDecreaseInSeconds() {
        return timeForThirstToDecreaseInSeconds;
    }

    public static Map<ItemStack, Double> getItemList() {
        return itemList;
    }

    public static boolean isAfkEnabled() {
        return afkEnabled;
    }

    public static int getTimeOut() {
        return timeOut;
    }

    public static boolean isAfkChatReset() {
        return afkChatReset;
    }

    public static boolean isAfkMoveReset() {
        return afkMoveReset;
    }

    public static boolean isAfkInteractReset() {
        return afkInteractReset;
    }

    public static boolean isAfkOnlyInWater() {
        return afkOnlyInWater;
    }

}
