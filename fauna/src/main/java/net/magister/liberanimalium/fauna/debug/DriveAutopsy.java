package net.magister.liberanimalium.fauna.debug;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.RelationType;
import net.magister.liberanimalium.fauna.arbiter.Arbiter;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.drive.Drive;
import net.magister.liberanimalium.fauna.drive.DriveContext;
import net.magister.liberanimalium.fauna.drive.EnmityDrive;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.drive.RivalDrive;
import net.magister.liberanimalium.fauna.know.Representation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DriveAutopsy {
   private static final Logger LOGGER = LoggerFactory.getLogger("FaunaAutopsy");
   private static final float ALIVE = 0.1F;
   private static final float DEAD = 0.02F;
   private static final Set<String> WATCHED = Set.of("play", "rival", "enmity", "prey");
   private static final Map<UUID, float[]> LAST_RAW = new HashMap<>();

   private DriveAutopsy() {
   }

   public static void watch(AnimalHandle handle, FaunaMind mind) {
      if (!FaunaDebug.anyoneWatching()) {
         if (!LAST_RAW.isEmpty()) {
            LAST_RAW.clear();
         }
      } else {
         Arbiter arbiter = mind.arbiter();
         List<Drive> drives = arbiter.drives();
         float[] last = LAST_RAW.computeIfAbsent(handle.faunaId(), id -> {
            float[] fresh = new float[drives.size()];
            Arrays.fill(fresh, -1.0F);
            return fresh;
         });

         for (int i = 0; i < drives.size(); i++) {
            Drive drive = drives.get(i);
            float raw = arbiter.rawOf(i);
            float prev = last[i];
            last[i] = raw;
            if (WATCHED.contains(drive.id())) {
               boolean holderNow = arbiter.holder() == drive;
               if (!(prev < 0.1F) && !(raw > 0.02F) && (holderNow || !(prev < 0.25F))) {
                  LOGGER.debug(
                     "[FAUNA-AUTOPSY] {} t={} {} collapsed raw {}->{} (smoothed {} holder={} released={}) | {}",
                     new Object[]{
                        shortId(handle.faunaId()),
                        mind.context().age(),
                        drive.id(),
                        fmt(prev),
                        fmt(raw),
                        fmt(arbiter.scoreOf(i)),
                        holderNow,
                        arbiter.released(),
                        terms(drive.id(), mind.context())
                     }
                  );
               }
            }
         }
      }
   }

   private static String terms(String id, DriveContext context) {
      long now = context.age();
      switch (id) {
         case "enmity": {
            Representation rep = EnmityDrive.worstEnemy(context, null);
            if (rep == null) {
               return "no enemy elected (evicted, or relation no longer ATTACKS)";
            }

            double distance = context.estimate(rep).distanceTo(context.position());
            return "rep="
               + shortId(rep.id())
               + " menace="
               + fmt(EnmityDrive.menace(context, rep))
               + " intensity="
               + fmt(rep.relation().intensity())
               + " presence="
               + fmt(rep.presence())
               + " find="
               + fmt(rep.find(now))
               + " alarm="
               + fmt(rep.alarm())
               + " dist="
               + fmt((float)distance)
               + " unreachable="
               + rep.unreachableAt(now, 200);
         }
         case "rival": {
            Representation rep = RivalDrive.worstRival(context);
            if (rep == null) {
               return "no rival elected: " + whyNoCandidate(context, RelationType.RIVAL);
            }

            double distance = context.estimate(rep).distanceTo(context.position());
            return "rep="
               + shortId(rep.id())
               + " held="
               + fmt(RivalDrive.held(context, rep))
               + " alarm="
               + fmt(rep.alarm())
               + " find="
               + fmt(rep.find(now))
               + " gradient="
               + fmt(RivalDrive.gradientOf(context, rep))
               + " dist="
               + fmt((float)distance)
               + " settled="
               + context.settledWith(rep.id())
               + " dispute="
               + context.disputeOpenWith(rep.id());
         }
         case "play": {
            Representation rep = PlayDrive.partner(context);
            if (rep == null) {
               return "no partner: " + whyNoPartner(context);
            }

            double distance = context.estimate(rep).distanceTo(context.position());
            return "rep="
               + shortId(rep.id())
               + " find="
               + fmt(rep.find(now))
               + " dist="
               + fmt((float)distance)
               + " playing="
               + context.playingWith(rep.id())
               + " played="
               + context.playedWith(rep.id());
         }
         case "prey":
            return "hunger-side collapse; see the prey election on the panel";
         default:
            return "";
      }
   }

   private static String whyNoCandidate(DriveContext context, RelationType type) {
      StringBuilder out = new StringBuilder();

      for (Representation rep : context.known()) {
         if (rep.relation().type() == type) {
            if (out.length() > 0) {
               out.append(", ");
            }

            out.append(shortId(rep.id()))
               .append("[alarm=")
               .append(fmt(rep.alarm()))
               .append(" settled=")
               .append(context.settledWith(rep.id()))
               .append(" dispute=")
               .append(context.disputeOpenWith(rep.id()))
               .append(" find=")
               .append(fmt(rep.find(context.age())))
               .append(']');
         }
      }

      return out.length() > 0 ? "candidates all score 0: " + out : "NO rep holds type " + type + " (flipped away, or evicted)";
   }

   private static String whyNoPartner(DriveContext context) {
      StringBuilder out = new StringBuilder();

      for (Representation rep : context.known()) {
         if (out.length() > 0) {
            out.append(", ");
         }

         out.append(shortId(rep.id()))
            .append("[played=")
            .append(context.playedWith(rep.id()))
            .append(" playing=")
            .append(context.playingWith(rep.id()))
            .append(" type=")
            .append(rep.relation().type())
            .append(" find=")
            .append(fmt(rep.find(context.age())))
            .append(']');
      }

      return out.length() > 0 ? out.toString() : "nothing is known at all";
   }

   private static String shortId(UUID id) {
      String s = id.toString();
      return s.substring(0, 8);
   }

   private static String fmt(float value) {
      return String.format(Locale.ROOT, "%.3f", value);
   }
}
