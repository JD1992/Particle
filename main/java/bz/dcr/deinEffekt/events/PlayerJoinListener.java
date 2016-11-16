package bz.dcr.deinEffekt.events;

import bz.dcr.deinEffekt.DeinEffekt;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

/**
 * PlayerJoinListener für Aktivierung der Partikeleffekte
 */
public class PlayerJoinListener implements Listener {
	
	private final DeinEffekt plugin;
	
	public PlayerJoinListener ( DeinEffekt plugin ) {
		this.plugin = plugin;
	}
	
	/**
	 * Event mit Aufruf der #getEffekt() Methode aus der MongoHandler-Klasse
	 * Überprüfung ob Effekte vorhanden, gewähler Effekt(aus DB) nutzbar und hinzufügen zur Hashmap für das Spawnen der Effekte
	 * @param event PlayerJoinEvent
	 */
	@EventHandler
	public void onPlayerJoin( PlayerJoinEvent event) {
		plugin.mongoHandler.getEffekt( event.getPlayer() );
	}
	
}
