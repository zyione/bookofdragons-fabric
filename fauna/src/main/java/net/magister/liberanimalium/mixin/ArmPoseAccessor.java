package net.magister.liberanimalium.mixin;

import net.minecraft.client.model.HumanoidModel.ArmPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmPose.class)
public interface ArmPoseAccessor {
    @Invoker("<init>")
    static ArmPose liberanimalium$construct(String name, int ordinal, boolean twoHanded) {
        throw new AssertionError("Mixin constructor invoker not transformed");
    }
    @Accessor(value = "$VALUES", remap = false)
    static ArmPose[] liberanimalium$values() { throw new AssertionError("Mixin accessor not transformed"); }
    @Mutable @Accessor(value = "$VALUES", remap = false)
    static void liberanimalium$values(ArmPose[] values) { throw new AssertionError("Mixin accessor not transformed"); }
}
