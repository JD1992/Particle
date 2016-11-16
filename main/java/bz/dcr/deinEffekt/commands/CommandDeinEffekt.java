package bz.dcr.deinEffekt.commands;

import bz.dcr.deinEffekt.DeinEffekt;
import bz.dcr.deinEffekt.util.ParticleObject;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Command /deineffekt mit Subcommands:
 * - Erstellung
 * - Veränderung
 * - Anzeige Item Veränderung
 * - Löschung
 * - Aktivierung
 * - Deaktivierung
 * - Reloaden der Config
 */
public class CommandDeinEffekt implements CommandExecutor {
	
	private final DeinEffekt plugin;
	
	public CommandDeinEffekt ( DeinEffekt plugin ) {
		this.plugin = plugin;
	}
	
	/**
	 * Hauptcommand mit switch der Subcommands
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param command       Ausgeführter command
	 * @param label         Aliases des commands
	 * @param args          Argumente die an den Command angehängt wurden
	 *
	 * @return Boolean der wiedergibt ob die Ausführung erfolgreich war
	 */
	@Override
	public boolean onCommand ( CommandSender commandSender, Command command, String label, String[] args ) {
		
		if ( ! ( commandSender.hasPermission( "permission.admin" ) ) ) {
			plugin.sendConfigString( commandSender, "message.noPermission" );
			return true;
		}
		if ( args.length == 0 ) {
			plugin.sendConfigString( commandSender, "message.command" );
			return true;
		}
		switch ( args[ 0 ].toLowerCase() ) {
			case "reload":
				plugin.reload( commandSender );
				break;
			case "setitem":
				plugin.mongoHandler.updateEffektItem( commandSender, args );
				break;
			case "activate":
				plugin.mongoHandler.activateEffekt( commandSender, args );
				break;
			case "deactivate":
				plugin.mongoHandler.deactivateEffekt( commandSender, args );
				break;
			case "create":
				createEffekt( commandSender, args, false );
				break;
			case "update":
				createEffekt( commandSender, args, true );
				break;
			case "delete":
				plugin.mongoHandler.deleteEffekt( commandSender, args );
				break;
		}
		return true;
	}
	
	/**
	 * Kontrollstruktur die bei "create" und "update" überprüft ob die Eingaben vom Typ her korrekt sind.
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param args          Argumente die an den Command angehängt wurden (zu überprüfen)
	 * @param update        Boolean der zwischen create und update unterscheidet
	 */
	private void createEffekt ( CommandSender commandSender, String[] args, boolean update ) {
		if ( ! ( commandSender instanceof Player ) ) { return; }
		Player player = ( Player ) commandSender;
		
		Particle particle;
		ParticleObject particleObject;
		ParticleObject particleObj;
		Double x, y, z, xd, yd, zd, extra;
		int count, delay, cost;
		int id;
		
		try {
			x = Double.parseDouble( args[ 2 ] );
			y = Double.parseDouble( args[ 3 ] );
			z = Double.parseDouble( args[ 4 ] );
			
			xd = Double.parseDouble( args[ 5 ] );
			yd = Double.parseDouble( args[ 6 ] );
			zd = Double.parseDouble( args[ 7 ] );
			
			extra = Double.parseDouble( args[ 8 ] );
			count = Integer.parseInt( args[ 9 ] );
			
			delay = Integer.parseInt( args[ 10 ] );
			cost = Integer.parseInt( args[ 11 ] );
			if ( update ) {
				id = Integer.parseInt( args[ 1 ] );
				particleObj = getParticle( id );
				if ( particleObj == null ) {
					throw new NullPointerException( "Diesen Partikel gibt es nicht" );
				}
				particleObject = new ParticleObject( particleObj.getParticle(), x, y, z, xd, yd, zd, count, extra, delay, cost,
						false, particleObj.getItemStack(), particleObj.getDbID() );
				
			} else {
				particle = Particle.valueOf( args[ 1 ] );
				Material material = Material.DIRT;
				String name = "§5TEST: " + particle.toString();
				int amount = 1;
				short damage = 0;
				
				ItemStack itemStack = new ItemStack( material, amount, damage );
				ItemMeta itemMeta = itemStack.getItemMeta();
				itemMeta.setDisplayName( name );
				itemStack.setItemMeta( itemMeta );
				
				particleObject = new ParticleObject( particle, x, y, z, xd, yd, zd, count, extra, delay, cost, false, itemStack, - 1 );
			}
			plugin.mongoHandler.insertEffekt( player, particleObject, update );
		} catch ( Exception ex ) {
			if ( update ) {
				commandSender.sendMessage(
						plugin.prefix + "§4/deineffekt update <ID> <x y z> " +
						"<xd yd zd> <Geschwindigkeit> <Anzahl> <Verzögerung> <Kosten> §c§loder" );
				commandSender.sendMessage(
						plugin.prefix + "§4/deineffekt update <ID> <x y z> " +
						"<R G B> <Helligkeit> 0 <Verzögerung> <Kosten>" );
				commandSender.sendMessage(
						plugin.prefix + "§4Stelle sicher das es diese ID wirklich gibt." );
			} else {
				commandSender.sendMessage(
						plugin.prefix + "§4/deineffekt create <Partikelname> <x y z> " +
						"<xd yd zd> <Geschwindigkeit> <Anzahl> <Verzögerung> <Kosten> §c§loder" );
				commandSender.sendMessage(
						plugin.prefix + "§4/deineffekt create <Partikelname> <x y z> " +
						"<R G B> <Helligkeit> 0 <Verzögerung> <Kosten>" );
			}
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Gibt den Particel zur ID zurück
	 *
	 * @param id ID des zu bekommenden Particle
	 *
	 * @return Particle passend zur ID oder null falls nicht vorhanden
	 */
	private ParticleObject getParticle ( int id ) {
		
		for ( ParticleObject entry : plugin.pEffectList ) {
			if ( entry.getDbID() == id ) {
				return entry;
			}
		}
		return null;
	}
	
}
