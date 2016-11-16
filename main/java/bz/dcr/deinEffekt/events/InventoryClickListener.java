package bz.dcr.deinEffekt.events;

import bz.dcr.deinEffekt.DeinEffekt;
import bz.dcr.deinEffekt.util.ItemHandler;
import bz.dcr.deinEffekt.util.ParticleObject;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

/**
 * Listener für Inventarclicks in Kauf-/Auswahlinventar
 */
public class InventoryClickListener implements Listener {
	
	private final DeinEffekt plugin;
	private       Player     player;
	
	public InventoryClickListener ( DeinEffekt plugin ) {
		this.plugin = plugin;
	}
	
	/**
	 * Überprüfen ob angeklicktes Inventar eines der Richtigen ist und welches
	 *
	 * @param event InventoryClickEvent
	 */
	@EventHandler
	public void onInventoryClick ( InventoryClickEvent event ) {
		Inventory inventory = event.getClickedInventory();
		if ( inventory == null ) {
			event.setCancelled( true );
			return;
		}
		if ( event.getWhoClicked() instanceof Player ) { player = ( Player ) event.getWhoClicked(); }
		String inventoryName = player.getOpenInventory().getTopInventory().getTitle();
		if ( inventoryName.equals( plugin.getConfigValue( "plugin.names.buyInventory" ) ) ||
		     inventoryName.equals( plugin.getConfigValue( "plugin.names.buyAcceptInventory" ) ) ||
		     inventoryName.equals( plugin.getConfigValue( "plugin.names.selectionInventory" ) ) ) {
			event.setCancelled( true );
			
			if ( inventory.getTitle().equals( plugin.getConfigValue( "plugin.names.buyInventory" ) ) ) {
				buyInventory( event );
				return;
			}
			if ( inventory.getTitle().equals( plugin.getConfigValue( "plugin.names.buyAcceptInventory" ) ) ) {
				buyAcceptInventory( event );
				return;
			}
			if ( inventory.getTitle().equals( plugin.getConfigValue( "plugin.names.selectionInventory" ) ) ) {
				selectionInventory( event );
			}
		}
	}

	/**
	 * Methode zum Aufruf des Auswahl-Inventares
	 *
	 * @param event InventoryClickEvent
	 */
	
	private void selectionInventory ( InventoryClickEvent event ) {
		event.setCancelled( true );
		if ( checkVipEffekt( event ) ) { return; }
		ItemStack current = event.getCurrentItem();
		if ( current.getType() == Material.SULPHUR ||
		     current.getType() == Material.STAINED_GLASS_PANE ||
		     current.getType() == Material.INK_SACK ) { return; }
		if ( current.hasItemMeta() ) {
			event.setCurrentItem( new ItemStack( Material.AIR ) );
			plugin.mongoHandler.updateChosenEffekt( player, current.getItemMeta().getDisplayName() );
		}
	}
	
	/**
	 * Methode zum Aufruf des Kauf-Inventares
	 *
	 * @param event InventoryClickEvent
	 */
	private void buyInventory ( InventoryClickEvent event ) {
		if ( checkVipEffekt( event ) ) { return; }
		if ( event.getRawSlot() < 9 ) {
			int checkID = event.getRawSlot() + 9;
			ItemStack checkStack = event.getClickedInventory().getItem( checkID );
			if ( checkStack.getType() == Material.INK_SACK && checkStack.getDurability() == 10 ) {
				player.closeInventory();
				plugin.sendConfigString( player, "message.alreadyBought" );
				return;
			}
		}
		if ( event.getCurrentItem().getType() == Material.INK_SACK ||
		     event.getCurrentItem().getType() == Material.STAINED_GLASS_PANE ) { return; }
		openBuyInventory( event.getCurrentItem() );
	}
	
