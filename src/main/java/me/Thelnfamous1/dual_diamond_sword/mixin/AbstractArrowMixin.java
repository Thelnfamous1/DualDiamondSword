package me.Thelnfamous1.dual_diamond_sword.mixin;

import me.Thelnfamous1.dual_diamond_sword.PhysicsCheck;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import javax.annotation.Nullable;

@Mixin(AbstractArrow.class)
public abstract class AbstractArrowMixin implements PhysicsCheck {

    @Shadow public abstract boolean isNoPhysics();

    @Unique
    @Nullable
    private HitResult currentHitResult;

    @ModifyVariable(method = "tick", at = @At(value = "LOAD", ordinal = 5))
    private HitResult cacheHitResult(HitResult hitResult){
        this.currentHitResult = hitResult;
        return hitResult;
    }


    @ModifyVariable(method = "tick", at = @At(value = "LOAD", ordinal = 2))
    private boolean modifyIsNoPhysicsLocalForEntityHit(boolean isNoPhysics){
        if(isNoPhysics && this.currentHitResult != null && this.currentHitResult.getType() == HitResult.Type.ENTITY){
            return !this.canBypassNoPhysicsToHitEntities(); // negate it to undo the negation in the target method
        }
        return this.isNoPhysics();
    }

    @ModifyVariable(method = "tick", at = @At(value = "LOAD", ordinal = 7))
    private boolean resetIsNoPhysicsLocal(boolean isNoPhysics){
        return this.isNoPhysics();
    }
}
