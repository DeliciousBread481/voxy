package me.cortex.voxy.commonImpl.mixin.chunky;

import me.cortex.voxy.common.world.service.VoxelIngestService;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import org.popcraft.chunky.platform.FabricWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(FabricWorld.class)
public class MixinFabricWorld {
    @Inject(method = "getChunkAtAsync", at = @At("RETURN"))
    private void voxy$ingestAfterChunkReady(int x, int z, CallbackInfoReturnable<CompletableFuture<Void>> cir) {
        cir.getReturnValue().thenRun(() -> {
            ServerLevel world = ((FabricWorld) (Object) this).getWorld();
            LevelChunk chunk = world.getChunk(x, z);
            if (chunk != null) {
                VoxelIngestService.tryAutoIngestChunk(chunk);
            }
        });
    }
}
