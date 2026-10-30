package net.magister.liberanimalium.fauna.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.behavior.Ladders;
import net.magister.liberanimalium.fauna.drive.Drives;
import net.magister.liberanimalium.fauna.maneuver.MoveStyles;

public final class ExpressionSpace {
   private static volatile Set<String> cached;
   private static volatile int cachedDrives = -1;
   private static volatile int cachedLadders = -1;

   private ExpressionSpace() {
   }

   public static Set<String> all() {
      int drives = Drives.all().size();
      int ladders = Ladders.ladderedDrives().size();
      Set<String> out = cached;
      if (out == null || cachedDrives != drives || cachedLadders != ladders) {
         out = derive();
         cached = out;
         cachedDrives = drives;
         cachedLadders = ladders;
      }

      return out;
   }

   public static boolean contains(String key) {
      return all().contains(key);
   }

   public static List<String> nearMisses(String key, int limit) {
      List<String> ranked = new ArrayList<>(all());
      ranked.sort((a, b) -> Integer.compare(sharedPrefix(b, key), sharedPrefix(a, key)));
      return List.copyOf(ranked.subList(0, Math.min(limit, ranked.size())));
   }

   private static int sharedPrefix(String a, String b) {
      int n = Math.min(a.length(), b.length());
      int i = 0;

      while (i < n && a.charAt(i) == b.charAt(i)) {
         i++;
      }

      return i;
   }

   private static Set<String> derive() {
      Set<String> out = new LinkedHashSet<>();

      for (String drive : Drives.all()) {
         out.add(ExpressionKey.drive(drive, ExpressionKey.Edge.BEGIN).key());
         out.add(ExpressionKey.drive(drive, ExpressionKey.Edge.END).key());
      }

      for (String drive : Ladders.ladderedDrives()) {
         for (String rung : Ladders.rungsOf(drive)) {
            out.add(ExpressionKey.rung(drive, rung, ExpressionKey.Edge.ENTER).key());
            out.add(ExpressionKey.rung(drive, rung, ExpressionKey.Edge.EXIT).key());
         }
      }

      for (MoveStyles.Primitive beat : MoveStyles.Primitive.values()) {
         out.add(ExpressionKey.beat(beat.name(), ExpressionKey.Edge.BEGIN).key());
         out.add(ExpressionKey.beat(beat.name(), ExpressionKey.Edge.END).key());
      }

      for (MoveMode from : MoveMode.values()) {
         for (MoveMode to : MoveMode.values()) {
            if (from != to) {
               out.add(ExpressionKey.mode(from.name(), to.name()).key());
            }
         }
      }

      for (PoseFlag pose : PoseFlag.values()) {
         out.add(ExpressionKey.pose(pose, ExpressionKey.Edge.RAISE).key());
         out.add(ExpressionKey.pose(pose, ExpressionKey.Edge.DROP).key());
      }

      for (ExpressionKey.Body body : ExpressionKey.Body.values()) {
         out.add(ExpressionKey.body(body).key());
      }

      return Collections.unmodifiableSet(out);
   }
}
