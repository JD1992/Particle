package bz.dcr.deinEffekt;

import bz.dcr.deinEffekt.commands.CommandDeinEffekt;
import bz.dcr.deinEffekt.commands.CommandEffekte;
import bz.dcr.deinEffekt.events.InventoryClickListener;
import bz.dcr.deinEffekt.events.NPCClickListener;
import bz.dcr.deinEffekt.events.PlayerJoinListener;
import bz.dcr.deinEffekt.util.MongoHandler;
import bz.dcr.deinEffekt.util.ParticleObject;
import javafx.util.Pair;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Hauptklasse mit allem relevanten zum Betrieb des Plugins
 */
public class DeinEffekt extends JavaPlugin {
	
	/**
	 * TODO
	 * + Delay in Runnable (Particle abhängig -> bei Erstellung)
	 * + onEnable -> Effekte auslesen und zwischenspeichern
	 * + Spielerbefehl (/effekte)
	 * + Inventar um zu wählen
	 * +> Anzeige
	 * +> Klickbar
	 * +> Umschaltung der Auswahl
	 * +> Admin alle Effekte/Spieler nur freigeschaltete
	 * + Spieler in Hashmap (Join Event)
	 * + Spieler aus Hashmap (Runnable Online Check)
	 * + Datenbank (MONGOAPI)
	 * +> Effekte Eintragen
	 * +> Effekte Item setzen
	 * +> Effekte Auslesen
	 * +> Updaten
	 * +> Löschen
	 * + Itemstack speichern
	 * + Itemstack auslesen
	 * + Permissions für Befehle
	 * + MongoDB auf Config umbauen
	 * + Kaufbar machen
	 * -* Fehler wenn Datenbank nicht eingeschaltet
	 * + Message verarbeitung überarbeiten (alle commands)
	 * + Args verarveitung überarbeiten (alle commands) {args ohne spezifische länge annehmen}
	 * + Gekaufte Effekt aus den Spielerdaten löschen, wenn diese gelöscht wurde
	 */
	public FileConfiguration config;
	
	public String  prefix;
	public Logger  logger;
	public boolean debug;
	
	public Economy economy;
	public boolean economyActive;
	
	private BukkitTask scheduler = null;
	public MongoHandler mongoHandler;
	public HashMap < Player, Pair < ParticleObject, Integer > > pEffect = new HashMap <>();
	
	public ArrayList < ParticleObject > pEffectList = new ArrayList <>();
	
	/**
	 * Methode bei deaktivierung des Plugins
	 */
	@Override
	public void onDisable () {
		scheduler.cancel();
		mongoHandler.close();
		logger.info( "Das Plugin wurde deaktiviert." );
	}
	
	/**
	 * Methode bei aktivierung des Plugins
	 */
	@Override
	public void onEnable () {
		initPlugin();
	}
	
	/**
	 * Grundsetzliche Punkte für den Plugin Start abarbeiten
	 */
	private void initPlugin () {
		initConfig();
		
		prefix = ChatColor.translateAlternateColorCodes( '&', config.getString( "plugin.userPrefix" ) ) + " ";
		debug = config.getBoolean( "plugin.debug" );
		
		logger = this.getLogger();
		logger.info( "Das Plugin wird aktiviert." );
		
		logger.info( "Verbindung zur Datenbank eingeleitet." );
		initDatabase();
		logger.info( "Registrieren der Commands." );
		initCommands();
		logger.info( "Registrieren der Listener." );
		initListener();
		logger.info( "Scheduler starten." );
		initScheduler();
		logger.info( "Effektliste laden." );
		mongoHandler.getEffektList();
		logger.info( "Economy laden." );
		economyActive = setupEconomy();
	}
	
	/**
	 * Methode zur Initialisierung des Datenbank
	 */
	private void initDatabase () {
		mongoHandler = new MongoHandler( this );
	}
	
	/**
	 * Setze der Standard Werte der Config falls nicht vorhanden
	 */
	private void initConfig () {
		config = this.getConfig();
		
		setConfigPermissions();
		setConfigValues();
		setConfigMessages();
		setConfigMongo();
		
		config.options().copyDefaults( true );
		saveConfig();
	}
	
	/**
	 * Config Standard-Werte für Permissions
	 */
	private void setConfigPermissions () {
		config.addDefault( "permission.admin", "deineffekt.admin" );
		config.addDefault( "permission.team", "deineffekt.team" );
		config.addDefault( "permission.vip", "deineffekt.vip" );
	}
	
	/**
	 * Config Standard-Werte für Einstellungs-/Anzeigen-/Funktionswerten
	 */
	private void setConfigValues () {
		config.addDefault( "plugin.db.lastCreated", 0 );
		
		config.addDefault( "plugin.names.npc", "Effektshop" );
		config.addDefault( "plugin.names.buyInventory", "&5Effekte kaufen" );
		config.addDefault( "plugin.names.buyAcceptInventory", "&5Bestätigen" );
		config.addDefault( "plugin.names.selectionInventory", "&5Effekte" );
		
		config.addDefault( "plugin.items.on", "&a&lAn" );
		config.addDefault( "plugin.items.off", "&c&lAus" );
		config.addDefault( "plugin.items.bought", "&a&lGekauft" );
		config.addDefault( "plugin.items.forSale", "&e&lKaufen" );
		config.addDefault( "plugin.items.vipOnly", "&6&lVIP" );
		config.addDefault( "plugin.items.buy", "&2&lKaufen" );
		config.addDefault( "plugin.items.decline", "&4&lAbbrechen" );
		config.addDefault( "plugin.items.deactivated", "&c&lDeaktiviert" );
		
		config.addDefault( "plugin.items.price.color", "&6" );
		config.addDefault( "plugin.items.price.currency", "DM" );
		
		
		config.addDefault( "plugin.userPrefix", "&6&o&ldeinEffekt&8>" );
		config.addDefault( "plugin.consolePrefix", "[deinEffekt]" );
		config.addDefault( "plugin.runnableDelay", 1 );
		config.addDefault( "plugin.debug", false );
	}
	
