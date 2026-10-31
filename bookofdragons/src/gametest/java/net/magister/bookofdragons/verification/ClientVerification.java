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
    private int spawnedAtServerTick;
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
        // Automated windows may lose focus; keep the integrated server ticking.
        client.options.pauseOnLostFocus = false;
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
        if (phase == 2 && Boolean.getBoolean("bookofdragons.instanceCompatibility")) {
            if (client.screen != null && client.screen.getClass().getName().startsWith("io.github.apace100.origins.screen."))
                client.setScreen(null);
            // Client ticks continue while an onboarding screen pauses the server.
            if (client.getSingleplayerServer().getTickCount() - spawnedAtServerTick < 20) return;
        }
        if (++wait < 60) return;
        wait = 0;
        if (phase == 1) {
            var id = client.player.getUUID();
            var server = client.getSingleplayerServer();
            serverAction = server.submit(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                if (Boolean.getBoolean("bookofdragons.instanceCompatibility")
                        && server.getCommands().getDispatcher().getRoot().getChild("origin") != null) {
                    var source = player.createCommandSourceStack().withPermission(4);
                    if (server.getCommands().performPrefixedCommand(source, "origin set @s origins:origin origins:human") < 1)
                        throw new IllegalStateException("Could not initialize the isolated player's Human origin");
                    if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("origins-classes")
                            && server.getCommands().performPrefixedCommand(source, "origin set @s origins-classes:class origins-classes:nitwit") < 1)
                        throw new IllegalStateException("Could not initialize the isolated player's neutral class");
                }
                for (var existing : level.getAllEntities()) {
                    if (existing instanceof DragonBase) existing.discard();
                }
                BlockPos floor = player.blockPosition();
                player.setGameMode(GameType.CREATIVE);
                player.getInventory().setItem(0, new ItemStack(net.magister.bookofdragons.item.ModItems.BOOK_OF_DRAGONS.get()));
                var variantBook = new ItemStack(net.magister.bookofdragons.item.ModItems.BOOK_OF_DRAGONS.get());
                variantBook.setHoverName(net.minecraft.network.chat.Component.literal("How to Train your Dragon"));
                player.getInventory().setItem(1, variantBook);
                player.getInventory().setItem(2, new ItemStack(net.magister.bookofdragons.item.ModItems.RATTLE_STAFF.get()));
                player.getInventory().selected = 0;
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
                spawnedAtServerTick = server.getTickCount();
            });
            phase = 2;
        } else if (phase == 2) {
            client.setScreen(null);
            verifyItemModels(client);
            if (Boolean.getBoolean("bookofdragons.instanceCompatibility")) {
                var server = client.getSingleplayerServer();
                var id = client.player.getUUID();
                serverAction = server.submit(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    DragonBase body = (DragonBase)player.serverLevel().getEntity(dragon);
                    var part = Arrays.stream(body.getParts()).filter(p -> p.distanceToSqr(body) > 0.1 && p.distanceToSqr(body) < 64)
                        .findFirst().orElseThrow(() -> new IllegalStateException("No offset hitbox after " + body.tickCount
                            + " body ticks at " + body.position() + "; parts=" + Arrays.toString(Arrays.stream(body.getParts()).map(p -> p.position()).toArray())));
                    var source = player.createCommandSourceStack().withPermission(4).withPosition(part.position());
                    String target = "@e[type=bookofdragons:nightfury,sort=nearest,limit=1,distance=..10]";
                    try {
                        var selector = new net.minecraft.commands.arguments.selector.EntitySelectorParser(new com.mojang.brigadier.StringReader(target)).parse();
                        if (selector.findSingleEntity(source) != body) throw new IllegalStateException("Instance selector returned a hitbox");
                    } catch (com.mojang.brigadier.exceptions.CommandSyntaxException error) { throw new IllegalStateException(error); }
                    body.setOwnerUUID(null); body.setTame(false); body.setTamingRitualCompleted(false);
                    server.getCommands().performPrefixedCommand(source, "data modify entity " + target + " Owner set from entity @s UUID");
                    server.getCommands().performPrefixedCommand(source, "data merge entity " + target + " {TamingRitualCompleted:1b,AwaitingTamingRitual:0b}");
                    if (!body.isTame() || !id.equals(body.getOwnerUUID()) || !body.canBeMountedBy(player))
                        throw new IllegalStateException("Instance ownership commands failed to permit saddled mounting");
                    passed.add("Nearest-dragon ownership commands select the parent and preserve saddled mounting");
                });
            }
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
    private void verifyItemModels(Minecraft client) {
        // Starter-kit mods can populate inventory asynchronously on first join.
        // Query controlled stacks rather than assuming particular hotbar contents.
        var book = new ItemStack(net.magister.bookofdragons.item.ModItems.BOOK_OF_DRAGONS.get());
        var renamed = book.copy();
        renamed.setHoverName(net.minecraft.network.chat.Component.literal("How to Train your Dragon"));
        var stacks = List.of(book, renamed, new ItemStack(net.magister.bookofdragons.item.ModItems.RATTLE_STAFF.get()));
        Set<net.minecraft.resources.ResourceLocation> bookSprites = new HashSet<>();
        for (int slot = 0; slot < 3; slot++) {
            var stack = stacks.get(slot);
            var model = client.getItemRenderer().getModel(stack, client.level, client.player, 0);
            if (!(model instanceof net.magister.bookofdragons.platform.fabric.SeparateItemModels.SeparateModel separate))
                throw new IllegalStateException("Separate item model not loaded: " + stack);
            for (var display : net.minecraft.world.item.ItemDisplayContext.values()) {
                var selected = separate.forDisplay(display);
                var quads = selected.getQuads(null, null, net.minecraft.util.RandomSource.create(0));
                if (quads.isEmpty()) throw new IllegalStateException("Invisible item model: " + stack + " / " + display);
                for (var quad : quads) {
                    var sprite = quad.getSprite().contents().name();
                    if (sprite.getPath().equals("missingno")) throw new IllegalStateException("Missing item texture: " + stack);
                    if (slot < 2 && display == net.minecraft.world.item.ItemDisplayContext.GUI) bookSprites.add(sprite);
                }
            }
        }
        if (bookSprites.size() != 2) throw new IllegalStateException("Renamed book texture override did not select a distinct icon");
        passed.add("Book, renamed book and rattlestaff have textured geometry in every display context");
    }
}
