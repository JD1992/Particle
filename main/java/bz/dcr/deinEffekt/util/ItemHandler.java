package bz.dcr.deinEffekt.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Hilfsmethoden zur einfachen Erstellung von Itemstack
 */
public class ItemHandler {
	
	/**
	 * Item ohne Lore
	 * @param material Material des Items
	 * @param name Name des Items
	 * @param damage Damagewert des Items
	 * @param count Anzahl der Items
	 * @return Itemstack mit gesetzten Werten
	 */
	public static ItemStack getItem ( Material material, String name, int damage, int count ) {
		ItemStack itemStack = new ItemStack( material, count, ( short ) damage );
		ItemMeta  itemMeta  = itemStack.getItemMeta();
		itemMeta.setDisplayName( name );
		itemStack.setItemMeta( itemMeta );
		
		return itemStack;
	}
	
	/**
	 * Item mit Lore
	 * @param material Material des Items
	 * @param name Name des Items
	 * @param lore Zeilen der Lore in Listenform
	 * @param damage Damagewert des Items
	 * @param count Anzahl der Items
	 * @return Itemstack mit gesetzten Werten
	 */
	static ItemStack getItem ( Material material, String name, ArrayList < String > lore, int damage, int count ) {
		ItemStack itemStack = new ItemStack( material, count, ( short ) damage );
		ItemMeta  itemMeta  = itemStack.getItemMeta();
		itemMeta.setDisplayName( name );
		itemMeta.setLore( lore );
		itemStack.setItemMeta( itemMeta );
		
		return itemStack;
	}

}
