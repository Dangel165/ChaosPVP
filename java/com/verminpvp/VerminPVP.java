package com.verminpvp;

import com.verminpvp.commands.ClassCommand;
import com.verminpvp.commands.EndGameCommand;
import com.verminpvp.commands.ExcludeCommand;
import com.verminpvp.commands.GameModeCommand;
import com.verminpvp.commands.HelpCommand;
import com.verminpvp.commands.IncludeCommand;
import com.verminpvp.commands.LobbyCommand;
import com.verminpvp.commands.LobbyRemoveCommand;
import com.verminpvp.commands.MapRemoveCommand;
import com.verminpvp.commands.MapSetCommand;
import com.verminpvp.commands.ParticleCommand;
import com.verminpvp.commands.PracticeModeCommand;
import com.verminpvp.commands.PracticeModeClassCommand;
import com.verminpvp.commands.PracticeModeEndCommand;
import com.verminpvp.commands.PracticeModeStartCommand;
import com.verminpvp.commands.StartGameCommand;
import com.verminpvp.gui.ClassSelectionGUI;
import com.verminpvp.gui.TeamSelectionGUI;
import com.verminpvp.handlers.*;
import com.verminpvp.managers.*;
import com.verminpvp.ui.CooldownDisplay;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main plugin class for ChaosPVP - A class-based PVP system
 * 
 * This plugin provides 7 unique character classes with distinct abilities,
 * passive effects, and progression mechanics for Minecraft Paper 1.21.11
 */
public class VerminPVP extends JavaPlugin {
    
    private static VerminPVP instance;
    
    // Managers
    private ClassManager classManager;
    private CooldownManager cooldownManager;
    private ResourceTracker resourceTracker;
    private ItemProvider itemProvider;
    private TeamManager teamManager;
    private GameManager gameManager;
    private ExcludeManager excludeManager;
    private MapManager mapManager;
    private LobbyManager lobbyManager;
    private DataManager dataManager;
    private DraftPickManager draftPickManager;
    
    // UI
    private CooldownDisplay cooldownDisplay;
    
    // Handlers
    private DamageHandler damageHandler;
    private EffectApplicator effectApplicator;
    private SwordsmanHandler swordsmanHandler;
    private ScientistHandler scientistHandler;
    private PlagueSpreaderHandler plagueSpreaderHandler;
    private ShieldSoldierHandler shieldSoldierHandler;
    private CriticalCutterHandler criticalCutterHandler;
    private NavigatorHandler navigatorHandler;
    private CaptainHandler captainHandler;
    private ShapeshifterHandler shapeshifterHandler;
    private JugglerHandler jugglerHandler;
    private DragonFuryHandler dragonFuryHandler;
    private UndeadHandler undeadHandler;
    private StamperHandler stamperHandler;
    private TimeEngraverHandler timeEngraverHandler;
    private WorldProtectionHandler worldProtectionHandler;
    private TeamKillPreventionHandler teamKillPreventionHandler;
    private PlayerDeathHandler playerDeathHandler;
    private PlayerJoinHandler playerJoinHandler;
    private PlayerQuitHandler playerQuitHandler;
    private FreezeProtectionHandler freezeProtectionHandler;
    private PlayerRespawnHandler playerRespawnHandler;
    
    // GUI
    private ClassSelectionGUI classSelectionGUI;
    private TeamSelectionGUI teamSelectionGUI;
    
    @Override
    public void onEnable() {
        instance = this;
        
        getLogger().info("ChaosPVP is enabling...");
        
        // Initialize managers
        initializeManagers();
        
        // Load data from file
        dataManager.loadData();
        
        // Initialize handlers
        initializeHandlers();
        
        // Register event listeners
        registerEventListeners();
        
        // Register commands
        registerCommands();
        
        getLogger().info("ChaosPVP has been enabled successfully!");
    }
    
