package net.minecraft.world.item;

import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.stats.StatisticList;
import net.minecraft.world.EnumHand;
import net.minecraft.world.InteractionResultWrapper;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.entity.projectile.EntityEgg;
import net.minecraft.world.level.World;

public class ItemEgg extends Item {

    public ItemEgg(Item.Info item_info) {
        super(item_info);
    }

    @Override
    public InteractionResultWrapper<ItemStack> a(World world, EntityHuman entityhuman, EnumHand enumhand) {
        ItemStack itemstack = entityhuman.b(enumhand);

        // world.playSound((EntityHuman) null, entityhuman.locX(), entityhuman.locY(), entityhuman.locZ(), SoundEffects.EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F)); // CraftBukkit - moved down
        if (!world.isClientSide) {
            EntityEgg entityegg = new EntityEgg(world, entityhuman);

            entityegg.setItem(itemstack);
            entityegg.a(entityhuman, entityhuman.getXRot(), entityhuman.getYRot(), 0.0F, 1.5F, 1.0F);
            // CraftBukkit start
            if (!world.addEntity(entityegg)) {
                if (entityhuman instanceof net.minecraft.server.level.EntityPlayer) {
                    ((net.minecraft.server.level.EntityPlayer) entityhuman).getBukkitEntity().updateInventory();
                }
                return InteractionResultWrapper.fail(itemstack);
            }
            // CraftBukkit end
        }
        world.playSound((EntityHuman) null, entityhuman.locX(), entityhuman.locY(), entityhuman.locZ(), SoundEffects.EGG_THROW, SoundCategory.PLAYERS, 0.5F, 0.4F / (world.getRandom().nextFloat() * 0.4F + 0.8F));

        entityhuman.b(StatisticList.ITEM_USED.b(this));
        if (!entityhuman.getAbilities().instabuild) {
            itemstack.subtract(1);
        }

        return InteractionResultWrapper.a(itemstack, world.isClientSide());
    }
}
