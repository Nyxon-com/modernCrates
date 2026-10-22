//made with love by nyxon

package net.modern.crates;

import net.modern.crates.animation.AnimationSession;
import net.modern.crates.animation.AnimationRegistry;
import net.modern.crates.command.CommandRegistrar;
import net.modern.crates.config.MessageService;
import net.modern.crates.crate.CrateDisplayService;
import net.modern.crates.crate.CratePlacementService;
import net.modern.crates.crate.CrateRegistry;
import net.modern.crates.crate.CrateWriter;
import net.modern.crates.database.DatabaseService;
import net.modern.crates.database.PlayerDataStore;
import net.modern.crates.gui.GuiListener;
import net.modern.crates.gui.GuiManager;
import net.modern.crates.item.ExternalItemReloadListener;
import net.modern.crates.item.ExternalItemService;
import net.modern.crates.item.NexoItemsLoadedListener;
import net.modern.crates.key.KeyService;
import net.modern.crates.listener.CrateListener;
import net.modern.crates.listener.PlayerDataListener;
import net.modern.crates.placeholder.ModernCratesExpansion;
import net.modern.crates.reward.RewardService;
import net.modern.crates.stats.StatsService;
import net.modern.crates.util.AsyncWorkService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class ModernCrates extends JavaPlugin {

  @Override
  public void onLoad() {
    adoptDataFolder("HazeCrates");
    adoptDataFolder("NyxonCrates");
  }

  private void adoptDataFolder(String legacyName) {
    java.io.File target = getDataFolder();
    java.io.File parent = target.getParentFile();
    if (parent == null) {
      return;
    }
    java.io.File legacy = new java.io.File(parent, legacyName);
    if (!legacy.isDirectory()) {
      return;
    }
    if (target.exists()) {
      String[] kids = target.list();
      if (kids != null && kids.length > 0) {
        return;
      }
      if (!target.delete()) {
        return;
      }
    }
    if (legacy.renameTo(target)) {
      getLogger().info("Migrata cartella dati " + legacy.getName() + " -> " + target.getName());
    } else {
      getLogger().warning("Impossibile rinominare " + legacy.getPath() + ": spostala a mano in " + target.getPath());
    }
  }

  private MessageService        messages;
  private CrateRegistry         crates;
  private DatabaseService       database;
  private AsyncWorkService      asyncWork;
  private PlayerDataStore       playerData;
  private StatsService          stats;
  private KeyService            keys;
  private RewardService         rewards;
  private AnimationRegistry     animations;
  private GuiManager            guiManager;
  private CrateWriter           crateWriter;
  private ExternalItemService   externalItems;
  private CratePlacementService placements;
  private CrateDisplayService   display;
  private CommandRegistrar      commands;
  private final java.util.Map<java.util.UUID, AnimationSession> animSessions =
      new java.util.concurrent.ConcurrentHashMap<>();

  private volatile boolean externalPluginsReady = false;
  private volatile boolean itemsAdderPending = false;
  private volatile boolean nexoPending = false;
  private BukkitTask externalRefreshTask;

  @Override
  public void onEnable() {
    ensureDefaultFiles();

    externalItems = new ExternalItemService(this);
    messages      = new MessageService(this);
    crates        = new CrateRegistry(this);
    animations    = new AnimationRegistry(this);
    asyncWork     = new AsyncWorkService(
        getConfig().getInt("database-async-workers", 2),
        getConfig().getInt("async-max-pending-actions", 256), getLogger());
    database      = new DatabaseService(this, asyncWork);
    playerData    = new PlayerDataStore(this, database, asyncWork);
    database.initialize().whenComplete((unused, error) -> {
      if (error != null) getLogger().severe("Database startup failed: " + error.getMessage());
      if (isEnabled()) getServer().getScheduler().runTask(this, playerData::start);
    });

    stats       = new StatsService(database, this);
    keys        = new KeyService(this, database);
    rewards     = new RewardService(this);
    guiManager  = new GuiManager(this);
    crateWriter = new CrateWriter(this);
    placements  = new CratePlacementService(this, asyncWork);
    display     = new CrateDisplayService(this);

    commands = new CommandRegistrar(this);
    commands.register();

    getServer().getPluginManager().registerEvents(new CrateListener(this), this);
    getServer().getPluginManager().registerEvents(new GuiListener(this), this);
    getServer().getPluginManager().registerEvents(new PlayerDataListener(this), this);

    itemsAdderPending = getServer().getPluginManager().isPluginEnabled("ItemsAdder");
    nexoPending = getServer().getPluginManager().isPluginEnabled("Nexo");

    if (itemsAdderPending) {
      getServer().getPluginManager().registerEvents(new Listener() {
        @EventHandler
        public void onIaLoad(dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent e) {
          getServer().getScheduler().runTask(ModernCrates.this, () -> {
            itemsAdderPending = false;
            onExternalPackLoaded("ItemsAdder");
          });
        }
      }, this);
    }
    if (nexoPending) {
      getServer().getPluginManager().registerEvents(new NexoItemsLoadedListener(this), this);
    }

    reloadConfig();
    messages.reload();
    animations.reload();
    crates.reload();
    keys.reload();

    if (!itemsAdderPending && !nexoPending) {
      externalPluginsReady = true;
      getServer().getScheduler().runTask(this, () -> {
        placements.scanAndRepair();
        display.startAll();
      });
    }

    if (getServer().getPluginManager().isPluginEnabled("MMOItems")) {
      getServer().getPluginManager().registerEvents(new ExternalItemReloadListener(this), this);
    }

    if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI"))
      new ModernCratesExpansion(this).register();
  }

  @Override
  public void onDisable() {
    if (commands != null) commands.unregister();
    net.modern.crates.animation.opening.world.support.OpeningProps.clearAll();
    net.modern.crates.animation.opening.world.support.TempOpenChest.restoreAll();
    if (rewards != null) rewards.shutdown();
    if (display  != null) display.stopAll();
    if (externalRefreshTask != null) {
      externalRefreshTask.cancel();
      externalRefreshTask = null;
    }
    if (playerData != null) playerData.shutdown();
    if (placements != null) placements.shutdown();
    if (asyncWork != null) asyncWork.close();
    if (database != null) database.close();
  }

  public void reloadPlugin() {
    ensureDefaultFiles();
    reloadConfig();
    messages.reload();
    animations.reload();
    crates.reload();
    keys.reload();
    if (rewards != null) rewards.reloadBroadcastQueue();
    startDisplay();
  }

  public void scheduleExternalItemRefresh(String reason, long delayTicks) {
    if (crates == null) return;
    if (externalRefreshTask != null) {
      externalRefreshTask.cancel();
    }
    long delay = Math.max(1L, delayTicks);
    externalRefreshTask = getServer().getScheduler().runTaskLater(this, () -> {
      externalRefreshTask = null;
      externalPluginsReady = true;
      getLogger().info("[ModernCrates] " + reason + " – refreshing crate items.");
      crates.reload();
      keys.reload();
      startDisplay();
    }, delay);
  }

  public void onNexoItemsLoaded() {
    getServer().getScheduler().runTask(this, () -> {
      nexoPending = false;
      onExternalPackLoaded("Nexo");
    });
  }

  private void onExternalPackLoaded(String source) {
    if (!Bukkit.isPrimaryThread()) {
      getServer().getScheduler().runTask(this, () -> onExternalPackLoaded(source));
      return;
    }
    getLogger().info("[ModernCrates] " + source + " data loaded.");
    crates.reload();
    keys.reload();
    if (itemsAdderPending || nexoPending) {
      getLogger().info("[ModernCrates] Waiting for remaining item plugins before starting displays.");
      return;
    }
    boolean first = !externalPluginsReady;
    externalPluginsReady = true;
    if (first) {
      placements.scanAndRepair();
      display.startAll();
    } else {
      startDisplay();
    }
  }

  public void startDisplay() {
    if (!externalPluginsReady || display == null) return;
    if (Bukkit.isPrimaryThread()) {
      display.startAll();
      return;
    }
    getServer().getScheduler().runTask(this, () -> display.startAll());
  }

  public boolean externalPluginsReady() { return externalPluginsReady; }

  public void ensureDefaultFiles() {
    saveDefaultConfig();
    saveResourceIfMissing("messages.yml");
    saveResourceIfMissing("animations.yml");
    saveResourceIfMissing("keys.yml");
    java.io.File cratesDir = new java.io.File(getDataFolder(), "crates");
    cratesDir.mkdirs();
    java.io.File exampleCrate = new java.io.File(cratesDir, "example.yml");
    if (!exampleCrate.exists()) saveResource("crates/example.yml", false);
    mergeConfigDefaults();
  }

  private void mergeConfigDefaults() {
    try (java.io.InputStream in = getResource("config.yml")) {
      if (in == null) return;
      org.bukkit.configuration.file.YamlConfiguration bundled =
          org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(
              new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
      boolean changed = false;
      for (String key : bundled.getKeys(true)) {
        if (!getConfig().contains(key)) {
          getConfig().set(key, bundled.get(key));
          changed = true;
        }
      }
      if (changed) saveConfig();
    } catch (Exception ignored) {
    }
  }

  private void saveResourceIfMissing(String resourcePath) {
    java.io.File file = new java.io.File(getDataFolder(), resourcePath);
    if (!file.exists()) saveResource(resourcePath, false);
  }

  public MessageService        messages()      { return messages; }
  public CrateRegistry         crates()        { return crates; }
  public DatabaseService       database()      { return database; }
  public PlayerDataStore       playerData()    { return playerData; }
  public StatsService          stats()         { return stats; }
  public KeyService            keys()          { return keys; }
  public RewardService         rewards()       { return rewards; }
  public AnimationRegistry     animations()    { return animations; }
  public GuiManager            guiManager()    { return guiManager; }
  public CrateWriter           crateWriter()   { return crateWriter; }
  public ExternalItemService   externalItems() { return externalItems; }
  public CratePlacementService placements()    { return placements; }
  public CrateDisplayService   display()       { return display; }

  public void startAnimSession(java.util.UUID uuid, AnimationSession session) {
    animSessions.put(uuid, session);
  }
  public boolean tryStartAnimSession(java.util.UUID uuid, AnimationSession session) {
    return animSessions.putIfAbsent(uuid, session) == null;
  }
  public AnimationSession takeAnimSession(java.util.UUID uuid) {
    return animSessions.remove(uuid);
  }
  public AnimationSession animSession(java.util.UUID uuid) {
    return animSessions.get(uuid);
  }
  public boolean hasAnimSession(java.util.UUID uuid) {
    return animSessions.containsKey(uuid);
  }
}
