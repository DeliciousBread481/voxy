package me.cortex.voxy.client.mixin.minecraft;

import me.cortex.voxy.client.config.VoxyConfig;
import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.FogRenderer.FogMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;

@Mixin(value = FogRenderer.class,remap = true)
public class MixinFogRenderer {
    private static final float DISABLED_FOG_DISTANCE = 999999999.0f;

    @Inject(
        method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V",
        at = @At("TAIL"),
        cancellable = true
    )
    private static void voxy$overrideFog(
        Camera camera,
        FogMode fogMode,
        float viewDistance,
        boolean thickFog,
        float tickDelta,
        CallbackInfo ci
    ) {
        if (!VoxyConfig.CONFIG.isRenderingEnabled()) return;

        var vrs = IGetVoxyRenderSystem.getNullable();
        if (vrs == null) return;

        if (RenderSystem.getShaderFogEnd() < 10.0f) return;

        // 1.21.1 下 Voxy 的后处理会直接读取当前 fog 参数，只有在用户关闭该选项时才应清空它。
        if (!VoxyConfig.CONFIG.renderVanillaFog) {
            RenderSystem.setShaderFogStart(DISABLED_FOG_DISTANCE);
            RenderSystem.setShaderFogEnd(DISABLED_FOG_DISTANCE);
        }
    }
}
