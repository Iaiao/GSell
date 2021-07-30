package mc.iaiao.gsell;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings ( "WeakerAccess" )
public class GUI {
  private static final HashMap <Inventory, GUI> inventories = new HashMap <>();
  private static final GSell plugin = GSell.getInstance();
  private static final int guiRow = plugin.getConfig().getInt( "gui.gui-row" ) - 1;
  private static final String name = ChatColor.translateAlternateColorCodes( '&', plugin.getConfig().getString( "translations.gui-name" ) );
  private static final int rows = plugin.getConfig().getInt( "gui.rows" );
  private static final HashMap <Material, Float> prices = new HashMap <>();
  private static final Material sellMaterial = Material.matchMaterial( plugin.getConfig().getString( "sell-item.not-empty.material" ) );
  private static final Material emptySellMaterial = Material.matchMaterial( plugin.getConfig().getString( "sell-item.empty.material" ) );
  private static final List <String> sellLore = plugin.getConfig().getStringList( "sell-item.not-empty.lore" );
  private static final List <String> emptySellLore = plugin.getConfig().getStringList( "sell-item.empty.lore" );
  private static final String sellName = ChatColor.translateAlternateColorCodes( '&', plugin.getConfig().getString( "sell-item.not-empty.name" ) );
  private static final String emptySellName = ChatColor.translateAlternateColorCodes( '&', plugin.getConfig().getString( "sell-item.empty.name" ) );
  private static final boolean transformEnchantments = plugin.getConfig().getBoolean( "transform-enchantments" );
  private static float priceOther = 0f;
  private static float enchantPriceOther = 0f;
  private static boolean transformEnchantBooks = plugin.getConfig().getBoolean( "transform-enchant-books" );
  private static final HashMap<Enchantment, Float> enchantPrices = new HashMap <>();

  static {
    ConfigurationSection materialPrices = plugin.getConfig().getConfigurationSection( "prices" );
    for ( String key : materialPrices.getKeys( false ) ) {
      if ( key.equalsIgnoreCase( "other" ) ) {
        priceOther = (float) materialPrices.getDouble( key );
      } else {
        Material material = Material.matchMaterial( key );
        assert material != null;
        prices.put( material, (float) materialPrices.getDouble( key ) );
      }
    }
    if( transformEnchantments ) {
      ConfigurationSection enchantsPrices = plugin.getConfig().getConfigurationSection( "enchant-prices" );
      for ( String key : enchantsPrices.getKeys( false ) ) {
        if ( key.equalsIgnoreCase( "other" ) ) {
          enchantPriceOther = (float) enchantsPrices.getDouble( key );
        } else {
          Enchantment ench = Enchantment.getByName( key.toUpperCase() );
          assert ench != null;
          enchantPrices.put( ench, (float) enchantsPrices.getDouble( key ) );
        }
      }
    }
  }

  private Inventory inventory;
  private Player player;

  public GUI( Player player ) {
    this.player = player;
    this.inventory = Bukkit.createInventory( null, rows * 9, name );
    inventories.put( this.inventory, this );
    reloadGuiRow();
  }

  public static void removeGUI( Inventory inv ) {
    inventories.remove( inv );
  }

  public static GUI fromInventory( Inventory inv ) {
    return inventories.get( inv );
  }

  public void reloadGuiRow() {
    for ( int i = 0; i < 9; ++i ) {
      int slot = guiRow * 9 + i;
      if ( plugin.getConfig().contains( "gui.items." + ( i + 1 ) ) ) {
        if ( plugin.getConfig().getString( "gui.items." + ( i + 1 ) + ".material" ).equalsIgnoreCase( "SELL" ) ) {
          ItemStack item = craftSellItem();
          this.inventory.setItem( slot, item );
        } else {
          Material material = Material.matchMaterial( plugin.getConfig().getString( "gui.items." + ( i + 1 ) + ".material" ) );
          assert material != null;
          ItemStack item = new ItemStack( material, plugin.getConfig().getInt( "gui.items." + ( i + 1 ) + ".amount" ) );
          if ( !material.equals( Material.AIR ) ) {
            ItemMeta meta = item.getItemMeta();
            if ( plugin.getConfig().contains( "gui.items." + ( i + 1 ) + ".lore" ) ) {
              List <String> lore = new ArrayList <>();
              for ( String line : plugin.getConfig().getStringList( "gui.items." + ( i + 1 ) + ".lore" ) ) {
                lore.add(
                        ChatColor.translateAlternateColorCodes(
                                '&', line
                                        .replace( "{player}", this.player.getName() )
                        )
                );
              }
              meta.setLore( lore );
            }
            if ( plugin.getConfig().contains( "gui.items." + ( i + 1 ) + ".name" ) ) {
              meta.setDisplayName(
                      ChatColor.translateAlternateColorCodes(
                              '&', plugin.getConfig().getString( "gui.items." + ( i + 1 ) + ".name" )
                      )
              );
            }
            item.setItemMeta( meta );
          }
          this.inventory.setItem( slot, item );
        }
      }
    }
  }

