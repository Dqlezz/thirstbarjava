package com.haroldstudios.thirstbar.utility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;

public class PlayerUtils {

    private static Class<?> craftPlayerClass;
    private static Class<?> entityHumanClass;
    private static boolean isNewVersion;

    static {
        String nmsver = Bukkit.getServer().getClass().getPackage().getName();
        nmsver = nmsver.substring(nmsver.lastIndexOf(".") + 1);
        int major = Integer.parseInt(nmsver.substring(nmsver.indexOf("_") + 1, nmsver.lastIndexOf("_")));
        try {
            if (major < 17) {
                isNewVersion = false;
                craftPlayerClass = Class.forName("org.bukkit.craftbukkit." + nmsver + ".entity.CraftPlayer");
                entityHumanClass = Class.forName("net.minecraft.server." + nmsver + ".EntityHuman");
            } else {
                isNewVersion = true;
            }
            Bukkit.getServer().getConsoleSender().sendMessage("[ThirstBar] Successfully hooked into version " + nmsver);
        } catch (Exception e) {
            Bukkit.getServer().getConsoleSender().sendMessage("[ThirstBar] " + ChatColor.RED + "Version " + nmsver + " is not compatible with this plugin!");
            Bukkit.getServer().getPluginManager().getPlugin("ThirstBar").onDisable();
        }
    }

    /**
     * Gets if player can be affected by thirst
     *
     * @param player Player to check against
     * @return If player should be affected by thirst
     */
    public static boolean canBypassThirst(Player player) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) return true;
        if (player.hasPermission("thirst.bypass")) return true;
        return ConfigValue.getBlacklistedWorlds().contains(player.getWorld().getName());
    }

    /**
     * Gets the absorption hearts of player (Their new health bar)
     *
     * @param player Player to get value of
     * @return float absorption hearts (health bar)
     */
    public static float getAbsorptionHearts(Player player) {
        if (isNewVersion) {
            return (float) player.getAbsorptionAmount();
        } else {
            try {
                Object craftPlayer = craftPlayerClass.cast(player);
                Method craftPlayerHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
                Object entityPlayer = craftPlayerHandleMethod.invoke(craftPlayer);
                Method entityHumanAbsorptionMethod = entityHumanClass.getDeclaredMethod("getAbsorptionHearts");
                Object amount = entityHumanAbsorptionMethod.invoke(entityPlayer);
                return (Float) amount;
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0f;
            }
        }
    }

    /**
     * Sets the absorption hearts of player (Their new health bar)
     *
     * @param player Player to set value for
     */
    public static void setAbsorptionHearts(Player player, float amount) {
        if (amount <= 0) {
            player.setHealth(0);
        } else {
            if (isNewVersion) {
                player.setAbsorptionAmount(amount);
            } else {
                try {
                    Object craftPlayer = craftPlayerClass.cast(player);
                    Method craftPlayerHandleMethod = craftPlayerClass.getDeclaredMethod("getHandle");
                    Object entityPlayer = craftPlayerHandleMethod.invoke(craftPlayer);
                    Method entityHumanAbsorptionMethod = entityHumanClass.getDeclaredMethod("setAbsorptionHearts", Float.TYPE);
                    entityHumanAbsorptionMethod.invoke(entityPlayer, amount);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Sets the maximum amount of thirst player can have
     *
     * @param player Player to set of
     * @param val    double value to set
     */
    public static void setMaxThirst(Player player, double val) {
        // If attribute exists (post 1.8 or 1.13 - can't remember which)
        try {
            Class.forName("org.bukkit.attribute.Attribute");
            Class.forName("org.bukkit.attribute.AttributeInstance");
            new AttributeModifier().setMaxThirst(player, val);
        } catch (Exception e) {
            player.setMaxHealth(val);
        }
    }

    /**
     * Set thirst player has
     *
     * @param player player to set value for
     * @param val    double Value to set
     */
    public static void setThirst(Player player, double val) {
        player.setHealth(val);
    }

    /**
     * Gets Thirst value for player
     *
     * @param player Player to get from
     * @return double thirst value
     */
    public static double getThirst(Player player) {
        return player.getHealth();
    }

}
