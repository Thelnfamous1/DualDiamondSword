package me.Thelnfamous1.dual_diamond_sword;

import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

public class BoomerangSwordEntity extends AbstractArrow implements ItemSupplier, IEntityAdditionalSpawnData {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(BoomerangSwordEntity.class, EntityDataSerializers.ITEM_STACK);

    private boolean hitTarget;
    public int clientSideReturnTridentTickCount;
    public BoomerangSwordEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BoomerangSwordEntity(Level pLevel, LivingEntity pShooter, ItemStack pStack) {
        super(DualDiamondSword.BOOMERANG_SWORD.get(), pShooter, pLevel);
        this.setItem(pStack.copy());
        if (pShooter instanceof Player) {
            this.pickup = AbstractArrow.Pickup.ALLOWED;
        }
    }

    @Override
    protected ItemStack getPickupItem() {
        return this.getItemRaw().copy();
    }

    public void setItem(ItemStack pStack) {
        if (!pStack.is(this.getDefaultItem()) || pStack.hasTag()) {
            this.getEntityData().set(DATA_ITEM_STACK, Util.make(pStack.copy(), (is) -> is.setCount(1)));
        }
    }

    @Override
    public ItemStack getItem() {
        ItemStack itemRaw = this.getItemRaw();
        return itemRaw.isEmpty() ? new ItemStack(this.getDefaultItem()) : itemRaw;
    }

    protected ItemStack getItemRaw() {
        return this.getEntityData().get(DATA_ITEM_STACK);
    }

    protected Item getDefaultItem(){
        return DualDiamondSword.DUAL_DIAMOND_SWORD.get();
    }


    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        ItemStack itemRaw = this.getItemRaw();
        if (!itemRaw.isEmpty()) {
            pCompound.put("Item", itemRaw.save(new CompoundTag()));
        }

    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        ItemStack item = ItemStack.of(pCompound.getCompound("Item"));
        this.setItem(item);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    @Override
    public void tick() {
        if (this.inGroundTime > 4) {
            this.hitTarget = true;
        }

        Entity owner = this.getOwner();
        int loyalty = 3;
        if ((this.hitTarget || this.isNoPhysics()) && owner != null) {
            if (!this.isAcceptableReturnOwner()) {
                if (!this.level.isClientSide && this.pickup == AbstractArrow.Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }

                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 vec3 = owner.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() + vec3.y * 0.015D * (double)loyalty, this.getZ());
                if (this.level.isClientSide) {
                    this.yOld = this.getY();
                }

                double d0 = 0.05D * (double)loyalty;
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(vec3.normalize().scale(d0)));
                if (this.clientSideReturnTridentTickCount == 0) {
                    this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
                }

                ++this.clientSideReturnTridentTickCount;
            }
        }

        super.tick();
    }

    private boolean isAcceptableReturnOwner() {
        Entity owner = this.getOwner();
        if (owner != null && owner.isAlive()) {
            return !(owner instanceof ServerPlayer) || !owner.isSpectator();
        } else {
            return false;
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity hitEntity = pResult.getEntity();
        float damage = 6.0F;
        if (hitEntity instanceof LivingEntity livingentity) {
            damage += EnchantmentHelper.getDamageBonus(this.getItemRaw(), livingentity.getMobType());
        }

        Entity owner = this.getOwner();
        DamageSource damagesource = DamageSource.trident(this, owner == null ? this : owner);
        this.hitTarget = true;
        SoundEvent hitSound = SoundEvents.TRIDENT_HIT;
        if (hitEntity.hurt(damagesource, damage)) {
            if (hitEntity.getType() == EntityType.ENDERMAN) {
                return;
            }

            if (hitEntity instanceof LivingEntity victim) {
                if (owner instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(victim, owner);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity)owner, victim);
                }

                this.doPostHurtEffects(victim);
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(hitSound, 1.0F, 1.0F);
    }

    @Override
    public Packet<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(FriendlyByteBuf buffer) {
        Entity owner = this.getOwner();
        int id = owner == null ? 0 : owner.getId();
        buffer.writeVarInt(id);
    }

    @Override
    public void readSpawnData(FriendlyByteBuf additionalData) {
        int id = additionalData.readVarInt();
        this.setOwner(id == 0 ? null : this.level.getEntity(id));
    }
}
