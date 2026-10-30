package net.magister.bookofdragons.verification;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.magister.bookofdragons.entity.ModEntities;
import net.magister.bookofdragons.entity.base.dragon.DragonBase;
import net.magister.bookofdragons.client.gui.book.BookOfDragonsScreen;
import net.magister.bookofdragons.inventory.DragonInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/** Repeatable real-client rendering and menu checks without desktop input. */
public final class ClientVerification implements ClientModInitializer {
    private int tick, phase, wait;
    private UUID dragon;
    private CompletableFuture<?> serverAction;
    private final Path output = Path.of("verification");
    private final List<String> passed = new ArrayList<>();
    public void onInitializeClient() {
        if (!Boolean.getBoolean("bookofdragons.clientVerification")) return;
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try { step(client); }
            catch (Throwable failure) {
                try { Files.createDirectories(output); Files.writeString(output.resolve("failure.txt"), failure.toString()); }
                catch (Exception ignored) { failure.printStackTrace(); }
                org.slf4j.LoggerFactory.getLogger("port-verification").error("CLIENT VERIFICATION FAILED", failure);
                client.stop();
            }
        });
    }
    private void step(Minecraft client) throws Exception {
        if (++tick > 6000) throw new IllegalStateException("Client verification timed out at phase " + phase);
        if (phase == 0) {
            if (!(client.screen instanceof TitleScreen) || client.getOverlay() != null) return;
            Files.createDirectories(output);
            String world = "Port Verification";
            if (Files.exists(client.gameDirectory.toPath().resolve("saves").resolve(world).resolve("level.dat"))) {
                client.createWorldOpenFlows().loadLevel(client.screen, world);
            } else {
                var settings = new net.minecraft.world.level.LevelSettings(world, GameType.CREATIVE, false,
                    net.minecraft.world.Difficulty.PEACEFUL, true, new net.minecraft.world.level.GameRules(), net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
                client.createWorldOpenFlows().createFreshLevel(world, settings,
                    new net.minecraft.world.level.levelgen.WorldOptions(12012001L, false, false),
                    net.minecraft.world.level.levelgen.presets.WorldPresets::createNormalWorldDimensions);
            }
            phase = 1;
            return;
        }
        if (client.level == null || client.player == null || client.getSingleplayerServer() == null) return;
        if (serverAction != null) {
            if (!serverAction.isDone()) return;
            serverAction.join(); serverAction = null;
        }
        if (++wait < 60) return;
        wait = 0;
        if (phase == 1) {
            var id = client.player.getUUID();
            var server = client.getSingleplayerServer();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                for (var existing : level.getAllEntities()) {
                    if (existing instanceof DragonBase) existing.discard();
                }
                BlockPos floor = player.blockPosition();
                player.setGameMode(GameType.CREATIVE);
                double x = floor.getX(), y = 110;
                server.getCommands().performPrefixedCommand(server.createCommandSourceStack(),
                    "fill " + (floor.getX()-22) + " 109 " + (floor.getZ()-5) + " " + (floor.getX()+22) + " 109 " + (floor.getZ()+48) + " minecraft:stone");
                level.setDayTime(6000);
                level.setWeatherParameters(6000, 0, false, false);
                player.teleportTo(level, x, y + 2, floor.getZ(), 0, 0);
                List<EntityType<? extends DragonBase>> types = List.of(ModEntities.NIGHTFURY.get(), ModEntities.DEADLYNADDER.get(),
                    ModEntities.MONSTROUSNIGHTMARE.get(), ModEntities.GRONCKLE.get(), ModEntities.HIDDEOUS_ZIPPLEBACK.get(),
                    ModEntities.SKRILL.get(), ModEntities.WHISPERING_DEATH.get(), ModEntities.TERRIBLE_TERROR.get(), ModEntities.SPEED_STINGER.get());
                int i = 0;
                for (var type : types) {
                    DragonBase body = type.create(level);
                    body.moveTo(x + (i % 3 - 1) * 9, y + 1, floor.getZ() + 12 + i / 3 * 10, 180, 0);
                    body.finalizeSpawn(level, level.getCurrentDifficultyAt(body.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null, null);
                    body.setGrowthStage(2);
                    if (!body.hasGenotype()) throw new IllegalStateException("Spawn did not generate genetics: " + type);
                    body.setNoAi(true); body.adopt(id); body.setBond(id, 1000);
                    if (i == 0) {
                        dragon = body.getUUID();
                        body.getInventory().setItem(0, new ItemStack(Items.SADDLE));
                        ((net.magister.bookofdragons.entity.component.InventoryComponent)body.componentRegistry.get(net.magister.bookofdragons.entity.component.InventoryComponent.class)).updateEquipment();
                    }
                    level.addFreshEntity(body); i++;
                }
            });
            phase = 2;
        } else if (phase == 2) {
            client.setScreen(null);
            snapshot(client, "01-gallery.png");
            for (var entity : client.level.entitiesForRendering()) {
                if (entity instanceof DragonBase body) {
                    var texture = net.magister.liberanimalium.render.FaunaRendererAz.getTextureLocationCached(body);
                    if (texture.getNamespace().equals("liberanimalium") && texture.getPath().endsWith("_default.png"))
                        throw new IllegalStateException("Dragon lacks synchronized genetic texture: " + body.getType());
                }
            }
            passed.add("Nine species finalized, genetics synchronized and world frames rendered");
            client.setScreen(new BookOfDragonsScreen());
            phase = 3;
        } else if (phase == 3) {
            if (!(client.screen instanceof BookOfDragonsScreen)) throw new IllegalStateException("Book screen did not remain open");
            snapshot(client, "02-book.png");
            passed.add("Book opened, rendered and requested synchronized data");
            List<String> pages = new ArrayList<>();
            for (int index = 0; index < 100; index++) pages.add("Page " + index + " " + "notes ".repeat(160));
            net.magister.bookofdragons.network.ControlNetwork.sendToServer(
                net.magister.bookofdragons.network.message.SaveNotesMessage.species(net.magister.bookofdragons.entity.data.DragonType.NIGHTFURY, pages));
            client.setScreen(null);
            var server = client.getSingleplayerServer(); var id = client.player.getUUID();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                DragonBase body = (DragonBase)player.serverLevel().getEntity(dragon);
                player.teleportTo(body.getX(), body.getY(), body.getZ() - 3);
                body.openGui(player);
            });
            phase = 4;
        } else if (phase == 4) {
            if (!(client.screen instanceof DragonInventoryScreen)) throw new IllegalStateException("Extended dragon menu did not open: " + client.screen);
            snapshot(client, "03-inventory.png");
            passed.add("Extended menu payload, inventory screen and entity preview rendered");
            client.player.closeContainer();
            var server = client.getSingleplayerServer(); var id = client.player.getUUID();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var saved = net.magister.bookofdragons.discovery.DiscoveryStore.get(player).orElseThrow().notes(net.magister.bookofdragons.entity.data.DragonType.NIGHTFURY);
                if (saved.size() != 100 || !saved.get(99).startsWith("Page 99 ")) throw new IllegalStateException("Fragmented 100-page note save did not arrive intact");
                DragonBase body = (DragonBase)player.serverLevel().getEntity(dragon);
                if (!player.startRiding(body, true)) throw new IllegalStateException("Mount refused");
            });
            phase = 5;
        } else if (phase == 5) {
            if (!(client.player.getVehicle() instanceof DragonBase)) throw new IllegalStateException("Mount not synchronized");
            snapshot(client, "04-mounted-hud.png");
            passed.add("Mount, rider camera and HUD rendered without error");
            passed.add("100-page note save crossed the real Fabric C2S transport intact");
            ((DragonBase)client.player.getVehicle()).onPlayerJump(100);
            var server = client.getSingleplayerServer(); var id = client.player.getUUID();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                DragonBase body = (DragonBase)player.getVehicle();
                if (!body.canFly()) throw new IllegalStateException("Adult Night Fury cannot fly");
                body.onPlayerJump(100);
            });
            phase = 6;
        } else if (phase == 6) {
            DragonBase body = (DragonBase)client.player.getVehicle();
            if (body == null || !body.isFlying()) throw new IllegalStateException("Takeoff did not reach synchronized flight: " + (body == null ? "no mount" : "mode=" + body.getTransportMode() + ", transition=" + body.getLocomotionOrchestrator().activeTransitionName() + ", y=" + body.getY()));
            snapshot(client, "05-flight.png");
            passed.add("Adult takeoff transitioned to synchronized flight and rendered rider camera");
            net.magister.liberanimalium.network.FaunaNetwork.sendToServer(new net.magister.liberanimalium.network.ControlMessagePrimaryAttack(true));
            phase = 7;
        } else if (phase == 7) {
            snapshot(client, "06-breath-charge.png");
            var server = client.getSingleplayerServer(); var id = client.player.getUUID();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                DragonBase body = (DragonBase)player.getVehicle();
                var breath = body.componentRegistry.get(net.magister.liberanimalium.chassis.component.ranged.OmniAttackHandler.class);
                if (breath == null || !breath.hasAttack() || !breath.isCharging()) throw new IllegalStateException("Primary-attack C2S did not start configured breath charge");
            });
            net.magister.liberanimalium.network.FaunaNetwork.sendToServer(new net.magister.liberanimalium.network.ControlMessagePrimaryAttack(false));
            phase = 8;
        } else if (phase == 8) {
            snapshot(client, "07-breath-release.png");
            passed.add("Primary attack press/release traveled through real C2S handler and rendered breath frames");
            net.magister.liberanimalium.network.FaunaNetwork.sendToServer(new net.magister.liberanimalium.network.ControlMessageDismount());
            phase = 9;
        } else if (phase == 9) {
            if (client.player.getVehicle() != null) throw new IllegalStateException("Network dismount failed");
            passed.add("C2S dismount detached rider on server and client");
            Files.write(output.resolve("passed.txt"), passed);
            org.slf4j.LoggerFactory.getLogger("port-verification").info("CLIENT VERIFICATION PASSED: {}", passed);
            client.stop(); phase = 10;
        }
    }
    private void snapshot(Minecraft client, String name) throws Exception {
        try (var image = Screenshot.takeScreenshot(client.getMainRenderTarget())) { image.writeToFile(output.resolve(name)); }
    }
}
