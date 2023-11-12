package me.Thelnfamous1.dual_diamond_sword;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

public class BoomerangSwordEntity extends AbstractArrow implements ItemSupplier, IEntityAdditionalSpawnData, PhysicsCheck {
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK = SynchedEntityData.defineId(BoomerangSwordEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> DATA_RETURNING_TO_OWNER = SynchedEntityData.defineId(BoomerangSwordEntity.class, EntityDataSerializers.BOOLEAN);
    public static final String RETURNING_TO_OWNER_TAG = "ReturningToOwner";
    public static final String ITEM_TAG = "Item";
    public int clientSideReturnTridentTickCount;
    @Nullable
    private IntOpenHashSet returningIgnoreEntityIds;
    private Vec3 origin = Vec3.ZERO;
    public BoomerangSwordEntity(EntityType<? extends AbstractArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BoomerangSwordEntity(Level pLevel, LivingEntity pShooter, ItemStack pStack) {
        super(DualDiamondSword.BOOMERANG_SWORD.get(), pShooter, pLevel);
        this.setItem(pStack.copy());
        if (pShooter instanceof Player) {
            this.pickup = AbstractArrow.Pickup.ALLOWED;
        }
        this.origin = this.position();
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

    public boolean isReturningToOwner(){
        return this.entityData.get(DATA_RETURNING_TO_OWNER);
    }

    protected void setReturningToOwner(boolean returningToOwner){
        this.entityData.set(DATA_RETURNING_TO_OWNER, returningToOwner);
    }


    @Override
    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        ItemStack itemRaw = this.getItemRaw();
        if (!itemRaw.isEmpty()) {
            pCompound.put(ITEM_TAG, itemRaw.save(new CompoundTag()));
        }
        pCompound.putBoolean(RETURNING_TO_OWNER_TAG, this.isReturningToOwner());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        ItemStack item = ItemStack.of(pCompound.getCompound(ITEM_TAG));
        this.setItem(item);

        this.setReturningToOwner(pCompound.getBoolean(RETURNING_TO_OWNER_TAG));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.getEntityData().define(DATA_ITEM_STACK, ItemStack.EMPTY);
        this.getEntityData().define(DATA_RETURNING_TO_OWNER, false);
    }

    @Override
    public void tick() {
        if(!this.level.isClientSide){
            if(this.inGroundTime > 0 || this.position().distanceToSqr(this.origin) > 900.0D){ // can go no more than 30 blocks away from thrower
                this.setReturningToOwner(true);
            }
        }

        Entity owner = this.getOwner();
        if ((this.isReturningToOwner() || this.isNoPhysics()) && owner != null) {
            if (!this.isAcceptableReturnOwner()) {
                if (!this.level.isClientSide && this.pickup == AbstractArrow.Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }

                this.discard();
            } else {
                this.setNoPhysics(true);
                Vec3 returnVec = owner.getEyePosition().subtract(this.position());
                this.setPosRaw(this.getX(), this.getY() /*+ returnVec.y * 0.015D * 3.0D*/, this.getZ());
                if (this.level.isClientSide) {
                    this.yOld = this.getY();
                }
                this.setDeltaMovement(this.getDeltaMovement().scale(0.95D).add(returnVec.normalize().scale(BoomerangSwordItem.VELOCITY * 0.1D)));
                if (this.clientSideReturnTridentTickCount == 0) {
                    //this.playSound(SoundEvents.TRIDENT_RETURN, 10.0F, 1.0F);
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



    private void resetIgnoredEntities() {
        if (this.returningIgnoreEntityIds != null) {
            this.returningIgnoreEntityIds.clear();
        }

    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        Entity hitEntity = pResult.getEntity();
        float damage = (float) this.getItemRaw().getAttributeModifiers(EquipmentSlot.MAINHAND).get(Attributes.ATTACK_DAMAGE)
                .stream()
                .map(AttributeModifier::getAmount)
                .mapToDouble(Double::doubleValue)
                .sum();
        if (hitEntity instanceof LivingEntity victim) {
            damage += EnchantmentHelper.getDamageBonus(this.getItemRaw(), victim.getMobType());
        }

        Entity owner = this.getOwner();
        DamageSource damageSource = DamageSource.thrown(this, owner == null ? this : owner);
        boolean wasReturningToOwner = this.isReturningToOwner();
        if(!this.level.isClientSide)
            this.setReturningToOwner(true);

        // track entities that have already been hit
        if (this.returningIgnoreEntityIds == null) {
            this.returningIgnoreEntityIds = new IntOpenHashSet(5);
        }
        this.returningIgnoreEntityIds.add(hitEntity.getId());

        SoundEvent hitSound = SoundEvents.TRIDENT_HIT;
        if (hitEntity.hurt(damageSource, damage)) {
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

        if(!wasReturningToOwner) this.setDeltaMovement(this.getDeltaMovement().multiply(-0.01D, -0.1D, -0.01D));
        this.playSound(hitSound, 1.0F, 1.0F);
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        this.resetIgnoredEntities();
    }

    @Override
    public void playerTouch(Player pEntity) {
        if (this.ownedBy(pEntity) || this.getOwner() == null) {
            super.playerTouch(pEntity);
        }

    }

    @Override
    protected boolean tryPickup(Player pPlayer) {
        return super.tryPickup(pPlayer) || this.isNoPhysics() && this.ownedBy(pPlayer) && pPlayer.getInventory().add(this.getPickupItem());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        if(this.isReturningToOwner() && entity == this.getOwner()){
            return false;
        }

        return super.canHitEntity(entity) && (this.returningIgnoreEntityIds == null || !this.returningIgnoreEntityIds.contains(entity.getId()));
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

    @Override
    public void tickDespawn() {
        if (this.pickup != AbstractArrow.Pickup.ALLOWED) {
            super.tickDespawn();
        }

    }

    @Override
    public boolean shouldRender(double pX, double pY, double pZ) {
        return true;
    }

    @Override
    public boolean canBypassNoPhysicsToHitEntities() {
        return this.isReturningToOwner();
    }
}
