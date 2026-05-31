package com.csesp.entitycompass;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

public class EntityCompassItem extends Item {
    public EntityCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        openMenu(world, user);

        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        openMenu(context.getWorld(), context.getPlayer());
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        openMenu(user.getEntityWorld(), user);
        return ActionResult.SUCCESS;
    }

    private static void openMenu(World world, PlayerEntity user) {
        if (user != null && !world.isClient() && user instanceof ServerPlayerEntity player) {
            EntityCompassMod.openTargetMenu(player);
        }
    }
}