  public ItemStack craftSellItem() {
    ItemStack sellItem = new ItemStack( emptySellMaterial );
    float cost = countCost();
    int count = 0;
    for ( int row = 0; row < rows; ++row ) {
      if ( row == guiRow ) continue;
      for ( int i = 0; i < 9; ++i ) {
        ItemStack item = this.inventory.getItem( row * 9 + i );
        if (
                !sellItem.getType().equals( sellMaterial )
                        && item != null
                        && !item.getType().equals( Material.AIR )
                        && item.getAmount() != 0
        ) {
          sellItem.setType( sellMaterial );
        }
        if ( item != null ) {
          count += item.getAmount();
        }
      }
    }
    boolean empty = count == 0;
    List <String> lore = new ArrayList <>();
    for ( String line : empty ? emptySellLore : sellLore ) {
      lore.add(
              ChatColor.translateAlternateColorCodes(
                      '&', line
                              .replace( "{count}", String.valueOf( count ) )
                              .replace( "{cost}", String.valueOf( cost ) )
              )
      );
    }
    ItemMeta meta = sellItem.getItemMeta();
    meta.setLore( lore );
    meta.setDisplayName(
            ( empty ? emptySellName : sellName )
                    .replace( "{cost}", String.valueOf( cost ) )
                    .replace( "{count}", String.valueOf( count ) )
    );
    sellItem.setItemMeta( meta );
    return sellItem;
  }

  public int countItems() {
    int count = 0;
    for ( int row = 0; row < rows; ++row ) {
      if ( row == guiRow ) continue;
      for ( int i = 0; i < 9; ++i ) {
        ItemStack item = this.inventory.getItem( row * 9 + i );
        if (
                item != null
                        && !item.getType().equals( Material.AIR )
                        && item.getAmount() != 0
        ) {
          ++count;
        }
      }
    }
    return count;
  }

  public float countCost() {
    float result = 0;
    for ( int row = 0; row < rows; ++row ) {
      if ( row == guiRow ) continue;
      for ( int i = 0; i < 9; i++ ) {
        int slot = row * 9 + i;
        ItemStack item = inventory.getItem( slot );
        if ( item != null && !item.getType().equals( Material.AIR ) && item.getAmount() != 0 ) {
          float price = prices.containsKey( item.getType() ) ? prices.get( item.getType() ) : priceOther;
          ItemMeta meta = item.getItemMeta();
          if( transformEnchantments && meta != null ) {
            HashMap<Enchantment, Integer> enchantments = new HashMap <>();
            if( transformEnchantBooks && item.getType().equals( Material.ENCHANTED_BOOK ) ) {
              EnchantmentStorageMeta bookMeta = (EnchantmentStorageMeta) meta;
              enchantments.putAll( bookMeta.getStoredEnchants() );
            }
            enchantments.putAll( meta.getEnchants() );
            if( enchantments.size() != 0 ) {
              for( Map.Entry<Enchantment, Integer> entry : enchantments.entrySet() ) {
                if( enchantPrices.containsKey( entry.getKey() ) ) {
                  price += enchantPrices.get( entry.getKey() ) * entry.getValue();
                } else {
                  price += enchantPriceOther * entry.getValue();
                }
              }
            }
          }
          result += price * item.getAmount();
        }
      }
    }
    float multiplier = 1;
    int i = 0;
    do {
      if ( this.player.hasPermission( "gsell.multiplier.x" + ( i / 10f ) ) ) {
        multiplier = i / 10f;
      }
    } while ( i++ < 100 );
    return result * multiplier;
  }

  public Inventory getInventory() {
    return inventory;
  }

  public Player getPlayer() {
    return player;
  }
}