	/**
	 * Methode zum Aufruf des Kaufbestätigungs-Inventares
	 *
	 * @param event InventoryClickEvent
	 */
	private void buyAcceptInventory ( InventoryClickEvent event ) {
		if ( event.getCurrentItem().getDurability() == 14 ) {
			player.closeInventory();
			return;
		}
		if ( event.getCurrentItem().getDurability() == 5 ) {
			ItemStack item = event.getInventory().getItem( 2 );
			int id = getID( item.getItemMeta().getDisplayName() );
			ParticleObject effekt = getParticle( id );
			
			if ( effekt != null ) {
				if ( plugin.economy.has( player, effekt.getCost() ) ) {
					plugin.economy.withdrawPlayer( player, effekt.getCost() );
					plugin.mongoHandler.buyEffekt( player, id );
				} else {
					plugin.sendConfigString( player, "message.notEnoughMoney" );
				}
			} else {
				plugin.sendConfigString( player, "message.error" );
			}
		}
		player.closeInventory();
	}
	
	/**
	 * Überprüfen ob der Effekt nur für VIP verfügbar
	 *
	 * @param event InventoryClickEvent
	 */
	private boolean checkVipEffekt ( InventoryClickEvent event ) {
		if ( event.getRawSlot() < 9 ) {
			if ( event.getClickedInventory().getItem( event.getRawSlot() + 9 ).getType() == Material.SULPHUR ) {
				player.closeInventory();
				plugin.sendConfigString( player, "message.buyVip" );
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Erstellen des Kaufbestätigungsinventares
	 *
	 * @param item InventoryClickEvent
	 */
	private void openBuyInventory ( ItemStack item ) {
		Inventory buyInventory = Bukkit.createInventory( null, InventoryType.HOPPER,
				plugin.getConfigValue( "plugin.names.buyAcceptInventory" ) );
		
		ItemStack buy = ItemHandler.getItem( Material.WOOL, plugin.getConfigValue( "plugin.items.buy" ), 5, 1 );
		ItemStack decline = ItemHandler.getItem( Material.WOOL, plugin.getConfigValue( "plugin.items.decline" ), 14, 1 );
		ParticleObject effekt = getParticle( getID( item.getItemMeta().getDisplayName() ) );
		if ( effekt == null ) {
			player.closeInventory();
			plugin.sendConfigString( player, "message.error" );
			return;
		}
		ItemStack itemstack = effekt.getItemStack();
		ArrayList < String > lore = new ArrayList <>();
		lore.add( plugin.getConfigValue( "plugin.items.price.color" ) +
		          effekt.getCost() + " " +
		          plugin.getConfigValue( "plugin.items.price.currency" ) );
		ItemMeta itemMeta = itemstack.getItemMeta();
		itemMeta.setLore( lore );
		itemstack.setItemMeta( itemMeta );
		buyInventory.setItem( 0, buy );
		buyInventory.setItem( 1, ItemHandler.getItem( Material.STAINED_GLASS_PANE, " ", 15, 1 ) );
		buyInventory.setItem( 2, itemstack );
		buyInventory.setItem( 3, ItemHandler.getItem( Material.STAINED_GLASS_PANE, " ", 15, 1 ) );
		buyInventory.setItem( 4, decline );
		
		player.openInventory( buyInventory );
	}
	
	/**
	 * ParticleObject aus ID erhalten
	 *
	 * @param id InventoryClickEvent
	 *
	 * @return ParticleObject oder null, falls ID nicht vorhanden
	 */
	private ParticleObject getParticle ( int id ) {
		
		for ( ParticleObject entry : plugin.pEffectList ) {
			if ( entry.getDbID() == id ) {
				return entry;
			}
		}
		return null;
	}
	
	/**
	 * Dekodieren der im displayname versteckten ID
	 *
	 * @param displayName DisplayName des angeklicktes Items
	 *
	 * @return dekodierte DatenbankID
	 */
	private int getID ( String displayName ) {
		String sid;
		int id = - 1;
		try {
			int control = Integer.parseInt( displayName.substring( 1, 2 ) ) * 2;
			sid = displayName.substring( displayName.length() - control, displayName.length() ).replace( "§", "" );
			id = Integer.parseInt( sid );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
			plugin.sendConfigString( player, "message.error" );
			return id;
		}
		return id;
	}
	
}