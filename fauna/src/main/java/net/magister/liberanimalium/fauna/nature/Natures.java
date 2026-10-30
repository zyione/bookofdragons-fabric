package net.magister.liberanimalium.fauna.nature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.behavior.Buck;
import net.magister.liberanimalium.fauna.behavior.Contest;
import net.magister.liberanimalium.fauna.behavior.Escort;
import net.magister.liberanimalium.fauna.behavior.Explore;
import net.magister.liberanimalium.fauna.behavior.Flee;
import net.magister.liberanimalium.fauna.behavior.Forage;
import net.magister.liberanimalium.fauna.behavior.Hunt;
import net.magister.liberanimalium.fauna.behavior.Investigate;
import net.magister.liberanimalium.fauna.behavior.Loaf;
import net.magister.liberanimalium.fauna.behavior.Obey;
import net.magister.liberanimalium.fauna.behavior.Perch;
import net.magister.liberanimalium.fauna.behavior.Pilfer;
import net.magister.liberanimalium.fauna.behavior.Play;
import net.magister.liberanimalium.fauna.behavior.Rejoin;
import net.magister.liberanimalium.fauna.behavior.Rest;
import net.magister.liberanimalium.fauna.behavior.Shoo;
import net.magister.liberanimalium.fauna.behavior.Solicit;
import net.magister.liberanimalium.fauna.behavior.Spar;
import net.magister.liberanimalium.fauna.behavior.Unstick;
import net.magister.liberanimalium.fauna.behavior.Wander;
import net.magister.liberanimalium.fauna.behavior.Warn;
import net.magister.liberanimalium.fauna.drive.BegDrive;
import net.magister.liberanimalium.fauna.drive.BuckDrive;
import net.magister.liberanimalium.fauna.drive.CommandDrive;
import net.magister.liberanimalium.fauna.drive.EnmityDrive;
import net.magister.liberanimalium.fauna.drive.ExploreDrive;
import net.magister.liberanimalium.fauna.drive.ForageDrive;
import net.magister.liberanimalium.fauna.drive.FriendDrive;
import net.magister.liberanimalium.fauna.drive.LoafDrive;
import net.magister.liberanimalium.fauna.drive.NoiseDrive;
import net.magister.liberanimalium.fauna.drive.PackDrive;
import net.magister.liberanimalium.fauna.drive.PerchDrive;
import net.magister.liberanimalium.fauna.drive.PilferDrive;
import net.magister.liberanimalium.fauna.drive.PlayDrive;
import net.magister.liberanimalium.fauna.drive.PreyDrive;
import net.magister.liberanimalium.fauna.drive.RestDrive;
import net.magister.liberanimalium.fauna.drive.RivalDrive;
import net.magister.liberanimalium.fauna.drive.SpaceDrive;
import net.magister.liberanimalium.fauna.drive.SparDrive;
import net.magister.liberanimalium.fauna.drive.StuckDrive;
import net.magister.liberanimalium.fauna.drive.ThreatDrive;
import net.magister.liberanimalium.fauna.drive.WanderDrive;
import net.magister.liberanimalium.fauna.drive.WarnDrive;
import net.magister.liberanimalium.fauna.know.Beckons;
import net.magister.liberanimalium.fauna.know.Bedding;
import net.magister.liberanimalium.fauna.know.Chests;
import net.magister.liberanimalium.fauna.know.Larder;
import net.magister.liberanimalium.fauna.know.Noises;
import net.magister.liberanimalium.fauna.know.Places;
import net.magister.liberanimalium.fauna.know.Territory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Natures {
   private static final Nature STANDARD = Nature.builder("standard")
      .pursue(CommandDrive::new, Obey::new, ObeyHandoff::new, Halter.class)
      .pursue(BuckDrive::new, Buck::new, BuckHandoff::new, Halter.class)
      .pursue(ThreatDrive::new, Flee::new, FleeHandoff::new)
      .pursue(WarnDrive::new, Warn::new, WarnHandoff::new)
      .pursue(StuckDrive::new, Unstick::new, UnstickHandoff::new)
      .pursue(PreyDrive::new, Hunt::new, HuntHandoff::new, Arms.class)
      .pursue(ForageDrive::new, Forage::new, ForageHandoff::new, Gullet.class)
      .pursue(BegDrive::new, Solicit::new, SolicitHandoff::new, Halter.class)
      .pursue(EnmityDrive::new, Hunt::new, FightHandoff::new, Arms.class)
      .pursue(RivalDrive::new, Contest::new, ContestHandoff::new, Arms.class)
      .pursue(SpaceDrive::new, Shoo::new, ShooHandoff::new)
      .pursue(PackDrive::new, Rejoin::new, RejoinHandoff::new)
      .pursue(FriendDrive::new, Escort::new, EscortHandoff::new)
      .pursue(PlayDrive::new, Play::new, PlayHandoff::new)
      .pursue(SparDrive::new, Spar::new, SparHandoff::new, Arms.class, Halter.class)
      .pursue(RestDrive::new, Rest::new, RestHandoff::new)
      .pursue(NoiseDrive::new, Investigate::new, InvestigateHandoff::new)
      .pursue(ExploreDrive::new, Explore::new, ExploreHandoff::new)
      .pursue(WanderDrive::new, Wander::new, WanderHandoff::new)
      .faculty(Noises::new)
      .faculty(Bedding::new)
      .faculty(Territory::new)
      .faculty(Larder::new)
      .faculty(Places::new)
      .build();
   private static final Nature UNRIDEABLE = STANDARD.without("unrideable", "buck");
   private static final Nature CATALOGUE = STANDARD.extend("catalogue")
      .pursue(LoafDrive::new, Loaf::new, LoafHandoff::new)
      .faculty(Chests::new)
      .pursue(PerchDrive::new, Perch::new, PerchHandoff::new, Halter.class)
      .faculty(Beckons::new)
      .pursue(PilferDrive::new, Pilfer::new, PilferHandoff::new, Gullet.class)
      .build();
   private static final Map<String, List<String>> CATALOGUE_FACULTIES = Map.of("loaf", List.of("chests"), "perch", List.of("beckons"));
   private static final Logger LOGGER = LoggerFactory.getLogger("Natures");
   private static final Nature GRAZER = Nature.builder("grazer")
      .pursue(ThreatDrive::new, Flee::new, FleeHandoff::new)
      .pursue(WarnDrive::new, Warn::new, WarnHandoff::new)
      .pursue(StuckDrive::new, Unstick::new, UnstickHandoff::new)
      .pursue(SpaceDrive::new, Shoo::new, ShooHandoff::new)
      .pursue(PackDrive::new, Rejoin::new, RejoinHandoff::new)
      .pursue(FriendDrive::new, Escort::new, EscortHandoff::new)
      .pursue(PlayDrive::new, Play::new, PlayHandoff::new)
      .pursue(RestDrive::new, Rest::new, RestHandoff::new)
      .pursue(NoiseDrive::new, Investigate::new, InvestigateHandoff::new)
      .pursue(ExploreDrive::new, Explore::new, ExploreHandoff::new)
      .pursue(WanderDrive::new, Wander::new, WanderHandoff::new)
      .faculty(Noises::new)
      .faculty(Bedding::new)
      .faculty(Places::new)
      .build();

   private Natures() {
   }

   public static Nature standard() {
      return STANDARD;
   }

   public static Nature unrideable() {
      return UNRIDEABLE;
   }

   public static Nature grazer() {
      return GRAZER;
   }

   public static Nature catalogue() {
      return CATALOGUE;
   }

   public static Nature shape(Nature base, SpeciesProfile.DriveSet drives, String species) {
      if (drives.isNone()) {
         return base;
      }

      Nature out = base;
      List<String> drop = new ArrayList<>();

      for (String id : drives.without()) {
         if (base.ids().contains(id)) {
            drop.add(id);
         } else {
            LOGGER.warn("[Natures] {}: drives.without names '{}', which {} does not pursue", new Object[]{species, id, base.name()});
         }
      }

      if (!drop.isEmpty()) {
         out = out.without(base.name() + "-" + String.join("-", drop), drop.toArray(String[]::new));
      }

      List<String> add = new ArrayList<>();

      for (String id : drives.with()) {
         if (!out.ids().contains(id)) {
            if (CATALOGUE.recipeOf(id) != null) {
               add.add(id);
            } else {
               LOGGER.warn("[Natures] {}: drives.with names '{}', which the catalogue does not compose", species, id);
            }
         }
      }

      if (add.isEmpty()) {
         return out;
      }

      Nature.Builder builder = out.extend(out.name() + "+" + String.join("+", add));

      for (String id : add) {
         builder.pursue(id, CATALOGUE.recipeOf(id), CATALOGUE.needsOf(id));

         for (String faculty : CATALOGUE_FACULTIES.getOrDefault(id, List.of())) {
            builder.facultyIfAbsent(faculty, CATALOGUE.facultyRecipeOf(faculty));
         }
      }

      return builder.build();
   }
}
