package com.haroldstudios.thirstbar;

import com.haroldstudios.thirstbar.listener.PacketListener;
import com.haroldstudios.thirstbar.listener.PlayerListener;
import com.haroldstudios.thirstbar.utility.ConfigValue;
import com.haroldstudios.thirstbar.utility.PlayerUtils;
import me.mattstudios.mf.base.CommandManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.NPC;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;

public final class ThirstBar extends JavaPlugin {

    private boolean mayDisableHealthPacket = false;

    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        // Sets up config utility class values
        ConfigValue.initialize(this);

        CommandManager cm = new CommandManager(this);
        cm.register(new ThirstCommand(this));
        cm.hideTabComplete(true);

        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        if (ConfigValue.isHurtAnimationDisabled()) {
            if (this.getServer().getPluginManager().getPlugin("ProtocolLib") == null) {
                this.getServer().getConsoleSender().sendMessage("[ThirstBar] " + ChatColor.RED + "Could not found ProtocolLib. Please install ProtocolLib to cancel the player hurt animation.");
            } else {
                this.getServer().getConsoleSender().sendMessage("[ThirstBar] " + ChatColor.GREEN + "Successfully hooked into ProtocolLib.");
                new PacketListener(this);
            }
        }

        this.startThirstTimer();
        this.startThirstDamageTimer();
    }

    @Override
    public void onDisable() {
    }

    void startThirstTimer() {
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            mayDisableHealthPacket = true;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player instanceof NPC || PlayerUtils.canBypassThirst(player)) continue;
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
            }
        }, 20L, (long) ConfigValue.getTimeForThirstToDecreaseInSeconds() * 20);
    }

    void startThirstDamageTimer() {
        this.getServer().getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player instanceof NPC || PlayerUtils.canBypassThirst(player))
                    continue;
                if (PlayerUtils.getThirst(player) <= 1.0) {
                    player.damage(ConfigValue.getNoThirstBarDamagePerTimerTick());
                }
                if (!(PlayerUtils.getThirst(player) > 0.0) || !(PlayerUtils.getAbsorptionHearts(player) <= 0.0f))
                    continue;
                PlayerUtils.setThirst(player, 0.0);
            }
        }, 20L, (long) ConfigValue.getNoThirstBarDamageTimerTickInSeconds() * 20);
    }

    public boolean canDisableHealthPacket() {
        return mayDisableHealthPacket;
    }

}
