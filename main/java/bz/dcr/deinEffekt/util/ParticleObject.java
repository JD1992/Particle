package bz.dcr.deinEffekt.util;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * Klasse zur Erstellung neuer ParticleObjecte
 */
public class ParticleObject extends BuyableItem {
	
	private Particle particle;
	
	private double x;
	private double y;
	private double z;
	
	private int count;
	
	private double offsetX;
	private double offsetY;
	private double offsetZ;
	
	private double extra;
	private int    delay;
	
	private int dbID;
	
	public ParticleObject (
			Particle particle,
			double x, double y, double z,
			double offsetX, double offsetY, double offsetZ,
			int count, double extra, int delay, int cost,
			boolean active,
			ItemStack itemStack,
			int dbID) {
		this.particle = particle;
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		this.count = count;
		
		this.offsetX = offsetX;
		this.offsetY = offsetY;
		this.offsetZ = offsetZ;
		
		this.extra = extra;
		this.delay = delay;
		this.cost = cost;
		this.active = active;
		
		this.itemStack = itemStack;
		this.dbID = dbID;
		
	}
	
	public void spawn ( Player player ) {
		Location ploc = player.getLocation();
		player.getWorld().spawnParticle(
				this.getParticle(),
				
				this.getX() + ploc.getX(),
				this.getY() + ploc.getY(),
				this.getZ() + ploc.getZ(),
				
				this.getCount(),
				
				this.getOffsetX(),
				this.getOffsetY(),
				this.getOffsetZ(),
				
				this.getExtra()
		);
	}
	
	public Particle getParticle () {
		return particle;
	}
	
	double getX () {
		return x;
	}
	
	double getY () {
		return y;
	}
	
	double getZ () {
		return z;
	}
	
	int getCount () {
		return count;
	}
	
	double getOffsetX () {
		return offsetX;
	}
	
	double getOffsetY () {
		return offsetY;
	}
	
	double getOffsetZ () {
		return offsetZ;
	}
	
	double getExtra () {
		return extra;
	}
	
	public int getDelay () {
		return delay;
	}
	
	public int getDbID () {
		return dbID;
	}
	
}
