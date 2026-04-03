package me.cortex.voxy.commonImpl.mixin.distanthorizons;

import com.seibel.distanthorizons.core.api.internal.SharedApi;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.wrapperInterfaces.chunk.IChunkWrapper;
import loaderCommon.fabric.com.seibel.distanthorizons.common.wrappers.chunk.ChunkWrapper;
import me.cortex.voxy.common.Logger;
import me.cortex.voxy.common.world.service.VoxelIngestService;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SharedApi.class, remap = false)
public class MixinSharedApi {
    @Inject(method = "queueChunkUpdate", at = @At(
            value = "NEW",
            target = "Lcom/seibel/distanthorizons/core/api/internal/chunkUpdating/ChunkUpdateData;"),
            remap = false
    )
    private static void beforeChunkUpdateCreation(
            IChunkWrapper chunkWrapper,
            IDhLevel dhLevel,
            CallbackInfo ci
    ) {
        if (!(chunkWrapper instanceof ChunkWrapper cw)) {
            Logger.error("DH MixinSharedApi: Unknown chunk wrapper class: " + chunkWrapper.getClass().getName());
            return;
        }

        ChunkAccess chunkAccess = cw.getChunk();
        if (!(chunkAccess instanceof LevelChunk wc)) {
            Logger.error("DH MixinSharedApi: ChunkAccess is not LevelChunk: " + chunkAccess.getClass().getName());
            return;
        }

        VoxelIngestService.tryAutoIngestChunk(wc);
    }
}
