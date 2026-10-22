//made with love by nyxon

package net.modern.crates.crate;

import net.modern.crates.item.DisplayIcon;
import net.modern.crates.item.ItemProvider;
import net.modern.crates.item.ItemSpec;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;
import java.util.List;

public record RewardDefinition(
    String id,
    ItemSpec itemSpec,
    ItemStack icon,
    ItemStack displayIcon,
    List<String> commands,
    int weight,
    String permission,
    boolean broadcast,
    boolean loreOverride,
    int previewSlot
) {
  public RewardDefinition(String id, ItemSpec itemSpec, ItemStack icon, List<String> commands,
              int weight, String permission, boolean broadcast, boolean loreOverride) {
    this(id, itemSpec, icon, DisplayIcon.light(icon), commands, weight, permission, broadcast, loreOverride, -1);
  }

  public RewardDefinition(String id, ItemSpec itemSpec, ItemStack icon, List<String> commands,
              int weight, String permission, boolean broadcast, boolean loreOverride, int previewSlot) {
    this(id, itemSpec, icon, DisplayIcon.light(icon), commands, weight, permission, broadcast, loreOverride, previewSlot);
  }

  public Component displayComponent() {
    if (icon == null || icon.getType().isAir()) {
      return Component.text(id);
    }
    return DisplayIcon.visibleName(icon);
  }

  public String displayName() {
    return plainName();
  }

  public String plainName() {
    return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
        .serialize(displayComponent())
        .trim();
  }

  public boolean liveProviderAppearance() {
    return !loreOverride
        && itemSpec != null
        && itemSpec.provider() != ItemProvider.VANILLA;
  }

  public ItemStack preview() {
    if (displayIcon == null || displayIcon.getType().isAir()) {
      return DisplayIcon.light(icon);
    }
    return displayIcon.clone();
  }

  public ItemStack prize() {
    if (icon == null || icon.getType().isAir()) {
      return null;
    }
    return icon.clone();
  }
}
