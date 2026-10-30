package net.magister.liberanimalium.platform.fabric;

import java.util.IdentityHashMap;
import java.util.Map;
import com.mojang.blaze3d.platform.InputConstants;
import net.magister.liberanimalium.mixin.ArmPoseAccessor;
import net.magister.liberanimalium.platform.client.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;

public final class FabricClientServices implements ClientServices {
    private static final Map<ArmPose, ArmPoses.Transform> TRANSFORMS = new IdentityHashMap<>();
    public static final Map<Item, HandPose> ITEM_HANDS = new IdentityHashMap<>();
    public static boolean active(KeyMapping key) { return !(key instanceof ContextKey custom) || custom.active(); }
    public KeyMapping key(String name, KeyContext context, InputConstants.Type type, int code, String category) {
        return new ContextKey(name, context, type, code, category);
    }
    public synchronized ArmPose armPose(String name, boolean twoHanded, ArmPoses.Transform transform) {
        ArmPose[] values = ArmPoseAccessor.liberanimalium$values();
        for (ArmPose existing : values) if (existing.name().equals(name)) throw new IllegalArgumentException("Duplicate arm pose " + name);
        ArmPose pose = ArmPoseAccessor.liberanimalium$construct(name, values.length, twoHanded);
        ArmPose[] extended = java.util.Arrays.copyOf(values, values.length + 1);
        extended[values.length] = pose;
        ArmPoseAccessor.liberanimalium$values(extended);
        TRANSFORMS.put(pose, java.util.Objects.requireNonNull(transform));
        return pose;
    }
    public static boolean pose(ArmPose pose, HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        ArmPoses.Transform transform = TRANSFORMS.get(pose);
        if (transform == null) return false;
        transform.apply(model, entity, arm);
        return true;
    }
    private static final class ContextKey extends KeyMapping {
        private final KeyContext context;
        ContextKey(String name, KeyContext context, InputConstants.Type type, int code, String category) {
            super(name, type, code, category); this.context = context;
        }
        private boolean active() {
            if (context instanceof KeyContext.Custom custom) return custom.isActive();
            return context == KeyContext.UNIVERSAL || (context == KeyContext.IN_GAME ? Minecraft.getInstance().screen == null : Minecraft.getInstance().screen != null);
        }
        @Override public boolean isDown() { return active() && super.isDown(); }
        @Override public boolean consumeClick() {
            boolean consumed = super.consumeClick();
            return active() && consumed;
        }
    }
}
