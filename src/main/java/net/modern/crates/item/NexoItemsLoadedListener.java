// made by nyxon
package net.modern.crates.item;

import net.modern.crates.ModernCrates;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class NexoItemsLoadedListener implements Listener {

    private final ModernCrates plugin;

    public NexoItemsLoadedListener(ModernCrates plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNexoItemsLoaded(com.nexomc.nexo.api.events.NexoItemsLoadedEvent event) {
        plugin.onNexoItemsLoaded();
    }
}
