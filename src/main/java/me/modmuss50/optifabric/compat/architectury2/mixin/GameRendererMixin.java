package me.modmuss50.optifabric.compat.architectury2.mixin;

import me.modmuss50.optifabric.compat.InterceptingMixin;
import me.modmuss50.optifabric.compat.PlacatingSurrogate;
import me.modmuss50.optifabric.compat.Shim;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.Window;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(GameRenderer.class)
@InterceptingMixin("dev/architectury/mixin/fabric/client/MixinGameRenderer")
abstract class GameRendererMixin {
	@Shim
	private native void renderScreenPre(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, MatrixStack matrices);

	@PlacatingSurrogate
	private void renderScreenPre(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window) {
	}

	@Inject(method = "render(FJZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD,
			cancellable = true)
	private void renderScreenPre(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, MatrixStack matrices) {
		renderScreenPre(tickDelta, startTime, tick, call, mouseX, mouseY, matrices);
	}

	@Shim
	private native void renderScreenPost(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, MatrixStack matrices);

	@PlacatingSurrogate
	private void renderScreenPost(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window) {
	}

	@Inject(method = "render(FJZ)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", shift = Shift.AFTER, ordinal = 0),
			locals = LocalCapture.CAPTURE_FAILHARD)
	private void renderScreenPost(float tickDelta, long startTime, boolean tick, CallbackInfo call, int mouseX, int mouseY, Window window, MatrixStack matrices) {
		renderScreenPost(tickDelta, startTime, tick, call, mouseX, mouseY, matrices);
	}
}