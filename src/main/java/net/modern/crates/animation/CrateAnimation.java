//made with love by nyxon

package net.modern.crates.animation;

import net.modern.crates.crate.CrateDefinition;
import net.modern.crates.crate.RewardDefinition;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public interface CrateAnimation {
  void play(Player player, Location location,
        CrateDefinition crate, RewardDefinition reward,
        Runnable reveal);

  default boolean isWorldAnimation() { return false; }
}
