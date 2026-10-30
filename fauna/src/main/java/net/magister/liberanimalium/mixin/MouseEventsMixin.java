package net.magister.liberanimalium.mixin;
import net.magister.liberanimalium.platform.event.ClientEvents;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseEventsMixin {
    @Inject(method = "onPress", at = @At("HEAD"))
    private void liberanimalium$mouse(long window, int button, int action, int modifiers, CallbackInfo ci) {
        if (window == net.minecraft.client.Minecraft.getInstance().getWindow().getWindow())
            ClientEvents.MOUSE_BUTTON.allEach(l -> l.onButton(button, action, modifiers));
    }
}
