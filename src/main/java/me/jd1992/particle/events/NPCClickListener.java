package me.jd1992.particle.events;

import me.jd1992.particle.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Event für Klick auf NPC und öffnen des Kaufinventares
 */
public class NPCClickListener implements Listener {
	
	private final Particle plugin;
	
	public NPCClickListener ( Particle plugin) {
		this.plugin = plugin;
	}
	
	@EventHandler
	public void onInteract ( PlayerInteractEntityEvent event ) {
		Player player = event.getPlayer();
		if (!plugin.economyActive ) {
			plugin.sendPluginMessage( player, "message.noEconomy" );
		}
		if ( event.getHand() == EquipmentSlot.HAND ) {
			if ( event.getRightClicked().hasMetadata( "NPC" ) &&
			     event.getRightClicked().getName().equals( plugin.getConfigValue( "plugin.names.npc" ) ) ) {
				plugin.mongoHandler.openBuyInventory( player );
			}
		}
	}
	
}