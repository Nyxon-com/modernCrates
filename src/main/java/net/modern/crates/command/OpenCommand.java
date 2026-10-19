// made by nyxon
package net.modern.crates.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import co.aikar.commands.bukkit.contexts.OnlinePlayer;
import net.modern.crates.ModernCrates;
import net.modern.crates.crate.CrateDefinition;
import net.modern.crates.listener.CrateListener;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandAlias("open")
@Description("Apri una crate se hai la chiave o il forziere")
public final class OpenCommand extends BaseCommand {

    private final CrateListener crates;

    public OpenCommand(ModernCrates plugin) {
        this.crates = new CrateListener(plugin);
    }

    @Default
    @CommandCompletion("@crates")
    @Syntax("<crate>")
    public void onOpen(Player player, CrateDefinition crate) {
        crates.tryOpen(player, crate, CrateCommands.openLocation(player, crate), false);
    }

    @Default
    @CommandPermission("nyxoncrates.admin")
    @CommandCompletion("@players @crates")
    @Syntax("<giocatore> <crate>")
    public void onOpenOther(CommandSender sender, OnlinePlayer target, CrateDefinition crate) {
        Player receiver = target.getPlayer();
        crates.tryOpen(receiver, crate, CrateCommands.openLocation(receiver, crate), false, true);
    }
}
