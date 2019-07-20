package me.jd1992.particle;

import me.jd1992.particle.util.ParticleObject;
import javafx.util.Pair;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;

/**
 * Runnable zum spawnen der Partikeleffekte
 */
class RunnableEffekt implements Runnable {
	
	private final Particle plugin;
	
	RunnableEffekt ( Particle plugin) {
		this.plugin = plugin;
	}
	
	/**
	 * Run-Methode des Runnable
	 */
	@Override
	public void run () {
		for ( Map.Entry < Player, Pair < ParticleObject, Integer > > entry : new HashMap <>( plugin.pEffect ).entrySet() ) {
			Player player = entry.getKey();
			if ( ! player.isOnline() ) {
				plugin.pEffect.remove( player );
				continue;
			}
			Pair < ParticleObject, Integer > particelDelay = entry.getValue();
			ParticleObject particel = particelDelay.getKey();
			
			if ( player.getGameMode() == GameMode.SPECTATOR ) { continue; }
			if ( player.hasPotionEffect( PotionEffectType.INVISIBILITY ) ) { continue; }
			if ( particel == null || ! ( plugin.pEffectList.contains( particel ) ) ) {
				plugin.pEffect.remove( player );
				continue;
			}
			
			int delay = particelDelay.getValue();
			int runnableDelay = plugin.config.getInt( "plugin.runnableDelay" );
			if ( delay < particel.getDelay() * runnableDelay ) {
				plugin.pEffect.replace( player, new Pair <>( particel, delay + runnableDelay ) );
				continue;
			}
			particel.spawn( player );
			plugin.pEffect.replace( player, new Pair <>( particel, 0 ) );
		}
	}
}