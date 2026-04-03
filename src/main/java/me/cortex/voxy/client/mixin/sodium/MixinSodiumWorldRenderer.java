package me.cortex.voxy.client.mixin.sodium;

import me.cortex.voxy.client.core.IGetVoxyRenderSystem;
import me.cortex.voxy.client.core.rendering.Viewport;
import me.cortex.voxy.client.core.util.IrisUtil;
import me.cortex.voxy.commonImpl.VoxyCommon;
import me.cortex.voxy.commonImpl.VoxyInstance;
import net.caffeinemc.mods.sodium.client.gl.device.CommandList;
import net.caffeinemc.mods.sodium.client.render.SodiumWorldRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.caffeinemc.mods.sodium.client.render.chunk.ChunkRenderMatrices;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SodiumWorldRenderer.class, remap = false)
public class MixinSodiumWorldRenderer {
    @Inject(method = "initRenderer", at = @At("TAIL"), remap = false)
    private void voxy$injectThreadUpdate(CommandList cl, CallbackInfo ci) {
        var vi = VoxyCommon.getInstance();
        if (vi != null) vi.updateDedicatedThreads();
    }

    @Unique
    private ChunkRenderMatrices voxy$capturedMatrices;

    // Sodium 0.6.x path
    @Inject(
        method = "drawChunkLayer",
        at = @At("HEAD"),
        require = 0,
        remap = false
    )
    private void voxy$captureMatricesSodium(
            RenderType renderLayer,
            ChunkRenderMatrices matrices,
            double x,
            double y,
            double z,
            CallbackInfo ci
    ) {
        this.voxy$capturedMatrices = matrices;
    }

    @Inject(method = "drawChunkLayer", at = @At("TAIL"), require = 0, remap = false)
    private void injectRenderSodium(RenderType renderLayer, ChunkRenderMatrices matrices, double x, double y, double z, CallbackInfo ci) {
        this.doRender(this.voxy$capturedMatrices, renderLayer, x, y, z);
    }
    
    @Unique
    private void doRender(ChunkRenderMatrices matrices, RenderType renderLayer, double x, double y, double z) {
        if (renderLayer == RenderType.solid()) {
            var renderer = ((IGetVoxyRenderSystem) Minecraft.getInstance().levelRenderer).getVoxyRenderSystem();
            if (renderer != null) {
                Viewport<?> viewport = null;
                if (IrisUtil.irisShaderPackEnabled()) {
                    viewport = renderer.getViewport();
                } else {
                    viewport = renderer.setupViewport(matrices, x, y, z);
                }
                renderer.renderOpaque(viewport);
            }
        }
    }
}
