package com.haroldstudios.thirstbar;

import com.haroldstudios.thirstbar.utility.ConfigValue;
import me.mattstudios.mf.annotations.*;
import me.mattstudios.mf.base.CommandBase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@Command("thirst")
@Alias("thirstbar")
public class ThirstCommand extends CommandBase {

    private final ThirstBar plugin;

    public ThirstCommand(final ThirstBar plugin) {
        this.plugin = plugin;
    }

    @Default
    @Permission("thirstbar.help")
    public void executeDefaultCommand(CommandSender sender) {
        sender.sendMessage("§7--§b--§7--§bThirstBar§7--§b--§7--");
        sender.sendMessage("§7- §b/thirstbar §7- This menu");
        sender.sendMessage("§7- §b/thirstbar reload §7- Reloads the configuration");
        sender.sendMessage("§7--§b--§7--§bThirstBar§7--§b--§7--");
    }

    @SubCommand("help")
    @Permission("thirstbar.help")
    public void executeHelpCommand(CommandSender sender) {
        executeDefaultCommand(sender);
    }

    @SubCommand("reload")
    @Permission("thirstbar.reload")
    public void executeReloadCommand(CommandSender sender) {
        plugin.reloadConfig();
        Bukkit.getServer().getScheduler().cancelTasks(plugin);
        ConfigValue.initialize(plugin);
        plugin.startThirstTimer();
        plugin.startThirstDamageTimer();
        sender.sendMessage("§7[§bThirstBar§7] §aPlugin successfully reloaded.");
    }

}
