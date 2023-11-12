package me.Thelnfamous1.dual_diamond_sword;

public interface PhysicsCheck {
    default boolean canBypassNoPhysicsToHitEntities(){
        return false;
    }
}
