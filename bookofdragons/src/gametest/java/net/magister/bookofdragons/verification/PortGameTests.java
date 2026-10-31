package net.magister.bookofdragons.verification;

import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.magister.bookofdragons.datagen.data.DragonDiscoveryData;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.entity.data.DragonType;
import net.magister.bookofdragons.effect.ModEffects;
import net.magister.bookofdragons.effect.Paralysis;
import net.magister.bookofdragons.platform.fabric.DiscoveryCarrier;
import net.magister.liberanimalium.body.nav.noise.FaunaGameEvents;
import net.magister.liberanimalium.platform.fabric.EntityData;
import net.magister.liberanimalium.platform.part.PartBody;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.phys.AABB;

public final class PortGameTests implements FabricGameTest {
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void entityIndependentRayQueriesRemainSupported(GameTestHelper helper) {
        var position = helper.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1));
        var context = new net.minecraft.world.level.ClipContext(position, position.add(0, -1, 0),
            net.minecraft.world.level.ClipContext.Block.COLLIDER,
            net.minecraft.world.level.ClipContext.Fluid.NONE, null);
        helper.getLevel().clip(context);
        helper.succeed();
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void explosionListenersFilterEntitiesBeforeDamage(GameTestHelper helper) {
        Pig protectedPig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        Pig exposedPig = helper.spawn(EntityType.PIG, new BlockPos(2, 2, 1));
        float initialHealth = protectedPig.getHealth();
        var explosion = new net.minecraft.world.level.Explosion(helper.getLevel(), null,
            protectedPig.getX(), protectedPig.getY(), protectedPig.getZ(), 2.0F, false,
            net.minecraft.world.level.Explosion.BlockInteraction.KEEP);
        int[] calls = {0};
        net.magister.liberanimalium.platform.event.CommonEvents.EXPLOSION.register((level, event, blocks, entities) -> {
            if (event == explosion) {
                calls[0]++;
                helper.assertTrue(entities.contains(protectedPig), "Explosion callback missed nearby entity");
                entities.remove(protectedPig);
            }
        });
        explosion.explode();
        helper.assertTrue(calls[0] == 1, "Explosion callback must run exactly once");
        helper.assertTrue(protectedPig.getHealth() == initialHealth, "Filtered entity took explosion damage");
        helper.assertTrue(exposedPig.getHealth() < exposedPig.getMaxHealth(), "Unfiltered entity escaped explosion damage");
        helper.succeed();
    }
    private static net.magister.liberanimalium.platform.net.PacketContext sender(net.minecraft.server.level.ServerPlayer player) {
        return new net.magister.liberanimalium.platform.net.PacketContext() {
            public void enqueueWork(Runnable work) { work.run(); }
            public net.minecraft.server.level.ServerPlayer sender() { return player; }
        };
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void nonOwnersCannotCommandOrReleaseNearbyDragons(GameTestHelper helper) {
        var owner = helper.makeMockServerPlayerInLevel();
        var stranger = helper.makeMockServerPlayerInLevel();
        // Test helper may reuse the mock profile. Assign distinct identity explicitly.
        stranger.setUUID(UUID.randomUUID());
        DragonBase body = helper.spawn(ModEntities.NIGHTFURY.get(), new BlockPos(1, 2, 1));
        body.setNoAi(true); body.adopt(owner.getUUID());
        owner.setPos(body.position()); stranger.setPos(body.position());
        var commands = body.componentRegistry.get(net.magister.liberanimalium.chassis.component.CommandComponent.class);
        commands.setCommandForced(net.magister.liberanimalium.chassis.component.command.Stance.STAY,
            net.magister.liberanimalium.chassis.component.command.Modifier.REST);
        net.magister.liberanimalium.network.SetCommandMessage.handle(new net.magister.liberanimalium.network.SetCommandMessage(body.getUUID(),
            net.magister.liberanimalium.chassis.component.command.Stance.FOLLOW,
            net.magister.liberanimalium.chassis.component.command.Modifier.TRAIL), sender(stranger));
        helper.assertTrue(commands.getStance() == net.magister.liberanimalium.chassis.component.command.Stance.STAY, "Stranger changed stance");
        net.magister.bookofdragons.network.message.ReleaseDragonMessage.handle(new net.magister.bookofdragons.network.message.ReleaseDragonMessage(body.getUUID()), sender(stranger));
        helper.assertTrue(body.isOwnedBy(owner), "Stranger released owned dragon");
        owner.setPos(body.getX() + 100, body.getY(), body.getZ());
        net.magister.liberanimalium.network.SetCommandMessage.handle(new net.magister.liberanimalium.network.SetCommandMessage(body.getUUID(),
            net.magister.liberanimalium.chassis.component.command.Stance.FOLLOW,
            net.magister.liberanimalium.chassis.component.command.Modifier.TRAIL), sender(owner));
        helper.assertTrue(commands.getStance() == net.magister.liberanimalium.chassis.component.command.Stance.STAY, "Distant owner bypassed command range");
        helper.succeed();
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void equipmentAndEggDataSurviveItemAndEntityRoundTrips(GameTestHelper helper) {
        DragonBase body = helper.spawn(ModEntities.NIGHTFURY.get(), new BlockPos(1, 2, 1));
        body.setNoAi(true); body.adopt(UUID.randomUUID());
        body.getInventory().setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE));
        body.getInventory().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST));
        helper.assertTrue(body.isSaddled(), "Saddle inventory did not update equipment");
        CompoundTag saved = body.saveWithoutId(new CompoundTag());
        DragonBase restored = ModEntities.NIGHTFURY.get().create(helper.getLevel()); restored.load(saved);
        helper.assertTrue(restored.isSaddled() && restored.getInventory().getItem(1).is(net.minecraft.world.item.Items.CHEST), "Equipment lost on reload");
        var egg = ModEntities.DRAGON_EGG_ENTITY.get().create(helper.getLevel());
        UUID owner = UUID.randomUUID();
        egg.setDragonType(DragonType.GRONCKLE); egg.setOwnerUUID(owner); egg.setCurrentHatchTime(1234);
        var stack = egg.createItemStack();
        var next = ModEntities.DRAGON_EGG_ENTITY.get().create(helper.getLevel()); next.loadFromItemStack(stack);
        helper.assertTrue(next.getDragonType() == DragonType.GRONCKLE && next.getOwnerUUID().orElseThrow().equals(owner), "Egg type or owner lost in item");
        helper.assertTrue(next.getCurrentHatchTime() == 1234, "Egg incubation lost in item");
        helper.succeed();
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void malformedControlsRejectNonFiniteValuesBeforeMutation(GameTestHelper helper) {
        for (float value : new float[]{Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
            try {
                buf.writeFloat(value); buf.writeBoolean(true); buf.writeBoolean(false);
                boolean rejected = false;
                try { net.magister.liberanimalium.network.ControlMessageThrottleSync.decode(buf); }
                catch (IllegalArgumentException expected) { rejected = true; }
                helper.assertTrue(rejected, "Non-finite throttle was decoded");
            } finally { buf.release(); }
        }
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        try {
            buf.writeVarInt(1); buf.writeVarLong(0); buf.writeVarInt(Integer.MAX_VALUE);
            boolean rejected = false;
            try { net.magister.liberanimalium.network.PartSyncPayload.decode(buf); }
            catch (IllegalArgumentException expected) { rejected = true; }
            helper.assertTrue(rejected, "Oversized multipart array accepted");
        } finally { buf.release(); }
        helper.succeed();
    }
    private static List<EntityType<? extends DragonBase>> species() {
        return List.of(ModEntities.NIGHTFURY.get(), ModEntities.DEADLYNADDER.get(), ModEntities.MONSTROUSNIGHTMARE.get(),
            ModEntities.GRONCKLE.get(), ModEntities.HIDDEOUS_ZIPPLEBACK.get(), ModEntities.SKRILL.get(),
            ModEntities.WHISPERING_DEATH.get(), ModEntities.TERRIBLE_TERROR.get(), ModEntities.SPEED_STINGER.get());
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void activatedEggHatchesGeneticBabyWithParentLineage(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        var egg = helper.spawn(ModEntities.DRAGON_EGG_ENTITY.get(), new BlockPos(1, 2, 1));
        egg.setDragonType(DragonType.TERRIBLE_TERROR); egg.setOwnerUUID(owner);
        egg.setFromTamedParents(true); egg.setIsActivated(true); egg.setCurrentHatchTime(0);
        helper.runAfterDelay(4, () -> {
            var babies = helper.getLevel().getEntitiesOfClass(DragonBase.class, egg.getBoundingBox().inflate(4), body -> body.getType() == ModEntities.TERRIBLE_TERROR.get());
            helper.assertTrue(babies.size() == 1, "Activated egg did not hatch exactly one baby");
            DragonBase baby = babies.get(0);
            helper.assertTrue(baby.isBaby() && baby.getGrowthStage() == 0 && baby.hasGenotype(), "Hatched baby missing age, stage or genetics");
            CompoundTag saved = baby.saveWithoutId(new CompoundTag());
            DragonBase restored = ModEntities.TERRIBLE_TERROR.get().create(helper.getLevel()); restored.load(saved);
            helper.assertTrue(restored.hasGenotype() && restored.isBaby(), "Hatched genetics or age lost on reload");
            helper.succeed();
        });
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE, timeoutTicks = 100)
    public void everyDragonCreatesTicksAndRetainsOwnershipOnSave(GameTestHelper helper) {
        UUID owner = UUID.randomUUID();
        List<DragonBase> dragons = new ArrayList<>();
        for (EntityType<? extends DragonBase> type : species()) {
            DragonBase dragon = helper.spawn(type, new BlockPos(1, 3, 1));
            dragon.finalizeSpawn(helper.getLevel(), helper.getLevel().getCurrentDifficultyAt(dragon.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
            helper.assertTrue(dragon.hasGenotype(), "Finalized spawn lacks genetics: " + type);
            dragon.setNoAi(true);
            dragon.adopt(owner);
            EntityData.get(dragon).putString("port-test", "persisted");
            helper.assertTrue(dragon.getMaxHealth() > 0, "Missing attributes for " + type);
            helper.assertTrue(dragon.isTame() && owner.equals(dragon.getOwnerUUID()), "Adoption did not establish ownership");
            CompoundTag saved = dragon.saveWithoutId(new CompoundTag());
            DragonBase restored = (DragonBase)type.create(helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.isTame() && owner.equals(restored.getOwnerUUID()), "Ownership lost on reload: " + type);
            helper.assertTrue("persisted".equals(EntityData.get(restored).getString("port-test")), "Entity data lost on reload");
            helper.assertTrue(restored.getParts().length == dragon.getParts().length, "Multipart shape changed on reload");
            dragons.add(dragon);
        }
        helper.runAfterDelay(10, () -> {
            for (DragonBase dragon : dragons) helper.assertTrue(dragon.isAlive() && dragon.tickCount > 0, "Dragon did not tick");
            helper.succeed();
        });
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void soundListenerAndMultipartQueriesAreRegistered(GameTestHelper helper) {
        helper.assertTrue(FaunaGameEvents.CALL.get().is(FaunaGameEvents.FAUNA_CAN_LISTEN), "Fauna call event is absent from listener tag");
        DragonBase dragon = helper.spawn(ModEntities.NIGHTFURY.get(), new BlockPos(1, 3, 1));
        dragon.setNoAi(true);
        helper.assertTrue(dragon.getParts().length > 0, "No multipart hitboxes");
        PartBody<?> part = dragon.getParts()[0];
        helper.assertTrue(helper.getLevel().getEntity(part.getId()) == part, "Server cannot resolve part interaction ID");
        AABB bounds = part.getBoundingBox().inflate(1);
        helper.assertTrue(helper.getLevel().getEntities((Entity)null, bounds, entity -> entity == part).contains(part), "Part absent from collision/target queries");
        helper.succeed();
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void paralysisDisablesAndRestoresAiWithImmunity(GameTestHelper helper) {
        Pig pig = helper.spawn(EntityType.PIG, new BlockPos(1, 2, 1));
        helper.assertTrue(pig.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 20)), "Paralysis rejected valid target");
        helper.assertTrue(pig.isNoAi(), "Paralysis did not hold AI");
        pig.removeEffect(ModEffects.PARALYSIS.get());
        helper.assertTrue(!pig.isNoAi(), "AI not restored after effect removal");
        helper.assertTrue(!pig.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 20)), "Immediate reapplication ignored immunity");
        pig.setNoAi(true);
        EntityData.get(pig).remove("bookofdragons:paralysis_immune_until");
        pig.addEffect(new MobEffectInstance(ModEffects.PARALYSIS.get(), 20));
        pig.removeEffect(ModEffects.PARALYSIS.get());
        helper.assertTrue(pig.isNoAi(), "Effect removal changed pre-existing NoAI");
        helper.succeed();
    }
    @GameTest(template = FabricGameTest.EMPTY_STRUCTURE)
    public void discoveryAndNotesSurviveSerializationAndClone(GameTestHelper helper) {
        DragonDiscoveryData data = new DragonDiscoveryData();
        data.learn(DragonType.NIGHTFURY, "test:known");
        data.setNotes(DragonType.NIGHTFURY, List.of("first page", "second page"));
        CompoundTag saved = new CompoundTag(); data.saveToNBT(saved);
        DragonDiscoveryData restored = new DragonDiscoveryData(); restored.loadFromNBT(saved);
        DragonDiscoveryData cloned = new DragonDiscoveryData(); cloned.copyFrom(restored);
        helper.assertTrue(cloned.facts(DragonType.NIGHTFURY).contains("test:known"), "Discovery fact lost");
        helper.assertTrue(cloned.notes(DragonType.NIGHTFURY).equals(List.of("first page", "second page")), "Notes lost or reordered");
        var player = helper.makeMockPlayer();
        helper.assertTrue(player instanceof DiscoveryCarrier, "Player discovery mixin not installed");
        helper.succeed();
    }
}
