package me.Thelnfamous1.dual_diamond_sword;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class BoomerangSwordItem extends SwordItem {

    public static final float VELOCITY = 2.5F;

    public BoomerangSwordItem(Tier pTier, int pAttackDamageModifier, float pAttackSpeedModifier, Properties pProperties) {
        super(pTier, pAttackDamageModifier, pAttackSpeedModifier, pProperties);
    }

    @Override
    public void releaseUsing(ItemStack pStack, Level pLevel, LivingEntity pEntityLiving, int pTimeLeft) {
        if (pEntityLiving instanceof Player player) {
            int useTicks = this.getUseDuration(pStack) - pTimeLeft;
            if (useTicks >= 10) {
                if (!pLevel.isClientSide) {
                    pStack.hurtAndBreak(1, player, (p) -> p.broadcastBreakEvent(pEntityLiving.getUsedItemHand()));
                    BoomerangSwordEntity boomerangSword = new BoomerangSwordEntity(pLevel, player, pStack);
                    boomerangSword.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, VELOCITY, 1.0F);
                    if (player.getAbilities().instabuild) {
                        boomerangSword.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                    }

                    pLevel.addFreshEntity(boomerangSword);
                    pLevel.playSound(null, boomerangSword, SoundEvents.TRIDENT_THROW, SoundSource.PLAYERS, 1.0F, 1.0F);
                    if (!player.getAbilities().instabuild) {
                        player.getInventory().removeItem(pStack);
                    }
                }

                player.awardStat(Stats.ITEM_USED.get(this));

            }
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pHand) {
        ItemStack itemInHand = pPlayer.getItemInHand(pHand);
        if (itemInHand.getDamageValue() >= itemInHand.getMaxDamage() - 1) {
            return InteractionResultHolder.fail(itemInHand);
        } else {
            pPlayer.startUsingItem(pHand);
            return InteractionResultHolder.consume(itemInHand);
        }
    }

    @Override
    public int getUseDuration(ItemStack pStack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack pStack) {
        return UseAnim.SPEAR;
    }
}
