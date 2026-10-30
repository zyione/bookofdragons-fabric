package net.magister.liberanimalium.body;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;

public final class StuckArrows {
   public static final int MOST = 24;
   private static final String LIST = "arrows";
   private static final String BONE = "bone";
   private static final String SEED = "seed";
   private static String through = "";

   private StuckArrows() {
   }

   public static CompoundTag with(CompoundTag tag, String bone, int seed) {
      ListTag list = tag.getList("arrows", 10).copy();
      CompoundTag one = new CompoundTag();
      one.putString("bone", bone);
      one.putInt("seed", seed);
      list.add(one);

      while (list.size() > 24) {
         list.remove(0);
      }

      CompoundTag out = new CompoundTag();
      out.put("arrows", list);
      return out;
   }

   public static CompoundTag withoutOldest(CompoundTag tag) {
      ListTag list = tag.getList("arrows", 10).copy();
      if (!list.isEmpty()) {
         list.remove(0);
      }

      CompoundTag out = new CompoundTag();
      out.put("arrows", list);
      return out;
   }

   public static int count(CompoundTag tag) {
      return tag.getList("arrows", 10).size();
   }

   public static List<StuckArrows.Arrow> read(CompoundTag tag) {
      ListTag list = tag.getList("arrows", 10);
      if (list.isEmpty()) {
         return List.of();
      }

      List<StuckArrows.Arrow> out = new ArrayList<>(list.size());

      for (int i = 0; i < list.size(); i++) {
         CompoundTag one = list.getCompound(i);
         out.add(new StuckArrows.Arrow(one.getString("bone"), one.getInt("seed")));
      }

      return out;
   }

   public static int fallTicks(int count) {
      return 20 * Math.max(1, 30 - count);
   }

   public static boolean through(String bone, BooleanSupplier blow) {
      String was = through;
      through = bone;

      try {
         return blow.getAsBoolean();
      } finally {
         through = was;
      }
   }

   public static boolean struck(FaunaBase body, DamageSource source, boolean landed) {
      if (landed && !body.level().isClientSide()) {
         if (source.getDirectEntity() instanceof AbstractArrow arrow && !(arrow instanceof ThrownTrident) && arrow.getPierceLevel() <= 0) {
            body.getEntityData()
               .set(
                  FaunaBase.DATA_STUCK_ARROWS, with((CompoundTag)body.getEntityData().get(FaunaBase.DATA_STUCK_ARROWS), through, body.getRandom().nextInt())
               );
            return landed;
         } else {
            return landed;
         }
      } else {
         return landed;
      }
   }

   public static void tick(FaunaBase body) {
      CompoundTag stuck = (CompoundTag)body.getEntityData().get(FaunaBase.DATA_STUCK_ARROWS);
      int count = count(stuck);
      if (count > 0 && (body.tickCount + body.getId()) % fallTicks(count) == 0) {
         body.getEntityData().set(FaunaBase.DATA_STUCK_ARROWS, withoutOldest(stuck));
      }
   }

   public record Arrow(String bone, int seed) {
   }
}
