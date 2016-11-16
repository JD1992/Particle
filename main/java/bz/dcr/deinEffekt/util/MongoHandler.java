package bz.dcr.deinEffekt.util;

import bz.dcr.deinEffekt.DeinEffekt;
import com.mongodb.Block;
import com.mongodb.ConnectionString;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.MongoClient;
import com.mongodb.async.client.MongoClients;
import com.mongodb.async.client.MongoCollection;
import com.mongodb.async.client.MongoDatabase;
import javafx.util.Pair;
import org.bson.Document;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.CountDownLatch;

import static com.mongodb.client.model.Filters.eq;

/**
 * - Klasse die alles beinhaltet, das mit der MongoDatenbank agiert
 * - angehängt sind Hilfsmethoden die an mehreren Stellen genutzt werden
 */
public class MongoHandler {
	
	private DeinEffekt    plugin   = null;
	private MongoClient   client   = null;
	private MongoDatabase database = null;
	
	private MongoCollection < Document > effekte = null;
	private MongoCollection < Document > bought  = null;
	
	/**
	 * Initialisierung der MongoHandlerklasse.
	 * - Verbinden -> connect()
	 * - Datenbank bekommen -> getDatabase(Name)
	 * - Collections bekommen -> getCollection(Name)
	 */
	public MongoHandler ( DeinEffekt plugin ) {
		try {
			this.plugin = plugin;
			this.client = this.connect();
			this.database = this.getDatabase( "effektDB" );
			this.effekte = this.getCollection( "effekte" );
			this.bought = this.getCollection( "bought" );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
			plugin.logger.info( "Es ist ein Fehler bei der Verbindung mit der Datenbank aufgetreten." );
			plugin.onDisable();
		}
		
	}
	
	/**
	 * Verbindung mit der MongoDB Datenbank aufbauen.
	 *
	 * @return Verbindung für weitere Aktionen
	 */
	private MongoClient connect () throws Exception {
		String db = "mongodb://";
		String user = plugin.config.getString( "mongo.username" );
		String pass = plugin.config.getString( "mongo.password" );
		String userpass = "";
		String host = plugin.config.getString( "mongo.host" );
		String port = ":" + plugin.config.getString( "mongo.port" );
		String dbName = "/" + plugin.config.getString( "mongo.dbName" );
		
		if ( ! ( user.equals( "" ) ) || ! ( pass.equals( "" ) ) ) {
			userpass = user + ":" + pass;
		}
		
		plugin.logger.info( "======= MongoDB: START =======" );
		MongoClient mongoClient;
		mongoClient = MongoClients.create( new ConnectionString( db + userpass + host + port + dbName ) );
		
		final CountDownLatch latch = new CountDownLatch( 1 );
		
		MongoClient finalMongoClient = mongoClient;
		SingleResultCallback < Void > callbackWhenFinished = ( result, t ) -> {
			plugin.logger.info( "Datenbank erfolgreich geladen." );
			if ( t != null ) {
				plugin.logger.info( "listDatabaseNames() hat einen Fehler erzeugt: " + t.getMessage() );
				finalMongoClient.close();
			}
			latch.countDown();
		};
		
		mongoClient.listDatabaseNames().forEach( s -> plugin.logger.info( "Datenbank geladen: " + s ), callbackWhenFinished );
		latch.await();
		plugin.logger.info( "======= MongoDB: ENDE =======" );
		return mongoClient;
	}
	
	/**
	 * Verbindung mit der MongoDB Datenbank schließen.
	 */
	public void close () {
		client.close();
	}
	
	/**
	 * Anfordern der Datenbank
	 *
	 * @param name Name der anzufordernden Datenbank
	 *
	 * @return Datenbank für weitere Aktionen
	 */
	private MongoDatabase getDatabase ( String name ) {
		return client.getDatabase( name );
	}
	