    @Override
    public void onDisable() {
        getLogger().info("ChaosPVP is disabling...");
        
        // Save data to file
        if (dataManager != null) {
            dataManager.saveData();
        }
        
        // Cleanup class manager (removes scoreboard teams)
        if (classManager != null) {
            classManager.cleanup();
        }
        
        getLogger().info("ChaosPVP has been disabled successfully!");
        instance = null;
    }
    
    /**
     * Initialize all managers
     */
    private void initializeManagers() {
        classManager = new ClassManager();
        cooldownManager = new CooldownManager();
        resourceTracker = new ResourceTracker();
        itemProvider = new ItemProvider(this);
        teamManager = new TeamManager();
        excludeManager = new ExcludeManager();
        mapManager = new MapManager();
        lobbyManager = new LobbyManager();
        gameManager = new GameManager(this, classManager, teamManager);
        cooldownDisplay = new CooldownDisplay(this, cooldownManager);
        
        // Initialize DataManager (must be after MapManager and LobbyManager)
        dataManager = new DataManager(this, mapManager, lobbyManager);
        
        // Set ItemProvider in ClassManager
        classManager.setItemProvider(itemProvider);
        
        // Set TeamManager and GameManager in ClassManager (for ClassTagManager)
        classManager.setManagers(teamManager, gameManager);
        
        // Set ExcludeManager, MapManager, and LobbyManager in GameManager
        gameManager.setManagers(excludeManager, mapManager, lobbyManager);
        
        getLogger().info("Managers initialized");
    }
    
    /**
     * Initialize all handlers
     */
    private void initializeHandlers() {
        damageHandler = new DamageHandler();
        effectApplicator = new EffectApplicator(this);
        
        swordsmanHandler = new SwordsmanHandler(this, classManager, gameManager, cooldownManager, 
            resourceTracker, itemProvider, damageHandler);
        
        scientistHandler = new ScientistHandler(this, classManager, 
            resourceTracker, itemProvider, effectApplicator, damageHandler, teamManager, gameManager);
        
        plagueSpreaderHandler = new PlagueSpreaderHandler(this, classManager, cooldownManager, 
            itemProvider, effectApplicator, damageHandler, teamManager, gameManager);
        
        shieldSoldierHandler = new ShieldSoldierHandler(this, classManager, cooldownManager, 
            itemProvider, effectApplicator, damageHandler);
        
        criticalCutterHandler = new CriticalCutterHandler(this, classManager, cooldownManager,
            itemProvider, damageHandler);
        
        navigatorHandler = new NavigatorHandler(this, classManager, cooldownManager, 
            itemProvider, effectApplicator, damageHandler);
        
        captainHandler = new CaptainHandler(this, classManager, cooldownManager, 
            itemProvider, teamManager, gameManager, effectApplicator, damageHandler);
        
        shapeshifterHandler = new ShapeshifterHandler(this, classManager, gameManager,
            cooldownManager, damageHandler);
        
        jugglerHandler = new JugglerHandler(this, classManager, cooldownManager,
            itemProvider, damageHandler, teamManager, gameManager);
        
        dragonFuryHandler = new DragonFuryHandler(this, classManager, gameManager,
            cooldownManager, damageHandler, teamManager);
        
        undeadHandler = new UndeadHandler(this, classManager, gameManager, damageHandler);
        
        stamperHandler = new StamperHandler(this, classManager, cooldownManager,
            itemProvider, damageHandler, teamManager, gameManager);
        
        timeEngraverHandler = new TimeEngraverHandler(this, classManager, cooldownManager,
            itemProvider, damageHandler, effectApplicator, teamManager, gameManager);
        
        worldProtectionHandler = new WorldProtectionHandler(gameManager);
        
        teamKillPreventionHandler = new TeamKillPreventionHandler(gameManager, teamManager);
        
        playerDeathHandler = new PlayerDeathHandler(gameManager);
        
        playerJoinHandler = new PlayerJoinHandler(gameManager, classManager);
        
        playerQuitHandler = new PlayerQuitHandler(this, classManager);
        
        freezeProtectionHandler = new FreezeProtectionHandler(gameManager);
        
        playerRespawnHandler = new PlayerRespawnHandler(gameManager);
        
        classSelectionGUI = new ClassSelectionGUI(this, classManager);
        teamSelectionGUI = new TeamSelectionGUI(teamManager, classSelectionGUI, excludeManager);
        
        // Initialize DraftPickManager
        draftPickManager = new DraftPickManager(this, teamManager, classManager, classSelectionGUI);
        
        // Set GameManager in DraftPickManager
        draftPickManager.setGameManager(gameManager);
        
        // Set handlers in ClassManager
        classManager.setHandlers(scientistHandler, plagueSpreaderHandler,
            shieldSoldierHandler, navigatorHandler, criticalCutterHandler,
            shapeshifterHandler, jugglerHandler, dragonFuryHandler, undeadHandler,
            swordsmanHandler, stamperHandler, timeEngraverHandler);
        
        getLogger().info("Handlers initialized");
    }
    
