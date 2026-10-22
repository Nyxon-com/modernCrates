//made with love by nyxon

package net.modern.crates.command;

import net.modern.crates.ModernCrates;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

public final class CommandSupport {

  private CommandSupport() {}

  public static void fail(ModernCrates plugin, CommandSender sender, String key, Map<String, String> values) {
    if (sender instanceof Player player) {
      plugin.messages().send(player, key, values);
    } else {
      sender.sendMessage(plain(plugin, key, values));
    }
  }

  public static String plain(ModernCrates plugin, String key, Map<String, String> values) {
    return plugin.messages().raw(key, values).replaceAll("<[^>]+>", "");
  }
}