	/**
	 * Anfordern einer Collection(MYSQL = Tabelle)
	 *
	 * @param name Name der anzufordernden Collection
	 *
	 * @return Collection für weitere Aktionen
	 */
	private MongoCollection < Document > getCollection ( String name ) {
		return this.database.getCollection( name );
	}
	
	/**
	 * Neuladen der Effekte aus der Datenbank und speichern in Hashmap
	 */
	public void loadChosenEffects () {
		plugin.pEffect = new HashMap <>();
		Block < Document > printDocumentBlock = doc -> {
			Player player = Bukkit.getPlayer( UUID.fromString( doc.getString( "uuid" ) ) );
			if ( ! player.isOnline() ) { return; }
			ParticleObject particle = null;
			int id = doc.getInteger( "eChosen" );
			particle = getParticle( id );
			plugin.pEffect.put( player, new Pair <>( particle, 0 ) );
			
		};
		
		SingleResultCallback < Void > callbackWhenFinished = ( result, t ) -> System.out.println( "Spielereffekte erfolgreich geladen." );
		
		this.bought.find().forEach( printDocumentBlock, callbackWhenFinished );
	}
	
	/**
	 * Umwandeln einer Effekt-ID in ein Partikel-Objekt
	 *
	 * @param id Effekt-ID des zu bekommenden Effektes
	 *
	 * @return Effekte der zu holenden ID
	 */
	private ParticleObject getParticle ( int id ) {
		for ( ParticleObject entry : plugin.pEffectList ) {
			if ( entry.getDbID() == id ) { return entry; }
		}
		return null;
	}
	
	/**
	 * Einfügen eines Effektes in die Datenbank
	 *
	 * @param particleObject Objekt der PartikelObject Klasse
	 */
	public void insertEffekt ( Player player, ParticleObject particleObject, boolean update ) {
		if ( update ) {
			ItemStack itemStack = particleObject.getItemStack();
			ItemMeta itemMeta = itemStack.getItemMeta();
			Document ditem = new Document( "material", itemStack.getData().getItemType().toString() )
					.append( "name", itemMeta.getDisplayName() )
					.append( "amount", itemStack.getAmount() )
					.append( "damage", itemStack.getDurability() );
			
			Document document = new Document( "id", particleObject.getDbID() )
					.append( "effekt", particleObject.getParticle().toString() )
					.append( "x", particleObject.getX() )
					.append( "y", particleObject.getY() )
					.append( "z", particleObject.getZ() )
					.append( "xd", particleObject.getOffsetX() )
					.append( "yd", particleObject.getOffsetY() )
					.append( "zd", particleObject.getOffsetZ() )
					.append( "count", particleObject.getCount() )
					.append( "extra", particleObject.getExtra() )
					.append( "delay", particleObject.getDelay() )
					.append( "cost", particleObject.getCost() )
					.append( "active", false )
					.append( "item", ditem );
			System.out.println( "PARTIKEL: " + document );
			System.out.println( "ID: " + particleObject.getDbID() );
			this.effekte.updateOne( eq( "id", particleObject.getDbID() ),
					new Document( "$set", document ),
					( result, t ) -> {
						if ( result.getModifiedCount() == 0 ) {
							plugin.sendPluginMessage( player, "Fehler!" );
						} else {
							plugin.sendConfigString( player, "message.effectUpdated" );
						}
					} );
		} else {
			int id = plugin.config.getInt( "plugin.db.lastCreated" ) + 1;
			plugin.config.set( "plugin.db.lastCreated", id );
			plugin.saveConfig();
			plugin.reloadConfig();
			
			ItemStack itemStack = particleObject.getItemStack();
			ItemMeta itemMeta = itemStack.getItemMeta();
			
			Document ditem = new Document( "material", itemStack.getData().getItemType().toString() )
					.append( "name", itemMeta.getDisplayName() )
					.append( "amount", itemStack.getAmount() )
					.append( "damage", itemStack.getDurability() );
			
			Document document = new Document( "id", id )
					.append( "effekt", particleObject.getParticle().toString() )
					.append( "x", particleObject.getX() )
					.append( "y", particleObject.getY() )
					.append( "z", particleObject.getZ() )
					.append( "xd", particleObject.getOffsetX() )
					.append( "yd", particleObject.getOffsetY() )
					.append( "zd", particleObject.getOffsetZ() )
					.append( "count", particleObject.getCount() )
					.append( "extra", particleObject.getExtra() )
					.append( "delay", particleObject.getDelay() )
					.append( "cost", particleObject.getCost() )
					.append( "active", false )
					.append( "item", ditem );
			this.effekte.insertOne( document,
					( result, t ) -> plugin.sendConfigString( player, "message.effectCreated" ) );
		}
		getEffektList();
	}
	
