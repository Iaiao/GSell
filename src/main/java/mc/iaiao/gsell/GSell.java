package mc.iaiao.gsell;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GSell extends JavaPlugin {
  private Economy economy = null;
  private List <String> commands;
  private List <String> commandsEmpty;

  public static GSell getInstance() {
    return (GSell) Bukkit.getPluginManager().getPlugin( "GSell" );
  }

  public void onEnable() {
    saveDefaultConfig();
    commands = getConfig().getStringList( "sell-item.not-empty.click-actions" );
    commandsEmpty = getConfig().getStringList( "sell-item.empty.click-actions" );
    RegisteredServiceProvider <Economy> provider = getServer().getServicesManager().getRegistration( Economy.class );
    if ( provider == null || provider.getProvider() == null ) {
      getLogger().warning( "Cannot hook into Vault economy. Continuing without Vault economy.\nIf your economy plugin is not using Vault, you can set click-event on the sell item to \"console /eco give {player} {cost}\" or similar" );
    } else {
      economy = provider.getProvider();
    }
    getServer().getPluginManager().registerEvents( new EventListener(), this );
    getCommand( "sell" ).setExecutor( ( sender, cmd, label, args ) -> {
      if ( sender instanceof Player ) {
        Player player = (Player) sender;
        GUI gui = new GUI( player );
        player.openInventory( gui.getInventory() );
      }
      return true;
    } );
  }

  public void sell( GUI gui ) {
    Player seller = gui.getPlayer();
    float money = gui.countCost();
    int items = gui.countItems();
    if ( money != 0 && items != 0 ) {
      if ( economy != null ) {
        economy.depositPlayer( seller, money );
      }
      for ( String cmd : commands ) {
        executeCommand(
                seller,
                cmd
                        .replace( "{player}", seller.getName() )
                        .replace( "{cost}", String.valueOf( money ) )
                        .replace( "{count}", String.valueOf( items ) )
        );
      }
    } else {
      for ( String cmd : commandsEmpty ) {
        executeCommand(
                seller,
                cmd.replace( "{player}", seller.getName() )
        );
      }
    }
  }

  private void executeCommand( Player sender, String cmd ) {
    if ( cmd.toLowerCase().startsWith( "console /" ) ) {
      String command = cmd.substring( 9 );
      getServer().dispatchCommand( getServer().getConsoleSender(), command );
    } else if( cmd.toLowerCase().startsWith( "run /" ) ) {
      String command = cmd.substring( 5 );
      getServer().dispatchCommand( sender, command );
    } else if( cmd.equalsIgnoreCase( "close" ) ) {
      sender.closeInventory();
    }
  }
}
