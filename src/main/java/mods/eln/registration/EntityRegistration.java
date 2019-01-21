package mods.eln.registration;

import cpw.mods.fml.common.registry.EntityRegistry;
import mods.eln.Vars;
import mods.eln.entity.ReplicatorEntity;
import mods.eln.i18n.I18N;
import mods.eln.misc.Utils;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import static mods.eln.i18n.I18N.TR_NAME;

public class EntityRegistration {

    public EntityRegistration() {

    }

    public void registerEntities() {
        registerReplicator();
    }

    private void registerReplicator() {
        int redColor = (255 << 16);
        int orangeColor = (255 << 16) + (200 << 8);

        if (Vars.replicatorRegistrationId == -1)
            Vars.replicatorRegistrationId = EntityRegistry.findGlobalUniqueEntityId();
        Utils.println("Replicator registred at" + Vars.replicatorRegistrationId);
        // Register mob
        EntityRegistry.registerGlobalEntityID(ReplicatorEntity.class, TR_NAME(I18N.Type.ENTITY, "EAReplicator"), Vars.replicatorRegistrationId, redColor, orangeColor);

        ReplicatorEntity.dropList.add(Vars.findItemStack("Iron Dust", 1));
        ReplicatorEntity.dropList.add(Vars.findItemStack("Copper Dust", 1));
        ReplicatorEntity.dropList.add(Vars.findItemStack("Gold Dust", 1));
        ReplicatorEntity.dropList.add(new ItemStack(Items.redstone));
        ReplicatorEntity.dropList.add(new ItemStack(Items.glowstone_dust));
        // Add mob spawn
        // EntityRegistry.addSpawn(ReplicatorEntity.class, 1, 1, 2, EnumCreatureType.monster, BiomeGenBase.plains);

    }
}
