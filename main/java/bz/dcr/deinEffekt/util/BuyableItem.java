package bz.dcr.deinEffekt.util;

import org.bukkit.inventory.ItemStack;

/**
 * Klasse für Vererbung von Felder/Methoden für Kaufbare Einträge
 */
class BuyableItem {
	
	int     cost   = 0;
	boolean active = false;
	
	boolean isActive () {
		return active;
	}
	
	ItemStack itemStack = null;
	
	public int getCost () {
		return cost;
	}
	
	public ItemStack getItemStack () {
		return itemStack;
	}
	
}
