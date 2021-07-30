package mc.iaiao.gsell;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

public class EventListener implements Listener {
  private static final GSell plugin = GSell.getInstance();
  private static final int rows = plugin.getConfig().getInt( "gui.rows" );
  private static final int guiRow = plugin.getConfig().getInt( "gui.gui-row" ) - 1;

  @EventHandler
  public void onClick( InventoryClickEvent e ) {
    if ( !( e.getWhoClicked() instanceof Player ) ) return;
    GUI gui = GUI.fromInventory( e.getClickedInventory() );
      /*
        player could shift-click in his own
        inventory and move the item to GSell GUI
      */
    if( gui == null ) {
      gui = GUI.fromInventory( e.getWhoClicked().getOpenInventory().getTopInventory() );
    }
    if( gui == null || e.getSlot() / 9 != guiRow  ) {
      if( gui != null ) {
        Bukkit.getScheduler().scheduleSyncDelayedTask( plugin, gui::reloadGuiRow, 1 );
      }
    }
    if ( gui != null ) {
      if ( e.getSlot() / 9 == guiRow ) {
        e.setCancelled( true );
        int slot = e.getSlot() % 9 + 1;
        ConfigurationSection item = plugin.getConfig().getConfigurationSection( "gui.items." + slot );
        if ( item.getString( "material" ).equalsIgnoreCase( "SELL" ) ) {
          plugin.sell( gui );
          GUI.removeGUI( e.getClickedInventory() );
          e.getWhoClicked().closeInventory();
          if ( !plugin.getConfig().getBoolean( "close-on-sell" ) ) {
            Bukkit.dispatchCommand( e.getWhoClicked(), "sell" );
          }
        }
      }
    }
  }

  @EventHandler
  public void onClose( InventoryCloseEvent e ) {
    if ( !( e.getPlayer() instanceof Player ) ) return;
    GUI gui = GUI.fromInventory( e.getInventory() );
    if ( gui != null ) {
      switch ( plugin.getConfig().getString( "action-on-close" ).toUpperCase() ) {
        case "DROP":
          for ( int i = 0; i < rows; ++i ) {
            if ( i == guiRow ) continue;
            for ( int j = 0; j < 9; j++ ) {
              int slot = i * 9 + j;
              ItemStack item = e.getInventory().getItem( slot );
              if ( item != null && !item.getType().equals( Material.AIR ) && item.getAmount() != 0 ) {
                e.getPlayer().getWorld().dropItem( e.getPlayer().getLocation(), item );
              }
            }
          }
          break;
        case "ADD_TO_INVENTORY":
          for ( int i = 0; i < rows; ++i ) {
            if ( i == guiRow ) continue;
            for ( int j = 0; j < 9; j++ ) {
              int slot = i * 9 + j;
              ItemStack item = e.getInventory().getItem( slot );
              if ( item != null && !item.getType().equals( Material.AIR ) && item.getAmount() != 0 ) {
                e.getPlayer().getInventory().addItem( item );
              }
            }
          }
          break;
        case "SELL":
          // pay and remove
          plugin.sell( gui );
        case "REMOVE":
          // nothing to do
          break;
        default:
          plugin.getLogger().warning( "Cannot resolve action-on-close: " + plugin.getConfig().getString( "action-on-close" ) + ". Check your config.yml." );
      }
      GUI.removeGUI( e.getInventory() );
    }
  }
}