	/**
	 * Laden der Effekte aus der Datenbank
	 */
	public void getEffektList () {
		plugin.pEffectList = new ArrayList <>();
		Block < Document > printDocumentBlock = doc -> {
			if ( plugin.debug ) { System.out.println( "Effekte: " + doc.toJson() ); }
			Document document = ( Document ) doc.get( "item" );
			Material material = Material.valueOf( document.getString( "material" ) );
			String name = document.getString( "name" );
			int amount = document.getInteger( "amount" );
			short damage = Short.parseShort( document.getInteger( "damage" ).toString() );
			
			ItemStack itemStack = new ItemStack( material, amount, damage );
			ItemMeta itemMeta = itemStack.getItemMeta();
			itemMeta.setDisplayName( name );
			itemStack.setItemMeta( itemMeta );
			
			ParticleObject pEffekt = new ParticleObject(
					Particle.valueOf( doc.getString( "effekt" ) ),
					doc.getDouble( "x" ), doc.getDouble( "y" ), doc.getDouble( "z" ),
					doc.getDouble( "xd" ), doc.getDouble( "yd" ), doc.getDouble( "zd" ),
					doc.getInteger( "count" ), doc.getDouble( "extra" ),
					doc.getInteger( "delay" ), doc.getInteger( "cost" ),
					doc.getBoolean( "active" ),
					itemStack,
					doc.getInteger( "id" )
			);
			plugin.pEffectList.add( pEffekt );
		};
		SingleResultCallback < Void > callbackWhenFinished = ( result, t ) -> {
			System.out.println( "Effekte erfolgreich geladen." );
			loadChosenEffects();
		};
		
		this.effekte.find().forEach( printDocumentBlock, callbackWhenFinished );
	}
	
	/**
	 * Holen des Effektes eines Spielers
	 * Überprüfen ob der Spieler den Effekt gekauft hat
	 *
	 * @param player Spieler dessen Effekt abgefragt wird
	 */
	public void getEffekt ( Player player ) {
		this.bought.find( eq( "uuid", player.getUniqueId().toString() ) ).first( ( doc, tr ) -> {
			if ( doc == null ) { return; }
			int chosen = doc.getInteger( "eChosen" );
			ParticleObject particle = getParticle( chosen );
			checkBought( doc, player, particle );
		} );
	}
	
	/**
	 * Überprüfen ob ein Spieler einen Effekt besitz(neuer Eintrag erstellen falls nicht vorhanden)
	 *
	 * @param doc      Mongo-Dokument mit mit Spieledaten der Datenbank
	 * @param player   Spieler der abfrage
	 * @param particle Gewählter Effekt aus der Datenbank
	 */
	private void checkBought ( Document doc, Player player, ParticleObject particle ) {
		String[] bought;
		String sb = doc.getString( "effekt" );
		int chosen = doc.getInteger( "eChosen" );
		if ( sb.length() > 2 ) {
			bought = sb.split( "," );
		} else {
			bought = ( "-1," + sb ).split( "," );
		}
		if ( player.hasPermission( plugin.getConfigPermission( "permission.admin" ) ) ||
		     player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) ||
		     player.hasPermission( plugin.getConfigPermission( "permission.vip" ) ) ) {
			if ( particle != null ) {
				System.out.println( "TEST3" );
				
				plugin.pEffect.put( player, new Pair <>( particle, 0 ) );
			}
			return;
		}
		String newBought = "-1";
		for ( String itemBought : bought ) {
			for ( ParticleObject particleObject : plugin.pEffectList ) {
				if ( itemBought.equals( String.valueOf( particleObject.getDbID() ) ) ) {
					newBought += "," + itemBought;
				}
			}
		}
		String[] finalNewBought = newBought.split( "," );
		System.out.println( newBought );
		
