package com.haroldstudios.thirstbar.listener;

import com.haroldstudios.thirstbar.ThirstBar;
import com.haroldstudios.thirstbar.utility.ConfigValue;
import com.haroldstudios.thirstbar.utility.PlayerUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerListener implements Listener {

    public static final List<Player> namesToCancelFor = new ArrayList<>();
    private final ThirstBar plugin;

    public PlayerListener(ThirstBar plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // If player has 0 absorption hearts (new health bar) then give them health. Otherwise, they would be dead.
        if (PlayerUtils.getAbsorptionHearts(player) <= 0.0f) {
            PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue());
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();

        PlayerUtils.setMaxThirst(player, ConfigValue.getMaxThirstBarValue());
        Bukkit.getScheduler().runTaskLater(plugin, () -> PlayerUtils.setThirst(player, ConfigValue.getMaxThirstBarValue() - 0.01), 1L);

        // Absorption Hearts now refers to health
        Bukkit.getScheduler().runTaskLater(plugin, () -> PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue()), 1L);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (namesToCancelFor.contains(event.getEntity())) {
            event.setDeathMessage(null);
            namesToCancelFor.remove(event.getEntity());
        }
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        double newHealth = PlayerUtils.getAbsorptionHearts(player) - event.getDamage();

        // If player has no absorption health, kill the player.
        PlayerUtils.setAbsorptionHearts(player, (float) newHealth);

        if (newHealth <= 0.0) {
            namesToCancelFor.add(player);
            PlayerUtils.setThirst(player, 0.0);
        }
    }

    @EventHandler
    public void onHeal(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof Player)) return;
        Player player = (Player) event.getEntity();

        // If setting in config states they must have full thirst bar to regen, cancle event and exit.
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED && ConfigValue.doesRequireFullThirstToRegen() && PlayerUtils.getThirst(player) < ConfigValue.getMaxThirstBarValue() - 2.98) {
            event.setCancelled(true);
            return;
        }
        if (event.getRegainReason() == EntityRegainHealthEvent.RegainReason.CUSTOM) {
            event.setCancelled(true);
            PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue());
            PlayerUtils.setThirst(player, ConfigValue.getMaxThirstBarValue() - 0.01);
            player.setFoodLevel(20);
        }
        int extraHp = 0;
        if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            extraHp = 4;
        }

        if (PlayerUtils.getAbsorptionHearts(player) + event.getAmount() > ConfigValue.getMaxHealthValue()) {
            PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue() + extraHp);
        } else {
            PlayerUtils.setAbsorptionHearts(player, (float) (PlayerUtils.getAbsorptionHearts(player) + event.getAmount() + extraHp));
        }
        event.setCancelled(true);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        if (!(PlayerUtils.getThirst(player) < ConfigValue.getMaxThirstBarValue())) return;

        AtomicReference<Double> thirstGain = new AtomicReference<>((double) 0);
        if (event.getItem().getItemMeta() instanceof PotionMeta) {
            PotionMeta meta = (PotionMeta) event.getItem().getItemMeta();
            if (!meta.hasCustomEffects()) {
                thirstGain.set(ConfigValue.getConsumableThirstGain());
            }
        }
        ConfigValue.getItemList().forEach((item, replenishes) -> {
            if (item.isSimilar(event.getItem())) {
                thirstGain.set(replenishes);
            }
        });
        if (thirstGain.get() == null) return;

        if (PlayerUtils.getThirst(player) + thirstGain.get() <= ConfigValue.getMaxThirstBarValue()) {
            PlayerUtils.setThirst(player, PlayerUtils.getThirst(player) + thirstGain.get());
            return;
        }
        PlayerUtils.setThirst(player, ConfigValue.getMaxThirstBarValue() - 0.01);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();

        int extraHp = 0;
        if (player.hasPotionEffect(PotionEffectType.ABSORPTION)) {
            extraHp = 4;
        }

        if (!(PlayerUtils.getAbsorptionHearts(player) <= 0.0f)) return;
        PlayerUtils.setAbsorptionHearts(player, (float) ConfigValue.getMaxHealthValue() + extraHp);
    }

}
