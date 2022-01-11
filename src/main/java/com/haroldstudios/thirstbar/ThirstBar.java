package com.haroldstudios.thirstbar;

import com.haroldstudios.thirstbar.listener.PacketListener;
import com.haroldstudios.thirstbar.listener.PlayerListener;
import com.haroldstudios.thirstbar.utility.ConfigValue;
import com.haroldstudios.thirstbar.utility.PlayerUtils;
import me.mattstudios.mf.base.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ThirstBar extends JavaPlugin {

    private boolean mayDisableHealthPacket = false;
    private ConcurrentHashMap<UUID, Long> afkMap = new ConcurrentHashMap<>();
    public Runnable thirstRunnable;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        // Sets up config utility class values
        ConfigValue.initialize(this);

        CommandManager cm = new CommandManager(this);
        cm.register(new ThirstCommand(this));
        cm.hideTabComplete(true);

        getServer().getPluginManager().registerEvents(new PlayerListener(this, afkMap), this);

        if (ConfigValue.isHurtAnimationDisabled()) {
            if (this.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                this.getServer().getConsoleSender().sendMessage("[ThirstBar] " + ChatColor.RED + "Could not found ProtocolLib. Please install ProtocolLib to cancel the player hurt animation.");
            } else {
                this.getServer().getConsoleSender().sendMessage("[ThirstBar] " + ChatColor.GREEN + "Successfully hooked into ProtocolLib.");
                new PacketListener(this);
            }
        }

        this.startThirstTimer();
    }

    void startThirstTimer() {
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            mayDisableHealthPacket = true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player instanceof NPC || PlayerUtils.canBypassThirst(player)) continue;
                if (ConfigValue.isAfkEnabled()) {
                    if (afkMap.containsKey(player.getUniqueId()) && afkMap.get(player.getUniqueId()) + ConfigValue.getTimeOut() * 100L <= System.currentTimeMillis()) {
                        if (ConfigValue.isAfkOnlyInWater()) {
                            Material material = player.getLocation().getBlock().getType();
                            if (material == Material.WATER) {
                                continue;
                            }
                        } else {
                            continue;
                        }
                    }
                }
                int extraHp = 0;
                if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
                    extraHp = 4;
                }

                if (!player.hasPlayedBefore() || PlayerUtils.getAbsorptionHearts(player) <= 0.0f) {
                    PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue() + extraHp);
                }
                if (PlayerUtils.getThirst(player) - ConfigValue.calculateThirst(player) > 0.0) {
                    PlayerUtils.setThirst(player, PlayerUtils.getThirst(player) - ConfigValue.calculateThirst(player));
                    continue;
                }
                if (PlayerUtils.getThirst(player) <= 0.0) continue;
                PlayerUtils.setThirst(player, 0.01);
            }
            mayDisableHealthPacket = false;
            for (Player value : Bukkit.getOnlinePlayers()) {
                Player player;
                player = value;
                if (player instanceof NPC) continue;
                player.setFoodLevel(player.getFoodLevel());
                if (PlayerUtils.canBypassThirst(player))
                    continue;
                if (PlayerUtils.getThirst(player) <= 1.0) {
                    player.damage(ConfigValue.getNoThirstBarDamagePerTimerTick());
                }
                if (!(PlayerUtils.getThirst(player) > 0.0) || !(PlayerUtils.getAbsorptionHearts(player) <= 0.0f))
                    continue;
                PlayerUtils.setThirst(player, 0.0);
            }
        }, 20L, (long) ConfigValue.getTimeForThirstToDecreaseInSeconds() * 20);
    }

    public boolean canDisableHealthPacket() {
        return mayDisableHealthPacket;
    }

}
