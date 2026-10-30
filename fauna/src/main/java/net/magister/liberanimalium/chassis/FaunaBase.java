package net.magister.liberanimalium.chassis;

import com.mojang.logging.LogUtils;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import org.jetbrains.annotations.Nullable;
import mod.azure.azurelib.animation.AzAnimator;
import mod.azure.azurelib.animation.AzAnimatorAccessor;
import mod.azure.azurelib.util.MoveAnalysis;
import net.magister.liberanimalium.anim.ExpressionEventClient;
import net.magister.liberanimalium.anim.event.EventTable;
import net.magister.liberanimalium.anim.rig.EntityFooting;
import net.magister.liberanimalium.anim.rig.Footing;
import net.magister.liberanimalium.anim.rig.RigSpec;
import net.magister.liberanimalium.anim.rig.RiggedBody;
import net.magister.liberanimalium.anim.rig.StepsSpec;
import net.magister.liberanimalium.body.BabyArt;
import net.magister.liberanimalium.body.BodyPlan;
import net.magister.liberanimalium.body.CullBox;
import net.magister.liberanimalium.body.PartSpec;
import net.magister.liberanimalium.body.SpeciesAssets;
import net.magister.liberanimalium.body.StuckArrows;
import net.magister.liberanimalium.body.WorldSense;
import net.magister.liberanimalium.body.anim.binding.AnimSignalSync;
import net.magister.liberanimalium.body.anim.binding.AnimationBindingLoader;
import net.magister.liberanimalium.body.anim.data.BoneAccessCache;
import net.magister.liberanimalium.body.data.CrippledFeature;
import net.magister.liberanimalium.body.data.FaunaWeightClass;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayProfile;
import net.magister.liberanimalium.body.gameplay.SpeciesGameplayRegistry;
import net.magister.liberanimalium.body.gameplay.TamingPredicate;
import net.magister.liberanimalium.body.gameplay.VariantGameplay;
import net.magister.liberanimalium.body.grip.Grip;
import net.magister.liberanimalium.body.locomotion.ClimbEngine;
import net.magister.liberanimalium.body.locomotion.DiveModule;
import net.magister.liberanimalium.body.locomotion.FlightEngine;
import net.magister.liberanimalium.body.locomotion.FlightGait;
import net.magister.liberanimalium.body.locomotion.LocomotionMode;
import net.magister.liberanimalium.body.locomotion.LocomotionOrchestrator;
import net.magister.liberanimalium.body.locomotion.ModeChangeCause;
import net.magister.liberanimalium.body.locomotion.Plunge;
import net.magister.liberanimalium.body.locomotion.RiderSteeringSource;
import net.magister.liberanimalium.body.locomotion.SteeringIntent;
import net.magister.liberanimalium.body.locomotion.WalkEngine;
import net.magister.liberanimalium.body.locomotion.WaterDepth;
import net.magister.liberanimalium.body.locomotion.WaterEngine;
import net.magister.liberanimalium.body.locomotion.transition.LocomotionTransition;
import net.magister.liberanimalium.body.locomotion.transition.TakeoffTransition;
import net.magister.liberanimalium.body.nav.noise.FaunaNoiseListener;
import net.magister.liberanimalium.body.nav.pathfinding.BodyFit;
import net.magister.liberanimalium.body.nav.pathfinding.NavSnapshot;
import net.magister.liberanimalium.body.part.FaunaPartEntity;
import net.magister.liberanimalium.body.physics.PhysicsSheet;
import net.magister.liberanimalium.body.state.AttackMotion;
import net.magister.liberanimalium.body.state.BodyStance;
import net.magister.liberanimalium.body.state.FaunaStateContext;
import net.magister.liberanimalium.body.state.FaunaStateEvaluator;
import net.magister.liberanimalium.body.state.RiderMotion;
import net.magister.liberanimalium.body.state.TransportMode;
import net.magister.liberanimalium.body.stats.BondLevel;
import net.magister.liberanimalium.body.stats.FaunaProfile;
import net.magister.liberanimalium.body.stats.FaunaStatSheet;
import net.magister.liberanimalium.body.stats.TamingStage;
import net.magister.liberanimalium.chassis.ability.AbilityMoment;
import net.magister.liberanimalium.chassis.ability.FaunaAbility;
import net.magister.liberanimalium.chassis.component.AttitudeComponent;
import net.magister.liberanimalium.chassis.component.BondingLedgerComponent;
import net.magister.liberanimalium.chassis.component.CarryComponent;
import net.magister.liberanimalium.chassis.component.CombatComponent;
import net.magister.liberanimalium.chassis.component.CommandComponent;
import net.magister.liberanimalium.chassis.component.DiveBreakComponent;
import net.magister.liberanimalium.chassis.component.FaunaComponentRegistry;
import net.magister.liberanimalium.chassis.component.FaunaFlightRotationComponent;
import net.magister.liberanimalium.chassis.component.FaunaMultipartComponent;
import net.magister.liberanimalium.chassis.component.FaunaSpineCascadeComponent;
import net.magister.liberanimalium.chassis.component.GenotypeComponent;
import net.magister.liberanimalium.chassis.component.GripComponent;
import net.magister.liberanimalium.chassis.component.GrowthComponent;
import net.magister.liberanimalium.chassis.component.IncapacitationComponent;
import net.magister.liberanimalium.chassis.component.LimbReachComponent;
import net.magister.liberanimalium.chassis.component.RiderInputComponent;
import net.magister.liberanimalium.chassis.component.RollComponent;
import net.magister.liberanimalium.chassis.component.RoostTenancyComponent;
import net.magister.liberanimalium.chassis.component.StatsComponent;
import net.magister.liberanimalium.chassis.component.ThrottleComponent;
import net.magister.liberanimalium.chassis.component.command.Modifier;
import net.magister.liberanimalium.chassis.component.command.Stance;
import net.magister.liberanimalium.chassis.component.interaction.IFaunaInteractionHandler;
import net.magister.liberanimalium.chassis.component.interaction.LootInteractionHandler;
import net.magister.liberanimalium.chassis.component.interaction.PerchInteractionHandler;
import net.magister.liberanimalium.chassis.component.interaction.RidingInteractionHandler;
import net.magister.liberanimalium.chassis.component.melee.CombatReach;
import net.magister.liberanimalium.chassis.component.melee.IMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.melee.OmniMeleeAttackHandler;
import net.magister.liberanimalium.chassis.component.qte.FaunaQteComponent;
import net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler;
import net.magister.liberanimalium.chassis.component.simulation.FaunaNeedsSystem;
import net.magister.liberanimalium.chassis.component.simulation.FaunaStaminaSystem;
import net.magister.liberanimalium.compat.Entities;
import net.magister.liberanimalium.compat.SyncedDataSink;
import net.magister.liberanimalium.config.RideSettings;
import net.magister.liberanimalium.config.WorldRules;
import net.magister.liberanimalium.event.FaunaWorldEvents;
import net.magister.liberanimalium.fauna.api.AnimalHandle;
import net.magister.liberanimalium.fauna.api.Armament;
import net.magister.liberanimalium.fauna.api.Arms;
import net.magister.liberanimalium.fauna.api.Beckoned;
import net.magister.liberanimalium.fauna.api.Buck;
import net.magister.liberanimalium.fauna.api.Check;
import net.magister.liberanimalium.fauna.api.Delivery;
import net.magister.liberanimalium.fauna.api.ExpressionKey;
import net.magister.liberanimalium.fauna.api.Gullet;
import net.magister.liberanimalium.fauna.api.Halter;
import net.magister.liberanimalium.fauna.api.HeardSound;
import net.magister.liberanimalium.fauna.api.Kind;
import net.magister.liberanimalium.fauna.api.Kindness;
import net.magister.liberanimalium.fauna.api.Ladder;
import net.magister.liberanimalium.fauna.api.MoveIntent;
import net.magister.liberanimalium.fauna.api.MoveMode;
import net.magister.liberanimalium.fauna.api.Order;
import net.magister.liberanimalium.fauna.api.PoseFlag;
import net.magister.liberanimalium.fauna.api.SensedBody;
import net.magister.liberanimalium.fauna.api.SensedItem;
import net.magister.liberanimalium.fauna.api.Signals;
import net.magister.liberanimalium.fauna.api.SoundKind;
import net.magister.liberanimalium.fauna.api.SpeciesProfile;
import net.magister.liberanimalium.fauna.api.SpeciesProfiles;
import net.magister.liberanimalium.fauna.api.TamingMatrix;
import net.magister.liberanimalium.fauna.api.Terrain;
import net.magister.liberanimalium.fauna.api.Urged;
import net.magister.liberanimalium.fauna.api.Valence;
import net.magister.liberanimalium.fauna.api.Wings;
import net.magister.liberanimalium.fauna.core.FaunaMind;
import net.magister.liberanimalium.fauna.core.FaunaTicker;
import net.magister.liberanimalium.fauna.core.PairSession;
import net.magister.liberanimalium.fauna.identity.Trait;
import net.magister.liberanimalium.fauna.know.Bend;
import net.magister.liberanimalium.fauna.know.Courtship;
import net.magister.liberanimalium.fauna.know.Knowledge;
import net.magister.liberanimalium.fauna.know.Representation;
import net.magister.liberanimalium.fauna.move.MoveTuning;
import net.magister.liberanimalium.fauna.move.NavSolver;
import net.magister.liberanimalium.fauna.sense.SenseTuning;
import net.magister.liberanimalium.genetics.GeneticsPreset;
import net.magister.liberanimalium.genetics.GeneticsSpawner;
import net.magister.liberanimalium.genetics.Genotype;
import net.magister.liberanimalium.genetics.PaletteDraws;
import net.magister.liberanimalium.genetics.PalettePoolRegistry;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfig;
import net.magister.liberanimalium.genetics.config.SpeciesGeneticsConfigRegistry;
import net.magister.liberanimalium.interop.Decks;
import net.magister.liberanimalium.interop.SubLevels;
import net.magister.liberanimalium.network.FaunaExpressionEventMessage;
import net.magister.liberanimalium.network.FaunaNetwork;
import net.magister.liberanimalium.platform.Services;
import net.magister.liberanimalium.platform.part.PartBody;
import net.magister.liberanimalium.platform.registry.Regs;
import net.magister.liberanimalium.progression.FaunaInteractionEvents;
import net.magister.liberanimalium.qte.QteMinigame;
import net.magister.liberanimalium.qte.QteOutcome;
import net.magister.liberanimalium.qte.QteTuning;
import net.magister.liberanimalium.qte.games.TameCircleQte;
import net.magister.liberanimalium.render.BoneSpots;
import net.magister.liberanimalium.render.vfx.ColdBreath;
import net.magister.liberanimalium.render.vfx.FaunaFlightParticleHelper;
import net.magister.liberanimalium.render.vfx.FaunaWingTrailManager;
import net.magister.liberanimalium.render.vfx.SleepBubbles;
import net.magister.liberanimalium.render.vfx.SwimBubbles;
import net.magister.liberanimalium.render.vfx.SwimWake;
import net.magister.liberanimalium.ride.render.RiderComponent;
import net.magister.liberanimalium.roost.RoostBlockEntity;
import net.magister.liberanimalium.sound.FaunaSoundHelper;
import net.magister.liberanimalium.sound.SoundEventKey;
import net.magister.liberanimalium.sound.VoiceGate;
import net.magister.liberanimalium.sound.client.SoundClientHelper;
import net.magister.liberanimalium.spawn.DespawnRule;
import net.magister.liberanimalium.spawn.SpawnSettings;
import net.magister.liberanimalium.util.PerfClock;
import net.magister.liberanimalium.util.Quantum;
import net.magister.liberanimalium.util.SaveKeys;
import net.magister.liberanimalium.variant.CreatureVariant;
import net.magister.liberanimalium.variant.CreatureVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.Saddleable;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.Entity.MoveFunction;
import net.minecraft.world.entity.Entity.MovementEmission;
import net.minecraft.world.entity.Entity.RemovalReason;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.Brain.Provider;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.DynamicGameEventListener;
import net.minecraft.world.level.levelgen.Heightmap.Types;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.joml.Vector3f;
import org.slf4j.Logger;

