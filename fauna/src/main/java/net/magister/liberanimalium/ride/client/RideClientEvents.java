package net.magister.liberanimalium.ride.client;

import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.body.locomotion.RideTuning;
import net.magister.liberanimalium.body.locomotion.SurfaceFrame;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.chassis.FaunaBase;
import net.magister.liberanimalium.chassis.Notices;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.chassis.component.ranged.IPrimaryAttackHandler;
import net.magister.liberanimalium.compat.ClientFrame;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.debug.client.CascadeDebugHUD;
import net.magister.liberanimalium.interop.ForeignArms;
import net.magister.liberanimalium.interop.ForeignCameras;
import net.magister.liberanimalium.interop.ForeignModels;
import net.magister.liberanimalium.interop.SubLevels;
import net.magister.liberanimalium.network.ControlMessageBoost;
import net.magister.liberanimalium.network.ControlMessageBrake;
import net.magister.liberanimalium.network.ControlMessageDismount;
import net.magister.liberanimalium.network.ControlMessageFreeLook;
import net.magister.liberanimalium.network.ControlMessageGrab;
import net.magister.liberanimalium.network.ControlMessagePrimaryAttack;
import net.magister.liberanimalium.network.ControlMessageSecondaryAbility;
import net.magister.liberanimalium.network.ControlMessageSprint;
import net.magister.liberanimalium.network.ControlMessageStruggle;
import net.magister.liberanimalium.network.ControlMessageTameKey;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.network.RidePlaceMessage;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.event.CameraAngles;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.magister.liberanimalium.platform.event.CommonEvents;
import net.magister.liberanimalium.platform.event.FieldOfView;
import net.magister.liberanimalium.platform.event.HudLayer;
import net.magister.liberanimalium.qte.QteInput;
import net.magister.liberanimalium.qte.client.ClientQteState;
import net.magister.liberanimalium.qte.client.QteOverlay;
import net.magister.liberanimalium.ride.flight.rotation.FaunaCameraState;
import net.magister.liberanimalium.ride.touch.TouchEase;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

public final class RideClientEvents {
   private static Boolean lastSentBoost;
   private static Boolean lastSentBrake;
   private static Boolean lastSentSprint;
   private static Boolean lastSentGrab;
   private static boolean sprintToggled = false;
   private static boolean ridePlaceSent = false;
   private static boolean lastSprintKeyState = false;
   private static boolean lastPrimaryAbilityState = false;
   private static boolean lastFreeCameraState = false;
   private static long dismountArmedAt = Long.MIN_VALUE;
   private static long lastRenderNanoTime = 0L;
   private static boolean wasRollingForSmoother = false;
   private static CameraType preMountCameraType = null;
   private static CameraType lastRidingCameraType = null;
   private static double smoothedFovOffset = 0.0;
   private static boolean wasUp;
   private static boolean wasDown;
   private static boolean wasLeft;
   private static boolean wasRight;

   private RideClientEvents() {
   }

   public static void clientInit() {
      ClientEvents.CAMERA_ANGLES.register(RideClientEvents::onCameraSetup);
      ClientEvents.KEY.register(RideClientEvents::onKeyInput);
      ClientEvents.SCREEN_OPENING.register(RideClientEvents::onScreenOpen);
      ClientEvents.MOUSE_BUTTON.register(RideClientEvents::onMouseInput);
      ClientEvents.HUD_LAYER_PRE.register(RideClientEvents::onRenderGuiOverlayPre);
      ClientEvents.HUD_AFTER_HOTBAR.register(RideClientEvents::onRenderGuiOverlay);
      ClientEvents.CLIENT_TICK_END.register(RideClientEvents::onClientTick);
      ClientEvents.FRAME_START.register(RideClientEvents::onRenderTick);
      ClientEvents.FOV.register(RideClientEvents::onComputeFov);
      CommonEvents.MOUNT.register(RideClientEvents::onEntityMount);
   }

