package mods.eln.generic;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;

/**
 * GenericCreativeTab
 *
 * Sadly, due to the way that Electrical Age uses the same blockID's and itemID's for everything, it is basically
 * impossible to just render regular blocks. In some cases, I've allowed for you to just render an item, but I've also
 * created a function that will take a resource name and look it up in the items section of the textures.
 *
 * It's defintely a hack, but there's not much I can do since I can't seem to find a way to suggest Items that have
 * damage to be rendered (since that's what it would require to render the items as they are now).
 *
 * If someone finds a way to do that, PLEASE rewrite this to use that system.
 *
 * EDIT: This whole class won't work, since I can't place damaged items in sections either.
 * The mod needs a registration rewrite.
 */
public class GenericCreativeTab extends CreativeTabs {

    public Item item;

    /**
     * GenericCreativeTab: used to create CreativeTabs for the game.
     *
     * @param label name for the tab. "[Eln] " gets appended, so keep in mind for lang packs
     * @param item the item to represent your tab. If this is null, Minecraft Dirt is used
     */
    public GenericCreativeTab(String label, Item item) {
        //super("[Eln] " + label); // TODO: rewrite registration system, then revert to this.
        super(label);
        if (item == null) {
            // if you see dirt, you're passing null. Simple as that :)
            this.item = GameRegistry.findItemStack("minecraft", "dirt", 1).getItem();
        } else {
            this.item = item;
        }
    }

    /**
     * GenericCreativeTab: used to create CreativeTabs for the game
     *
     * @param label name for the tab. "[Eln] gets appended, so keep in mind for lang packs
     * @param resource the item's resource image filename (sans extension) in assets.eln.textures.items ("ct-" gets prepended to the image name)
     */
    public GenericCreativeTab(String label, String resource) {
        super("[Eln] " + label);
        if (resource.isEmpty()) {
            // if you see dirt, you're passing empty string. Simple as that :)
            this.item = GameRegistry.findItemStack("minecraft", "dirt", 1).getItem();
        } else {
            Item i = new Item()
                .setUnlocalizedName("eln:ct-" + label)
                .setTextureName("eln:ct-" + resource);
            GameRegistry.registerItem(i, "eln." + label);
            this.item = i;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Item getTabIconItem() {
        return (item);
    }
}