    /**
     * Register all event listeners
     */
    private void registerEventListeners() {
        Bukkit.getPluginManager().registerEvents(swordsmanHandler, this);
        Bukkit.getPluginManager().registerEvents(scientistHandler, this);
        Bukkit.getPluginManager().registerEvents(plagueSpreaderHandler, this);
        Bukkit.getPluginManager().registerEvents(shieldSoldierHandler, this);
        Bukkit.getPluginManager().registerEvents(criticalCutterHandler, this);
        Bukkit.getPluginManager().registerEvents(navigatorHandler, this);
        Bukkit.getPluginManager().registerEvents(captainHandler, this);
        Bukkit.getPluginManager().registerEvents(shapeshifterHandler, this);
        Bukkit.getPluginManager().registerEvents(jugglerHandler, this);
        Bukkit.getPluginManager().registerEvents(dragonFuryHandler, this);
        Bukkit.getPluginManager().registerEvents(undeadHandler, this);
        Bukkit.getPluginManager().registerEvents(stamperHandler, this);
        Bukkit.getPluginManager().registerEvents(timeEngraverHandler, this);
        Bukkit.getPluginManager().registerEvents(worldProtectionHandler, this);
        Bukkit.getPluginManager().registerEvents(teamKillPreventionHandler, this);
        Bukkit.getPluginManager().registerEvents(playerDeathHandler, this);
        Bukkit.getPluginManager().registerEvents(playerJoinHandler, this);
        Bukkit.getPluginManager().registerEvents(playerQuitHandler, this);
        Bukkit.getPluginManager().registerEvents(freezeProtectionHandler, this);
        Bukkit.getPluginManager().registerEvents(playerRespawnHandler, this);
        Bukkit.getPluginManager().registerEvents(classSelectionGUI, this);
        Bukkit.getPluginManager().registerEvents(teamSelectionGUI, this);
        
        getLogger().info("Event listeners registered");
    }
    
