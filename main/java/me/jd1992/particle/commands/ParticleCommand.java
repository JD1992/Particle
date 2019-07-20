package me.jd1992.particle.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Command /effekte: Öffnung der Effektauswahl
 */
public class ParticleCommand implements CommandExecutor {
	
	private final me.jd1992.particle.Particle plugin;
	
	public ParticleCommand (me.jd1992.particle.Particle plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand ( CommandSender commandSender, Command command, String label, String[] args ) {
		if ( args.length == 0 ) {
			if ( ! ( commandSender instanceof Player ) ) {
				plugin.sendConfigString( commandSender, "message.noPermission" );
				return true;
			}
			Player player = ( Player ) commandSender;
			plugin.mongoHandler.openSelectionInventory( player );
			return true;
		}
		plugin.sendPluginMessage( commandSender, "§4/effekte - Öffnet die Effektauswahl" );
		return true;
	}
	
}