public abstract class FaunaBase
   extends TamableAnimal
   implements AzAnimatorAccessor<UUID, FaunaBase>,
   Saddleable,
   PlayerRideableJumping,
   AnimalHandle,
   Decks.Standing,
   Gullet,
   Wings,
   Arms,
   Halter,
   RiggedBody {
   public final FaunaComponentRegistry componentRegistry;
   private static final Logger LOGGER = LogUtils.getLogger();
   private FaunaProfile profile;
   private final FaunaDispatcher dispatcher;
   public final MoveAnalysis moveAnalysis;
   private AzAnimator<UUID, FaunaBase> animator;
   public static final float HOVER_SPEED_THRESHOLD = 0.15F;
   public static final int TRANSITION_DURATION = 15;
   public int deadzoneDelayTimer = 0;
   public boolean inDeadzone = true;
   private int flightModeTransitionTimer = 0;
   private float capturedTransitionRoll = 0.0F;
   private boolean wasFreeLookingOnLastTick = false;
   private int wallHitPoseTicks = 0;
   private final RiderSteeringSource riderSource = new RiderSteeringSource(this);
   private int wingRollDeblendTicks = 0;
   private final List<IFaunaInteractionHandler> interactionHandlers = new ArrayList<>();
   private static final int ABSENT_CHECK_TICKS = 20;
   private FaunaMind faunaMind;
   private WorldSense worldSense;
   private static final double DOOR_REACH = 2.5;
   @Nullable
   private Object subLevelUnder;
   @Nullable
   private Vec3 subLevelPlace;
   @Nullable
   private Vec3 ridePlace;
   @Nullable
   private Vec3 heldAt;
   private int movedTick = -1;
   private boolean vanillaTravelled;
   private boolean travelTail;
   private static final double RIDE_PLACE_REACH_SQR = 256.0;
   private boolean facingSent;
   @Nullable
   private UnaryOperator<Vec3> groundRide;
   private Vec3 groundOld = Vec3.ZERO;
   private static final float SUB_LEVEL_STEP_MARGIN = 0.125F;
   public static final double HOP_RISE = 2.0;
   private boolean hopLaunch;
   private boolean hopAirborne;
   private boolean leaping;
   private boolean boostFromFlight;
   private boolean hopFalling;
   private static final double PERCH_REACH = 3.0;
   private static final double PERCH_HEIGHT_SHARE = 0.75;
   private static final double SETTLE_REACH = 1.5;
   private static final double SETTLE_CLEARANCE = 0.001;
   private static final int LANDING_RING_SAMPLES = 8;
   @org.jetbrains.annotations.Nullable
   private MoveIntent faunaIntent;
   private boolean mindTrigger;
   private final Arms riddenArms = new Arms() {
      @Override
      public double strikeRange() {
         return FaunaBase.this.strikeRange();
      }

      @Override
      public boolean strike(UUID targetId) {
         FaunaBase.this.letGoOfMindTrigger();
         return false;
      }

      @Override
      public Armament armament() {
         return FaunaBase.this.armament();
      }

      @Override
      public void holdTrigger(boolean pressed) {
         FaunaBase.this.letGoOfMindTrigger();
      }

      @Override
      public void dropTrigger() {
         FaunaBase.this.letGoOfMindTrigger();
      }

      @Override
      public boolean canAimAt(Vec3 point) {
         return FaunaBase.this.canAimAt(point);
      }

      @Override
      public boolean electAbility(@Nullable UUID engagedWith, boolean spent, boolean fleeing, int committedTicks) {
         FaunaBase.this.letGoOfMindTrigger();
         return false;
      }
   };
   private int callHold;
   private final Footing groundFooting = new EntityFooting(this) {
      @Override
      public Vec3 groundOld() {
         return FaunaBase.this.groundOld();
      }

      @Override
      public UnaryOperator<Vec3> ride() {
         ClimbEngine climb = FaunaBase.this.componentRegistry.get(ClimbEngine.class);
         UnaryOperator<Vec3> left = climb == null ? null : climb.leftRide();
         return left != null ? left : FaunaBase.this.groundRide();
      }
   };
   private static final double SNATCH_REACH = 1.0;
   public static final int STAGE_BABY = 0;
   public static final int STAGE_JUVENILE = 1;
   public static final int STAGE_ADULT = 2;
   public static final int STAGE_ELDER = 3;
   public static final int COMMAND_FREE = Stance.FREE.ordinal();
   private static final EntityDataAccessor<Integer> DATA_GROWTH_STAGE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_GROWTH_PROGRESS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final int SPAWN_ANIMATION_DURATION = 30;
   private static final EntityDataAccessor<Integer> DATA_SPAWN_SCALE_TICKS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<String> DATA_VARIANT_SKIN = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<String> DATA_VARIANT_ID = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.STRING);
   private static final EntityDataAccessor<Integer> DATA_TRANSPORT_MODE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ATTACK_MOTION = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_TEMPERAMENT = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ANIM_SIGNALS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SOCIAL_SIGNALS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_FAUNA_POSES = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_COMMAND = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_COMMAND_MODIFIER = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_ORDER_HELD = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SLEEP_DISTURB_TICKS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_ABILITY_COOLDOWN_TICKS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Boolean> DATA_IS_BOOSTING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_DIVE_BREAKING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_SPRINTING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Boolean> DATA_IS_ROARING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<CompoundTag> DATA_GENOTYPE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.COMPOUND_TAG);
   private static final EntityDataAccessor<ItemStack> DATA_CARRIED_ITEM = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.ITEM_STACK);
   private static final EntityDataAccessor<Float> DATA_HEAD_YAW = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_HEAD_PITCH = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Byte> DATA_LOOK_RUNG = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BYTE);
   private static final EntityDataAccessor<Integer> DATA_PART_REPORTER = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_HUNGER_LEVEL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> DATA_ART_VERSION = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_STAMINA = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Float> DATA_MAX_STAMINA = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   private static final EntityDataAccessor<Boolean> DATA_QTE_ACTIVE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   private static final EntityDataAccessor<Integer> DATA_SECONDARY_COOLDOWN = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_SECONDARY_COOLDOWN_MAX = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_INCAP_FLAGS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Integer> DATA_INCAP_COUNT = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private static final EntityDataAccessor<Float> DATA_INCAP_DEBUFF = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Optional<BlockPos>> DATA_ROOST_POS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
   static final EntityDataAccessor<Boolean> DATA_IS_BRAKING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   static final EntityDataAccessor<Boolean> DATA_IS_FREE_LOOKING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Float> DATA_YAW_SPEED = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   static final EntityDataAccessor<Float> DATA_ROLL_INPUT = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Float> DATA_HORIZONTAL_SPEED = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Float> DATA_SYNCED_PITCH = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Float> DATA_SYNCED_ROLL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Float> DATA_DODGE_ROLL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<Byte> DATA_GRIP_STATE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BYTE);
   public static final EntityDataAccessor<Integer> DATA_GRIP_HELD = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Vector3f> DATA_RIDER_MOTION = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.VECTOR3);
   public static final EntityDataAccessor<Vector3f> DATA_GRIP_PULL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.VECTOR3);
   public static final EntityDataAccessor<Float> DATA_GRIP_LOAD = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   public static final EntityDataAccessor<CompoundTag> DATA_STUCK_ARROWS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.COMPOUND_TAG);
   public static final EntityDataAccessor<Float> DATA_THROTTLE_LEVEL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.FLOAT);
   static final EntityDataAccessor<Boolean> DATA_IS_ACCELERATING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   static final EntityDataAccessor<Boolean> DATA_IS_DECELERATING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Integer> BREATH_CURRENT_FUEL = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Boolean> BREATH_IS_BREATHING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Integer> BREATH_COOLDOWN = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> BREATH_FULL_RECHARGE_COOLDOWN = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> BREATH_WINDUP_TICKS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> PROJ_TICK_SINCE_LAST_FIRE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> PROJ_FIRING_TICKS = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Boolean> PROJ_IS_CHARGING = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.BOOLEAN);
   public static final EntityDataAccessor<Integer> MELEE_ATTACK_PHASE = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   public static final EntityDataAccessor<Integer> MELEE_ATTACK_SLOT = SynchedEntityData.defineId(FaunaBase.class, EntityDataSerializers.INT);
   private int stateTransitionCooldown = 0;
   private FaunaWingTrailManager wingTrailManager;
   private final BoneAccessCache boneAccessCache = new BoneAccessCache();
   private final VoiceGate voiceGate = new VoiceGate();
   private FaunaStaminaSystem staminaSystem;
   private final FaunaHeadTrackingSystem headTrackingSystem;
   protected final FaunaStateContext stateContext;
   private int noSaddleRideTicks = 0;
   private GeneticsPreset spawnPreset = GeneticsPreset.NONE;
   private static final PerfClock.Section CLOCK_TICK = PerfClock.server("body.tick");
   private static final PerfClock.Section CLOCK_AI = PerfClock.server("body.aiStep");
   private static final PerfClock.Section CLOCK_TRAVEL = PerfClock.server("body.travel");
   private static final PerfClock.Section CLOCK_COMPONENTS = PerfClock.server("body.components");
   private static final PerfClock.Section CLOCK_CLIENT_COMPONENTS = PerfClock.client("body.components");
   private static final PerfClock.Section CLOCK_CLIENT_TICK = PerfClock.client("body.tick");
   private static final PerfClock.Section CLOCK_GRIP = PerfClock.server("body.grip");
   private static final PerfClock.Section CLOCK_CLIENT_GRIP = PerfClock.client("body.grip");
   private static final PerfClock.Section CLOCK_CLIENT_AI = PerfClock.client("body.aiStep");
   private static final PerfClock.Section CLOCK_CLIENT_TRAVEL = PerfClock.client("body.travel");
   private String lastTravelBranch = "none";
   public static final float FALL_CATCH_BLOCKS = 6.0F;
   private boolean wasEstranged;
   private int unwatchedTicks;
   private float bodyFacing = Float.NaN;
   private int bodyFacingTick = Integer.MIN_VALUE;
   private static final double REFERENCE_MASS = 0.648;
   private static final double MOOD_RISE = 0.35;
   private static final double MOOD_SPREAD = 0.3;
   private static final AtomicInteger LOAFERS = new AtomicInteger();
   @org.jetbrains.annotations.Nullable
   private ResourceLocation variantId;
   private static final int ROAR_PULSE_TICKS = 40;
   private int roarTicks;
   private long refusalPulseUntilTick;
   private static final double THROW_PUSH = 0.9;
   private static final double THROW_LIFT = 0.55;
   @Nullable
   private Component courtLine;
   @Nullable
   private UUID courtLineFor;
   private long courtLineTick = Long.MIN_VALUE;
   private final EyeState eyeState = new EyeState();
   @Nullable
   private Vec3 rideAt;
   private Vec3 ridePace = Vec3.ZERO;
   private static final double RIDE_PACE_MOST = 8.0;

   public int getWingRollDeblendTicks() {
      return this.wingRollDeblendTicks;
   }

   public void setWingRollDeblendTicks(int ticks) {
      this.wingRollDeblendTicks = ticks;
   }

   public AzAnimator<UUID, FaunaBase> getAnimatorOrNull() {
      return this.animator;
   }

   public void setAnimator(AzAnimator<UUID, FaunaBase> animator) {
      this.animator = animator;
   }

   protected List<IFaunaInteractionHandler> buildInteractionHandlers() {
      List<IFaunaInteractionHandler> chain = new ArrayList<>();
      chain.add(new LootInteractionHandler());
      if (this.profile.canPerch) {
         chain.add(new PerchInteractionHandler());
      }

      if (this.profile.canBeRidden) {
         chain.add(new RidingInteractionHandler());
      }

      return chain;
   }

   protected void tickHostInteractions() {
   }

   protected void tickHost() {
   }

   protected void finalizeHostSpawn() {
   }

   public boolean isSaddled() {
      return false;
   }

   protected void shedTack() {
   }

   public FaunaBase(EntityType<? extends TamableAnimal> entityType, Level level) {
      super(entityType, level);
      this.lookControl = new FaunaLookControl(this);
      this.profile = this.buildProfile();
      this.componentRegistry = new FaunaComponentRegistry(this);
      this.componentRegistry.register(new FaunaNeedsSystem());
      this.componentRegistry.register(new BondingLedgerComponent());
      this.componentRegistry.register(new CarryComponent());
      this.componentRegistry.register(new GrowthComponent());
      this.componentRegistry.register(new GenotypeComponent());
      this.componentRegistry.register(new StatsComponent());
      this.componentRegistry.register(new CommandComponent());
      this.componentRegistry.register(new CombatComponent());
      this.componentRegistry.register(new RoostTenancyComponent());
      this.registerHostComponents(this.componentRegistry);
      if (this.profile.canFly || this.profile.hasMode(LocomotionMode.SWIM)) {
         this.componentRegistry.register(new RiderInputComponent());
         this.componentRegistry.register(new ThrottleComponent());
         if (this.profile.canFly) {
            this.componentRegistry.register(new RollComponent());
         }

         this.componentRegistry.register(new FaunaFlightRotationComponent());
         if (this.profile.canFly) {
            this.componentRegistry.register(new DiveBreakComponent());
         }
      } else if (this.profile.canBeRidden) {
         this.componentRegistry.register(new RiderInputComponent());
      }

      this.componentRegistry.register(new FaunaSpineCascadeComponent());
      if (this.profile.hasMode(LocomotionMode.WALK)) {
         this.componentRegistry.register(new WalkEngine());
      }

      if (this.profile.hasMode(LocomotionMode.FLY)) {
         this.componentRegistry.register(new FlightEngine());
      }

      if (this.profile.hasMode(LocomotionMode.SWIM)) {
         this.componentRegistry.register(new WaterEngine());
      }

      if (this.profile.hasMode(LocomotionMode.DIVE)) {
         this.componentRegistry.register(new DiveModule());
      }

      if (this.profile.hasMode(LocomotionMode.CLIMB)) {
         this.componentRegistry.register(new ClimbEngine());
      }

      this.componentRegistry.register(new LocomotionOrchestrator());
      if (!this.profile.hasMode(LocomotionMode.WALK)) {
         this.entityData.set(DATA_TRANSPORT_MODE, this.restingMode().toTransportMode().ordinal());
      }

      this.componentRegistry.register(new AttitudeComponent());
      this.componentRegistry.register(new LimbReachComponent());
      this.componentRegistry.register(new GripComponent());
      this.componentRegistry.register(new RiderComponent());
      this.componentRegistry.register(new IncapacitationComponent());
      this.componentRegistry.register(new FaunaQteComponent());
      this.componentRegistry.register(new FaunaNoiseListener());
      SpeciesAssets.noteSpecies(this.speciesId(), SpeciesAssets.hostOf(this));
      this.componentRegistry.register(new FaunaMultipartComponent());
      this.reservePartIds();
      this.registerSpeciesComponents(this.componentRegistry);
      this.faunaMind = new FaunaMind(this);
      this.setMaxUpStep(1.0F);

      for (IFaunaInteractionHandler handler : this.buildInteractionHandlers()) {
         if (handler == null) {
            throw new IllegalStateException(
               this.getClass().getSimpleName()
                  + ".buildInteractionHandlers() returned a null handler: a handler kept in a field must be created inside that method, not at the field's declaration"
            );
         }

         this.interactionHandlers.add(handler);
      }

      this.dispatcher = new FaunaDispatcher(this);
      this.moveAnalysis = new MoveAnalysis(this);
      this.staminaSystem = new FaunaStaminaSystem(this);
      this.headTrackingSystem = new FaunaHeadTrackingSystem(this);
      this.stateContext = new FaunaStateContext(this);
   }

   public Provider<?> brainProvider() {
      return Brain.provider(List.of(), List.of());
   }

   private void forgetAbsentPlayers() {
      for (Representation rep : List.copyOf(this.faunaMind.knowledge().all())) {
         if (rep.isPlayer() && !rep.visible()) {
            Player who = this.level().getPlayerByUUID(rep.id());
            if (who == null || who.isCreative() || who.isSpectator()) {
               this.faunaMind.forget(rep.id(), true);
            }
         }
      }
   }

   protected void customServerAiStep() {
      if (this.roarTicks > 0 && --this.roarTicks == 0) {
         this.setRoaring(false);
      }

      if (this.faunaMind != null) {
         FaunaTicker.tick(this.faunaMind);
         if (this.tickCount % 20 == 0) {
            this.forgetAbsentPlayers();
         }

         this.tickEstrangement();
         this.entityData.set(DATA_ORDER_HELD, this.faunaMind.commandHolds());
      }

      super.customServerAiStep();
   }

   @Override
   public FaunaMind faunaMind() {
      return this.faunaMind;
   }

   @Override
   public UUID faunaId() {
      return this.getUUID();
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public UUID ownerId() {
      return this.getOwnerUUID();
   }

   @Override
   public Order order() {
      CommandComponent command = this.componentRegistry.get(CommandComponent.class);
      return command.freeForTicks() < 60L ? Order.STAY : Order.of(command.getStance());
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public Vec3 home() {
      BlockPos home = this.componentRegistry.get(CommandComponent.class).home();
      return home == null ? null : Vec3.atBottomCenterOf(home);
   }

   @Override
   public long freeFor() {
      return this.componentRegistry.get(CommandComponent.class).freeForTicks();
   }

   @Override
   public boolean steered() {
      return this.steeredByRider();
   }

   @Override
   public boolean carrying() {
      return this.isVehicle();
   }

   @Override
   public boolean riddenBy(UUID candidate) {
      for (Entity passenger : this.getPassengers()) {
         if (passenger.getUUID().equals(candidate)) {
            return true;
         }
      }

      return false;
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public Vec3 ownerPosition() {
      UUID owner = this.getOwnerUUID();
      if (owner != null && super.level() instanceof ServerLevel server) {
         Entity entity = server.getEntity(owner);
         return entity == null ? null : entity.position();
      } else {
         return null;
      }
   }

   private double carriedShare() {
      Vec3 v = this.getDeltaMovement();
      double top = Math.max(0.001, this.topSpeed(MoveMode.AIR));
      return Math.max(Math.hypot(v.x, v.z) / top, Math.max(this.stallFraction(), 0.1));
   }

   @Override
   public boolean ownerCrouching() {
      UUID owner = this.getOwnerUUID();
      if (owner != null && super.level() instanceof ServerLevel server) {
         Entity entity = server.getEntity(owner);
         return entity != null && entity.isShiftKeyDown();
      } else {
         return false;
      }
   }

   protected MovementEmission getMovementEmission() {
      return this.hasFaunaPose(PoseFlag.STALKING) ? MovementEmission.SOUNDS : super.getMovementEmission();
   }

   @Override
   public MoveMode modeAt(Vec3 point) {
      Level level = super.level();
      BlockPos at = BlockPos.containing(point);
      if (!level.getFluidState(at).isEmpty()) {
         return MoveMode.WATER;
      }

      double support = this.groundHeightNear(point, point.y, 0.0);
      return !Double.isNaN(support) && point.y - support <= this.maxUpStep() + 1.0 ? MoveMode.GROUND : MoveMode.AIR;
   }

   @Override
   public SpeciesProfile species() {
      if (this.variantId != null) {
         CreatureVariant variant = CreatureVariants.get(this.variantId);
         if (variant != null && variant.profile() != null) {
            return variant.profile();
         }
      }

      return SpeciesProfiles.get(this.speciesId());
   }

   @Override
   public Vec3 bodyPosition() {
      return this.position();
   }

   @Override
   public boolean bodyOnGround() {
      return this.onGround();
   }

   @Override
   public Vec3 velocity() {
      return this.getDeltaMovement();
   }

   @Override
   public Vec3 eyePosition() {
      List<FaunaPartEntity> heads = this.headParts();
      return heads.isEmpty() ? this.getEyePosition() : heads.get(0).getEyePosition();
   }

   @Override
   public Vec3 lookDirection() {
      return this.getLookAngle();
   }

   @Override
   public AABB bounds() {
      return this.getBoundingBox();
   }

   @Override
   public float health() {
      return this.getHealth();
   }

   @Override
   public float maxHealth() {
      return this.getMaxHealth();
   }

   @Override
   public MoveMode currentMode() {
      return switch (this.getTransportMode()) {
         case AIRBORNE -> MoveMode.AIR;
         case AQUATIC -> MoveMode.WATER;
         default -> MoveMode.GROUND;
      };
   }

   @Override
   public boolean inWater() {
      return this.isInWater();
   }

   @Override
   public boolean stoodOn() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart != null && multipart.stoodOn();
   }

   public boolean canBeCollidedWith() {
      return this.stoodOn();
   }

   @Override
   public ServerLevel serverLevel() {
      return (ServerLevel)super.level();
   }

   @Override
   public LivingEntity entity() {
      return this;
   }

   @Override
   public NavSolver.NeighborSource navSource(BlockPos goal) {
      return NavSnapshot.of(this, goal);
   }

   @Override
   public boolean onDeck() {
      return this.worldSense().onDeck();
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public BlockPos deckGoal(BlockPos world) {
      return this.worldSense().deckGoal(world);
   }

   @Override
   public Vec3 keepPlace(Vec3 world) {
      return Decks.keep(this, world);
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public Vec3 placeNow(Vec3 kept) {
      return Decks.now(this.level(), kept);
   }

   @Override
   public Map<MoveMode, Float> effectiveModeCost() {
      EnumMap<MoveMode, Float> out = new EnumMap<>(MoveMode.class);
      SpeciesProfile.Movement movement = this.species().movement();

      for (MoveMode mode : MoveMode.values()) {
         boolean bodyCan = switch (mode) {
            case GROUND -> this.profile.hasMode(LocomotionMode.WALK);
            case AIR -> this.canFly();
            case WATER -> this.profile.hasMode(LocomotionMode.SWIM);
            case CLIMB -> false;
         };
         out.put(mode, bodyCan ? movement.cost().getOrDefault(mode, Float.POSITIVE_INFINITY) : Float.POSITIVE_INFINITY);
      }

      return out;
   }

   @Override
   public boolean lineOfSight(Vec3 from, Vec3 to) {
      return this.worldSense().lineOfSight(from, to);
   }

   @Override
   public boolean sightLine(Vec3 from, Vec3 to) {
      return this.worldSense().sightLine(from, to);
   }

   @Override
   public boolean headClear() {
      return this.worldSense().headClear();
   }

   public boolean sees(Entity other) {
      return this.headClear() && this.sightLine(this.eyePosition(), other.getEyePosition());
   }

   public Kind kindOf(Entity other) {
      return this.worldSense().kindOf(other);
   }

   @Override
   public float fatigueLevel() {
      return this.staminaSystem == null ? 0.0F : Mth.clamp(1.0F - this.staminaSystem.getStaminaPercentage(), 0.0F, 1.0F);
   }

   @Override
   public boolean asleep() {
      return this.hasFaunaPose(PoseFlag.SLEEPING);
   }

   @Override
   public long dayTime() {
      return this.level().getDayTime();
   }

   private WorldSense worldSense() {
      if (this.worldSense == null) {
         this.worldSense = new WorldSense(this, this, new WorldSense.Appraisal() {
            @Override
            public float craving(ItemStack stack) {
               return FaunaBase.this.craving(stack);
            }

            @Override
            public boolean isFood(ItemStack stack) {
               return FaunaBase.this.isFood(stack);
            }

            @Override
            public ItemStack heldFood(LivingEntity other) {
               return FaunaBase.this.heldFood(other);
            }

            @Override
            public boolean wantsTouchFrom(LivingEntity other) {
               return FaunaBase.this.wantsTouchFrom(other);
            }
         });
      }

      return this.worldSense;
   }

   @Override
   public Terrain terrain() {
      return this.worldSense().terrain();
   }

   @Override
   public Optional<Halter.Roost> roost() {
      BlockPos at = this.getRoostPosition();
      if (!(at != null && this.level() instanceof ServerLevel server)) {
         return Optional.empty();
      } else {
         return server.isLoaded(at) && server.getBlockEntity(at) instanceof RoostBlockEntity roost
            ? Optional.of(new Halter.Roost(roost.root().getBlockPos(), roost.footprintMin(), roost.footprintMax()))
            : Optional.empty();
      }
   }

   @Override
   public void wonAgainst(UUID loser) {
      if (this.level() instanceof ServerLevel server) {
         if (server.getEntity(loser) instanceof FaunaBase beaten) {
            BlockPos home = beaten.getRoostPosition();
            if (home != null && server.isLoaded(home)) {
               if (!(Vec3.atCenterOf(home).distanceTo(this.position()) > this.species().social().territoryRadius())) {
                  if (server.getBlockEntity(home) instanceof RoostBlockEntity roost) {
                     roost.forfeit(beaten, this);
                  }
               }
            }
         }
      }
   }

   @org.jetbrains.annotations.Nullable
   public BlockPos getRoostPosition() {
      return (BlockPos)((Optional)this.entityData.get(DATA_ROOST_POS)).orElse(null);
   }

   public void setRoostPosition(@org.jetbrains.annotations.Nullable BlockPos pos) {
      this.entityData.set(DATA_ROOST_POS, Optional.ofNullable(pos));
   }

   @Override
   public Ladder stagePool() {
      return this.species().taming().pool();
   }

   @Override
   public TamingMatrix tamingMatrix() {
      return this.species().taming().matrix();
   }

   @Override
   public Vec3 sightDirection() {
      return this.headTrackingSystem == null ? this.lookDirection() : this.headTrackingSystem.headLookDirection();
   }

   @Override
   public boolean isBlockedAt(Vec3 point) {
      return this.worldSense().isBlockedAt(point);
   }

   @Nullable
   @Override
   public Vec3 passDoor(BlockPos cell) {
      BlockState state = this.level().getBlockState(cell);
      if (!(state.getBlock() instanceof DoorBlock door)) {
         return null;
      } else {
         if (!door.isOpen(state)) {
            if (!this.profile.opensDoors || !(this.level() instanceof ServerLevel) || !DoorBlock.isWoodenDoor(state)) {
               return null;
            }

            double reach = this.getBbWidth() * 0.5 + 2.5;
            Vec3 doorAt = this.placeNow(Vec3.atBottomCenterOf(cell));
            if (doorAt == null || doorAt.distanceToSqr(this.position()) > reach * reach) {
               return null;
            }

            door.setOpen(this, this.level(), state, cell, true);
            state = this.level().getBlockState(cell);
         }

         return BodyFit.doorPassage(state, this.level(), cell);
      }
   }

   @Override
   public float stepHeight() {
      return super.maxUpStep();
   }

   public void move(MoverType type, Vec3 movement) {
      if (type == MoverType.PLAYER && this.ridePlace != null) {
         if (this.heldAt == null) {
            this.heldAt = this.position();
         }

         this.setPos(this.position().add(movement));
      } else {
         this.movedTick = this.tickCount;
         GripComponent.moveThrough(this, () -> super.move(type, movement));
      }
   }

   public void absMoveTo(double x, double y, double z, float yRot, float xRot) {
      if (this.ridePlace != null) {
         Vec3 back = this.heldAt != null ? this.heldAt : this.position();
         this.heldAt = null;
         super.absMoveTo(back.x, back.y, back.z, yRot, xRot);
      } else {
         super.absMoveTo(x, y, z, yRot, xRot);
      }
   }

   private void vanillaTravel(Vec3 travelVector) {
      this.vanillaTravelled = true;
      super.travel(travelVector);
   }

   public boolean isControlledByLocalInstance() {
      return !this.travelTail && super.isControlledByLocalInstance();
   }

   private boolean drivesOnSubLevels() {
      return SubLevels.present() && !this.isPassenger() && this.isControlledByLocalInstance();
   }

   private void finishOnSubLevels() {
      boolean grounded = !this.isFlying() && this.getTransportMode() == TransportMode.GROUNDED && !this.getLocomotionOrchestrator().isTransitionActive();
      if (grounded && this.movedTick != this.tickCount) {
         this.move(MoverType.SELF, new Vec3(0.0, WalkEngine.fall(0.0), 0.0));
      }

      if (!this.vanillaTravelled) {
         this.travelTail = true;

         try {
            super.travel(Vec3.ZERO);
         } finally {
            this.travelTail = false;
         }
      }

      this.subLevelUnder = SubLevels.under(this);
      this.subLevelPlace = SubLevels.plot(this.subLevelUnder, this.position());
      float turn = grounded ? SubLevels.turn(this.subLevelUnder) : 0.0F;
      if (turn != 0.0F) {
         this.turnWithSubLevel(turn);
      }
   }

   @Nullable
   @Override
   public Vec3 subLevelPlace() {
      return this.subLevelPlace;
   }

   public void acceptRidePlace(@Nullable Vec3 place) {
      if (!this.level().isClientSide()) {
         Vec3 world = place == null ? null : SubLevels.world(SubLevels.at(this.level(), place), place);
         boolean held = world != null && world.distanceToSqr(this.position()) <= 256.0;
         this.ridePlace = held ? place : null;
         if (!held) {
            this.heldAt = null;
         }
      }
   }

   private void holdRidePlace() {
      if (!this.level().isClientSide() && this.getControllingPassenger() instanceof Player) {
         SubLevels.forget(this);
         SubLevels.untrack(this);
         if (this.ridePlace != null) {
            Vec3 at = SubLevels.world(SubLevels.at(this.level(), this.ridePlace), this.ridePlace);
            if (at == null) {
               this.ridePlace = null;
               this.heldAt = null;
            } else {
               this.setPos(at);
            }
         }
      }
   }

   private void handOverOnSubLevel() {
      boolean ridden = this.getControllingPassenger() instanceof Player;
      if (this.level().isClientSide()) {
         if (this.subLevelPlace == null || this.isControlledByLocalInstance()) {
            return;
         }

         SubLevels.pin(this, this.subLevelPlace);
         this.subLevelUnder = null;
         this.subLevelPlace = null;
      } else if (ridden && this.ridePlace == null) {
         this.ridePlace = SubLevels.plot(SubLevels.under(this), this.position());
         SubLevels.untrack(this);
      } else if (!ridden && this.ridePlace != null) {
         Object subLevel = SubLevels.at(this.level(), this.ridePlace);
         this.ridePlace = null;
         this.heldAt = null;
         SubLevels.resume(this, subLevel);
         this.subLevelUnder = subLevel;
      }
   }

   private void readGround() {
      Vec3 old = new Vec3(this.xo, this.yo, this.zo);
      this.groundRide = SubLevels.present() && !this.isFlying() ? SubLevels.ride(SubLevels.under(this), old) : null;
      this.groundOld = this.groundRide == null ? old : this.groundRide.apply(old);
   }

   @Nullable
   public UnaryOperator<Vec3> groundRide() {
      return this.groundRide;
   }

   public Vec3 groundOld() {
      return this.groundOld;
   }

   private void turnWithSubLevel(float degrees) {
      this.setYRot(this.getYRot() - degrees);
      this.yBodyRot -= degrees;
      this.yHeadRot -= degrees;
      if (!Float.isNaN(this.bodyFacing)) {
         this.bodyFacing -= degrees;
      }

      if (this.level().isClientSide() && this.getControllingPassenger() instanceof Player rider) {
         rider.setYRot(rider.getYRot() - degrees);
         rider.yBodyRot -= degrees;
         rider.yHeadRot -= degrees;
      }
   }

   public float maxUpStep() {
      float step = super.maxUpStep();
      return SubLevels.present() ? step + 0.125F : step;
   }

   private void call() {
      if (this.level() instanceof ServerLevel server) {
         if (this.onGround()) {
            this.callHold = this.species().calls().hold();
         }

         double reach = SenseTuning.LOUDNESS.get(SoundKind.CALL) * 2.0F;
         Vec3 from = this.position();
         FaunaWorldEvents.forEachMindWithin(server, this, reach, mind -> {
            AnimalHandle listener = mind.handle();
            Kind me = listener.entity() instanceof FaunaBase other ? other.kindOf(this) : null;
            HeardSound sound = new HeardSound(SoundKind.CALL, from, this.getUUID(), mind.age(), (float)listener.bodyPosition().distanceTo(from), me);
            if (listener.entity() instanceof FaunaBase other) {
               other.hear(sound);
            } else {
               mind.perception().deliver(listener, sound);
            }
         });
      }
   }

   @Override
   public boolean settleOn(BlockPos block) {
      if (!this.level().isClientSide() && this.onGround()) {
         VoxelShape shape = this.level().getBlockState(block).getCollisionShape(this.level(), block);
         if (shape.isEmpty()) {
            return false;
         }

         Vec3 seat = new Vec3(block.getX() + 0.5, block.getY() + shape.max(Axis.Y), block.getZ() + 0.5);
         Vec3 step = seat.subtract(this.position());
         if (!(step.horizontalDistance() > 1.5) && !(Math.abs(step.y) > 1.5)) {
            if (!this.level().noCollision(this, this.getBoundingBox().move(step).move(0.0, 0.001, 0.0))) {
               return false;
            }

            this.setPos(seat);
            this.setDeltaMovement(Vec3.ZERO);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean perchOn(UUID owner) {
      if (!this.level().isClientSide() && this.profile.canPerch) {
         Player player = this.level().getPlayerByUUID(owner);
         if (player == null || !this.isOwnedBy(player)) {
            return false;
         }

         if (!player.getPassengers().isEmpty()) {
            return false;
         }

         if (!this.isPassenger() && !this.isVehicle() && this.onGround() && !this.isInWater()) {
            if (this.distanceTo(player) > 3.0 + this.getBbWidth()) {
               return false;
            }

            if (this.mustLeavePerch(player)) {
               return false;
            }

            if (!this.startRiding(player, true)) {
               return false;
            }

            tellOwnerItsPassengers(player);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean perched() {
      return this.getVehicle() instanceof Player;
   }

   public boolean startRiding(Entity vehicle, boolean force) {
      return Vehicles.mayBoard(this, vehicle, this.boardingCooldown) && super.startRiding(vehicle, force);
   }

   @Override
   public void leavePerch() {
      if (this.getVehicle() instanceof Player owner) {
         this.stopRiding();
         tellOwnerItsPassengers(owner);
      }
   }

   public void beckon(Player player) {
      FaunaMind mind = this.faunaMind();
      if (mind != null) {
         mind.perception().post(this, new Beckoned(player.getUUID()));
      }
   }

   public void urge(Player owner, Entity target, boolean calm) {
      FaunaMind mind = this.faunaMind();
      if (mind != null && !this.level().isClientSide()) {
         mind.perception().deliver(this, new Urged(owner.getUUID(), target.getUUID(), this.kindOfAttacker(target), target.position(), calm, mind.age()));
      }
   }

   private boolean mustLeavePerch(Player owner) {
      return !owner.isAlive()
         || owner.isSpectator()
         || owner.isPassenger()
         || owner.isInWater()
         || owner.isInPowderSnow
         || owner.isSleeping()
         || owner.getAbilities().flying
         || owner.isFallFlying()
         || owner.isAutoSpinAttack()
         || owner.hurtTime > 0
         || this.hurtTime > 0;
   }

   public static boolean setDownPet(Player owner) {
      boolean any = false;

      for (Entity passenger : List.copyOf(owner.getPassengers())) {
         if (passenger instanceof FaunaBase pet && pet.perched() && pet.isOwnedBy(owner)) {
            pet.leavePerch();
            any = true;
         }
      }

      return any;
   }

   public void releaseToWild() {
      if (!this.level().isClientSide()) {
         UUID was = this.getOwnerUUID();
         if (was != null) {
            this.letGoOfOwner();
            this.componentRegistry.get(CommandComponent.class).setCommandForced(Stance.FREE, Stance.FREE.defaultModifier());
            this.setOwnerUUID(null);
            this.setTame(false);
            this.onOwnerChanged(was, null);
         }
      }
   }

   public void transferTo(Player newOwner) {
      if (!this.level().isClientSide()) {
         UUID was = this.getOwnerUUID();
         if (was != null && !was.equals(newOwner.getUUID())) {
            this.letGoOfOwner();
            this.setOwnerUUID(newOwner.getUUID());
            this.onOwnerChanged(was, newOwner.getUUID());
         }
      }
   }

   private void letGoOfOwner() {
      this.leavePerch();
      this.ejectPassengers();
   }

   protected void onOwnerChanged(UUID was, @org.jetbrains.annotations.Nullable UUID now) {
   }

   public void rideTick() {
      super.rideTick();
      if (this.getVehicle() instanceof Player owner) {
         this.seatOnShoulder(owner);
         if (!this.level().isClientSide() && this.mustLeavePerch(owner)) {
            this.leavePerch();
         }
      } else {
         Vehicles.leaveAtWill(this, this.faunaIntent);
      }
   }

   private void seatOnShoulder(Player owner) {
      double side = owner.getShoulderEntityLeft().isEmpty() ? 1.0 : -1.0;
      double lateral = side * (owner.getBbWidth() + this.getBbWidth()) * 0.5;
      float yaw = owner.yBodyRot * (float) (Math.PI / 180.0);
      this.setPos(owner.getX() + Mth.cos(yaw) * lateral, owner.getY() + owner.getBbHeight() * 0.75, owner.getZ() + Mth.sin(yaw) * lateral);
      this.setYRot(owner.yBodyRot);
      this.yBodyRot = owner.yBodyRot;
   }

   private static void tellOwnerItsPassengers(Player owner) {
      if (owner instanceof ServerPlayer player) {
         player.serverLevel().getChunkSource().broadcastAndSend(player, new ClientboundSetPassengersPacket(owner));
      }
   }

   public boolean isInWall() {
      return !this.perched() && super.isInWall();
   }

   public boolean canRiderInteract() {
      return this.perched();
   }

   public boolean hopping() {
      return this.hopAirborne;
   }

   @Override
   public double hopHeight() {
      return this.profile.canJump ? 2.0 : 0.0;
   }

   @Override
   public boolean hop(double rise) {
      if (!(this.hopHeight() <= 0.0) && this.onGround() && !(rise > this.hopHeight() + 0.125)) {
         Vec3 velocity = this.getDeltaMovement();
         this.setDeltaMovement(velocity.x, WalkEngine.hopSpeed(rise), velocity.z);
         this.hasImpulse = true;
         this.hopLaunch = true;
         this.hopAirborne = true;
         this.leaping = false;
         this.hopFalling = false;
         this.express(ExpressionKey.body(ExpressionKey.Body.HOP_BEGIN));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean leap(UUID target, Vec3 at) {
      if (!(this.hopHeight() <= 0.0) && this.onGround()) {
         Vec3 mark = at;
         if (target != null && this.level() instanceof ServerLevel server && server.getEntity(target) instanceof LivingEntity prey && prey.isAlive()) {
            mark = prey.position();
         }

         Vec3 flat = new Vec3(mark.x - this.getX(), 0.0, mark.z - this.getZ());
         double distance = flat.length();
         Vec3 jaws = CombatReach.biteOrigin(this, mark);
         double carry = distance - Math.min(distance, Math.hypot(jaws.x - this.getX(), jaws.z - this.getZ()));
         if (carry > WalkEngine.leapReach(this.hopHeight())) {
            return false;
         }

         double rise = WalkEngine.hopSpeed(this.hopHeight());
         Vec3 along = distance < 1.0E-6 ? Vec3.ZERO : flat.scale(carry / distance / WalkEngine.airTicks(rise));
         this.setDeltaMovement(along.x, rise, along.z);
         if (flat.lengthSqr() >= 1.0E-6) {
            this.setYRot((float)(Mth.atan2(flat.z, flat.x) * 180.0F / (float)Math.PI) - 90.0F);
            this.yBodyRot = this.getYRot();
         }

         this.hasImpulse = true;
         this.hopLaunch = true;
         this.hopAirborne = true;
         this.leaping = true;
         this.hopFalling = false;
         this.express(ExpressionKey.body(ExpressionKey.Body.HOP_BEGIN));
         return true;
      } else {
         return false;
      }
   }

   @Override
   public double groundHeightNear(Vec3 point, double fromY, double maxRise) {
      return this.worldSense().groundHeightNear(point, fromY, maxRise);
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public BlockPos findStandableNear(BlockPos approx) {
      return !this.profile.hasMode(LocomotionMode.WALK) && this.profile.hasMode(LocomotionMode.SWIM)
         ? this.worldSense().findSwimmableNear(approx, this.profile.hasMode(LocomotionMode.DIVE))
         : this.worldSense().findStandableNear(approx);
   }

   @Override
   public BlockPos findSwimmableNear(BlockPos approx) {
      return this.profile.hasMode(LocomotionMode.SWIM) ? this.worldSense().findSwimmableNear(approx, false) : null;
   }

   @Override
   public double turnRateDegrees(double speedPerTick) {
      MoveMode mode = this.currentMode();
      return mode != MoveMode.AIR && mode != MoveMode.WATER
         ? Math.max(1.0, this.getStatSheet().groundTurn())
         : Math.max(1.0, FlightEngine.maxTurnSpeed(this, Math.max(0.0, speedPerTick)));
   }

   @Override
   public float stallFraction() {
      double top = this.getAttributeValue(Attributes.FLYING_SPEED);
      return top <= 0.15F ? 1.0F : (float)(0.15F / top);
   }

   @Override
   public double airGapBelow(Vec3 point) {
      return this.worldSense().airGapBelow(point);
   }

   @Override
   public boolean overWater(Vec3 point) {
      return this.worldSense().overWater(point);
   }

   @Override
   public double landHeightUnder(Vec3 point) {
      return this.serverLevel().getHeight(Types.MOTION_BLOCKING_NO_LEAVES, Mth.floor(point.x), Mth.floor(point.z));
   }

   @Override
   public double airUnder(Vec3 point, double upTo) {
      Level level = super.level();
      int x = Mth.floor(point.x);
      int z = Mth.floor(point.z);
      int top = Mth.floor(point.y);
      int bottom = Mth.floor(point.y - upTo);
      MutableBlockPos cursor = new MutableBlockPos();

      for (int y = top; y >= bottom; y--) {
         cursor.set(x, y, z);
         if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty() || !level.getFluidState(cursor).isEmpty()) {
            return Math.max(0.0, Math.min(upTo, point.y - (y + 1)));
         }
      }

      return upTo;
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public BlockPos landingSiteNear(BlockPos approx, int radius) {
      int stride = MoveTuning.airLatticeStep(this.getBbWidth());

      for (int ring = 0; ring <= radius; ring += stride) {
         for (int i = 0; i < 8; i++) {
            double angle = i * (Math.PI / 4);
            BlockPos probe = ring == 0 ? approx : approx.offset((int)Math.round(Math.sin(angle) * ring), 0, (int)Math.round(Math.cos(angle) * ring));
            BlockPos site = this.findStandableNear(probe);
            if (site != null && this.canAlightAt(site)) {
               return site;
            }

            if (ring == 0) {
               break;
            }
         }
      }

      return null;
   }

   @Override
   public double topSpeed(MoveMode mode) {
      FaunaStatSheet stats = this.getStatSheet();

      return switch (mode) {
         case AIR -> stats.airSpeed();
         case WATER -> WaterEngine.topSpeed(this, stats);
         default -> stats.groundSpeed();
      };
   }

   @Override
   public int modeChangeTicks() {
      return TakeoffTransition.launchWindowTicks(this);
   }

   @Override
   public boolean canTakeOff() {
      if (!this.isModeAvailable(LocomotionMode.FLY)) {
         return false;
      } else {
         return this.currentMode() == MoveMode.AIR ? true : TakeoffTransition.hasHeadroomAbove(this, TakeoffTransition.headroom(this));
      }
   }

   private boolean canAlightAt(BlockPos site) {
      int top = site.getY() + MoveTuning.skyClearance(this.getBbWidth()) + Mth.ceil(this.getBbHeight()) - 1;
      return BodyFit.columnClear(
         this.level(), new MutableBlockPos(), BodyFit.radiusCells(this.getBbWidth()), site.getX(), site.getY(), top, site.getZ()
      );
   }

   @Override
   public boolean takingOff() {
      LocomotionOrchestrator orchestrator = this.getLocomotionOrchestrator();
      return orchestrator != null && orchestrator.isTakeoffActive();
   }

   @Override
   public boolean incapacitated() {
      return this.isIncapacitated();
   }

   @Override
   public void requestMoveMode(MoveMode mode) {
      LocomotionOrchestrator orchestrator = this.getLocomotionOrchestrator();
      if (orchestrator != null && !this.steeredByRider()) {
         switch (mode) {
            case GROUND:
               if (this.currentMode() == MoveMode.AIR && !this.onGround()) {
                  orchestrator.requestTransition(LocomotionTransition.LAND, ModeChangeCause.LANDING);
                  return;
               }

               if (!this.profile.hasMode(LocomotionMode.WALK)) {
                  return;
               }

               orchestrator.requestMode(LocomotionMode.WALK, ModeChangeCause.LANDING);
               break;
            case AIR:
               if (this.currentMode() == MoveMode.AIR) {
                  return;
               }

               if (!this.isModeAvailable(LocomotionMode.FLY)) {
                  return;
               }

               orchestrator.requestTransition(LocomotionTransition.TAKEOFF, ModeChangeCause.AI_TAKEOFF);
               break;
            case WATER:
               orchestrator.requestMode(LocomotionMode.SWIM, ModeChangeCause.WATER_ENTER);
            case CLIMB:
         }
      }
   }

   @Override
   public float navRange() {
      return Math.max(64.0F, (float)this.getAttributeValue(Attributes.FOLLOW_RANGE));
   }

   @Override
   public double nearestPlayerDistance(double max) {
      Player nearest = this.serverLevel().getNearestPlayer(this.getX(), this.getY(), this.getZ(), max, (Predicate)null);
      return nearest == null ? Double.MAX_VALUE : nearest.position().distanceTo(this.position());
   }

   @Override
   public void applyMove(MoveIntent intent) {
      this.faunaIntent = intent;
   }

   private void driveFromFauna(MoveIntent intent) {
      LocomotionOrchestrator orchestrator = this.getLocomotionOrchestrator();
      if (orchestrator != null) {
         if (orchestrator.activeMode() == LocomotionMode.FLY && !this.canFly() && !orchestrator.isTransitionActive()) {
            orchestrator.requestMode(LocomotionMode.WALK, ModeChangeCause.GROUNDED);
         }

         if (this.stoodOn()) {
            intent = MoveIntent.hold(intent.mode());
         }

         if (!intent.moving()) {
            float held = (float)Mth.clamp(intent.direction().y, -1.0, 1.0);
            orchestrator.tick(new SteeringIntent(0.0F, 0.0F, this.getYRot(), 0.0F, held, false), LocomotionOrchestrator.IntentSource.AI);
         } else {
            float yaw = WalkEngine.pathYaw(intent.direction().x, intent.direction().z);
            boolean airborne = intent.mode() == MoveMode.AIR;
            boolean sprint = intent.running() && this.species().movement().canSprint();
            float forward = intent.speed();
            double demand = intent.direction().y;
            float vertical = (float)Mth.clamp(demand, -1.0, 1.0);
            float pitch = 0.0F;
            if (airborne) {
               float nose = FlightGait.noseFor(demand, forward);
               if (FlightGait.hasNose(nose)) {
                  pitch = Mth.clamp(FlightGait.noseFor(demand, this.carriedShare()), -65.0F, 65.0F);
                  vertical = 0.0F;
               }
            }

            orchestrator.tick(new SteeringIntent(forward, 0.0F, yaw, pitch, vertical, sprint), LocomotionOrchestrator.IntentSource.AI);
         }
      }
   }

   @Override
   public Armament armament() {
      OmniAttackHandler breath = this.componentRegistry.get(OmniAttackHandler.class);
      return breath != null && breath.hasAttack()
         ? new Armament(
            true,
            breath.canAttack(),
            Math.min(breath.fuelTicksRemaining(), breath.staminaTicksRemaining()),
            breath.isStream(),
            breath.chargeTicks(),
            breath.definition().reach().filter(r -> r > 0.0F).map(r -> r.floatValue() + this.jawSpan()).orElse(Double.POSITIVE_INFINITY)
         )
         : Armament.NONE;
   }

   @Override
   public void holdTrigger(boolean pressed) {
      if (!this.steeredByRider()) {
         this.mindTrigger = pressed;
      }

      OmniAttackHandler breath = this.componentRegistry.get(OmniAttackHandler.class);
      if (breath != null) {
         breath.handlePlayerAttack(pressed);
      }

      this.express(ExpressionKey.body(pressed ? ExpressionKey.Body.TRIGGER_PRESS : ExpressionKey.Body.TRIGGER_RELEASE));
   }

   @Override
   public void dropTrigger() {
      this.mindTrigger = false;
      OmniAttackHandler breath = this.componentRegistry.get(OmniAttackHandler.class);
      if (breath != null) {
         breath.cancelCharge();
      }

      this.express(ExpressionKey.body(ExpressionKey.Body.TRIGGER_RELEASE));
   }

   private void letGoOfMindTrigger() {
      if (this.mindTrigger) {
         this.mindTrigger = false;
         OmniAttackHandler breath = this.componentRegistry.get(OmniAttackHandler.class);
         if (breath != null) {
            breath.cancelCharge();
            breath.handlePlayerAttack(false);
         }
      }
   }

   @Override
   public boolean canAimAt(Vec3 point) {
      return this.headTrackingSystem != null && this.headTrackingSystem.canAimAt(point);
   }

   @Override
   public void gazeAt(Vec3 target) {
      if (this.headTrackingSystem != null) {
         if (target == null) {
            this.headTrackingSystem.clearTarget(FaunaHeadTrackingSystem.LookPriority.AMBIENT);
         } else {
            this.headTrackingSystem.lookAt(target, FaunaHeadTrackingSystem.LookPriority.AMBIENT);
         }
      }
   }

   @Override
   public void gazeAt(Vec3 target, UUID creature) {
      if (this.headTrackingSystem != null) {
         Entity seen = creature != null && this.level() instanceof ServerLevel server ? server.getEntity(creature) : null;
         if (target != null && seen != null && seen.isAlive()) {
            this.headTrackingSystem.lookAt(seen, FaunaHeadTrackingSystem.LookPriority.AMBIENT);
         } else {
            this.gazeAt(target);
         }
      }
   }

   @Override
   public void applyLook(Vec3 target) {
      if (this.headTrackingSystem != null) {
         if (target == null) {
            this.headTrackingSystem.clearTarget(FaunaHeadTrackingSystem.LookPriority.ENTITY);
         } else {
            this.headTrackingSystem.lookAt(target, FaunaHeadTrackingSystem.LookPriority.ENTITY);
         }
      }
   }

   @Override
   public boolean electAbility(@Nullable UUID engagedWith, boolean spent, boolean fleeing, int committedTicks) {
      CombatComponent combat = this.componentRegistry.get(CombatComponent.class);
      if (combat == null) {
         return false;
      }

      LivingEntity target = null;
      if (engagedWith != null
         && super.level() instanceof ServerLevel serverLevel
         && serverLevel.getEntity(engagedWith) instanceof LivingEntity found
         && found.isAlive()) {
         target = found;
      }

      OmniAttackHandler breath = this.componentRegistry.get(OmniAttackHandler.class);
      int sinceBreath = breath == null ? Integer.MAX_VALUE : breath.ticksSinceBreathReleased();
      return combat.elect(new AbilityMoment(target, spent, fleeing, sinceBreath, committedTicks));
   }

   @Override
   public void setPose(PoseFlag flag, boolean on) {
      if (!this.level().isClientSide()) {
         int bit = 1 << flag.ordinal();
         int packed = (Integer)this.entityData.get(DATA_FAUNA_POSES);
         int updated = on ? packed | bit : packed & ~bit;
         if (updated != packed) {
            this.entityData.set(DATA_FAUNA_POSES, updated);
            if (flag == PoseFlag.LOAFING) {
               LOAFERS.addAndGet(on ? 1 : -1);
            }

            if (flag == PoseFlag.BUCKING && on) {
               this.beginRodeoStruggle();
            }

            this.express(ExpressionKey.pose(flag, on ? ExpressionKey.Edge.RAISE : ExpressionKey.Edge.DROP));
         }
      }
   }

   public void predictFaunaPose(PoseFlag flag, boolean on) {
      if (this.level().isClientSide() && this.isControlledByLocalInstance()) {
         int bit = 1 << flag.ordinal();
         int packed = (Integer)this.entityData.get(DATA_FAUNA_POSES);
         int updated = on ? packed | bit : packed & ~bit;
         if (updated != packed) {
            this.entityData.set(DATA_FAUNA_POSES, updated);
         }
      }
   }

   public void wallHitPoseFor(int ticks) {
      if (!this.level().isClientSide()) {
         this.setPose(PoseFlag.WALL_HIT, true);
         this.wallHitPoseTicks = Math.max(1, ticks);
      }
   }

   public boolean hasFaunaPose(PoseFlag flag) {
      return ((Integer)this.entityData.get(DATA_FAUNA_POSES) & 1 << flag.ordinal()) != 0;
   }

   @Override
   public boolean hasPose(PoseFlag flag) {
      return this.hasFaunaPose(flag);
   }

   @Override
   public void applySignals(Signals signals) {
      if (!this.level().isClientSide()) {
         this.setSyncedTemperamentPacked(AnimSignalSync.packTraits(signals.traits()));
         this.setSyncedAnimSignalsPacked(
            AnimSignalSync.packDynamic(
               signals.wariness(), AnimSignalSync.activityIndex(signals.activity()), this.bodySignalFlags() | (signals.begging() ? 2 : 0)
            )
         );
         this.setSyncedSocialSignalsPacked(AnimSignalSync.packSocial(signals.stance(), signals.regard()));
      }
   }

   private int bodySignalFlags() {
      int flags = 0;
      if (this.isEating()) {
         flags |= 8;
      }

      if (this.isRefusalPulse()) {
         flags |= 32;
      }

      return flags;
   }

   @Override
   public void express(ExpressionKey key) {
      if (!this.level().isClientSide()) {
         FaunaNetwork.sendToClientsTracking(new FaunaExpressionEventMessage(this.getId(), key.key()), this);
         if (this.species().calls().has(key.key()) && this.canCall()) {
            this.call();
         }
      }
   }

   @Override
   public void express(ExpressionKey key, UUID about) {
      this.express(key);
      if (about != null) {
         this.roar(about);
      }
   }

   private void roar(UUID about) {
      if (this.level() instanceof ServerLevel server) {
         this.setRoaring(true);
         this.roarTicks = 40;
         double var6 = SenseTuning.LOUDNESS.get(SoundKind.ROAR) * 2.0F;
         Vec3 from = this.position();
         FaunaWorldEvents.forEachMindWithin(server, this, var6, mind -> {
            AnimalHandle listener = mind.handle();
            Kind me = listener.entity() instanceof FaunaBase other ? other.kindOf(this) : null;
            HeardSound sound = new HeardSound(SoundKind.ROAR, from, this.getUUID(), mind.age(), (float)listener.bodyPosition().distanceTo(from), me, about);
            if (listener.entity() instanceof FaunaBase other) {
               other.hear(sound);
            } else {
               mind.perception().deliver(listener, sound);
            }
         });
      }
   }

   public boolean hear(HeardSound sound) {
      FaunaMind mind = this.faunaMind();
      if (mind != null && mind.perception().deliver(this, sound)) {
         if (!this.asleep()
            && !this.getSyncedIsIncapacitated()
            && this.headTrackingSystem != null
            && (this.getVehicle() == null || !this.getVehicle().getUUID().equals(sound.sourceId()))) {
            float strength = mind.perception().strengthOf(this, sound);
            int glance = Math.round(40.0F * strength);
            if (glance >= 6) {
               this.headTrackingSystem.glanceAt(sound.position(), glance, strength);
            }

            return true;
         } else {
            return true;
         }
      } else {
         return false;
      }
   }

   protected boolean canCall() {
      return this.callHold <= 0;
   }

   @Override
   public boolean held() {
      return this.callHold > 0;
   }

   public boolean spares(LivingEntity other) {
      return this.faunaMind != null ? this.faunaMind.knowledge().caresAbout(other.getUUID()) : other.getType() == this.getType();
   }

   @Override
   public String animSpeciesId() {
      return this.speciesId();
   }

   @Override
   public String animClipPrefix() {
      return "animation." + BabyArt.clipKey(this) + ".";
   }

   public String animBindingKey() {
      return BabyArt.bindingKey(this);
   }

   @Override
   public void playVoice(String name) {
      if (name.indexOf(58) >= 0) {
         SoundEvent sound = Regs.sound(ResourceLocation.tryParse(name));
         if (sound != null && this.level().isClientSide) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), sound, this.getSoundSource(), 1.0F, 1.0F, false);
         }
      } else {
         FaunaSoundHelper.playLocal(this, name);
      }
   }

   @Override
   public boolean hasClip(String fullClip) {
      FaunaDispatcher dispatcher = this.getDispatcher();
      return dispatcher != null && dispatcher.ready() && dispatcher.hasAnimation(fullClip);
   }

   @Override
   public void playClip(String layer, String fullClip) {
      FaunaDispatcher dispatcher = this.getDispatcher();
      if (dispatcher != null && dispatcher.ready()) {
         this.getStateContext().getBindingResolver().interrupt(layer, fullClip, this);
      }
   }

   @Override
   public Vec3 animPosition() {
      return this.position();
   }

   @Override
   public void animWash(int count) {
      if (this.level().isClientSide()) {
         FaunaFlightParticleHelper.burst(this, count);
      }
   }

   @Override
   public Vec3 animBoneSpot(String bone) {
      return !this.level().isClientSide() ? null : BoneSpots.get(this.getId(), bone, this.level().getGameTime());
   }

   @Override
   public Level animLevel() {
      return this.level();
   }

   @Override
   public EventTable animEvents() {
      return AnimationBindingLoader.get(this.animBindingKey()).events;
   }

   @Override
   public List<String> animPartBones() {
      List<String> bones = new ArrayList<>();

      for (PartSpec part : this.bodyPlan().parts()) {
         bones.add(part.bone());
         if (part.elongated()) {
            bones.add(part.toward());
         }
      }

      return bones;
   }

   @Override
   public RigSpec animRig() {
      RigSpec binding = AnimationBindingLoader.get(this.animBindingKey()).rig;
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart == null ? binding : multipart.rigFor(binding);
   }

   @Override
   public StepsSpec animSteps() {
      return AnimationBindingLoader.get(this.animBindingKey()).steps;
   }

   @Override
   public int animTick() {
      return this.tickCount;
   }

   @Override
   public boolean animStepsAllowed() {
      return this.getStateContext().getBindingResolver().stepsAllowed();
   }

   @Override
   public boolean animStepsMoving() {
      return this.getStateContext().getBindingResolver().stepsMoving();
   }

   @Override
   public boolean animGrounded() {
      return !this.isFlying() && (this.getTransportMode() == TransportMode.GROUNDED || this.isClimbing());
   }

   public Footing footing() {
      if (this.getVehicle() instanceof Player owner) {
         return ShoulderFooting.of(this, owner);
      } else {
         if (this.isClimbing()) {
            ClimbEngine climb = this.componentRegistry.get(ClimbEngine.class);
            if (climb != null && climb.footed()) {
               return climb.footing();
            }
         }

         return this.groundFooting;
      }
   }

   @Override
   public Footing animFooting() {
      return this.footing();
   }

   @Override
   public Object animAttitude() {
      return this.componentRegistry.get(AttitudeComponent.class);
   }

   @Override
   public Object animLimbReach() {
      return this.componentRegistry.get(LimbReachComponent.class);
   }

   @Override
   public void saveMind(CompoundTag tag) {
      if (this.faunaMind != null) {
         this.faunaMind.save(tag);
      }
   }

   @Override
   public void loadMind(CompoundTag tag) {
      if (this.faunaMind != null) {
         this.faunaMind.load(tag);
      }
   }

   public void updateDynamicGameEventListener(BiConsumer<DynamicGameEventListener<?>, ServerLevel> consumer) {
      FaunaNoiseListener ears = this.componentRegistry.get(FaunaNoiseListener.class);
      if (ears != null) {
         ears.updateDynamicGameEventListener(consumer);
      }
   }

   @Override
   public List<SensedBody> scanNearby(double radius) {
      return this.worldSense().scanNearby(radius);
   }

   @Override
   public List<SensedItem> scanItems(double radius) {
      return this.worldSense().scanItems(radius);
   }

   private boolean wantsTouchFrom(LivingEntity other) {
      if (!(other instanceof Player player)) {
         return false;
      } else {
         FaunaMind mind = this.faunaMind();
         if (mind != null && !this.isTame()) {
            Knowledge knowledge = mind.knowledge();
            if (!knowledge.courting(this, knowledge.about(player.getUUID()))) {
               return false;
            }

            Courtship courtship = knowledge.courtship();
            if (courtship.atMount(player.getUUID(), this.rideable())) {
               return this.getGrowthStage() >= this.mountStage() && player.getMainHandItem().isEmpty();
            }

            if (!courtship.playing(player.getUUID())) {
               return false;
            }

            Ladder.StageDef stage = mind.ladderFor().stageOf(Check.Engagement.TOUCH);
            if (stage == null) {
               return false;
            }

            if (this.tamingMatrix().act("groom") == Valence.HATES) {
               return false;
            }

            Check asked = courtship.asking(player.getUUID());
            if (asked == null || asked.engages() != Check.Engagement.TOUCH) {
               return false;
            }

            if (stage.crouched() && !player.isShiftKeyDown()) {
               return false;
            }

            TamingPredicate wanted = TamingPredicate.forKey(stage.item());
            return wanted == null ? player.getMainHandItem().isEmpty() && player.getOffhandItem().isEmpty() : wanted.test(this, player);
         } else {
            return false;
         }
      }
   }

   public float cravingFor(ItemStack stack) {
      return this.craving(stack);
   }

   public float relishFor(ItemStack stack) {
      return this.relish(stack);
   }

   protected float relish(ItemStack stack) {
      return 1.0F;
   }

   @Override
   public boolean eatItem(UUID itemId) {
      if (super.level() instanceof ServerLevel serverLevel) {
         if (!(serverLevel.getEntity(itemId) instanceof ItemEntity item && item.isAlive())) {
            return false;
         } else {
            if (this.isEating()) {
               return false;
            }

            this.startEating(item);
            return this.isEating();
         }
      } else {
         return false;
      }
   }

   @Override
   public boolean snatch(UUID holder) {
      if (super.level() instanceof ServerLevel serverLevel) {
         CarryComponent carry = this.componentRegistry.get(CarryComponent.class);
         if (carry != null && !carry.isCarrying()) {
            if (!(serverLevel.getEntity(holder) instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator())) {
               return false;
            } else if (!this.getBoundingBox().inflate(1.0).intersects(player.getBoundingBox())) {
               return false;
            } else {
               ItemStack hand = this.heldFood(player);
               if (!hand.isEmpty() && carry.takeFrom(hand, player.getUUID())) {
                  this.getLookControl().setLookAt(player);
                  return true;
               } else {
                  return false;
               }
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public UUID loot() {
      CarryComponent carry = this.componentRegistry.get(CarryComponent.class);
      return carry == null ? null : carry.victim();
   }

   @Override
   public boolean eatLoot() {
      if (!super.level().isClientSide() && !this.isEating() && !this.getCarriedItem().isEmpty()) {
         UUID victim = this.loot();
         ItemStack meal = this.getCarriedItem().copy();
         this.startEatingCarried();
         FaunaMind mind = this.faunaMind();
         if (this.isEating()
            && victim != null
            && mind != null
            && super.level() instanceof ServerLevel server
            && server.getEntity(victim) instanceof Player giver) {
            mind.perception()
               .deliver(
                  this,
                  new Kindness(victim, this.kindOfAttacker(giver), giver.position(), mind.age(), Delivery.DROPPED, this.cravingFor(meal), this.relishFor(meal))
               );
         }

         return this.isEating();
      } else {
         return false;
      }
   }

   @Override
   public void dropLoot() {
      CarryComponent carry = this.componentRegistry.get(CarryComponent.class);
      if (carry != null) {
         carry.dropHere();
      }
   }

   @Override
   public boolean takeItem(UUID itemId) {
      if (super.level() instanceof ServerLevel serverLevel) {
         if (serverLevel.getEntity(itemId) instanceof ItemEntity item && item.isAlive()) {
            CarryComponent carry = this.componentRegistry.get(CarryComponent.class);
            if (carry == null) {
               return false;
            }

            ItemStack gift = item.getItem().copy();
            Entity thrower = item.getOwner();
            if (!carry.take(item)) {
               return false;
            }

            FaunaMind mind = this.faunaMind();
            if (mind != null && thrower instanceof LivingEntity giver) {
               mind.perception()
                  .deliver(this, new Kindness(giver.getUUID(), this.kindOfAttacker(giver), giver.position(), mind.age(), Delivery.GIFT, this.cravingFor(gift)));
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public void stash() {
      CarryComponent carry = this.componentRegistry.get(CarryComponent.class);
      if (carry != null) {
         carry.stash();
      }
   }

   private float craving(ItemStack stack) {
      if (stack.isEmpty()) {
         return 0.0F;
      }

      Valence row = this.tamingMatrix().row(Regs.itemKey(stack.getItem()), key -> stack.is(TagKey.create(Registries.ITEM, key)));
      return row == null ? this.tierCraving(stack) : this.cravingOf(row);
   }

   private float cravingOf(Valence meaning) {
      return switch (meaning) {
         case CRAVES -> 1.0F;
         case LIKES -> 0.66F;
         case NEUTRAL, RESPECTS -> 0.12F;
         case DISLIKES, HATES -> 0.0F;
      };
   }

   private ItemStack heldFood(LivingEntity other) {
      ItemStack main = other.getMainHandItem();
      ItemStack off = other.getOffhandItem();
      boolean mainFood = this.isFood(main);
      boolean offFood = this.isFood(off);
      if (mainFood && offFood) {
         return this.craving(main) >= this.craving(off) ? main : off;
      } else if (mainFood) {
         return main;
      } else {
         return offFood ? off : ItemStack.EMPTY;
      }
   }

   public Kind kindOfAttacker(@org.jetbrains.annotations.Nullable Entity attacker) {
      return attacker == null ? Kind.UNKNOWN : this.worldSense().kindOf(attacker);
   }

   @Override
   public boolean strike(UUID targetId) {
      return this.swingAt(targetId, false);
   }

   @Override
   public boolean shove(UUID targetId) {
      return this.swingAt(targetId, true);
   }

   private boolean swingAt(UUID targetId, boolean inPlay) {
      if (super.level() instanceof ServerLevel serverLevel) {
         if (serverLevel.getEntity(targetId) instanceof LivingEntity target) {
            if (target.isAlive() && target.canBeSeenAsEnemy()) {
               IMeleeAttackHandler melee = this.componentRegistry.get(IMeleeAttackHandler.class);
               if (melee == null) {
                  return false;
               }

               this.express(ExpressionKey.body(ExpressionKey.Body.STRIKE));
               double reach = melee.meleeReach();
               if (reach <= 0.0) {
                  return false;
               }

               Vec3 origin = CombatReach.biteOrigin(this, target.position());
               Vec3 nearest = CombatReach.closestPoint(target, origin);
               if (origin.distanceToSqr(nearest) > reach * reach) {
                  return false;
               }

               this.getLookControl().setLookAt(target, 30.0F, 30.0F);
               return inPlay ? melee.shove(target) : melee.attack(target);
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @Override
   public double strikeRange() {
      IMeleeAttackHandler melee = this.componentRegistry.get(IMeleeAttackHandler.class);
      if (melee == null) {
         return 0.0;
      }

      double reach = melee.meleeReach();
      return reach <= 0.0 ? 0.0 : reach + this.jawSpan();
   }

   private double jawSpan() {
      double span = 0.0;

      for (FaunaPartEntity head : this.headParts()) {
         span = Math.max(span, head.position().distanceTo(this.position()));
      }

      return span > 0.0 ? span : this.getEyePosition().distanceTo(this.position());
   }

   public void feedOnKill(float preyMaxHealth) {
      FaunaNeedsSystem needs = this.componentRegistry.get(FaunaNeedsSystem.class);
      if (needs != null) {
         needs.feed(Math.round(preyMaxHealth * 2.5F));
      }
   }

   @Override
   public float hungerLevel() {
      FaunaNeedsSystem needs = this.componentRegistry.get(FaunaNeedsSystem.class);
      return needs == null ? 0.0F : 1.0F - needs.getFoodLevel() / 100.0F;
   }

   protected int mountedBondInterval() {
      return 120;
   }

   protected int unsaddledRideTicks() {
      return 350;
   }

   public static EntityDataAccessor<Integer> getGrowthStageData() {
      return DATA_GROWTH_STAGE;
   }

   public static EntityDataAccessor<Integer> getGrowthProgressData() {
      return DATA_GROWTH_PROGRESS;
   }

   public static EntityDataAccessor<Integer> getSpawnScaleTicksData() {
      return DATA_SPAWN_SCALE_TICKS;
   }

   public static EntityDataAccessor<Integer> getCommandData() {
      return DATA_COMMAND;
   }

   public static EntityDataAccessor<Integer> getCommandModifierData() {
      return DATA_COMMAND_MODIFIER;
   }

   public static EntityDataAccessor<CompoundTag> getGenotypeData() {
      return DATA_GENOTYPE;
   }

   public static EntityDataAccessor<Float> getStaminaData() {
      return DATA_STAMINA;
   }

   public static EntityDataAccessor<Float> getMaxStaminaData() {
      return DATA_MAX_STAMINA;
   }

   public boolean isQteActive() {
      return (Boolean)this.entityData.get(DATA_QTE_ACTIVE);
   }

   public void setQteActive(boolean active) {
      this.entityData.set(DATA_QTE_ACTIVE, active);
   }

   public static EntityDataAccessor<Integer> getSecondaryCooldownData() {
      return DATA_SECONDARY_COOLDOWN;
   }

   public static EntityDataAccessor<Integer> getSecondaryCooldownMaxData() {
      return DATA_SECONDARY_COOLDOWN_MAX;
   }

   public static EntityDataAccessor<Integer> getHungerLevelData() {
      return DATA_HUNGER_LEVEL;
   }

   public void updateIncapSyncData(boolean incapacitated, int count, float debuffFraction, List<CrippledFeature> crippledFeatures) {
      int flags = incapacitated ? 1 : 0;

      for (CrippledFeature f : crippledFeatures) {
         flags |= 1 << f.ordinal() + 1;
      }

      this.entityData.set(DATA_INCAP_FLAGS, flags);
      this.entityData.set(DATA_INCAP_COUNT, count);
      this.entityData.set(DATA_INCAP_DEBUFF, debuffFraction);
   }

   public boolean getSyncedIsOrderedToSit() {
      int stance = (Integer)this.entityData.get(DATA_COMMAND);
      int modifier = (Integer)this.entityData.get(DATA_COMMAND_MODIFIER) & 7;
      return stance == Stance.STAY.ordinal() && modifier == Modifier.REST.ordinal() && (Boolean)this.entityData.get(DATA_ORDER_HELD);
   }

   public boolean getSyncedOrderHeld() {
      return (Boolean)this.entityData.get(DATA_ORDER_HELD);
   }

   public boolean getSyncedIsIncapacitated() {
      return ((Integer)this.entityData.get(DATA_INCAP_FLAGS) & 1) != 0;
   }

   public float getSyncedDebuffFraction() {
      return (Float)this.entityData.get(DATA_INCAP_DEBUFF);
   }

   public boolean getSyncedIsFeatureCrippled(CrippledFeature feature) {
      return ((Integer)this.entityData.get(DATA_INCAP_FLAGS) & 1 << feature.ordinal() + 1) != 0;
   }

   public FaunaDispatcher getDispatcher() {
      return this.dispatcher;
   }

   public <T> T getData(EntityDataAccessor<T> accessor) {
      return (T)this.entityData.get(accessor);
   }

   public <T> void setData(EntityDataAccessor<T> accessor, T value) {
      this.entityData.set(accessor, value);
   }

   public FaunaStatSheet getStatSheet() {
      return this.componentRegistry.get(StatsComponent.class).getStatSheet();
   }

   public void recalculateStats() {
      this.componentRegistry.get(StatsComponent.class).recalculateStats();
   }

   public void applyStats() {
      this.componentRegistry.get(StatsComponent.class).applyStats();
   }

   protected abstract FaunaProfile buildProfile();

   protected void registerSpeciesComponents(FaunaComponentRegistry registry) {
   }

   private void reservePartIds() {
      int count = this.getParts().length;
      if (count != 0) {
         this.setId(ENTITY_COUNTER.getAndAdd(count + 1) + 1);
      }
   }

   public List<FaunaPartEntity> headParts() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart == null ? List.of() : multipart.getHeadParts();
   }

   public FaunaProfile profile() {
      return this.profile;
   }

   public boolean supportsFlying() {
      return this.profile.hasMode(LocomotionMode.FLY);
   }

   public boolean supportsRiding() {
      return this.profile.canBeRidden;
   }

   public boolean supportsJumping() {
      return this.profile.canJump;
   }

   public static Builder createFaunaAttributes() {
      return TamableAnimal.createMobAttributes()
         .add(Attributes.MAX_HEALTH, 20.0)
         .add(Attributes.ATTACK_DAMAGE, 2.0)
         .add(Attributes.MOVEMENT_SPEED, 0.2)
         .add(Attributes.FLYING_SPEED, 0.4)
         .add(Attributes.ARMOR, 0.0)
         .add(Attributes.FOLLOW_RANGE, 32.0)
         .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
   }

   public void onAddedToWorld() {
      // Vanilla Fabric has no superclass Forge lifecycle hook; Fabric entity-load bridge calls this method.

      for (Holder<Attribute> attribute : Regs.attributes()) {
         if (this.getAttributes().hasAttribute(attribute)) {
            this.getAttributes().getInstance(attribute);
         }
      }
   }

   public boolean abilityLoopActive() {
      return false;
   }

   public void onClientRemoval() {
      super.onClientRemoval();
   }

   protected void defineSynchedData() {
      super.defineSynchedData();
      this.defineData(this.entityData::define);
   }

   protected void defineData(SyncedDataSink data) {
      data.define(DATA_VARIANT_SKIN, "");
      data.define(DATA_VARIANT_ID, "");
      data.define(DATA_GROWTH_STAGE, 0);
      data.define(DATA_GROWTH_PROGRESS, 0);
      data.define(DATA_GENOTYPE, new CompoundTag());
      data.define(DATA_ART_VERSION, 1);
      data.define(DATA_TRANSPORT_MODE, TransportMode.GROUNDED.ordinal());
      data.define(DATA_ATTACK_MOTION, 0);
      data.define(DATA_TEMPERAMENT, 0);
      data.define(DATA_ANIM_SIGNALS, 0);
      data.define(DATA_SOCIAL_SIGNALS, 0);
      data.define(DATA_FAUNA_POSES, 0);
      data.define(DATA_COMMAND, COMMAND_FREE);
      data.define(DATA_COMMAND_MODIFIER, Modifier.GROUND.ordinal());
      data.define(DATA_ORDER_HELD, true);
      data.define(DATA_CARRIED_ITEM, ItemStack.EMPTY);
      data.define(DATA_SLEEP_DISTURB_TICKS, 0);
      data.define(DATA_ABILITY_COOLDOWN_TICKS, 0);
      data.define(DATA_IS_BOOSTING, false);
      data.define(DATA_IS_DIVE_BREAKING, false);
      data.define(DATA_IS_SPRINTING, false);
      data.define(DATA_IS_ROARING, false);
      data.define(DATA_SPAWN_SCALE_TICKS, 0);
      data.define(DATA_HEAD_YAW, 0.0F);
      data.define(DATA_HEAD_PITCH, 0.0F);
      data.define(DATA_LOOK_RUNG, (byte)0);
      data.define(DATA_PART_REPORTER, -1);
      data.define(DATA_HUNGER_LEVEL, FaunaNeedsSystem.getHungerThreshold());
      data.define(DATA_STAMINA, 100.0F);
      data.define(DATA_MAX_STAMINA, 100.0F);
      data.define(DATA_QTE_ACTIVE, false);
      data.define(DATA_SECONDARY_COOLDOWN, 0);
      data.define(DATA_SECONDARY_COOLDOWN_MAX, 0);
      data.define(DATA_INCAP_FLAGS, 0);
      data.define(DATA_INCAP_COUNT, 0);
      data.define(DATA_INCAP_DEBUFF, 0.0F);
      data.define(DATA_ROOST_POS, Optional.empty());
      data.define(BREATH_CURRENT_FUEL, 0);
      data.define(BREATH_IS_BREATHING, false);
      data.define(BREATH_COOLDOWN, 0);
      data.define(BREATH_FULL_RECHARGE_COOLDOWN, 0);
      data.define(BREATH_WINDUP_TICKS, 0);
      data.define(PROJ_TICK_SINCE_LAST_FIRE, 0);
      data.define(PROJ_FIRING_TICKS, 0);
      data.define(PROJ_IS_CHARGING, false);
      data.define(MELEE_ATTACK_PHASE, 0);
      OmniMeleeAttackHandler.EDA_MELEE_PHASE = MELEE_ATTACK_PHASE;
      data.define(MELEE_ATTACK_SLOT, 0);
      OmniMeleeAttackHandler.EDA_MELEE_SLOT = MELEE_ATTACK_SLOT;
      data.define(DATA_IS_BRAKING, false);
      data.define(DATA_IS_FREE_LOOKING, false);
      data.define(DATA_YAW_SPEED, 0.0F);
      data.define(DATA_ROLL_INPUT, 0.0F);
      data.define(DATA_HORIZONTAL_SPEED, 0.0F);
      data.define(DATA_SYNCED_PITCH, 0.0F);
      data.define(DATA_SYNCED_ROLL, 0.0F);
      data.define(DATA_DODGE_ROLL, 0.0F);
      data.define(DATA_RIDER_MOTION, new Vector3f());
      data.define(DATA_GRIP_STATE, (byte)0);
      data.define(DATA_GRIP_HELD, -1);
      data.define(DATA_GRIP_PULL, new Vector3f());
      data.define(DATA_GRIP_LOAD, 0.0F);
      data.define(DATA_STUCK_ARROWS, new CompoundTag());
      data.define(DATA_THROTTLE_LEVEL, 0.0F);
      data.define(DATA_IS_ACCELERATING, false);
      data.define(DATA_IS_DECELERATING, false);
   }

   private void generateWildGenetics(ServerLevelAccessor level) {
      GeneticsPreset preset = this.spawnPreset;
      this.spawnPreset = GeneticsPreset.NONE;
      if (!this.hasGenotype()) {
         String species = this.speciesId();

         try {
            SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(species);
            GeneticsSpawner.SpawnResult result = new GeneticsSpawner(this.random)
               .generateWildGenotype(config, species, level.getBiome(this.blockPosition()), preset);
            if (LOGGER.isTraceEnabled()) {
               LOGGER.trace(result.debugLog);
            }

            this.setGenotypeAndDraws(result.genotype, result.paletteDraws);
            this.recalculateStats();
         } catch (Exception e) {
            LOGGER.error("[GENETICS ERROR] Failed to generate genotype for {}: {}", new Object[]{species, e.getMessage(), e});
         }
      }
   }

   public void presetSpawnGenetics(GeneticsPreset preset) {
      this.spawnPreset = preset;
   }

   public SpawnGroupData finalizeSpawn(
      ServerLevelAccessor pLevel,
      DifficultyInstance pDifficulty,
      MobSpawnType pReason,
      @org.jetbrains.annotations.Nullable SpawnGroupData pSpawnData,
      @org.jetbrains.annotations.Nullable CompoundTag pDataTag
   ) {
      this.setGrowthStage(this.spawnStage());
      this.position();
      this.finalizeHostSpawn();
      this.generateWildGenetics(pLevel);
      return super.finalizeSpawn(pLevel, pDifficulty, pReason, pSpawnData, pDataTag);
   }

   public void tick() {
      PerfClock.Section section = this.level().isClientSide() ? CLOCK_CLIENT_TICK : CLOCK_TICK;
      long clock = section.start();

      try {
         if (this.level().isClientSide() && this.isVehicle() && this.isControlledByLocalInstance()) {
            if (this.subLevelUnder == null) {
               this.subLevelUnder = SubLevels.under(this);
            }

            SubLevels.unpin(this);
            this.facingSent = false;
         }

         this.tickTimed();
         this.holdRidePlace();
         this.readGround();
         this.readRidePace();
         GripComponent grip = this.grip();
         if (grip != null) {
            PerfClock.Section gripClock = this.level().isClientSide() ? CLOCK_CLIENT_GRIP : CLOCK_GRIP;
            long gripStart = gripClock.start();
            grip.afterMove();
            gripClock.end(gripStart);
         }
      } finally {
         section.end(clock);
      }
   }

   private void tickTimed() {
      if (this.level().isClientSide()) {
         ExpressionEventClient.tick(this, this.getId(), this.level().getGameTime());
         ColdBreath.tick(this);
         SleepBubbles.tick(this);
         SwimBubbles.tick(this);
      }

      this.tickHostInteractions();
      if (this.supportsThreeDMedium()) {
         boolean freeLook = this.isFreeLooking();
         if (freeLook != this.wasFreeLookingOnLastTick) {
            this.flightModeTransitionTimer = 15;
            FaunaFlightRotationComponent dfrcTrans = this.componentRegistry.get(FaunaFlightRotationComponent.class);
            if (dfrcTrans != null && dfrcTrans.isRolling()) {
               this.capturedTransitionRoll = dfrcTrans.getRoll();
            } else {
               this.capturedTransitionRoll = 0.0F;
            }

            this.wasFreeLookingOnLastTick = freeLook;
            if (dfrcTrans != null) {
               this.armThreeD(dfrcTrans);
            }
         }

         if (this.flightModeTransitionTimer > 0) {
            this.flightModeTransitionTimer--;
         }
      }

      if (this.stateTransitionCooldown > 0) {
         this.stateTransitionCooldown--;
      }

      if (!this.level().isClientSide() && this.wallHitPoseTicks > 0 && --this.wallHitPoseTicks == 0) {
         this.setPose(PoseFlag.WALL_HIT, false);
      }

      this.stateContext.update();
      RiderInputComponent riderInput = this.componentRegistry.get(RiderInputComponent.class);
      if (riderInput != null) {
         riderInput.captureInput();
      }

      if (!this.level().isClientSide()) {
         long clock = CLOCK_COMPONENTS.start();
         this.componentRegistry.tickServer();
         CLOCK_COMPONENTS.end(clock);
      }

      FaunaTick.fire(this);
      super.tick();
      this.headTrackingSystem.tick();
      if (this.level().isClientSide()) {
         this.eyeState.tick(this);
      }

      ClimbEngine climbing = this.componentRegistry.get(ClimbEngine.class);
      if (climbing != null && this.isClimbing() && !climbing.movedThisTick()) {
         climbing.follow();
      }

      if (this.supportsThreeDMedium()) {
         RollComponent roll = this.componentRegistry.get(RollComponent.class);
         if (roll != null) {
            roll.tickRollAccumulation(
               !roll.dodging() && !roll.rollingOver() ? this.getRollInput() : 0.0F, this.isFlying(), this.onGround(), this.getThrottleLevel()
            );
            int ask = riderInput == null ? 0 : riderInput.takeDodgeAsk();
            roll.tickDodge(this.level().isClientSide() && this.isControlledByLocalInstance() ? ask : 0);
         }

         FaunaFlightRotationComponent dfrc = this.componentRegistry.get(FaunaFlightRotationComponent.class);
         if (dfrc != null) {
            dfrc.tick();
            this.armThreeD(dfrc);
         }

         Vec3 vel = RiderMotion.of(this);
         float hSpeed = this.isClimbing() ? (float)vel.length() : (float)Math.sqrt(vel.x * vel.x + vel.z * vel.z);
         this.entityData.set(DATA_HORIZONTAL_SPEED, Quantum.step(hSpeed, 0.005F));
         ThrottleComponent throttle = this.componentRegistry.get(ThrottleComponent.class);
         if (throttle != null) {
            throttle.tickGroundedReset();
         }

         if (this.level().isClientSide() && !this.isControlledByLocalInstance()) {
            if (dfrc != null && this.inThreeDMedium()) {
               float synced = (Float)this.entityData.get(DATA_SYNCED_ROLL);
               dfrc.stepRoll(roll == null ? synced : roll.watched(synced));
            }
         } else if (!this.level().isClientSide() && (this.isVehicle() || this.inThreeDMedium()) && dfrc != null) {
            this.entityData.set(DATA_SYNCED_ROLL, Quantum.step(dfrc.getRoll(), 0.1F));
         }

         if (this.level().isClientSide() && this.isFlying()) {
            FaunaFlightParticleHelper.spawnWaterTrailParticles(this);
         }
      }

      if (this.level().isClientSide()) {
         SwimWake.tick(this);
         this.moveAnalysis.update();
         this.getWingTrailManager().tick();
         long clientClock = CLOCK_CLIENT_COMPONENTS.start();
         this.componentRegistry.tickClient();
         CLOCK_CLIENT_COMPONENTS.end(clientClock);
      } else {
         this.staminaSystem.tick();
         if (!this.level().isClientSide() && this.inThreeDMedium()) {
            if (this.isBoosting()) {
               this.staminaSystem.drainContinuous(0.5F);
            }

            if (this.isBraking()) {
               this.staminaSystem.drainContinuous(0.3F);
            }

            if (this.steeredByRider()) {
               FaunaFlightRotationComponent dfrcDodge = this.componentRegistry.get(FaunaFlightRotationComponent.class);
               RiderInputComponent dodgeInput = this.componentRegistry.get(RiderInputComponent.class);
               if (dfrcDodge != null && dfrcDodge.isRolling() && dodgeInput != null && Math.abs(dodgeInput.getStrafe()) > 0.1F) {
                  this.staminaSystem.drainContinuous(0.3F);
               }
            }

            if (this.isAccelerating() && this.steeredByRider()) {
               this.staminaSystem.drainContinuous(0.1F);
            }
         }

         if (!this.level().isClientSide() && this.supportsFlying()) {
            float yawSpeed = Mth.wrapDegrees(this.getYRot() - this.yRotO);
            float rollInput = 0.0F;
            if (this.isFlying() && this.steeredByRider()) {
               FaunaFlightRotationComponent dfrcTell = this.componentRegistry.get(FaunaFlightRotationComponent.class);
               RiderInputComponent tellInput = this.componentRegistry.get(RiderInputComponent.class);
               if (tellInput != null && (dfrcTell == null || !dfrcTell.isRolling()) && !this.isFreeLooking()) {
                  rollInput = tellInput.getStrafe();
               }
            }

            this.setYawSpeed(yawSpeed);
            this.setRollInput(rollInput);
            if (this.isFlying() && this.getControllingPassenger() == null) {
               float cp = (Float)this.entityData.get(DATA_SYNCED_PITCH);
               float tp = this.stateContext.getCalculatedPitch();
               if (Math.abs(cp - tp) > 1.0F) {
                  this.entityData.set(DATA_SYNCED_PITCH, tp);
               }
            }
         }

         this.dispatcher.tickBlink();
         this.tickHost();
         if (this.isVehicle()) {
            Entity passenger = this.getControllingPassenger();
            if (passenger instanceof Player player) {
               int interval = this.mountedBondInterval();
               if (interval > 0 && this.isTame() && this.tickCount % interval == 0) {
                  int affectionAmount = 1 + this.random.nextInt(11);
                  this.addBond(player.getUUID(), affectionAmount);
               }
            }
         }

         if (this.isVehicle()) {
            LivingEntity rider = this.getControllingPassenger();
            boolean canAttemptTakeoff = this.onGround() || this.isInWater();
            if (this.supportsFlying() && canAttemptTakeoff && this.isBoosting() && !this.isFlying() && this.stateTransitionCooldown == 0) {
               if (!this.canFly()) {
                  if (this.getGrowthStage() < this.flightStage() && rider instanceof Player player) {
                     Notices.tell(player, Component.translatable("liberanimalium.too_young_to_fly", new Object[]{this.getDisplayName()}));
                  } else if (rider instanceof Player player) {
                     IncapacitationComponent.refuses(this, player, CrippledFeature.FLYING);
                  }
               } else if (!this.isFlightUnlockedFor(rider) && rider instanceof Player player) {
                  Notices.tell(player, Component.translatable("liberanimalium.flight.not_bonded", new Object[]{this.getDisplayName()}));
               }
            }
         } else {
            boolean aiActive = this.faunaMind != null && (this.faunaMind.route().hasRoute() || this.faunaMind.route().solving());
            if (this.isFlying() && this.onGround() && !aiActive && this.stateTransitionCooldown == 0 && !GripComponent.keepsAloft(this)) {
               this.getLocomotionOrchestrator().requestMode(LocomotionMode.WALK, ModeChangeCause.LANDING);
            }

            if (this.getTransportMode() == TransportMode.GROUNDED) {
            }
         }

         if (this.getSleepDisturbTicks() > 0) {
            this.setSleepDisturbTicks(this.getSleepDisturbTicks() - 1);
         }

         if (this.callHold > 0) {
            this.callHold--;
         }

         if (this.getAbilityCooldownTicks() > 0) {
            this.setAbilityCooldownTicks(this.getAbilityCooldownTicks() - 1);
         }

         this.entityData.set(DATA_SECONDARY_COOLDOWN, this.componentRegistry.get(CombatComponent.class).getSecondaryAbilityRemainingCooldown());
         this.entityData.set(DATA_SECONDARY_COOLDOWN_MAX, this.componentRegistry.get(CombatComponent.class).getSecondaryAbilityMaxCooldown());
      }
   }

   public void heal(float pHealAmount) {
      super.heal(pHealAmount);
   }

   public AABB getBoundingBoxForCulling() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart != null && this.isMultipartEntity() ? multipart.cullBox() : this.getBoundingBox().inflate(CullBox.margin(this.getBbWidth()));
   }

   public void aiStep() {
      PerfClock.Section section = this.level().isClientSide() ? CLOCK_CLIENT_AI : CLOCK_AI;
      long clock = section.start();
      super.aiStep();
      section.end(clock);
      this.updateSwingTime();
      if (!this.level().isClientSide()) {
         FlightWatch.serverTick(this, this.faunaIntent);
      }

      if (!this.level().isClientSide()) {
         StuckArrows.tick(this);
      }

      if (!this.level().isClientSide()) {
         int limit = this.unsaddledRideTicks();
         if (limit > 0 && !this.isSaddled() && this.isVehicle() && this.isTame()) {
            this.noSaddleRideTicks++;
            if (this.noSaddleRideTicks > limit) {
               if (this.getControllingPassenger() instanceof Player player && !player.getAbilities().instabuild) {
                  this.ejectPassengers();
               }

               this.noSaddleRideTicks = 0;
            }
         } else {
            this.noSaddleRideTicks = 0;
         }
      }

      if (!this.level().isClientSide() && !this.steeredByRider()) {
         boolean hasSwim = this.profile.hasMode(LocomotionMode.SWIM);
         boolean aquatic = this.getTransportMode() == TransportMode.AQUATIC;
         boolean transitioning = this.getLocomotionOrchestrator().isTransitionActive();
         boolean settles = !this.steeredByRider() && this.settlesOnWater();
         boolean skimming = this.isFlying() && !settles && Plunge.flies(this);
         if (!hasSwim || aquatic || transitioning || skimming || !settles && !this.deepEnoughToSwim()) {
            if (hasSwim && aquatic && !transitioning && this.shallowEnoughToWalk()) {
               this.getLocomotionOrchestrator().requestTransition(LocomotionTransition.WATER_EXIT, ModeChangeCause.WATER_EXIT);
            }
         } else {
            this.getLocomotionOrchestrator().requestTransition(LocomotionTransition.WATER_ENTER, ModeChangeCause.WATER_ENTER);
         }
      }

      if (!this.level().isClientSide()) {
         if (!this.isVehicle() || !(this.getControllingPassenger() instanceof Player) || !this.canBeControlledBy()) {
            FaunaStateEvaluator.evaluate(this, this.stateContext);
         }

         AttackMotion.publish(this);
      }
   }

   public String lastTravelBranch() {
      return this.lastTravelBranch;
   }

   protected boolean heldStill() {
      return false;
   }

   public void travel(Vec3 pTravelVector) {
      PerfClock.Section section = this.level().isClientSide() ? CLOCK_CLIENT_TRAVEL : CLOCK_TRAVEL;
      long clock = section.start();

      try {
         boolean drives = this.drivesOnSubLevels();
         if (drives) {
            SubLevels.resume(this, this.subLevelUnder);
         } else {
            this.subLevelUnder = null;
            this.subLevelPlace = null;
            SubLevels.forget(this);
         }

         this.vanillaTravelled = false;
         if (this.heldStill()) {
            this.setDeltaMovement(Vec3.ZERO);
            this.setSpeed(0.0F);
         } else {
            this.travelTimed(pTravelVector);
         }

         if (drives) {
            this.finishOnSubLevels();
         }
      } finally {
         section.end(clock);
      }
   }

   private void armThreeD(FaunaFlightRotationComponent dfrc) {
      boolean driven = this.inThreeDMedium() && this.steeredByRider() && !this.isFreeLooking();
      if (!this.level().isClientSide()) {
         if (!driven) {
            dfrc.setRolling(false);
         }
      } else if (this.isControlledByLocalInstance()) {
         dfrc.setRolling(driven && (this.isAquatic() || RideSettings.isBarrelRollEnabled()));
      } else {
         dfrc.setRolling(false);
      }
   }

   private void catchFall() {
      if (!this.onGround()
         && !(this.fallDistance <= 6.0F)
         && this.getTransportMode() != TransportMode.AIRBORNE
         && this.supportsFlying()
         && !this.isClimbing()
         && !this.isInWater()
         && !this.getLocomotionOrchestrator().isTransitionActive()) {
         this.getLocomotionOrchestrator().requestMode(LocomotionMode.FLY, ModeChangeCause.FALL);
         if (this.isFlying()) {
            Vec3 v = this.getDeltaMovement();
            this.setXRot(Mth.clamp((float)Math.toDegrees(Math.atan2(-v.y, v.horizontalDistance())), -90.0F, 90.0F));
            this.xRotO = this.getXRot();
            ThrottleComponent throttle = this.componentRegistry.get(ThrottleComponent.class);
            if (throttle != null) {
               throttle.catchFall(v.length());
            }

            FaunaFlightRotationComponent dfrc = this.componentRegistry.get(FaunaFlightRotationComponent.class);
            if (dfrc != null && this.level().isClientSide()) {
               this.armThreeD(dfrc);
            }
         }
      }
   }

   private void travelTimed(Vec3 pTravelVector) {
      this.lastTravelBranch = "none";
      boolean hopping = this.hopLaunch;
      this.hopLaunch = false;
      if (this.hopAirborne && !hopping && this.onGround()) {
         this.hopAirborne = false;
         this.leaping = false;
         this.express(ExpressionKey.body(ExpressionKey.Body.HOP_END));
      } else if (this.hopAirborne && !hopping && !this.hopFalling && this.getDeltaMovement().y <= 0.0) {
         this.hopFalling = true;
         this.express(ExpressionKey.body(ExpressionKey.Body.HOP_FALL));
      }

      if (this.leaping && (this.isInWater() || this.isInLava())) {
         this.leaping = false;
      }

      if (this.isQteActive()) {
         this.setDeltaMovement(Vec3.ZERO);
         this.setSpeed(0.0F);
      } else if (this.callHold > 0 && this.onGround() && !this.steeredByRider()) {
         this.lastTravelBranch = "call";
         this.setDeltaMovement(Vec3.ZERO);
         this.setSpeed(0.0F);
      } else if (this.isPassenger()) {
         this.setDeltaMovement(Vec3.ZERO);
      } else if (Grip.held(this)) {
         this.setDeltaMovement(Vec3.ZERO);
      } else {
         boolean physicsHere = this.level().isClientSide() ? this.steeredByRider() && this.isControlledByLocalInstance() : !this.steeredByRider();
         if (physicsHere) {
            this.catchFall();
         }

         if (!this.steeredByRider() && this.faunaIntent != null && !this.isNoAi()) {
            this.lastTravelBranch = "FAUNA";
            if (hopping) {
               this.move(MoverType.SELF, this.getDeltaMovement());
            } else if (this.leaping) {
               Vec3 v = this.getDeltaMovement();
               this.setDeltaMovement(v.x, WalkEngine.fall(v.y), v.z);
               this.move(MoverType.SELF, this.getDeltaMovement());
            } else {
               this.driveFromFauna(this.faunaIntent);
            }
         } else if (this.isVehicle() && this.canBeControlledBy() && this.isControlledByLocalInstance()) {
            LivingEntity rider = this.getControllingPassenger();
            if (rider == null) {
               this.vanillaTravel(pTravelVector);
            } else {
               RiderInputComponent riderInput = this.componentRegistry.get(RiderInputComponent.class);
               if (riderInput == null) {
                  this.vanillaTravel(pTravelVector);
               } else if (this.isClimbing()) {
                  this.getLocomotionOrchestrator().tick(this.riderSource.climbIntent(rider), LocomotionOrchestrator.IntentSource.RIDER);
               } else {
                  if (this.isFlying()
                     && this.onGround()
                     && !this.isBoosting()
                     && !this.getLocomotionOrchestrator().isTransitionActive()
                     && !GripComponent.keepsAloft(this)) {
                     this.getLocomotionOrchestrator().requestMode(LocomotionMode.WALK, ModeChangeCause.LANDING);
                  }

                  if (this.isFlying() && !this.canFly() && !this.getLocomotionOrchestrator().isTransitionActive()) {
                     this.getLocomotionOrchestrator().requestMode(LocomotionMode.WALK, ModeChangeCause.GROUNDED);
                  }

                  if (!this.isBoosting()) {
                     this.boostFromFlight = false;
                  } else if (this.isFlying()) {
                     this.boostFromFlight = true;
                  }

                  boolean canSwim = this.profile.hasMode(LocomotionMode.SWIM);
                  boolean aquatic = this.getTransportMode() == TransportMode.AQUATIC;
                  boolean settles = this.settlesOnWater();
                  boolean skimming = this.isFlying() && !settles && Plunge.flies(this);
                  boolean swimming = canSwim && !skimming && (aquatic ? !this.shallowEnoughToWalk() : settles || this.deepEnoughToSwim());
                  if (swimming && !this.getLocomotionOrchestrator().isTransitionActive()) {
                     LocomotionOrchestrator orch = this.getLocomotionOrchestrator();
                     boolean takingOff = this.isBoosting()
                        && !this.boostFromFlight
                        && this.canFly()
                        && !this.isUnderWater()
                        && orch.requestTransition(LocomotionTransition.TAKEOFF, ModeChangeCause.TAKEOFF);
                     if (!takingOff) {
                        if (!aquatic) {
                           orch.requestMode(LocomotionMode.SWIM, ModeChangeCause.SPLASH_DOWN);
                        }

                        orch.tick(this.riderSource.swimIntent(rider), LocomotionOrchestrator.IntentSource.RIDER);
                     }
                  } else if (this.isFlying()) {
                     this.getLocomotionOrchestrator().tick(this.riderSource.flightIntent(rider), LocomotionOrchestrator.IntentSource.RIDER);
                  } else {
                     if (aquatic && this.profile.hasMode(LocomotionMode.WALK) && !this.getLocomotionOrchestrator().isTransitionActive()) {
                        this.getLocomotionOrchestrator().requestMode(LocomotionMode.WALK, ModeChangeCause.WATER_EXIT);
                     }

                     if (this.hasImpulse) {
                        this.hasImpulse = false;
                        this.move(MoverType.SELF, this.getDeltaMovement());
                     } else {
                        SteeringIntent ground = this.riderSource.groundIntent(rider);
                        this.getLocomotionOrchestrator().tick(ground, LocomotionOrchestrator.IntentSource.RIDER);
                        if (!aquatic && !this.getLocomotionOrchestrator().isTransitionActive() && ClimbEngine.reachesFor(this, ground.forward())) {
                           this.getLocomotionOrchestrator().requestMode(LocomotionMode.CLIMB, ModeChangeCause.CLIMB_ON);
                           ClimbEngine climb = this.componentRegistry.get(ClimbEngine.class);
                           if (climb != null && this.isClimbing()) {
                              climb.takeHold(rider);
                           }
                        }
                     }
                  }
               }
            }
         } else {
            float maxSpeed = (float)this.getAttributeValue(Attributes.MOVEMENT_SPEED);
            float multiplier = this.isSprinting() ? 1.0F : 0.2F;
            this.setSpeed(maxSpeed * multiplier);
            this.lastTravelBranch = "VANILLA";
            this.vanillaTravel(pTravelVector);
         }
      }
   }

   public InteractionResult mobInteract(Player pPlayer, InteractionHand pHand) {
      ItemStack itemstack = pPlayer.getItemInHand(pHand);
      if (!this.level().isClientSide()) {
         if (this.isVehicle() && pPlayer.equals(this.getControllingPassenger())) {
            return InteractionResult.PASS;
         }

         for (IFaunaInteractionHandler handler : this.interactionHandlers) {
            InteractionResult result = handler.handle(this, pPlayer, pHand);
            if (result != InteractionResult.PASS) {
               return result;
            }
         }

         return pPlayer.isShiftKeyDown() && itemstack.isEmpty() ? InteractionResult.FAIL : super.mobInteract(pPlayer, pHand);
      } else {
         boolean couldInteract = this.isVehicle() && pPlayer.equals(this.getControllingPassenger()) || itemstack.isEmpty() || this.isTame() || this.isFood(itemstack);
         if (!couldInteract) {
            return InteractionResult.FAIL;
         } else {
            return this.isTame() ? InteractionResult.SUCCESS : InteractionResult.CONSUME;
         }
      }
   }

   public final void activateSecondaryAbility(Player player) {
      this.componentRegistry.get(CombatComponent.class).activateSecondaryAbility(player);
   }

   public boolean canBeMountedBy(Player player) {
      if (!this.supportsRiding()) {
         return false;
      }

      if (this.getGrowthStage() >= this.mountStage() && !(this.getHealth() <= 0.0F)) {
         if (!this.level().isClientSide()) {
            if (this.isTame() && this.isEstranged()) {
               Notices.tell(player, Component.translatable("liberanimalium.taming.estranged", new Object[]{this.getName()}));
               return false;
            }

            if (!this.isTame()) {
               String blocked = this.rodeoBlockedBy(player);
               if (blocked != null) {
                  Notices.tell(player, Component.translatable("liberanimalium.courtship.mount." + blocked));
                  return false;
               }

               return true;
            }

            if (!this.isSaddled()) {
               Notices.tell(player, Component.translatable("liberanimalium.needs_saddle"));
               return false;
            }
         } else {
            if (!this.isTame()) {
               return true;
            }

            if (!this.isSaddled()) {
               return false;
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public boolean isEstranged() {
      if (!this.isTame()) {
         return false;
      }

      FaunaMind mind = this.faunaMind();
      return mind != null && mind.knowledge().estranged();
   }

   public boolean refusesTack() {
      return this.isEstranged();
   }

   private void tickEstrangement() {
      boolean now = this.isEstranged();
      if (now && !this.wasEstranged) {
         this.shedTack();
         this.throwRider();
      }

      this.wasEstranged = now;
   }

   @org.jetbrains.annotations.Nullable
   private String rodeoBlockedBy(Player player) {
      FaunaMind mind = this.faunaMind();
      if (mind == null) {
         return "not_ready";
      } else if (!mind.knowledge().courtship().atMount(player.getUUID(), this.rideable())) {
         return "not_ready";
      } else {
         Representation about = mind.knowledge().about(player.getUUID());
         if (about != null && Bend.isFighting(about.relation().type())) {
            return "fighting";
         } else {
            return this.isSleeping() ? "asleep" : null;
         }
      }
   }

   public int getBond(UUID playerUUID) {
      return this.componentRegistry.get(BondingLedgerComponent.class).getBond(playerUUID);
   }

   public TamingStage stageFor(UUID playerUUID) {
      return this.componentRegistry.get(BondingLedgerComponent.class).stageFor(playerUUID);
   }

   public boolean isFlightUnlockedFor(@org.jetbrains.annotations.Nullable Entity rider) {
      if (this.level().isClientSide()) {
         return true;
      } else {
         return !(rider instanceof Player player) ? true : this.isTame() && player.getUUID().equals(this.getOwnerUUID());
      }
   }

   public void setBond(UUID playerUUID, int bond) {
      this.componentRegistry.get(BondingLedgerComponent.class).setBond(playerUUID, bond);
   }

   public boolean holdsRoost() {
      return ((Optional)this.entityData.get(DATA_ROOST_POS)).isPresent();
   }

   public DespawnRule.Facts despawnFacts() {
      return new DespawnRule.Facts(
         this.holdsRoost(),
         this.isTame(),
         this.isPersistenceRequired(),
         this.isBaby(),
         this.componentRegistry.get(BondingLedgerComponent.class).highestBond() >= BondLevel.TOLERANT.getMinAffection(),
         this.isLeashed(),
         PairSession.involves(this.faunaId()),
         this.tickCount
      );
   }

   public boolean removeWhenFarAway(double distanceToClosestPlayer) {
      return DespawnRule.mayDespawn(this.despawnFacts());
   }

   public void checkDespawn() {
      if (!Services.despawnEventAnswered(this)) {
         if (this.level().getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful()) {
            this.discard();
         } else if (!this.isPersistenceRequired() && !this.requiresCustomPersistence()) {
            Player nearest = this.serverLevel().getNearestPlayer(this.getX(), this.getY(), this.getZ(), -1.0, (Predicate)null);
            if (nearest != null) {
               SpawnSettings.Despawn despawn = SpawnSettings.of(SpeciesAssets.hostOf(this)).despawn();
               double distance = nearest.distanceTo(this);
               this.unwatchedTicks = DespawnRule.unwatched(this.unwatchedTicks, distance, despawn);
               if (DespawnRule.timeUp(this.unwatchedTicks, this.random.nextDouble(), despawn) && this.removeWhenFarAway(distance * distance)) {
                  this.discard();
               }
            }
         } else {
            this.unwatchedTicks = 0;
         }
      }
   }

   public int awardGift(LivingEntity giver, ItemStack stack, Delivery delivery) {
      return this.componentRegistry.get(BondingLedgerComponent.class).awardGift(giver, stack, delivery);
   }

   public int awardGift(UUID giverId, Delivery delivery) {
      BondingLedgerComponent ledger = this.componentRegistry.get(BondingLedgerComponent.class);
      return ledger == null ? 0 : ledger.awardGift(giverId, delivery);
   }

   public ItemStack getCarriedItem() {
      return (ItemStack)this.entityData.get(DATA_CARRIED_ITEM);
   }

   public void setCarriedItem(ItemStack stack) {
      this.entityData.set(DATA_CARRIED_ITEM, stack == null ? ItemStack.EMPTY : stack);
   }

   public FaunaWingTrailManager getWingTrailManager() {
      if (this.wingTrailManager == null) {
         this.wingTrailManager = new FaunaWingTrailManager(this);
      }

      return this.wingTrailManager;
   }

   public void addBond(UUID playerUUID, int amount) {
      this.componentRegistry.get(BondingLedgerComponent.class).addBond(playerUUID, amount);
   }

   public void tame(Player pPlayer) {
      super.tame(pPlayer);
      this.navigation.stop();
      this.level().broadcastEntityEvent(this, (byte)7);
   }

   protected BodyRotationControl createBodyControl() {
      return new FaunaBodyControl(this);
   }

   protected PathNavigation createNavigation(Level pLevel) {
      GroundPathNavigation nav = new GroundPathNavigation(this, pLevel);
      nav.setCanOpenDoors(true);
      nav.setCanFloat(true);
      nav.setCanPassDoors(true);
      return nav;
   }

   public LocomotionOrchestrator getLocomotionOrchestrator() {
      return this.componentRegistry.get(LocomotionOrchestrator.class);
   }

   public boolean canFly() {
      return this.isModeAvailable(LocomotionMode.FLY);
   }

   public boolean isModeAvailable(LocomotionMode mode) {
      if (!this.profile.hasMode(mode)) {
         return false;
      }

      return switch (mode) {
         case FLY -> this.getGrowthStage() >= this.flightStage()
            && !this.getSyncedIsIncapacitated()
            && !this.getSyncedIsFeatureCrippled(CrippledFeature.FLYING)
            && !this.asleep();
         case SWIM, DIVE -> !this.getSyncedIsIncapacitated();
         case CLIMB -> !this.getSyncedIsIncapacitated() && !this.asleep() && PhysicsSheet.loaded() && PhysicsSheet.of(this).climb() != null;
         case WALK -> true;
      };
   }

   public float getBodyFacing() {
      return this.bodyFacing;
   }

   public void setBodyFacing(float yaw) {
      this.bodyFacing = yaw;
   }

   public boolean claimBodyFacingTick() {
      if (this.bodyFacingTick == this.tickCount) {
         return false;
      }

      this.bodyFacingTick = this.tickCount;
      return true;
   }

   private static double massProxy(Entity e) {
      return e.getBbWidth() * e.getBbWidth() * e.getBbHeight();
   }

   private double massDamping() {
      double mass = massProxy(this);
      return mass <= 0.0 ? 1.0 : Mth.clamp(0.648 / mass, 0.0, 1.0);
   }

   public void knockback(double pStrength, double pX, double pZ) {
      super.knockback(pStrength * this.massDamping(), pX, pZ);
   }

   protected void doPush(Entity pEntity) {
      this.shareOfPush(pEntity, () -> super.doPush(pEntity));
   }

   public void push(Entity pEntity) {
      this.shareOfPush(pEntity, () -> super.push(pEntity));
   }

   private void shareOfPush(Entity other, Runnable exchange) {
      double total = massProxy(this) + massProxy(other);
      if (total <= 0.0) {
         exchange.run();
      } else {
         double share = massProxy(other) / total;
         Vec3 velBefore = this.getDeltaMovement();
         exchange.run();
         Vec3 received = this.getDeltaMovement().subtract(velBefore);
         this.setDeltaMovement(velBefore.add(received.scale(share)));
      }
   }

   public boolean hurt(DamageSource pSource, float pAmount) {
      IncapacitationComponent incap = this.componentRegistry.get(IncapacitationComponent.class);
      if (incap != null && incap.shouldBlockDamage(pSource)) {
         return false;
      }

      if (pSource.is(DamageTypeTags.IS_FIRE)) {
         return false;
      }

      this.express(ExpressionKey.body(ExpressionKey.Body.HURT));
      UUID robbed = this.loot();
      if (robbed != null && pSource.getEntity() != null && robbed.equals(pSource.getEntity().getUUID())) {
         this.dropLoot();
      }

      this.setSleepDisturbTicks(500);
      this.setAbilityCooldownTicks(100);
      this.getLocomotionOrchestrator().interruptTransition(ModeChangeCause.DAMAGE);
      if (this.isFlying()) {
         this.getLocomotionOrchestrator().requestMode(LocomotionMode.FLY, ModeChangeCause.DAMAGE);
      }

      this.componentRegistry.get(CommandComponent.class).wakeFromRest();
      return StuckArrows.struck(this, pSource, super.hurt(pSource, pAmount));
   }

   public boolean hurtThrough(FaunaPartEntity part, DamageSource source, float amount) {
      return this.hurt(source, amount);
   }

   public boolean isInvulnerableTo(DamageSource pSource) {
      return super.isInvulnerableTo(pSource);
   }

   public boolean isFriendlyTarget(Entity target) {
      if (target == null) {
         return false;
      }

      UUID ownerUUID = this.getOwnerUUID();
      if (ownerUUID == null) {
         return false;
      }

      if (this.faunaMind != null) {
         Representation known = this.faunaMind.knowledge().of(target.getUUID());
         if (known != null && Bend.isFighting(known.relation().type())) {
            return false;
         }
      }

      if (target instanceof Player player && player.getUUID().equals(ownerUUID)) {
         return true;
      } else {
         if (this.getPassengers().contains(target)) {
            return true;
         }

         if (target instanceof FaunaBase otherBody && otherBody.isTame()) {
            UUID otherOwnerUUID = otherBody.getOwnerUUID();
            if (otherOwnerUUID != null && otherOwnerUUID.equals(ownerUUID)) {
               return true;
            }
         }

         return false;
      }
   }

   public static boolean isFriendlyFire(Entity shooter, Entity target) {
      return shooter instanceof FaunaBase body && body.isTame() ? body.isFriendlyTarget(target) : false;
   }

   public void setTarget(@org.jetbrains.annotations.Nullable LivingEntity pTarget) {
      if (!(this.isTame() && pTarget instanceof TamableAnimal tamedAnimal) || !tamedAnimal.isTame()) {
         super.setTarget(pTarget);
      }
   }

   @org.jetbrains.annotations.Nullable
   public LivingEntity engagedTarget() {
      if (this.faunaMind == null) {
         return null;
      }

      if (super.level() instanceof ServerLevel serverLevel) {
         UUID engaged = this.faunaMind.engagedId();
         if (engaged == null) {
            return null;
         } else {
            return serverLevel.getEntity(engaged) instanceof LivingEntity living && living.isAlive() ? living : null;
         }
      } else {
         return null;
      }
   }

   @org.jetbrains.annotations.Nullable
   public LivingEntity weaponTarget() {
      IMeleeAttackHandler melee = this.componentRegistry.get(IMeleeAttackHandler.class);
      LivingEntity committed = melee == null ? null : melee.committedVictim();
      return committed != null ? committed : this.engagedTarget();
   }

   public boolean doHurtTarget(Entity pEntity) {
      return this.componentRegistry.get(CombatComponent.class).doHurtTarget(pEntity);
   }

   public boolean killedEntity(ServerLevel pLevel, LivingEntity pEntity) {
      return this.componentRegistry.get(CombatComponent.class).killedEntity(pLevel, pEntity);
   }

   public void addAdditionalSaveData(CompoundTag compound) {
      super.addAdditionalSaveData(compound);
      this.saveMind(compound);
      compound.putInt("FaunaDataVersion", 1);
      compound.putInt("EntityArtVersion", (Integer)this.entityData.get(DATA_ART_VERSION));
      this.componentRegistry.saveAll(compound);
      if (this.variantId != null) {
         compound.putString("FaunaVariant", this.variantId.toString());
      }

      this.staminaSystem.save(compound);
      compound.putInt("TransportMode", (Integer)this.entityData.get(DATA_TRANSPORT_MODE));
      if (this.supportsThreeDMedium()) {
         compound.putBoolean("IsGoingDown", this.isBraking());
         compound.putBoolean("IsFreeLooking", this.isFreeLooking());
         compound.putFloat("ThrottleLevel", this.getThrottleLevel());
         RollComponent roll = this.componentRegistry.get(RollComponent.class);
         if (roll != null) {
            roll.save(compound);
         }
      }
   }

   public void readAdditionalSaveData(CompoundTag compound) {
      super.readAdditionalSaveData(compound);
      this.variantId = compound.contains("FaunaVariant") ? ResourceLocation.tryParse(compound.getString("FaunaVariant")) : null;
      if (this.variantId != null) {
         this.entityData.set(DATA_VARIANT_ID, this.variantId.toString());
         CreatureVariant variant = CreatureVariants.get(this.variantId);
         if (variant != null) {
            this.entityData.set(DATA_VARIANT_SKIN, variant.skin());
            if (variant.profile() != null) {
               this.faunaMind = new FaunaMind(this);
            }
         }
      }

      this.loadMind(compound);
      this.componentRegistry.loadAll(compound);
      int dataVersion = compound.getInt(SaveKeys.of(compound, "FaunaDataVersion", "DragonDataVersion"));
      if (dataVersion < 1) {
         this.migrateLegacyData();
      } else {
         int savedArtVersion = compound.getInt("EntityArtVersion");
         this.entityData.set(DATA_ART_VERSION, savedArtVersion);
         this.checkAndMigrateArtVersion();
      }

      if (compound.contains("TransportMode")) {
         this.entityData.set(DATA_TRANSPORT_MODE, compound.getInt("TransportMode"));
      }

      if (this.supportsThreeDMedium()) {
         this.setBraking(compound.getBoolean("IsGoingDown"));
         this.setFreeLooking(compound.getBoolean("IsFreeLooking"));
         this.setThrottleLevel(compound.getFloat("ThrottleLevel"));
         RollComponent roll = this.componentRegistry.get(RollComponent.class);
         if (roll != null) {
            roll.load(compound);
         }
      }

      this.refreshDimensions();
      this.staminaSystem.load(compound);
   }

   private void migrateLegacyData() {
      String bodyType = this.speciesId();

      try {
         SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(bodyType);
         GeneticsSpawner spawner = new GeneticsSpawner(this.random);
         GeneticsSpawner.SpawnResult result = spawner.generateWildGenotype(config, bodyType);
         this.setGenotypeAndDraws(result.genotype, result.paletteDraws);
         LOGGER.debug("[MIGRATION] Successfully updated older body genetics for {}", bodyType);
      } catch (Exception e) {
         LOGGER.error("[MIGRATION ERROR] Failed to regenerate legacy genetics for {}: {}", bodyType, e.getMessage());
      }

      this.entityData.set(DATA_TRANSPORT_MODE, this.restingMode().toTransportMode().ordinal());
      this.componentRegistry.get(CommandComponent.class).setCommandForced(Stance.FREE, Modifier.GROUND);
   }

   private void checkAndMigrateArtVersion() {
      if (this.hasGenotype() && !this.level().isClientSide()) {
         String bodyType = this.speciesId();
         SpeciesGeneticsConfig config = SpeciesGeneticsConfigRegistry.getConfig(bodyType);
         int currentConfigVersion = config.getArtVersion();
         int savedArtVersion = (Integer)this.entityData.get(DATA_ART_VERSION);
         boolean lostArt = this.carriesLostArt(bodyType, config);
         if (savedArtVersion < currentConfigVersion || lostArt) {
            try {
               GeneticsSpawner spawner = new GeneticsSpawner(this.random);
               GeneticsSpawner.SpawnResult result = spawner.generateWildGenotype(config, bodyType);
               this.setGenotypeAndDraws(result.genotype, result.paletteDraws);
               this.recalculateStats();
               if (lostArt) {
                  LOGGER.debug("[GENETICS] Regenerated genetics for {}: a colour or marking it carried is in no loaded pack", bodyType);
               } else {
                  LOGGER.debug("[ART MIGRATION] Regenerated genetics for {} from v{} to v{}", new Object[]{bodyType, savedArtVersion, currentConfigVersion});
               }
            } catch (Exception e) {
               LOGGER.warn("[ART MIGRATION] Failed to regenerate genetics for {}: {}", bodyType, e.getMessage());
            }
         }
      }
   }

   private boolean carriesLostArt(String bodyType, SpeciesGeneticsConfig config) {
      PaletteDraws draws = this.getPaletteDraws();
      if (draws != null && SpeciesGeneticsConfigRegistry.has(bodyType) && PalettePoolRegistry.hasPools(bodyType)) {
         draws.retainExpressed(config);
         return draws.hasLostArt();
      } else {
         return false;
      }
   }

   public void onSyncedDataUpdated(EntityDataAccessor<?> pKey) {
      super.onSyncedDataUpdated(pKey);
      if (this.level().isClientSide() && this.stateContext != null) {
         if (DATA_TRANSPORT_MODE.equals(pKey)) {
            TransportMode newMode = TransportMode.values()[this.entityData.get(DATA_TRANSPORT_MODE)];
            this.stateContext.setTransportMode(newMode);
         } else if (DATA_IS_DIVE_BREAKING.equals(pKey)) {
            this.stateContext.setDiveBreaking((Boolean)this.entityData.get(DATA_IS_DIVE_BREAKING));
         }
      }

      if (DATA_GROWTH_STAGE.equals(pKey) || DATA_VARIANT_ID.equals(pKey)) {
         this.refreshDimensions();
         this.recalculateStats();
      }

      if (DATA_GRIP_LOAD.equals(pKey) && this.level().isClientSide() && this.componentRegistry != null) {
         this.recalculateStats();
      }

      if (DATA_DODGE_ROLL.equals(pKey) && this.level().isClientSide() && this.componentRegistry != null && !this.isControlledByLocalInstance()) {
         float gain = (Float)this.entityData.get(DATA_DODGE_ROLL);
         RollComponent rolls = this.componentRegistry.get(RollComponent.class);
         if (gain != 0.0F && rolls != null) {
            rolls.watchDodge(gain > 0.0F ? 1 : -1, Math.abs(gain));
         }
      }

      if (DATA_GENOTYPE.equals(pKey)) {
         CompoundTag tag = (CompoundTag)this.entityData.get(DATA_GENOTYPE);
         this.componentRegistry.get(GenotypeComponent.class).handleGenotypeSync(tag);
      }

      if (DATA_IS_ROARING.equals(pKey) && this.level().isClientSide && this.isRoaring() && Services.platform().isClient()) {
         SoundClientHelper.handleRoarCameraShake(this);
      }
   }

   public void handleEntityEvent(byte pId) {
      if (pId == 18 || pId == 7) {
         this.showMood(ParticleTypes.HEART);
      } else if (pId == 6) {
         this.showMood(ParticleTypes.SMOKE);
      } else {
         super.handleEntityEvent(pId);
      }
   }

   public Vec3 moodPoint() {
      List<FaunaPartEntity> heads = this.headParts();
      AABB box = heads.isEmpty() ? this.getBoundingBox() : heads.get(0).getBoundingBox();
      return new Vec3((box.minX + box.maxX) * 0.5, box.maxY + 0.35, (box.minZ + box.maxZ) * 0.5);
   }

   public static double moodSpread() {
      return 0.3;
   }

   private void showMood(ParticleOptions particle) {
      Vec3 at = this.moodPoint();

      for (int i = 0; i < 7; i++) {
         this.level()
            .addParticle(
               particle,
               at.x + this.random.nextGaussian() * 0.3,
               at.y + this.random.nextDouble() * 0.3,
               at.z + this.random.nextGaussian() * 0.3,
               this.random.nextGaussian() * 0.02,
               this.random.nextGaussian() * 0.02,
               this.random.nextGaussian() * 0.02
            );
      }
   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      this.express(ExpressionKey.body(ExpressionKey.Body.FALL_IMPACT));
      return this.calculateFallDamage(pFallDistance, pMultiplier) <= 0 ? false : super.causeFallDamage(pFallDistance, pMultiplier, pSource);
   }

   protected int calculateFallDamage(float pFallDistance, float pDamageMultiplier) {
      return this.supportsFlying() ? 0 : super.calculateFallDamage(pFallDistance - 5.0F, pDamageMultiplier);
   }

   public void die(DamageSource pDamageSource) {
      FaunaSoundHelper.playSound(this, SoundEventKey.DEATH);
      BlockPos roost = this.getRoostPosition();
      if (roost != null && this.level() instanceof ServerLevel server && server.isLoaded(roost) && server.getBlockEntity(roost) instanceof RoostBlockEntity home) {
         home.release(this.faunaId());
      }

      GripComponent grip = this.grip();
      if (grip != null && !this.level().isClientSide()) {
         grip.release(GripComponent.Release.DROP);
      }

      super.die(pDamageSource);
   }

   public void remove(RemovalReason pReason) {
      if (!this.level().isClientSide() && this.hasFaunaPose(PoseFlag.LOAFING)) {
         LOAFERS.decrementAndGet();
      }

      GripComponent grip = this.grip();
      if (grip != null) {
         grip.bodyRemoved(pReason.shouldDestroy());
      }

      super.remove(pReason);
   }

   @org.jetbrains.annotations.Nullable
   public GripComponent grip() {
      return this.componentRegistry == null ? null : this.componentRegistry.get(GripComponent.class);
   }

   public static boolean anyLoafing() {
      return LOAFERS.get() > 0;
   }

   public FaunaNeedsSystem getNeedsSystem() {
      return this.componentRegistry.get(FaunaNeedsSystem.class);
   }

   public FaunaStaminaSystem getStaminaSystem() {
      return this.staminaSystem;
   }

   public boolean isIncapacitated() {
      IncapacitationComponent ic = this.componentRegistry.get(IncapacitationComponent.class);
      return ic != null && ic.isIncapacitated();
   }

   public boolean canBeSeenAsEnemy() {
      return !BodyStance.incapacitated(this) && super.canBeSeenAsEnemy();
   }

   public boolean isLowHealth() {
      return this.getHealth() < this.getMaxHealth() * 0.4F;
   }

   @org.jetbrains.annotations.Nullable
   public Vec3 trackedMouth() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart == null ? null : multipart.trackedPoint("mouth");
   }

   public BoneAccessCache getBoneAccessCache() {
      return this.boneAccessCache;
   }

   public VoiceGate voiceGate() {
      return this.voiceGate;
   }

   @org.jetbrains.annotations.Nullable
   public ResourceLocation variantId() {
      if (!this.level().isClientSide()) {
         return this.variantId;
      }

      String synced = (String)this.entityData.get(DATA_VARIANT_ID);
      return synced.isEmpty() ? null : ResourceLocation.tryParse(synced);
   }

   @org.jetbrains.annotations.Nullable
   public SpeciesGameplayProfile gameplay() {
      ResourceLocation id = this.variantId();
      CreatureVariant variant = id == null ? null : CreatureVariants.get(id);
      return variant != null ? VariantGameplay.of(variant, this.speciesId()) : SpeciesGameplayRegistry.getProfile(this.speciesId());
   }

   public float bodyScale() {
      SpeciesGameplayProfile gameplay = this.gameplay();
      return gameplay == null ? 1.0F : gameplay.scale();
   }

   public String variantSkin() {
      return (String)this.entityData.get(DATA_VARIANT_SKIN);
   }

   public void applyVariant(CreatureVariant variant) {
      this.variantId = variant.id();
      this.entityData.set(DATA_VARIANT_ID, variant.id().toString());
      if (!variant.name().isEmpty()) {
         this.setCustomName(Component.translatableWithFallback(variant.name(), variant.name()));
      }

      this.entityData.set(DATA_VARIANT_SKIN, variant.skin());
      if (!variant.genetics().isNone()) {
         this.presetSpawnGenetics(variant.genetics());
      }

      if (variant.profile() != null) {
         this.faunaMind = new FaunaMind(this);
      }

      this.refreshDimensions();
      this.recalculateStats();
   }

   public Genotype getGenotype() {
      return this.componentRegistry.get(GenotypeComponent.class).getGenotype();
   }

   public boolean hasGenotype() {
      return this.componentRegistry.get(GenotypeComponent.class).hasGenotype();
   }

   public PaletteDraws getPaletteDraws() {
      return this.componentRegistry.get(GenotypeComponent.class).getPaletteDraws();
   }

   public void setGenotypeAndDraws(Genotype genotype, PaletteDraws paletteDraws) {
      this.componentRegistry.get(GenotypeComponent.class).setGenotypeAndDraws(genotype, paletteDraws);
      this.entityData.set(DATA_ART_VERSION, SpeciesGeneticsConfigRegistry.getConfig(this.speciesId()).getArtVersion());
   }

   public int getGrowthStage() {
      return this.componentRegistry.get(GrowthComponent.class).getGrowthStage();
   }

   public void setGrowthStage(int stage) {
      this.componentRegistry.get(GrowthComponent.class).setGrowthStage(stage);
   }

   public int growthTicksPerStage() {
      return 36000;
   }

   public int flightStage() {
      return 1;
   }

   public int mountStage() {
      return 2;
   }

   protected int spawnStage() {
      return 2;
   }

   public EntityDimensions dimensionsAtStage(int stage) {
      float w = this.getType().getWidth();
      float h = this.getType().getHeight();

      return switch (stage) {
         case 0 -> EntityDimensions.scalable(w * 0.5F, h * 0.3F);
         case 1 -> EntityDimensions.scalable(w * 0.7F, h * 0.5F);
         case 2 -> EntityDimensions.scalable(w, h);
         case 3 -> EntityDimensions.scalable(w * 1.25F, h * 1.25F);
         default -> this.getType().getDimensions().scale(1.0F);
      };
   }

   public int getGrowthProgress() {
      return this.componentRegistry.get(GrowthComponent.class).getGrowthProgress();
   }

   public void setGrowthProgress(int progress) {
      this.componentRegistry.get(GrowthComponent.class).setGrowthProgress(progress);
   }

   public void setTame(boolean tamed) {
      super.setTame(tamed);
   }

   public int getSleepDisturbTicks() {
      return (Integer)this.entityData.get(DATA_SLEEP_DISTURB_TICKS);
   }

   public TransportMode getTransportMode() {
      return TransportMode.values()[this.entityData.get(DATA_TRANSPORT_MODE)];
   }

   public void setTransportMode(TransportMode mode) {
      if (mode == TransportMode.AIRBORNE && !this.canFly()) {
         mode = TransportMode.GROUNDED;
      }

      if (!this.profile.hasMode(LocomotionMode.fromTransportMode(mode))) {
         mode = this.restingMode().toTransportMode();
      }

      TransportMode oldMode = TransportMode.fromOrdinal((Integer)this.entityData.get(DATA_TRANSPORT_MODE));
      if (oldMode != mode || this.navigation == null) {
         this.entityData.set(DATA_TRANSPORT_MODE, mode.ordinal());
         this.stateContext.setTransportMode(mode);
         if (!this.level().isClientSide()) {
            this.setNoGravity(mode == TransportMode.AIRBORNE);
         }

         this.hasImpulse = false;
         this.stateTransitionCooldown = 10;
      }
   }

   public boolean deepEnoughToSwim() {
      if (!PhysicsSheet.loaded()) {
         return this.isInWater();
      }

      PhysicsSheet sheet = PhysicsSheet.of(this);
      float floatLine = sheet.weight(this.getWeightClass()).floatLine();
      return WaterDepth.share(this) >= sheet.water().enterShare() * floatLine;
   }

   public boolean skimsWater() {
      if (!this.isFlying()) {
         return false;
      }

      float share = PhysicsSheet.loaded() ? PhysicsSheet.of(this).water().skimSpeed() : 0.5F;
      Vec3 v = this.getDeltaMovement();
      return v.length() >= share * this.getAttributeValue(Attributes.FLYING_SPEED);
   }

   public boolean settlesOnWater() {
      if (this.isFlying() && this.isInWater() && this.profile.hasMode(LocomotionMode.SWIM) && !this.skimsWater()) {
         double height = Math.max(0.1, this.getBbHeight());
         double column = WaterDepth.depth(this) + WaterDepth.below(this, (int)Math.ceil(height) + 1);
         if (!PhysicsSheet.loaded()) {
            return column >= height * 0.5;
         }

         PhysicsSheet sheet = PhysicsSheet.of(this);
         return column >= sheet.water().enterShare() * sheet.weight(this.getWeightClass()).floatLine() * height;
      } else {
         return false;
      }
   }

   public boolean shallowEnoughToWalk() {
      if (!this.profile.hasMode(LocomotionMode.WALK) || !this.onGround()) {
         return false;
      }

      if (!PhysicsSheet.loaded()) {
         return !this.isInWater();
      }

      PhysicsSheet sheet = PhysicsSheet.of(this);
      float floatLine = sheet.weight(this.getWeightClass()).floatLine();
      return WaterDepth.share(this) < sheet.water().exitShare() * floatLine;
   }

   public int getMaxAirSupply() {
      if (this.profile == null) {
         return super.getMaxAirSupply();
      } else {
         SpeciesGameplayProfile own = this.gameplay();
         if (own != null && own.breathTicks() > 0) {
            return own.breathTicks();
         } else {
            return PhysicsSheet.loaded() ? PhysicsSheet.of(this).water().breathTicks() : super.getMaxAirSupply();
         }
      }
   }

   public LocomotionMode restingMode() {
      if (this.profile.hasMode(LocomotionMode.WALK)) {
         return LocomotionMode.WALK;
      } else {
         return this.profile.hasMode(LocomotionMode.SWIM) ? LocomotionMode.SWIM : LocomotionMode.FLY;
      }
   }

   public int getLayerTransportModeOrdinal() {
      return (Integer)this.entityData.get(DATA_TRANSPORT_MODE);
   }

   public void setLayerTransportModeOrdinal(int ordinal) {
      this.entityData.set(DATA_TRANSPORT_MODE, ordinal);
   }

   public int getLayerAttackOrdinal() {
      return (Integer)this.entityData.get(DATA_ATTACK_MOTION);
   }

   public int getSyncedTemperamentPacked() {
      return (Integer)this.entityData.get(DATA_TEMPERAMENT);
   }

   public void setSyncedTemperamentPacked(int packed) {
      this.entityData.set(DATA_TEMPERAMENT, packed);
   }

   public int getSyncedAnimSignalsPacked() {
      return (Integer)this.entityData.get(DATA_ANIM_SIGNALS);
   }

   public void setSyncedAnimSignalsPacked(int packed) {
      this.entityData.set(DATA_ANIM_SIGNALS, packed);
   }

   public int getSyncedSocialSignalsPacked() {
      return (Integer)this.entityData.get(DATA_SOCIAL_SIGNALS);
   }

   public void setSyncedSocialSignalsPacked(int packed) {
      this.entityData.set(DATA_SOCIAL_SIGNALS, packed);
   }

   public void setLayerAttackOrdinal(int ordinal) {
      this.entityData.set(DATA_ATTACK_MOTION, ordinal);
   }

   public void setSleepDisturbTicks(int ticks) {
      this.entityData.set(DATA_SLEEP_DISTURB_TICKS, ticks);
   }

   public int getAbilityCooldownTicks() {
      return (Integer)this.entityData.get(DATA_ABILITY_COOLDOWN_TICKS);
   }

   public void setAbilityCooldownTicks(int ticks) {
      this.entityData.set(DATA_ABILITY_COOLDOWN_TICKS, ticks);
   }

   public FaunaAbility getSecondaryAbility() {
      return this.componentRegistry.get(CombatComponent.class).getSecondaryAbility();
   }

   public void setSecondaryAbility(FaunaAbility ability) {
      this.componentRegistry.get(CombatComponent.class).setSecondaryAbility(ability);
   }

   public boolean isFlying() {
      return this.getTransportMode() == TransportMode.AIRBORNE;
   }

   public boolean isAquatic() {
      return this.getTransportMode() == TransportMode.AQUATIC;
   }

   public boolean isClimbing() {
      return this.getTransportMode() == TransportMode.CLIMBING;
   }

   protected void positionRider(Entity passenger, MoveFunction callback) {
      if (!this.hasPassenger(passenger)) {
         super.positionRider(passenger, callback);
      } else if (this.isClimbing()) {
         Vec3 centre = this.getBoundingBox().getCenter();
         callback.accept(passenger, centre.x, centre.y - passenger.getBbHeight() * 0.5, centre.z);
      } else {
         Vec3 seat = Entities.seat(this, passenger);
         callback.accept(passenger, seat.x, seat.y, seat.z);
      }
   }

   public boolean inThreeDMedium() {
      return this.isFlying() || this.isAquatic();
   }

   public boolean supportsThreeDMedium() {
      return this.supportsFlying() || this.profile.hasMode(LocomotionMode.SWIM);
   }

   public PhysicsSheet.Flight flightNumbers() {
      return PhysicsSheet.of(this).flightIn(this.isAquatic());
   }

   public float diveCap() {
      float plan = this.getStatSheet().diveCap();
      return plan > 0.0F && !this.isAquatic() ? plan : this.flightNumbers().diveOverdriveCap();
   }

   public PhysicsSheet.Attitude attitudeNumbers() {
      return PhysicsSheet.of(this).attitudeIn(this.isAquatic());
   }

   public PhysicsSheet.Throttle throttleNumbers() {
      return PhysicsSheet.of(this).throttleIn(this.isAquatic());
   }

   public boolean isBoosting() {
      return (Boolean)this.entityData.get(DATA_IS_BOOSTING);
   }

   public void setBoosting(boolean boosting) {
      this.entityData.set(DATA_IS_BOOSTING, boosting);
   }

   public boolean isBraking() {
      return (Boolean)this.entityData.get(DATA_IS_BRAKING);
   }

   public void setBraking(boolean v) {
      this.entityData.set(DATA_IS_BRAKING, v);
   }

   public boolean isFreeLooking() {
      return (Boolean)this.entityData.get(DATA_IS_FREE_LOOKING);
   }

   public void setFreeLooking(boolean v) {
      this.entityData.set(DATA_IS_FREE_LOOKING, v);
   }

   public float getThrottleLevel() {
      return (Float)this.entityData.get(DATA_THROTTLE_LEVEL);
   }

   public void setThrottleLevel(float v) {
      this.entityData.set(DATA_THROTTLE_LEVEL, Quantum.step(Mth.clamp(v, 0.0F, 2.0F), 0.005F));
   }

   public boolean isAccelerating() {
      return (Boolean)this.entityData.get(DATA_IS_ACCELERATING);
   }

   public void setAccelerating(boolean v) {
      this.entityData.set(DATA_IS_ACCELERATING, v);
   }

   public boolean isDecelerating() {
      return (Boolean)this.entityData.get(DATA_IS_DECELERATING);
   }

   public void setDecelerating(boolean v) {
      this.entityData.set(DATA_IS_DECELERATING, v);
   }

   public float getYawSpeed() {
      return (Float)this.entityData.get(DATA_YAW_SPEED);
   }

   public void setYawSpeed(float v) {
      this.entityData.set(DATA_YAW_SPEED, Quantum.step(v, 0.1F));
   }

   public float getRollInput() {
      return (Float)this.entityData.get(DATA_ROLL_INPUT);
   }

   public void setRollInput(float v) {
      this.entityData.set(DATA_ROLL_INPUT, Quantum.step(v, 0.005F));
   }

   public float getSyncedPitch() {
      return (Float)this.entityData.get(DATA_SYNCED_PITCH);
   }

   public float getSyncedRoll() {
      return (Float)this.entityData.get(DATA_SYNCED_ROLL);
   }

   public void setDodgeRoll(float signedGain) {
      this.entityData.set(DATA_DODGE_ROLL, signedGain);
   }

   public int getFlightModeTransitionTimer() {
      return this.flightModeTransitionTimer;
   }

   public float getCapturedTransitionRoll() {
      return this.capturedTransitionRoll;
   }

   public static float getTransitionDuration() {
      return 15.0F;
   }

   public float getAccumulatedRoll() {
      RollComponent roll = this.componentRegistry.get(RollComponent.class);
      return roll != null ? roll.getAccumulatedRoll() : 0.0F;
   }

   public boolean isActivelyRolling() {
      RollComponent roll = this.componentRegistry.get(RollComponent.class);
      return roll != null && roll.isActivelyRolling();
   }

   public void executeTakeoff(float power) {
      if (this.canFly()) {
         LivingEntity flightRider = this.getControllingPassenger();
         if (!this.isFlightUnlockedFor(flightRider)) {
            if (flightRider instanceof Player player) {
               Notices.tell(player, Component.translatable("liberanimalium.flight.not_bonded", new Object[]{this.getDisplayName()}));
            }
         } else if (this.level().isClientSide || this.getStaminaSystem().tryConsume(8.0F)) {
            if (this.getLocomotionOrchestrator().requestTransition(LocomotionTransition.TAKEOFF, ModeChangeCause.TAKEOFF)) {
               float initialThrottle = Math.max(0.2F, power);
               this.setThrottleLevel(initialThrottle);
               this.setXRot(-45.0F);
            }
         }
      }
   }

   public boolean isSprinting() {
      return (Boolean)this.entityData.get(DATA_IS_SPRINTING);
   }

   public void setSprinting(boolean sprinting) {
      this.entityData.set(DATA_IS_SPRINTING, sprinting);
   }

   public boolean isRoaring() {
      return (Boolean)this.entityData.get(DATA_IS_ROARING);
   }

   public void setRoaring(boolean roaring) {
      this.entityData.set(DATA_IS_ROARING, roaring);
   }

   public void startEating(ItemEntity foodItem) {
   }

   public void startEatingCarried() {
   }

   public boolean isEating() {
      return false;
   }

   public void pulseRefusal() {
      this.refusalPulseUntilTick = this.level().getGameTime() + 20L;
   }

   public boolean isRefusalPulse() {
      return this.level().getGameTime() < this.refusalPulseUntilTick;
   }

   public boolean isOrderedToSit() {
      return !this.componentRegistry.get(CommandComponent.class).isOrderedToSit() ? false : this.faunaMind == null || this.faunaMind.commandHolds();
   }

   protected void dropEquipment() {
      super.dropEquipment();
      this.componentRegistry.get(CarryComponent.class).dropHere();
   }

   @Override
   public <T> Optional<T> endowed(Class<T> endowment) {
      if ((endowment != Halter.class || this.profile.tameable)
         && (endowment != Arms.class || this.profile.armed)
         && (endowment != Gullet.class || this.profile.eats)) {
         return endowment == Arms.class && this.steeredByRider() ? Optional.of(endowment.cast(this.riddenArms)) : AnimalHandle.super.endowed(endowment);
      } else {
         return Optional.empty();
      }
   }

   public boolean isBaby() {
      return this.componentRegistry.get(GrowthComponent.class).isBaby();
   }

   @org.jetbrains.annotations.Nullable
   @Override
   public UUID unwelcomeRider() {
      Player aboard = this.ridingPlayer();
      if (aboard == null) {
         return null;
      } else if (this.isEstranged()) {
         return aboard.getUUID();
      } else {
         return this.isOwnedBy(aboard) ? null : aboard.getUUID();
      }
   }

   @Override
   public boolean struggling() {
      return this.isQteActive();
   }

   protected QteMinigame mountStruggle(float fierceness) {
      return new TameCircleQte(QteTuning.rodeo(fierceness, WorldRules.isQteTapAssistEnabled()));
   }

   private void beginRodeoStruggle() {
      if (!this.level().isClientSide()) {
         FaunaQteComponent host = this.componentRegistry.get(FaunaQteComponent.class);
         FaunaMind mind = this.faunaMind();
         UUID rider = this.unwelcomeRider();
         if (host != null && mind != null && rider != null && !host.isActive()) {
            float fierceness = 0.5F * (mind.temper().get(Trait.AGGRESSION) + mind.temper().get(Trait.DOMINANCE));
            host.begin(this.mountStruggle(fierceness), (outcome, score) -> {
               this.setPose(PoseFlag.BUCKING, false);
               if (outcome == QteOutcome.WON) {
                  FaunaInteractionEvents.post(FaunaInteractionEvents.Type.STRUGGLE_WON, this.level().getPlayerByUUID(rider), this, null);
               }

               FaunaMind live = this.faunaMind();
               if (live != null) {
                  live.perception().deliver(this, new Buck(rider, outcome == QteOutcome.WON, live.age()));
               }
            });
         }
      }
   }

   @Override
   public void throwRider() {
      if (!this.level().isClientSide()) {
         Player rider = this.ridingPlayer();
         if (rider != null) {
            rider.stopRiding();
            Vec3 away = Vec3.directionFromRotation(0.0F, this.getYRot()).scale(0.9).add(0.0, 0.55, 0.0);
            rider.push(away.x, away.y, away.z);
            rider.hurtMarked = true;
         }
      }
   }

   @Override
   public void adopt(UUID courter) {
      if (!this.level().isClientSide()) {
         Player person = this.level().getPlayerByUUID(courter);
         this.setOwnerUUID(courter);
         this.setTame(true);
         if (person != null) {
            this.tame(person);
         }
      }
   }

   @Override
   public boolean rideable() {
      return this.supportsRiding();
   }

   @Override
   public void tellCourter(UUID courter, int rung, int length) {
      Player who = this.level().getPlayerByUUID(courter);
      if (who != null && rung < length) {
         String line = rung == 0
            ? "liberanimalium.courtship.tolerates"
            : (rung == 1 ? "liberanimalium.courtship.accepted" : "liberanimalium.courtship.familiar");
         Component said = Component.translatable(line, new Object[]{this.getName()});
         this.sayToCourter(who, rung == 0 ? "tolerates" : (rung == 1 ? "accepted" : "familiar"), null, said);
         this.courtLine = said;
         this.courtLineFor = courter;
         this.courtLineTick = this.level().getGameTime();
      }
   }

   protected void sayToCourter(Player who, String kind, @Nullable Check check, Component line) {
      Notices.tell(who, line);
   }

   @Override
   public void tellAsk(UUID courter, Check check) {
      Player who = this.level().getPlayerByUUID(courter);
      if (who != null) {
         String id = check.id();
         int colon = id.indexOf(58);
         String word = id.substring(colon + 1);
         Ladder.StageDef stage = this.stagePool().stageOf(check);
         String key = (colon < 0 ? "liberanimalium" : id.substring(0, colon))
            + ".courtship.ask."
            + word
            + (stage != null && stage.item() != null ? ".item" : "");
         boolean joined = this.courtLine != null && courter.equals(this.courtLineFor) && this.courtLineTick == this.level().getGameTime();
         Component line = Component.translatableWithFallback(key, word, new Object[]{joined ? Component.translatable("liberanimalium.courtship.it") : this.getName()});
         this.sayToCourter(who, "ask", check, (Component)(joined ? this.courtLine.copy().append(" ").append(line) : line));
      }
   }

   @Override
   public void tellAskAnswered(UUID courter) {
      Player who = this.level().getPlayerByUUID(courter);
      if (who != null) {
         FaunaInteractionEvents.post(FaunaInteractionEvents.Type.ASK_ANSWERED, who, this, null);
      }
   }

   @Override
   public void tellCourterReset(UUID courter) {
      Player who = this.level().getPlayerByUUID(courter);
      if (who != null) {
         this.sayToCourter(who, "lost", null, Component.translatable("liberanimalium.courtship.lost", new Object[]{this.getName()}));
      }
   }

   @Override
   public int touchHold(UUID courter) {
      return 0;
   }

   public boolean wantsToPickUp(ItemStack stack) {
      return this.isFood(stack);
   }

   public EntityDimensions getDimensions(Pose pPose) {
      return this.componentRegistry.get(GrowthComponent.class).getDimensions(pPose);
   }

   public boolean hasActiveAbilityAnimation() {
      if (this.getSecondaryAbility() == null) {
         return false;
      }

      String animation = this.getSecondaryAbility().getActivationAnimation();
      int animLength = this.getSecondaryAbility().getAnimationLengthTicks();
      int cooldown = this.getSecondaryAbility().getCooldownTicks();
      int remaining = (Integer)this.entityData.get(DATA_SECONDARY_COOLDOWN);
      return !animation.isEmpty() && remaining > cooldown - animLength;
   }

   public String getActiveAbilityAnimation() {
      return this.getSecondaryAbility() == null ? "" : this.getSecondaryAbility().getActivationAnimation();
   }

   public boolean isSecondaryAbilityWindowOpen() {
      if (this.getSecondaryAbility() == null) {
         return false;
      }

      int animLength = this.getSecondaryAbility().getAnimationLengthTicks();
      int cooldown = this.getSecondaryAbility().getCooldownTicks();
      int remaining = (Integer)this.entityData.get(DATA_SECONDARY_COOLDOWN);
      return remaining > cooldown - animLength;
   }

   protected void playStepSound(BlockPos pPos, BlockState pBlock) {
      if (!this.isFlying()) {
         if (!this.isSprinting() && !this.isVehicle()) {
            if (this.getGrowthStage() == 0) {
               this.playSound(SoundEvents.WOLF_STEP, 0.1F, 1.5F);
            } else {
               this.playSound(SoundEvents.PIG_STEP, 0.2F, 0.5F);
            }
         } else if (this.tickCount % 3 == 0) {
            this.playSound(SoundEvents.PIG_STEP, 0.2F, 0.5F);
         }
      }
   }

   protected void spawnSprintParticle() {
   }

   public int getAmbientSoundInterval() {
      return 200;
   }

   public void playAmbientSound() {
      FaunaSoundHelper.playIdleSound(this);
   }

   protected void playHurtSound(DamageSource pSource) {
      FaunaSoundHelper.playSound(this, SoundEventKey.HURT);
   }

   protected SoundEvent getAmbientSound() {
      return FaunaSoundHelper.getAmbientSound(this);
   }

   protected SoundEvent getHurtSound(DamageSource pDamageSource) {
      return FaunaSoundHelper.getHurtSound(this);
   }

   protected SoundEvent getDeathSound() {
      return null;
   }

   public FaunaHeadTrackingSystem getHeadTrackingSystem() {
      return this.headTrackingSystem;
   }

   public EyeState eyeState() {
      return this.eyeState;
   }

   public float getNetworkHeadYaw() {
      return (Float)this.entityData.get(DATA_HEAD_YAW);
   }

   public float getNetworkHeadPitch() {
      return (Float)this.entityData.get(DATA_HEAD_PITCH);
   }

   public void setNetworkHeadYaw(float yaw) {
      this.entityData.set(DATA_HEAD_YAW, Quantum.step(yaw, 0.1F));
   }

   public void setNetworkHeadPitch(float pitch) {
      this.entityData.set(DATA_HEAD_PITCH, Quantum.step(pitch, 0.1F));
   }

   public boolean hasNetworkLookTarget() {
      return this.getNetworkLookRung() != FaunaHeadTrackingSystem.LookPriority.NONE;
   }

   public FaunaHeadTrackingSystem.LookPriority getNetworkLookRung() {
      FaunaHeadTrackingSystem.LookPriority[] rungs = FaunaHeadTrackingSystem.LookPriority.values();
      int i = (Byte)this.entityData.get(DATA_LOOK_RUNG);
      return i >= 0 && i < rungs.length ? rungs[i] : FaunaHeadTrackingSystem.LookPriority.NONE;
   }

   public void setNetworkLookRung(FaunaHeadTrackingSystem.LookPriority rung) {
      this.entityData.set(DATA_LOOK_RUNG, (byte)rung.ordinal());
   }

   public int partReporter() {
      return (Integer)this.entityData.get(DATA_PART_REPORTER);
   }

   public void setPartReporter(int playerId) {
      this.entityData.set(DATA_PART_REPORTER, playerId);
   }

   public BodyPlan bodyPlan() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart == null ? BodyPlan.EMPTY : multipart.plan();
   }

   protected void addPassenger(Entity passenger) {
      super.addPassenger(passenger);
      if (passenger instanceof Player) {
         this.getNavigation().stop();
         this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.0);
         this.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
         if (this.faunaMind != null) {
            this.faunaMind.route().clear();
         }

         this.recalculateStats();
         this.handWalkOver();
      }

      this.handOverOnSubLevel();
   }

   protected void removePassenger(Entity passenger) {
      boolean steered = !this.level().isClientSide() && this.ridePlace == null && this.steeredByRider() && passenger == this.getControllingPassenger();
      super.removePassenger(passenger);
      if (passenger instanceof Player) {
         this.setSprinting(false);
         this.recalculateStats();
      }

      if (steered) {
         this.keepRidePace();
      } else if (passenger instanceof Player) {
         this.handWalkOver();
      }

      this.handOverOnSubLevel();
   }

   private void handWalkOver() {
      WalkEngine walk = this.componentRegistry.get(WalkEngine.class);
      if (walk != null) {
         walk.takeOver();
      }
   }

   private void readRidePace() {
      if (!this.level().isClientSide()) {
         if (!this.steeredByRider()) {
            this.rideAt = null;
            this.ridePace = Vec3.ZERO;
         } else {
            Vec3 at = this.position();
            Vec3 step = this.rideAt == null ? Vec3.ZERO : at.subtract(this.rideAt);
            if (step.lengthSqr() > 64.0) {
               step = Vec3.ZERO;
            }

            this.ridePace = this.ridePace.add(step).scale(0.5);
            this.rideAt = at;
         }
      }
   }

   private void keepRidePace() {
      this.setDeltaMovement(this.ridePace);
      this.ridePace = Vec3.ZERO;
      this.rideAt = null;
      this.handWalkOver();
   }

   public void startSpawnAnimation() {
      this.componentRegistry.get(GrowthComponent.class).startSpawnAnimation();
   }

   public int getSpawnScaleTicks() {
      return this.componentRegistry.get(GrowthComponent.class).getSpawnScaleTicks();
   }

   public void setSpawnScaleTicks(int ticks) {
      this.componentRegistry.get(GrowthComponent.class).setSpawnScaleTicks(ticks);
   }

   public FaunaStateContext getStateContext() {
      return this.stateContext;
   }

   public float getHorizontalSpeed() {
      return (Float)this.entityData.get(DATA_HORIZONTAL_SPEED);
   }

   public void setHorizontalSpeed(float speed) {
      this.entityData.set(DATA_HORIZONTAL_SPEED, speed);
   }

   public void setIsDiveBreaking(boolean diveBreaking) {
      this.entityData.set(DATA_IS_DIVE_BREAKING, diveBreaking);
   }

   public boolean isDiveBreaking() {
      return (Boolean)this.entityData.get(DATA_IS_DIVE_BREAKING);
   }

   @org.jetbrains.annotations.Nullable
   public LivingEntity getControllingPassenger() {
      if (!(this.getFirstPassenger() instanceof Player player)) {
         return null;
      } else {
         return this.isOwnedBy(player) && this.isSaddled() ? player : null;
      }
   }

   @org.jetbrains.annotations.Nullable
   public Player ridingPlayer() {
      return this.getFirstPassenger() instanceof Player player ? player : null;
   }

   public boolean canBeControlledBy() {
      Entity passenger = this.getControllingPassenger();
      return passenger instanceof Player player && this.isOwnedBy(player);
   }

   public boolean steeredByRider() {
      return this.isVehicle() && this.canBeControlledBy();
   }

   protected boolean canAddPassenger(Entity pPassenger) {
      if (this.getSyncedIsIncapacitated()) {
         return false;
      } else if (this.isBaby()) {
         return false;
      } else {
         return this.getPassengers().size() >= this.getMaxPassengerCapacity() ? false : pPassenger instanceof Player;
      }
   }

   protected int getMaxPassengerCapacity() {
      return 2;
   }

   public boolean isSaddleable() {
      return false;
   }

   public void equipSaddle(@org.jetbrains.annotations.Nullable SoundSource pSource) {
   }

   public void onPlayerJump(int pJumpPower) {
      if (pJumpPower < 0) {
         return; // Reference negative jump-power branch performs no takeoff.
      } else {
         this.playSound(SoundEvents.HORSE_JUMP, 0.4F, 1.0F);
         this.handleStartJump(pJumpPower);
      }
   }

   public boolean canJump() {
      if (!this.isTame() || !this.isSaddled()) {
         return false;
      } else if (!(this.getControllingPassenger() instanceof Player)) {
         return false;
      } else if (!this.onGround() && !this.isInWater()) {
         return false;
      } else if (this.isClimbing()) {
         return false;
      } else if (this.isUnderWater()) {
         return false;
      } else {
         return this.supportsFlying() ? this.canFly() : this.supportsJumping();
      }
   }

   public void handleStartJump(int pJumpPower) {
      if (pJumpPower < 0) {
         pJumpPower = 0;
      }

      float power = pJumpPower / 100.0F;
      if (this.supportsFlying()) {
         this.executeTakeoff(power);
      }
   }

   public void handleStopJump() {
   }

   public int getJumpCooldown() {
      return 0;
   }

   public boolean isMultipartEntity() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart != null && multipart.getParts().length > 0;
   }

   public PartBody<?>[] getParts() {
      FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
      return multipart != null ? multipart.getParts() : new PartBody[0];
   }

   public void setId(int id) {
      super.setId(id);
      if (this.componentRegistry != null) {
         FaunaMultipartComponent multipart = this.componentRegistry.get(FaunaMultipartComponent.class);
         if (multipart != null) {
            PartBody<?>[] parts = multipart.getParts();

            for (int i = 0; i < parts.length; i++) {
               parts[i].setId(id + i + 1);
            }
         }
      }
   }

   @Override
   public long gameTime() {
      return this.level().getGameTime();
   }

   @Override
   public boolean peacefulWorld() {
      return this.level().getDifficulty() == Difficulty.PEACEFUL;
   }

   public boolean isClientSide() {
      return this.level().isClientSide();
   }

   @org.jetbrains.annotations.Nullable
   public Entity entityById(UUID id) {
      return this.level() instanceof ServerLevel sl ? sl.getEntity(id) : null;
   }

   public abstract String speciesId();

   protected void registerHostComponents(FaunaComponentRegistry registry) {
   }

   public abstract FaunaWeightClass getWeightClass();

   protected float tierCraving(ItemStack stack) {
      return 0.12F;
   }
}