    /**
     * Register all commands
     */
    private void registerCommands() {
        ClassCommand classCommand = new ClassCommand(classManager, classSelectionGUI);
        getCommand("chaospvp").setExecutor(classCommand);
        getCommand("chaospvp").setTabCompleter(classCommand);
        
        StartGameCommand startGameCommand = new StartGameCommand(this, classSelectionGUI, 
            teamSelectionGUI, gameManager, excludeManager, mapManager, lobbyManager, draftPickManager);
        getCommand("게임시작").setExecutor(startGameCommand);
        
        // Set StartGameCommand in TeamSelectionGUI
        teamSelectionGUI.setStartGameCommand(startGameCommand);
        
        // Set DraftPickManager in ClassSelectionGUI
        classSelectionGUI.setDraftPickManager(draftPickManager);
        
        EndGameCommand endGameCommand = new EndGameCommand(classManager, gameManager, teamManager);
        getCommand("게임종료").setExecutor(endGameCommand);
        
        GameModeCommand gameModeCommand = new GameModeCommand(gameManager);
        getCommand("게임모드").setExecutor(gameModeCommand);
        
        PracticeModeStartCommand practiceModeStartCommand = new PracticeModeStartCommand(this, classSelectionGUI, classManager, gameManager);
        getCommand("연습모드시작").setExecutor(practiceModeStartCommand);
        
        HelpCommand helpCommand = new HelpCommand();
        getCommand("도움말").setExecutor(helpCommand);
        
        ExcludeCommand excludeCommand = new ExcludeCommand(excludeManager);
        getCommand("인원제외").setExecutor(excludeCommand);
        
        IncludeCommand includeCommand = new IncludeCommand(excludeManager);
        getCommand("인원추가").setExecutor(includeCommand);
        
        LobbyCommand lobbyCommand = new LobbyCommand(lobbyManager);
        getCommand("로비").setExecutor(lobbyCommand);
        
        LobbyRemoveCommand lobbyRemoveCommand = new LobbyRemoveCommand(lobbyManager);
        getCommand("로비제거").setExecutor(lobbyRemoveCommand);
        
        MapSetCommand mapSetCommand = new MapSetCommand(mapManager);
        getCommand("맵지정").setExecutor(mapSetCommand);
        
        MapRemoveCommand mapRemoveCommand = new MapRemoveCommand(mapManager);
        getCommand("맵제거").setExecutor(mapRemoveCommand);
        
        ParticleCommand particleCommand = new ParticleCommand();
        getCommand("입자끄기").setExecutor(particleCommand);
        
        PracticeModeCommand practiceModeCommand = new PracticeModeCommand(mapManager, classSelectionGUI, classManager, gameManager);
        getCommand("연습모드").setExecutor(practiceModeCommand);
        
        PracticeModeEndCommand practiceModeEndCommand = new PracticeModeEndCommand(classManager, gameManager, this);
        getCommand("연습모드종료").setExecutor(practiceModeEndCommand);
        
        PracticeModeClassCommand practiceModeClassCommand = new PracticeModeClassCommand(classSelectionGUI, gameManager);
        getCommand("연습클래스").setExecutor(practiceModeClassCommand);
        
        getLogger().info("Commands registered");
    }
    
    /**
     * Get the plugin instance
     * @return The ChaosPVP plugin instance
     */
    public static VerminPVP getInstance() {
        return instance;
    }
    
    // Getters for managers and handlers
    
    public ClassManager getClassManager() {
        return classManager;
    }
    
    public CooldownManager getCooldownManager() {
        return cooldownManager;
    }
    
    public ResourceTracker getResourceTracker() {
        return resourceTracker;
    }
    
    public ItemProvider getItemProvider() {
        return itemProvider;
    }
    
    public TeamManager getTeamManager() {
        return teamManager;
    }
    
    public GameManager getGameManager() {
        return gameManager;
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    public CooldownDisplay getCooldownDisplay() {
        return cooldownDisplay;
    }
    
    public PlagueSpreaderHandler getPlagueSpreaderHandler() {
        return plagueSpreaderHandler;
    }
    
    public ShieldSoldierHandler getShieldSoldierHandler() {
        return shieldSoldierHandler;
    }
    
    public ShapeshifterHandler getShapeshifterHandler() {
        return shapeshifterHandler;
    }
    
    public JugglerHandler getJugglerHandler() {
        return jugglerHandler;
    }
    
    public UndeadHandler getUndeadHandler() {
        return undeadHandler;
    }
    
    public StamperHandler getStamperHandler() {
        return stamperHandler;
    }
    
    public TimeEngraverHandler getTimeEngraverHandler() {
        return timeEngraverHandler;
    }
}
