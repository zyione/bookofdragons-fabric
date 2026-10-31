package net.magister.bookofdragons.platform.fabric;

import java.util.Map;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.renderer.v1.model.ForwardingBakedModel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

/** Fabric equivalent of the three reference forge:separate_transforms models. */
public final class SeparateItemModels {
    private static final Map<String, String[]> MODELS = Map.of(
        "item/book_of_dragons", new String[]{"book_of_dragons_3d", "book_of_dragons_2d"},
        "item/book_of_dragons_httyd", new String[]{"book_of_dragons_3d_httyd", "book_of_dragons_2d_httyd"},
        "item/rattlestaff", new String[]{"rattlestaff_3d", "rattlestaff_2d"});

    public static void register() {
        ModelLoadingPlugin.register(context -> {
            MODELS.values().forEach(names -> {
                for (String name : names) context.addModels(id(name));
            });
            context.modifyModelAfterBake().register((model, bake) -> {
                if (!bake.id().getNamespace().equals("bookofdragons")) return model;
                String[] names = MODELS.get(bake.id().getPath());
                // The inventory entry is a second bake of the same item definition.
                if (names == null) names = MODELS.get("item/" + bake.id().getPath());
                if (names == null || model == null) return model;
                return new SeparateModel(model.getOverrides(),
                    bake.baker().bake(id(names[0]), bake.settings()),
                    bake.baker().bake(id(names[1]), bake.settings()));
            });
        });
    }

    private static ResourceLocation id(String name) { return new ResourceLocation("bookofdragons", "item/" + name); }

    public static final class SeparateModel extends ForwardingBakedModel {
        private final BakedModel flat;
        private final ItemOverrides overrides;
        private SeparateModel(ItemOverrides overrides, BakedModel held, BakedModel flat) {
            this.wrapped = held;
            this.flat = flat;
            this.overrides = overrides;
        }
        @Override public ItemOverrides getOverrides() { return overrides; }
        public BakedModel forDisplay(ItemDisplayContext display) {
            return switch (display) {
                case GUI, GROUND, FIXED -> flat;
                default -> wrapped;
            };
        }
    }
}