	/**
	 * Config Standard-Werte für Chatnachrichten
	 */
	private void setConfigMessages () {
		config.addDefault( "message.command", "&4/deineffekt <reload/create/update/delete/setitem/activate/deactivate>" );
		
		config.addDefault( "message.noConsole", "&rDer Befehl darf nur Ingame ausgeführt werden." );
		config.addDefault( "message.noPermission", "&4Du hast keine Berechtigung für diese Aktion." );
		config.addDefault( "message.noEffects", "&4Es stehen derzeit keine Effekte zur Auswahl." );
		config.addDefault( "message.noEffect", "&4Diesen Effekt gibt es nicht." );
		config.addDefault( "message.noEconomy", "&4Es kann nicht auf dein Konto zugegriffen werden. Melde das bitte im Support." );
		
		config.addDefault( "message.error", "&4Es ist ein Fehler aufgetreten, versuche es später erneut." );
		config.addDefault( "message.buyVip", "&4Kaufe dir VIP um alle Effekte nutzen zu können." );
		config.addDefault( "message.startReload", "&4Das Plugin wird neugeladen." );
		config.addDefault( "message.endReload", "&4Das Plugin wurde erfolgreich neugeladen." );
		
		config.addDefault( "message.effectCreated", "&4Ein neuer Effekt wurde erstellt." );
		config.addDefault( "message.effectUpdated", "&4Ein Effekt wurde aktualisiert." );
		config.addDefault( "message.effectDeleted", "&4Ein Effekt wurde entfernt." );
		config.addDefault( "message.effectChosen", "&4Deine Effektauswahl wurde erfolgreich aktualisiert." );
		config.addDefault( "message.effectRemoved", "&4Der Effekte den du gewählt hattest, wurde entfernt oder deaktiviert." );
		
		config.addDefault( "message.alreadyBought", "&4Diesen Effekt hast du bereits gekauft." );
		config.addDefault( "message.bought", "&4Du hast einen neuen Effekt gekauft." );
		config.addDefault( "message.notEnoughMoney", "&4Du hast nicht genug Geld." );
	}
	
	/**
	 * Config Standard-Werte für Datenbank Verbindungsdaten
	 */
	private void setConfigMongo () {
		config.addDefault( "mongo.host", "localhost" );
		config.addDefault( "mongo.port", "27017" );
		config.addDefault( "mongo.username", "" );
		config.addDefault( "mongo.password", "" );
		config.addDefault( "mongo.dbName", "effektDB" );
	}

	/**
	 * Initialisierung des Runnable
	 */
	private void initScheduler () {
		if ( scheduler != null ) {
			scheduler.cancel();
		}
		scheduler = Bukkit.getScheduler().runTaskTimerAsynchronously( this,
				new RunnableEffekt( this ), 20 * 2,
				config.getInt( "plugin.runnableDelay" ) );
	}
	
	/**
	 * Initialisierung der Kommands
	 */
	private void initCommands () {
		this.getCommand( "deineffekt" ).setExecutor( new CommandDeinEffekt( this ) );
		this.getCommand( "effekte" ).setExecutor( new CommandEffekte( this ) );
	}
	
	/**
	 * Initialisierung der Events
	 */
	private void initListener () {
		this.getServer().getPluginManager().registerEvents( new PlayerJoinListener( this ), this );
		this.getServer().getPluginManager().registerEvents( new InventoryClickListener( this ), this );
		this.getServer().getPluginManager().registerEvents( new NPCClickListener( this ), this );
	}
	
	/**
	 * Initialisierung der Economy/Vault Schnittstelle
	 */
	private boolean setupEconomy () {
		RegisteredServiceProvider < Economy > economyProvider = getServer().getServicesManager().getRegistration(
				net.milkbowl.vault.economy.Economy.class );
		if ( economyProvider != null ) {
			economy = economyProvider.getProvider();
		}
		return ( economy != null );
	}
	
	/**
	 * Config neuladen
	 */
	public void reload ( CommandSender sender ) {
		sendConfigString( sender, "message.startReload" );
		this.reloadConfig();
		this.saveConfig();
		Bukkit.getPluginManager().disablePlugin( this );
		Bukkit.getPluginManager().enablePlugin( this );
		mongoHandler.loadChosenEffects();
		sendConfigString( sender, "message.endReload" );
	}
	
	/**
	 * Strings aus der Config senden
	 */
	public void sendConfigString ( CommandSender sender, String node ) {
		String message = ChatColor.translateAlternateColorCodes( '&', this.config.getString( node ) );
		if ( sender instanceof Player ) {
			sender.sendMessage( prefix + message );
			return;
		}
		logger.info( ChatColor.stripColor( message ) );
	}
	
	/**
	 * Rückgabe von Werten aus der Config
	 */
	public String getConfigValue ( String node ) {
		return ChatColor.translateAlternateColorCodes( '&', this.config.getString( node ) );
	}
	
	/**
	 * Rückgabe von Permissions aus der config
	 */
	public String getConfigPermission ( String node ) {
		return this.config.getString( node );
	}
	
	/**
	 * Nachrichten senden die angepasst sind an das Plugin Layout
	 */
	public void sendPluginMessage ( CommandSender sender, String msg ) {
		String message = ChatColor.translateAlternateColorCodes( '&', msg );
		if ( sender instanceof Player ) {
			sender.sendMessage( prefix + message );
			return;
		}
		logger.info( ChatColor.stripColor( message ) );
	}
	
	
}