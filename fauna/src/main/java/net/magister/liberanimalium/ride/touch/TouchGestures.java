package net.magister.liberanimalium.ride.touch;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.magister.liberanimalium.body.touch.TouchState;
import net.magister.liberanimalium.network.TouchGestureMessage;
import net.magister.liberanimalium.platform.client.ArmPoses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class TouchGestures {
   private static final Map<Integer, UUID> BY_ENTITY_ID = new HashMap<>();
   private static final float ARM_LEVEL = (float) (-Math.PI / 2);
   private static final float ARM_INWARD = (float) (Math.PI / 10);
   public static final ArmPose REACHING_POSE = ArmPoses.custom("liberanimalium:touch_reach", false, TouchGestures::pose);

   private TouchGestures() {
   }

   private static void pose(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
      ModelPart limb = arm == HumanoidArm.RIGHT ? model.rightArm : model.leftArm;
      float weight = TouchEase.weight(entity.getUUID());
      boolean brush = TouchEase.kind(entity.getUUID()) == TouchState.Gesture.BRUSH;
      float x = brush ? limb.xRot * 0.5F - (float) (Math.PI / 5) : (float) (-Math.PI / 2);
      float y = brush ? 0.0F : (arm == HumanoidArm.RIGHT ? (float) (Math.PI / 10) : (float) (-Math.PI / 10));
      limb.xRot = Mth.lerp(weight, limb.xRot, x);
      limb.yRot = Mth.lerp(weight, limb.yRot, y);
      limb.zRot = Mth.lerp(weight, limb.zRot, 0.0F);
   }

   public static TouchState.Gesture gesture(Player player) {
      return TouchState.of(player);
   }

   public static boolean reaching(Player player) {
      return TouchState.of(player) == TouchState.Gesture.REACH;
   }

   public static boolean reaching(int entityId) {
      UUID id = BY_ENTITY_ID.get(entityId);
      return id != null && TouchState.of(id, true) == TouchState.Gesture.REACH;
   }

   public static void accept(TouchGestureMessage msg) {
      Minecraft mc = Minecraft.getInstance();
      if ((mc.level == null ? null : mc.level.getEntity(msg.playerId())) instanceof Player player) {
         TouchState.set(player.getUUID(), true, msg.gesture());
         if (msg.gesture() == TouchState.Gesture.NONE) {
            BY_ENTITY_ID.remove(msg.playerId());
         } else {
            BY_ENTITY_ID.put(msg.playerId(), player.getUUID());
         }
      }
   }

   public static void clear() {
      BY_ENTITY_ID.clear();
      TouchState.clear(true);
      TouchEase.clear();
   }
}
