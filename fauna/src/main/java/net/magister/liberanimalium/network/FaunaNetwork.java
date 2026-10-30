package net.magister.liberanimalium.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.net.Channel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public final class FaunaNetwork {
   private static final String PROTOCOL_VERSION = "8";
   private static final Channel INSTANCE = Services.network().channel(new ResourceLocation("liberanimalium", "main"), "8", false);
   private static final List<Consumer<Channel>> MORE = new ArrayList<>();

   private FaunaNetwork() {
   }

   public static void register() {
      INSTANCE.toServer("control_boost", ControlMessageBoost.class, ControlMessageBoost::encode, ControlMessageBoost::decode, ControlMessageBoost::handle);
      INSTANCE.toServer("control_brake", ControlMessageBrake.class, ControlMessageBrake::encode, ControlMessageBrake::decode, ControlMessageBrake::handle);
      INSTANCE.toServer(
         "control_free_look", ControlMessageFreeLook.class, ControlMessageFreeLook::encode, ControlMessageFreeLook::decode, ControlMessageFreeLook::handle
      );
      INSTANCE.toServer(
         "control_primary_attack",
         ControlMessagePrimaryAttack.class,
         ControlMessagePrimaryAttack::encode,
         ControlMessagePrimaryAttack::decode,
         ControlMessagePrimaryAttack::handle
      );
      INSTANCE.toServer(
         "control_secondary_ability",
         ControlMessageSecondaryAbility.class,
         ControlMessageSecondaryAbility::encode,
         ControlMessageSecondaryAbility::decode,
         ControlMessageSecondaryAbility::handle
      );
      INSTANCE.toServer("control_sprint", ControlMessageSprint.class, ControlMessageSprint::encode, ControlMessageSprint::decode, ControlMessageSprint::handle);
      INSTANCE.toServer("part_sync", PartSyncPayload.class, PartSyncPayload::encode, PartSyncPayload::decode, PartSyncPayload::handle);
      INSTANCE.toServer(
         "flight_tells_sync", FlightTellsSyncMessage.class, FlightTellsSyncMessage::encode, FlightTellsSyncMessage::decode, FlightTellsSyncMessage::handle
      );
      INSTANCE.toServer(
         "control_throttle_sync",
         ControlMessageThrottleSync.class,
         ControlMessageThrottleSync::encode,
         ControlMessageThrottleSync::decode,
         ControlMessageThrottleSync::handle
      );
      INSTANCE.toClient(
         "sync_omni_attack_definitions",
         SyncOmniAttackDefinitionsPacket.class,
         SyncOmniAttackDefinitionsPacket::encode,
         SyncOmniAttackDefinitionsPacket::decode,
         SyncOmniAttackDefinitionsPacket::handle
      );
      INSTANCE.toServer("control_roll", ControlMessageRoll.class, ControlMessageRoll::encode, ControlMessageRoll::decode, ControlMessageRoll::handle);
      INSTANCE.toClient("sync_data", SyncDataPacket.class, SyncDataPacket::encode, SyncDataPacket::decode, SyncDataPacket::handle);
      INSTANCE.toServer("set_command", SetCommandMessage.class, SetCommandMessage::encode, SetCommandMessage::decode, SetCommandMessage::handle);
      INSTANCE.toServer("cycle_stance", CycleStanceMessage.class, CycleStanceMessage::encode, CycleStanceMessage::decode, CycleStanceMessage::handle);
      INSTANCE.toServer(
         "fauna_debug_toggle", FaunaDebugToggleMessage.class, FaunaDebugToggleMessage::encode, FaunaDebugToggleMessage::decode, FaunaDebugToggleMessage::handle
      );
      INSTANCE.toClient("sync_qte", SyncQtePacket.class, SyncQtePacket::encode, SyncQtePacket::decode, SyncQtePacket::handle);
      INSTANCE.toClient(
         "fauna_expression_event",
         FaunaExpressionEventMessage.class,
         FaunaExpressionEventMessage::encode,
         FaunaExpressionEventMessage::decode,
         FaunaExpressionEventMessage::handle
      );
      INSTANCE.toClient("touch_gesture", TouchGestureMessage.class, TouchGestureMessage::encode, TouchGestureMessage::decode, TouchGestureMessage::handle);
      INSTANCE.toServer(
         "control_tame_key", ControlMessageTameKey.class, ControlMessageTameKey::encode, ControlMessageTameKey::decode, ControlMessageTameKey::handle
      );
      INSTANCE.toServer(
         "control_wall_hit", ControlMessageWallHit.class, ControlMessageWallHit::encode, ControlMessageWallHit::decode, ControlMessageWallHit::handle
      );
      INSTANCE.toServer(
         "control_dismount", ControlMessageDismount.class, ControlMessageDismount::encode, ControlMessageDismount::decode, ControlMessageDismount::handle
      );
      INSTANCE.toServer("control_grab", ControlMessageGrab.class, ControlMessageGrab::encode, ControlMessageGrab::decode, ControlMessageGrab::handle);
      INSTANCE.toServer(
         "control_struggle", ControlMessageStruggle.class, ControlMessageStruggle::encode, ControlMessageStruggle::decode, ControlMessageStruggle::handle
      );
      INSTANCE.toClient(
         "clear_attack_particles",
         ClearAttackParticlesMessage.class,
         ClearAttackParticlesMessage::encode,
         ClearAttackParticlesMessage::decode,
         ClearAttackParticlesMessage::handle
      );
      INSTANCE.toServer("control_dodge", ControlMessageDodge.class, ControlMessageDodge::encode, ControlMessageDodge::decode, ControlMessageDodge::handle);
      INSTANCE.toServer("ride_place", RidePlaceMessage.class, RidePlaceMessage::encode, RidePlaceMessage::decode, RidePlaceMessage::handle);
      INSTANCE.toClient("dodge_refused", DodgeRefusedMessage.class, DodgeRefusedMessage::encode, DodgeRefusedMessage::decode, DodgeRefusedMessage::handle);
      INSTANCE.toServer("grip_stance", GripStancePayload.class, GripStancePayload::encode, GripStancePayload::decode, GripStancePayload::handle);
      INSTANCE.toClient("breath_emit", BreathEmitMessage.class, BreathEmitMessage::encode, BreathEmitMessage::decode, BreathEmitMessage::handle);
      INSTANCE.toClient("grip_deck", GripDeckPayload.class, GripDeckPayload::encode, GripDeckPayload::decode, GripDeckPayload::handle);

      for (Consumer<Channel> packets : MORE) {
         packets.accept(INSTANCE);
      }
   }

   public static void extend(Consumer<Channel> packets) {
      MORE.add(packets);
   }

   public static <MSG> void sendToServer(MSG message) {
      INSTANCE.sendToServer(message);
   }

   public static <MSG> void sendToAllClients(MSG message) {
      INSTANCE.sendToAll(message);
   }

   public static <MSG> void sendToClientsTracking(MSG message, Entity entityToTrack) {
      INSTANCE.sendToTracking(message, entityToTrack);
   }

   public static <MSG> void sendToClientsTrackingAndSelf(MSG message, Entity entityToTrack) {
      INSTANCE.sendToTrackingAndSelf(message, entityToTrack);
   }

   public static <MSG> void sendToClientsNear(MSG message, ServerLevel level, double x, double y, double z, double radius) {
      INSTANCE.sendToNear(message, level, x, y, z, radius);
   }

   public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
      INSTANCE.sendToPlayer(message, player);
   }
}