   public static void onCameraSetup(CameraAngles event) {
      if (CameraShakeUtil.isShaking()) {
         float pitchOffset = CameraShakeUtil.getShakePitch();
         float yawOffset = CameraShakeUtil.getShakeYaw();
         event.setPitch(event.getPitch() + pitchOffset);
         event.setYaw(event.getYaw() + yawOffset);
      }

      Player player = Minecraft.getInstance().player;
      if (!(player != null && player.getVehicle() instanceof FaunaBase body)) {
         CameraLeanData.reset();
         FaunaCameraState.clearRoll();
      } else if (ClimbEngine.lookOf(body) != null) {
         cameraOnFace(event);
         CameraLeanData.reset();
      } else if (body.supportsThreeDMedium()) {
         FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (rotComp != null && rotComp.isRolling()) {
            camera3D(event, body, rotComp);
         } else {
            camera2D(event, body, rotComp);
         }
      } else {
         FaunaCameraState.clearRoll();
      }
   }

   private static void cameraOnFace(CameraAngles event) {
      if (FaunaCameraState.hasStableAngles()) {
         event.setYaw(FaunaCameraState.getStableYaw());
         event.setPitch(FaunaCameraState.getStablePitch());
         event.setRoll(event.getRoll() + FaunaCameraState.getStableRoll());
      }
   }

   private static void camera3D(CameraAngles event, FaunaBase body, FaunaFlightRotationComponent rotComp) {
      if (FaunaCameraState.hasStableAngles()) {
         float stableYaw = FaunaCameraState.getStableYaw();
         float stablePitch = FaunaCameraState.getStablePitch();
         float stableRoll = FaunaCameraState.getStableRoll();
         int timer = body.getFlightModeTransitionTimer();
         float weight = 1.0F;
         if (timer > 0) {
            float partialTicks = event.getPartialTick();
            float total = FaunaBase.getTransitionDuration();
            float smoothTimer = Math.max(0.0F, timer - partialTicks);
            weight = 1.0F - smoothTimer / total;
         }

         event.setYaw(stableYaw);
         event.setPitch(stablePitch);
         event.setRoll(event.getRoll() + stableRoll * weight);
      } else {
         float bodyRoll = rotComp.getRoll(event.getPartialTick());
         int timer = body.getFlightModeTransitionTimer();
         if (timer > 0) {
            float partialTicks = event.getPartialTick();
            float total = FaunaBase.getTransitionDuration();
            float smoothTimer = Math.max(0.0F, timer - partialTicks);
            float weight = 1.0F - smoothTimer / total;
            bodyRoll *= weight;
         }

         event.setRoll(event.getRoll() + bodyRoll);
      }
   }

   private static void camera2D(CameraAngles event, FaunaBase body, FaunaFlightRotationComponent rotComp) {
      FaunaCameraState.clearRoll();
      float rollRadians = rotComp != null ? Mth.wrapDegrees(rotComp.getRoll(event.getPartialTick())) * (float) (Math.PI / 180.0) : 0.0F;
      int timer = body.getFlightModeTransitionTimer();
      float transitionRoll = 0.0F;
      if (timer > 0) {
         rollRadians = 0.0F;
         float partialTicks = event.getPartialTick();
         float total = FaunaBase.getTransitionDuration();
         float smoothTimer = Math.max(0.0F, timer - partialTicks);
         float weight = smoothTimer / total;
         transitionRoll = body.getCapturedTransitionRoll() * weight;
      }

      float rollDegrees = (float)Math.toDegrees(rollRadians) + transitionRoll;
      float pitchRadians = rotComp != null ? rotComp.getCurrentPitchRadians() : 0.0F;
      float yawSpeed = body.getYawSpeed();
      float pitchDegrees = (float)Math.toDegrees(pitchRadians);
      float leanMultiplier = 1.0F;
      float scaledRoll = rollDegrees * leanMultiplier;
      float bodyTurnSpeed = body.flightNumbers().turnRate() * body.getStatSheet().agility();
      float yawLeanScale = bodyTurnSpeed / 30.0F;
      CameraLeanData.updateTarget(scaledRoll, pitchDegrees, yawSpeed, yawLeanScale);
      CameraLeanData.update();
      if (RideSettings.isCameraRollEnabled()) {
         float cameraTilt = (float)CameraLeanData.getCameraTilt();
         float totalRoll = event.getRoll() + -rollDegrees + cameraTilt;
         event.setRoll(totalRoll);
      }

      event.setPitch(event.getPitch() + TurbulenceShake.pitch());
      event.setYaw(event.getYaw() + TurbulenceShake.yaw());
      event.setRoll(event.getRoll() + TurbulenceShake.roll());
   }