		Document document = new Document( "name", player.getName() )
				.append( "effekt", newBought );
		this.bought.updateOne( eq( "uuid", player.getUniqueId().toString() ), new Document( "$set", document ), ( result, t ) -> {
			//if ( finalNewBought.contains( String.valueOf( chosen ) ) && particle.isActive() ) {
			if ( Arrays.asList( finalNewBought ).contains( String.valueOf( chosen ) ) && particle.isActive() ) {
				plugin.pEffect.put( player, new Pair <>( particle, 0 ) );
			} else {
				this.bought.updateOne( eq( "uuid", player.getUniqueId().toString() ),
						new Document( "$set", new Document( "eChosen", - 1 ) ),
						( result1, t1 ) -> plugin.sendConfigString( player, "message.effectRemoved" ) );
			}
		} );
	}
	
	/**
	 * @param player      Spieler dessen Effekt abgefragt wird
	 * @param displayName Name des angeklickten Items (mit angehängter ID im ColorCode)
	 *                    Beispiel: §2§5Feuer§1§4
	 *                    §2: Angabe wielange die angehängte ID ist (2 Stellen mal 2 um § mit einzubeziehen = 4 Stellen)
	 *                    §5Feuer: Farbcode und Name des Partikeleffektes
	 *                    §1: ersten beiden Stellen
	 *                    §4: nächsten beiden Stellen -> ohne § ist die ID = 14
	 */
	public void updateChosenEffekt ( Player player, String displayName ) {
		String sid;
		try {
			int control = Integer.parseInt( displayName.substring( 1, 2 ) ) * 2;
			sid = displayName.substring( displayName.length() - control, displayName.length() ).replace( "§", "" );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
			plugin.sendConfigString( player, "message.error" );
			getEffektList();
			return;
		}
		player.playSound( player.getLocation(), Sound.BLOCK_WOOD_BUTTON_CLICK_OFF, 1, 1 );
		player.closeInventory();
		
		this.bought.find( eq( "uuid", player.getUniqueId().toString() ) ).first( ( doc, tr ) -> {
			int id = - 1;
			try {
				id = Integer.parseInt( sid );
			} catch ( Exception ex ) {
				plugin.sendConfigString( player, "message.error" );
				if ( plugin.debug ) { ex.printStackTrace(); }
			}
			if ( doc == null ) {
				if ( ! player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) ) {
					plugin.sendConfigString( player, "message.buyVip" );
					return;
				}
				setPlayerEffekt( player, "- 1" );
			}
			ParticleObject chosenParticle = getParticle( id );
			
			if ( plugin.pEffect.containsKey( player ) ) {
				if ( plugin.pEffect.get( player ).getKey() == chosenParticle ) {
					id = - 1;
				}
			}
			if ( id == - 1 ) {
				plugin.pEffect.remove( player );
			} else {
				if ( plugin.pEffect.containsKey( player ) ) {
					plugin.pEffect.replace( player, new Pair <>( chosenParticle, 0 ) );
				} else {
					plugin.pEffect.put( player, new Pair <>( chosenParticle, 0 ) );
				}
				
			}
			setChosenEffekt( player, String.valueOf( id ) );
		} );
	}
	
	/**
	 * Aktivieren des Effektes
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param args          Übergebene Argumente
	 */
	public void activateEffekt ( CommandSender commandSender, String[] args ) {
		int id = - 1;
		try {
			id = Integer.parseInt( args[ 1 ] );
		} catch ( Exception ex ) {
			plugin.sendPluginMessage( commandSender, "&4/deineffekt activate <ID>" );
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
		if ( id == - 1 ) { return; }
		this.effekte.updateOne( eq( "id", id ), new Document( "$set", new Document( "active", true ) ),
				( result, t ) -> {
					if ( result.getModifiedCount() == 0 ) {
						plugin.sendConfigString( commandSender, "message.noEffect" );
						return;
					}
					plugin.sendConfigString( commandSender, "message.effectUpdated" );
				} );
		getEffektList();
	}
	
	/**
	 * Aktivieren des Effektes
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param args          Übergebene Argumente
	 */
	public void deactivateEffekt ( CommandSender commandSender, String[] args ) {
		int id = - 1;
		try {
			id = Integer.parseInt( args[ 1 ] );
		} catch ( Exception ex ) {
			plugin.sendPluginMessage( commandSender, "&4/deineffekt deactivate <ID>" );
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
		if ( id == - 1 ) { return; }
		this.effekte.updateOne( eq( "id", id ), new Document( "$set", new Document( "active", false ) ),
				( result, t ) -> {
					if ( result.getModifiedCount() == 0 ) {
						plugin.sendConfigString( commandSender, "message.noEffect" );
						return;
					}
					plugin.sendConfigString( commandSender, "message.effectUpdated" );
				} );
		getEffektList();
	}
	
	/**
	 * Update den gewählten Effekt des Spielers
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param chosen        ID des Effekts
	 */
	private void setChosenEffekt ( CommandSender commandSender, String chosen ) {
		try {
			if ( ! ( commandSender instanceof Player ) ) {
				commandSender.sendMessage( plugin.config.getString( "message.noConsole" ) );
				return;
			}
			Player player = ( Player ) commandSender;
			int iChosen = Integer.parseInt( chosen );
			this.bought.updateOne( eq( "uuid", player.getUniqueId().toString() ),
					new Document( "$set", new Document( "eChosen", iChosen ) ),
					( result, t ) -> plugin.sendConfigString( player, "message.effectChosen" ) );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Setzen eines neunen Anzeigeitems für einen Effekt
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param args          Argumente des Kommands
	 */
	public void updateEffektItem ( CommandSender commandSender, String[] args ) {
		try {
			if ( ! ( commandSender instanceof Player ) ) {
				commandSender.sendMessage( plugin.config.getString( "message.noConsole" ) );
				return;
			}
			Player player = ( Player ) commandSender;
			
			String material = Material.valueOf( args[ 2 ] ).toString();
			String name = args[ 3 ].replace( '_', ' ' );
			name = ChatColor.translateAlternateColorCodes( '&', name );
			int amount = Integer.parseInt( args[ 4 ] );
			short damage = Short.parseShort( args[ 5 ] );
			Document ditem = new Document( "material", material )
					.append( "name", name )
					.append( "amount", amount )
					.append( "damage", damage );
			this.effekte.updateOne( eq( "id", Integer.parseInt( args[ 1 ] ) ),
					new Document( "$set", new Document( "item", ditem ) ),
					( result, t ) -> {
						if ( result.getModifiedCount() == 0 ) {
							plugin.sendPluginMessage( player, "&4Diesen Effekt gibt es nicht." );
							return;
						}
						plugin.sendPluginMessage( player, "&4Das Item wurde aktualisiert." );
					}
			);
			getEffektList();
		} catch ( Exception ex ) {
			plugin.sendPluginMessage( commandSender, "&4/deineffekt setitem <ID> <Material> <Name> <Anzahl> <Schaden>" );
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Effekt aus MongoDB bekommen und in Hachmap als ParticelObject speichern
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 */
	private void setPlayerEffekt ( CommandSender commandSender, String choosen ) {
		try {
			if ( ! ( commandSender instanceof Player ) ) {
				commandSender.sendMessage( plugin.config.getString( "message.noConsole" ) );
				return;
			}
			Player player = ( Player ) commandSender;
			Document doc = new Document( "uuid", player.getUniqueId().toString() )
					.append( "effekt", String.valueOf( choosen ) )
					.append( "eChosen", - 1 );
			
			this.bought.insertOne( doc, ( result, t ) -> System.out.println( "Inserted bought!" ) );
		} catch ( Exception ex ) {
			commandSender.sendMessage( plugin.prefix + "/deineffekt buy <ID>" );
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
		
	}
	
	/**
	 * Öffnen des Inventares zu Effektauswahl
	 *
	 * @param player Spieler dem das Inventar geöffnet wird
	 */
	public void openSelectionInventory ( Player player ) {
		if ( plugin.pEffectList.size() == 0 ) {
			plugin.sendConfigString( player, "message.noEffects" );
			return;
		}
		try {
			this.bought.find( eq( "uuid", player.getUniqueId().toString() ) ).first( ( doc, t ) -> {
				if ( doc == null ) {
					createBought( player, false );
					return;
				}
				String[] bought = null;
				String sb = doc.getString( "effekt" );
				if ( sb.length() > 2 ) {
					bought = sb.split( "," );
				} else {
					bought = ( "-1," + sb ).split( "," );
				}
				int pos = 0;
				Inventory inventory = null;
				int active = 0;
				if ( ! player.hasPermission( plugin.getConfigPermission( "permission.admin" ) ) &&
				     ! player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) ) {
					for ( ParticleObject entry : plugin.pEffectList ) {
						
						if ( entry.isActive() ) { active++; }
					}
				} else {
					active = plugin.pEffectList.size();
				}
			
				int size = 18;
				if ( active > 9 ) {
					size = 36;
					if ( active > 18 ) {
						size = 54;
					}
				}
				inventory = Bukkit.createInventory( null, size, plugin.getConfigValue( "plugin.names.selectionInventory" ) );
				for ( int i = 0 ; i < inventory.getContents().length ; i++ ) {
					inventory.setItem( i, ItemHandler.getItem( Material.STAINED_GLASS_PANE, " ", 15, 1 ) );
				}
				for ( ParticleObject entry : plugin.pEffectList ) {
					if ( pos == 9 || pos == 27 ) { pos += 9; }
					if ( pos > 44 ) { return; }
					if ( player.hasPermission( plugin.config.getString( "permission.admin" ) ) || entry.isActive() ) {
						List < String > lore = new ArrayList <>();
						if ( entry.getCost() == - 1 ) {
							if ( ! player.hasPermission( plugin.getConfigPermission( "permission.admin" ) ) &&
							     ! player.hasPermission( plugin.getConfigPermission( "permission.vip" ) ) &&
							     ! player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) ) {
								continue;
							} else {
								lore.add( plugin.getConfigValue( "plugin.items.vipOnly" ) );
							}
						}
						int id = entry.getDbID();
						String sid = String.valueOf( id );
						ItemStack itemStack = entry.getItemStack();
						ItemMeta itemMeta = itemStack.getItemMeta();
						itemMeta.setDisplayName( getDisplayNameWithID( itemMeta, sid ) );
						if ( ! entry.isActive() ) { lore.add( plugin.getConfigValue( "plugin.items.deactivated" ) ); }
						if ( player.hasPermission( plugin.getConfigPermission( "permission.admin" ) ) ) {
							lore.add( "§4ID: " + sid );
							lore.add( "§4PARTIKEL: " + entry.getParticle() );
						}
						itemMeta.setLore( lore );
						itemStack.setItemMeta( itemMeta );
						inventory.setItem( pos, itemStack );
						inventory.setItem( pos + 9,
								ItemHandler.getItem( Material.SULPHUR, plugin.getConfigValue( "plugin.items.deactivated" ), 0, 1 ) );
						
						if ( ! Arrays.asList( bought ).contains( String.valueOf( id ) ) &&
						     ! player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) &&
						     ! player.hasPermission( plugin.getConfigPermission( "permission.vip" ) ) ) {
							pos++;
							continue;
						}
						
						if ( doc.getInteger( "eChosen" ) == id ) {
							inventory.setItem( pos + 9,
									ItemHandler.getItem( Material.INK_SACK, plugin.getConfigValue( "plugin.items.on" ), 10, 1 ) );
						} else {
							inventory.setItem( pos + 9,
									ItemHandler.getItem( Material.INK_SACK, plugin.getConfigValue( "plugin.items.off" ), 8, 1 ) );
						}
						pos++;
					}
				}
				player.openInventory( inventory );
			} );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Öffnen des Inventares zum Effektkauf
	 *
	 * @param player Spieler dem das Inventar geöffnet wird
	 */
	public void openBuyInventory ( Player player ) {
		if ( plugin.pEffectList.size() == 0 ) {
			plugin.sendConfigString( player, "message.noEffects" );
			return;
		}
		try {
			this.bought.find( eq( "uuid", player.getUniqueId().toString() ) ).first( ( doc, t ) -> {
				if ( doc == null ) {
					createBought( player, true );
					return;
				}
				String[] bought = null;
				String sb = doc.getString( "effekt" );
				if ( sb.length() > 2 ) {
					bought = sb.split( "," );
				} else {
					bought = ( "-1," + sb ).split( "," );
				}
				int pos = 0;
				Inventory inventory = null;
				int active = 0;
				for ( ParticleObject entry : plugin.pEffectList ) {
						if ( entry.isActive() ) { active++; }
				}
				int size = 18;
				if ( active > 9 ) {
					size = 36;
					if ( active > 18 ) {
						size = 54;
					}
				}
				inventory = Bukkit.createInventory( null, size, plugin.getConfigValue( "plugin.names.buyInventory" ) );
				for ( int i = 0 ; i < inventory.getContents().length ; i++ ) {
					inventory.setItem( i, ItemHandler.getItem( Material.STAINED_GLASS_PANE, " ", 15, 1 ) );
				}
				for ( ParticleObject entry : plugin.pEffectList ) {
					if ( pos == 9 || pos == 27 ) { pos += 9; }
					if ( pos > 44 ) { return; }
					int id = entry.getDbID();
					String sid = String.valueOf( id );
					if ( entry.active ) {
						List < String > lore = new ArrayList <>();
						if ( entry.getCost() == - 1 ) {
							//if ( ! player.hasPermission( plugin.getConfigPermission( "permission.admin" ) ) &&
							//     ! player.hasPermission( plugin.getConfigPermission( "permission.vip" ) ) &&
							//     ! player.hasPermission( plugin.getConfigPermission( "permission.team" ) ) ) {
							//	continue;
							//} else {
							lore.add( plugin.getConfigValue( "plugin.items.vipOnly" ) );
							//}
						}
						ItemStack itemStack = entry.getItemStack();
						ItemMeta itemMeta = itemStack.getItemMeta();
						
						itemMeta.setDisplayName( getDisplayNameWithID( itemMeta, sid ) );
						itemMeta.setLore( lore );
						itemStack.setItemMeta( itemMeta );
						inventory.setItem( pos, itemStack );
						
						if ( Arrays.asList( bought ).contains( String.valueOf( id ) ) ||
						     player.hasPermission( plugin.config.getString( "permission.team" ) ) ||
						     player.hasPermission( plugin.config.getString( "permission.vip" ) ) ) {
							inventory.setItem( pos + 9,
									ItemHandler.getItem( Material.INK_SACK, plugin.getConfigValue( "plugin.items.bought" ), 10, 1 ) );
						} else {
							if ( entry.getCost() != - 1 ) {
								ArrayList < String > sale = new ArrayList <>();
								sale.add( plugin.getConfigValue( "plugin.items.price.color" ) +
								          entry.getCost() + " " +
								          plugin.getConfigValue( "plugin.items.price.currency" ) );
								inventory.setItem( pos + 9,
										ItemHandler.getItem( Material.INK_SACK,
												plugin.getConfigValue( "plugin.items.forSale" ), sale, 8, 1 ) );
							} else {
								inventory.setItem( pos + 9,
										ItemHandler
												.getItem( Material.SULPHUR, plugin.getConfigValue( "plugin.items.deactivated" ), 0, 1 ) );
							}
						}
						pos++;
					}
				}
				
				player.openInventory( inventory );
			} );
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
	/**
	 * Eintrag eines neugekauften Effekts zu bereits gekauften
	 *
	 * @param player Spieler der den Effekt gekauft hat
	 * @param id     ID des gekauften Effektes
	 */
	public void buyEffekt ( Player player, int id ) {
		player.playSound( player.getLocation(), Sound.BLOCK_WOOD_BUTTON_CLICK_OFF, 1, 1 );
		player.closeInventory();
		this.bought.find( eq( "uuid", player.getUniqueId().toString() ) ).first( ( doc, tr ) -> {
			String sb = doc.getString( "effekt" );
			sb += "," + String.valueOf( id );
			this.bought.updateOne( eq( "uuid", player.getUniqueId().toString() ),
					new Document( "$set", new Document( "effekt", sb ) ),
					( result, t ) -> plugin.sendConfigString( player, "message.bought" ) );
		} );
	}
	
	/**
	 * Codieren der Effekt-ID in den Itemnamen
	 *
	 * @param itemMeta Meta des Items des Effektes
	 * @param sid      ID des Effektes
	 *
	 * @return Zusammengesetzter Itemname
	 */
	private String getDisplayNameWithID ( ItemMeta itemMeta, String sid ) {
		int control = sid.length();
		String idSet = "";
		if ( control > 1 ) {
			String[] ids = sid.split( "" );
			for ( String pid : ids ) {
				idSet += "§" + pid;
			}
		} else {
			idSet = "§" + sid;
		}
		return "§" + control + itemMeta.getDisplayName() + idSet;
	}
	
	/**
	 * Löschen eines Effektes
	 *
	 * @param commandSender CommandSender der den Kommand ausgefüghrt hat
	 * @param args          Argumente des Befehls
	 */
	public void deleteEffekt ( CommandSender commandSender, String[] args ) {
		try {
			int id = Integer.parseInt( args[ 1 ] );
			
			this.effekte.deleteOne( eq( "id", id ), ( result, t ) ->
					plugin.sendConfigString( commandSender, "message.effectDeleted" )
			);
		} catch ( Exception ex ) {
			plugin.sendPluginMessage( commandSender, "&4/deineffekt delete <ID>" );
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
		getEffektList();
	}
	
	/**
	 * Erstellen eines Kaufeintrages, falls nicht vorhanden
	 *
	 * @param player Spieler für den der Eintrag erstellt wird
	 * @param buy    Flag ob zum Auswahlmenü oder Kaufmenü weitergeleitet werden muss
	 */
	private void createBought ( Player player, boolean buy ) {
		try {
			Document doc = new Document( "uuid", player.getUniqueId().toString() )
					.append( "name", player.getName() )
					.append( "effekt", "- 1" )
					.append( "eChosen", - 1 );
			
			this.bought.insertOne( doc,
					( result, t ) -> {
						if ( buy ) {
							openBuyInventory( player );
						} else {
							openSelectionInventory( player );
						}
					}
			);
		} catch ( Exception ex ) {
			if ( plugin.debug ) { ex.printStackTrace(); }
		}
	}
	
}