   public static void onKeyInput(int key, int scanCode, int action, int modifiers) {
      Minecraft mc = Minecraft.getInstance();
      Player player = mc.player;
      if (player != null) {
         RideKeys.Bound keys = player.getVehicle() instanceof FaunaBase ridden ? RideKeys.of(ridden) : RideKeys.NONE;

         for (RideKeys.Bound host : RideKeys.all()) {
            if (RideKeys.clicked(host.flightHud()) && !Services.platform().isProduction()) {
               FlightDebugHUD.toggle();
            }

            if (host != keys) {
               host.drain();
            }
         }

         struggleKeys(player, keys, key, scanCode);
         dismountKey(player, keys);
         heldKeys(mc, player, keys);
         if (RideKeys.clicked(keys.secondaryAbility())) {
            FaunaNetwork.sendToServer(new ControlMessageSecondaryAbility());
         }

         sprintKey(player, keys);
         mouseKeysOnKeyboard(player, keys);
      }
   }

   private static void struggleKeys(Player player, RideKeys.Bound keys, int key, int scanCode) {
      boolean struggling = player.getVehicle() instanceof FaunaBase mount && ClientQteState.tameCircleActiveFor(mount.getId());
      if (struggling) {
         RideKeys.silenceShared(keys, key, scanCode);

         for (int i = 0; i < keys.struggleLetters().length; i++) {
            while (RideKeys.clicked(keys.struggleLetters()[i])) {
               FaunaNetwork.sendToServer(new ControlMessageTameKey(i));
            }
         }
      }
   }

   private static void dismountKey(Player player, RideKeys.Bound keys) {
      while (RideKeys.clicked(keys.dismount())) {
         if (player.getVehicle() instanceof FaunaBase dismountFrom) {
            long var6 = player.level().getGameTime();
            boolean grounded = !dismountFrom.isFlying();
            if (!grounded && (dismountArmedAt == Long.MIN_VALUE || var6 - dismountArmedAt > RideTuning.doubleTapTicks(dismountFrom))) {
               dismountArmedAt = var6;
               Notices.tell(player, Component.translatable("liberanimalium.dismount.again"));
            } else {
               dismountArmedAt = Long.MIN_VALUE;
               FaunaNetwork.sendToServer(new ControlMessageDismount());
            }
         } else {
            dismountArmedAt = Long.MIN_VALUE;
         }
      }
   }

   private static void heldKeys(Minecraft mc, Player player, RideKeys.Bound keys) {
      if (player.getVehicle() instanceof FaunaBase) {
         boolean jumpPressed = mc.options.keyJump.isDown();
         if (lastSentBoost == null || lastSentBoost != jumpPressed) {
            lastSentBoost = jumpPressed;
            FaunaNetwork.sendToServer(new ControlMessageBoost(jumpPressed));
         }

         boolean sprintPressed = RideKeys.down(keys.sprint());
         if (lastSentBrake == null || lastSentBrake != sprintPressed) {
            lastSentBrake = sprintPressed;
            FaunaNetwork.sendToServer(new ControlMessageBrake(sprintPressed));
         }

         boolean grabPressed = RideKeys.down(keys.grab());
         if (lastSentGrab == null || lastSentGrab != grabPressed) {
            lastSentGrab = grabPressed;
            FaunaNetwork.sendToServer(new ControlMessageGrab(grabPressed));
         }
      } else {
         lastSentBoost = null;
         lastSentBrake = null;
         lastSentGrab = null;
      }
   }

   private static void sprintKey(Player player, RideKeys.Bound keys) {
      if (player.getVehicle() instanceof FaunaBase body && (!body.inThreeDMedium() || body.isQteActive())) {
         boolean sprintKeyDown = RideKeys.down(keys.sprint());
         boolean shouldSprint;
         if (body.isQteActive()) {
            shouldSprint = sprintKeyDown;
         } else if (RideSettings.isSprintToggleMode()) {
            if (sprintKeyDown && !lastSprintKeyState) {
               sprintToggled = !sprintToggled;
            }

            shouldSprint = sprintToggled;
         } else {
            shouldSprint = sprintKeyDown;
         }

         lastSprintKeyState = sprintKeyDown;
         if (lastSentSprint == null || lastSentSprint != shouldSprint) {
            lastSentSprint = shouldSprint;
            FaunaNetwork.sendToServer(new ControlMessageSprint(shouldSprint));
         }
      } else {
         lastSentSprint = null;
         if (sprintToggled) {
            sprintToggled = false;
         }
      }
   }

   private static void mouseKeysOnKeyboard(Player player, RideKeys.Bound keys) {
      if (player.getVehicle() instanceof FaunaBase body) {
         boolean primaryDown = RideKeys.down(keys.primaryAbility());
         if (primaryDown != lastPrimaryAbilityState) {
            lastPrimaryAbilityState = primaryDown;
            if (body.componentRegistry.get(IPrimaryAttackHandler.class) != null) {
               FaunaNetwork.sendToServer(new ControlMessagePrimaryAttack(primaryDown));
            }
         }

         boolean freeCamDown = RideKeys.down(keys.freeCamera());
         if (freeCamDown != lastFreeCameraState) {
            lastFreeCameraState = freeCamDown;
            FaunaNetwork.sendToServer(new ControlMessageFreeLook(freeCamDown));
         }
      }
   }

   public static void dodgeRefused(int bodyId) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.level != null && mc.level.getEntity(bodyId) instanceof FaunaBase body) {
         RollComponent roll = body.componentRegistry.get(RollComponent.class);
         if (roll != null) {
            roll.refused();
         }
      }
   }

   public static void onScreenOpen(Screen screen) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.getVehicle() instanceof FaunaBase) {
         FaunaNetwork.sendToServer(new ControlMessageFreeLook(false));
      }
   }

   public static void onMouseInput(int button, int action, int modifiers) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.screen == null) {
         if (mc.player.getVehicle() instanceof FaunaBase body) {
            if (RideKeys.onMouse(RideKeys.of(body).freeCamera(), button)) {
               if (action == 1) {
                  FaunaNetwork.sendToServer(new ControlMessageFreeLook(true));
               } else if (action == 0) {
                  FaunaNetwork.sendToServer(new ControlMessageFreeLook(false));
               }
            }

            if (RideKeys.onMouse(RideKeys.of(body).primaryAbility(), button) && body.componentRegistry.get(IPrimaryAttackHandler.class) != null) {
               if (action == 1) {
                  FaunaNetwork.sendToServer(new ControlMessagePrimaryAttack(true));
               } else if (action == 0) {
                  FaunaNetwork.sendToServer(new ControlMessagePrimaryAttack(false));
               }
            }
         }
      }
   }

   public static boolean onRenderGuiOverlayPre(HudLayer layer) {
      Minecraft mc = Minecraft.getInstance();
      return layer == HudLayer.JUMP_METER && mc.player != null && mc.player.getVehicle() instanceof FaunaBase;
   }

   private static void struggle() {
      Minecraft mc = Minecraft.getInstance();
      boolean held = mc.player != null && Grip.held(mc.player) && mc.screen == null;
      boolean up = held && mc.options.keyUp.isDown();
      boolean down = held && mc.options.keyDown.isDown();
      boolean left = held && mc.options.keyLeft.isDown();
      boolean right = held && mc.options.keyRight.isDown();
      if (up && !wasUp) {
         sendStruggle(QteInput.Dir.UP);
      }

      if (down && !wasDown) {
         sendStruggle(QteInput.Dir.DOWN);
      }

      if (left && !wasLeft) {
         sendStruggle(QteInput.Dir.LEFT);
      }

      if (right && !wasRight) {
         sendStruggle(QteInput.Dir.RIGHT);
      }

      wasUp = up;
      wasDown = down;
      wasLeft = left;
      wasRight = right;
   }

   private static void sendStruggle(QteInput.Dir dir) {
      FaunaNetwork.sendToServer(new ControlMessageStruggle(dir));
   }

   public static void onRenderGuiOverlay(GuiGraphics guiGraphics, float partialTick) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.options.hideGui && mc.player.getVehicle() instanceof FaunaBase ridden && ridden.isQteActive()) {
         QteOverlay.render(guiGraphics, ridden.getId(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
      }

      FaunaBase holder = mc.player == null ? null : Grip.holderOf(mc.player);
      if (holder != null && !mc.options.hideGui) {
         QteOverlay.render(guiGraphics, holder.getId(), mc.getWindow().getGuiScaledWidth(), mc.getWindow().getGuiScaledHeight());
      }

      FlightDebugHUD.render(guiGraphics, partialTick);
      if (FlightDebugHUD.isEnabled()) {
         CascadeDebugHUD.render(guiGraphics, partialTick);
      }
   }

   public static void onClientTick() {
      struggle();
      Minecraft mcTick = Minecraft.getInstance();
      if (mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase mount && ClientQteState.tameCircleActiveFor(mount.getId())) {
         RideKeys.silenceActiveStruggle(RideKeys.of(mount));
      }

      ForeignCameras.riding(mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase);
      ForeignModels.look();
      Vec3 ridePlace = mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase ridden && ridden.isControlledByLocalInstance() ? ridden.subLevelPlace() : null;
      if (ridePlace != null || ridePlaceSent && mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase) {
         FaunaNetwork.sendToServer(new RidePlaceMessage(ridePlace));
      }

      ridePlaceSent = ridePlace != null;
      RideSettings.riding(mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase on ? RideKeys.hostOf(on) : null);
      if (mcTick.player != null && mcTick.player.getVehicle() instanceof FaunaBase body) {
         FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (rotComp != null && rotComp.isRolling()) {
            rotComp.captureTickIntent();
         }
      }
   }

   public static void onRenderTick() {
      holdForeignArms();
      long now = System.nanoTime();
      float frameDelta;
      if (lastRenderNanoTime == 0L) {
         frameDelta = 0.016F;
      } else {
         frameDelta = (float)((now - lastRenderNanoTime) / 1.0E9);
         if (frameDelta > 0.2F || frameDelta <= 0.0F) {
            frameDelta = 0.016F;
         }
      }

      lastRenderNanoTime = now;
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && !mc.isPaused()) {
         if (!(mc.player.getVehicle() instanceof FaunaBase body)) {
            TurbulenceShake.reset();
            SpeedLines.reset();
         } else {
            TurbulenceShake.update(body, frameDelta);
            SpeedLines.update(body, frameDelta);
            FaunaFlightRotationComponent rotComp = body.componentRegistry.get(FaunaFlightRotationComponent.class);
            boolean isRolling = rotComp != null && rotComp.isRolling();
            ClimbEngine climb = ClimbEngine.lookOf(body);
            boolean matrixCamera = isRolling || climb != null;
            if (!matrixCamera && wasRollingForSmoother) {
               FaunaCameraState.resetSmoother(null, null);
            }

            wasRollingForSmoother = matrixCamera;
            if (climb != null) {
               climb.levelLook(frameDelta);
               faceLook(mc.player, body, climb);
               if (RideSettings.isCameraLooseFollowEnabled()) {
                  float partialTick = ClientFrame.frameTime();
                  FaunaCameraState.updateSmoother(
                     climb.look(partialTick),
                     new Vector3d(
                        Mth.lerp(partialTick, body.xo, body.getX()),
                        Mth.lerp(partialTick, body.yo, body.getY()),
                        Mth.lerp(partialTick, body.zo, body.getZ())
                     ),
                     frameDelta
                  );
               }
            }

            if (isRolling) {
               rotComp.flushPendingInput(frameDelta);
               if (RideSettings.isCameraLooseFollowEnabled()) {
                  Quaternionf targetRot = rotComp.getRenderOrientation(1.0F);
                  float partialTick = ClientFrame.frameTime();
                  double interpX = Mth.lerp(partialTick, body.xo, body.getX());
                  double interpY = Mth.lerp(partialTick, body.yo, body.getY());
                  double interpZ = Mth.lerp(partialTick, body.zo, body.getZ());
                  Vector3d targetPos = new Vector3d(interpX, interpY, interpZ);
                  FaunaCameraState.updateSmoother(targetRot, targetPos, frameDelta);
               }
            }
         }
      }
   }

   private static void faceLook(Player rider, FaunaBase mount, ClimbEngine climb) {
      Vector3f at = SurfaceFrame.forward(climb.look(ClientFrame.frameTime()));
      float yaw = SurfaceFrame.yawOf(at, rider.getYRot());
      float pitch = SurfaceFrame.pitchOf(at);
      rider.setYRot(yaw);
      rider.yRotO = yaw;
      rider.setXRot(pitch);
      rider.xRotO = pitch;
      rider.yBodyRot = mount.yBodyRot;
      rider.yBodyRotO = mount.yBodyRotO;
      rider.yHeadRot = mount.yBodyRot + climb.headTurn();
      rider.yHeadRotO = mount.yBodyRotO + climb.headTurn();
   }

   public static void onComputeFov(FieldOfView event) {
      Minecraft mc = Minecraft.getInstance();
      if (mc.player != null && mc.player.getVehicle() instanceof FaunaBase body) {
         double currentFov = event.getFOV();
         double targetFovOffset = 0.0;
         if (!event.getCamera().isDetached()) {
            double offset = RideSettings.getFirstPersonFovOffset();
            if (Math.abs(offset) > 0.001) {
               currentFov += offset;
            }
         }

         if (RideSettings.isSpeedFovEffectEnabled() && body.supportsThreeDMedium()) {
            float throttle = body.getThrottleLevel();
            if (throttle > 0.8F) {
               PhysicsSheet.Flight flight = body.flightNumbers();
               float maxOverdrive = Math.max(body.diveCap(), flight.forwardOverdriveCap());
               maxOverdrive = Math.max(maxOverdrive, 1.0F);
               float throttleFraction = Mth.clamp((throttle - 0.8F) / (maxOverdrive - 0.8F), 0.0F, 1.0F);
               double modifier = throttleFraction * 0.2 * RideSettings.getSpeedFovEffectStrength();
               targetFovOffset = event.getFOV() * modifier;
            }
         }

         float deltaF = ClientFrame.frameDeltaTicks();
         smoothedFovOffset = Mth.lerp(0.1 * deltaF, smoothedFovOffset, targetFovOffset);
         currentFov += smoothedFovOffset;
         if (currentFov > 135.0) {
            currentFov = 135.0;
         }

         if (currentFov < 10.0) {
            currentFov = 10.0;
         }

         event.setFOV(currentFov);
      } else {
         smoothedFovOffset = 0.0;
      }
   }

   private static void holdForeignArms() {
      if (ForeignArms.present()) {
         Minecraft mc = Minecraft.getInstance();
         LocalPlayer player = mc.player;
         boolean expected = false;
         if (player != null && mc.options.getCameraType().isFirstPerson()) {
            boolean touching = TouchEase.weight(player.getUUID()) > 0.0F;
            boolean gripping = player.getVehicle() instanceof FaunaBase
               && player.getMainHandItem().isEmpty()
               && player.getOffhandItem().isEmpty()
               && player.getAttackAnim(1.0F) <= 0.0F;
            expected = touching || gripping;
         }

         ForeignArms.frame(expected);
      }
   }

   public static void onEntityMount(Entity rider, Entity mount, Level level, boolean mounting) {
      if (mount instanceof FaunaBase) {
         if (level.isClientSide()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && rider == mc.player) {
               if (mounting) {
                  ForeignCameras.noteMount();
                  ridePlaceSent = SubLevels.present();
                  preMountCameraType = mc.options.getCameraType();
                  CameraType target;
                  if (RideSettings.isRememberLastCameraPerspective() && lastRidingCameraType != null) {
                     target = lastRidingCameraType;
                  } else {
                     target = RideSettings.ridesInFirstPerson() ? CameraType.FIRST_PERSON : CameraType.THIRD_PERSON_BACK;
                  }

                  mc.options.setCameraType(target);
               } else {
                  if (RideSettings.isRememberLastCameraPerspective()) {
                     lastRidingCameraType = mc.options.getCameraType();
                  }

                  if (preMountCameraType != null) {
                     mc.options.setCameraType(preMountCameraType);
                     preMountCameraType = null;
                  }
               }
            }
         }
      }
   }
}
