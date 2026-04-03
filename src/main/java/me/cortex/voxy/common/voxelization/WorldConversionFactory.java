package me.cortex.voxy.common.voxelization;

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import me.cortex.voxy.client.core.util.ExpansionUtil;
import me.cortex.voxy.common.world.other.Mapper;
import me.cortex.voxy.common.world.other.Mipper;
import net.caffeinemc.mods.lithium.common.world.chunk.LithiumHashPalette;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.util.LinearCongruentialGenerator;
import net.minecraft.util.Mth;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ZeroBitStorage;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.GlobalPalette;
import net.minecraft.world.level.chunk.HashMapPalette;
import net.minecraft.world.level.chunk.LinearPalette;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.SingleValuePalette;
import java.util.WeakHashMap;

public class WorldConversionFactory {
    private static final boolean LITHIUM_INSTALLED = FabricLoader.getInstance().isModLoaded("lithium");
    private static final boolean CANARY_INSTALLED = FabricLoader.getInstance().isModLoaded("canary");

    private static final class Cache {
        private final int[] biomeCache = new int[4*4*4];
        private final WeakHashMap<Mapper, Reference2IntOpenHashMap<BlockState>> localMapping = new WeakHashMap<>();
        private int[] paletteCache = new int[1024];
        private Reference2IntOpenHashMap<BlockState> getLocalMapping(Mapper mapper) {
            return this.localMapping.computeIfAbsent(mapper, (a_)->new Reference2IntOpenHashMap<>());
        }
        private int[] getPaletteCache(int size) {
            if (this.paletteCache.length < size) {
                this.paletteCache = new int[size];
            }
            return this.paletteCache;
        }
    }

    //TODO: create a mapping for world/mapper -> local mapping
    private static final ThreadLocal<Cache> THREAD_LOCAL = ThreadLocal.withInitial(Cache::new);

    private static boolean setupLithiumLocalPallet(Palette<BlockState> vp, Reference2IntOpenHashMap<BlockState> blockCache, Mapper mapper, int[] pc)  {
        if (vp instanceof LithiumHashPalette<BlockState>) {
            for (int i = 0; i < vp.getSize(); i++) {
                BlockState state = null;
                int blockId = -1;
                try { state = vp.valueFor(i); } catch (Exception e) {}
                if (state != null) {
                    blockId = blockCache.getOrDefault(state, -1);
                    if (blockId == -1) {
                        blockId = mapper.getIdForBlockState(state);
                        blockCache.put(state, blockId);
                    }
                }
                pc[i] = blockId;
            }
            return true;
        }
        return false;
    }

    // 支持Canary MOD
    private static boolean setupCanaryLocalPallet(Palette<BlockState> vp, Reference2IntOpenHashMap<BlockState> blockCache, Mapper mapper, int[] pc) {
        String className = vp.getClass().getName();
        if (className.contains("canary") && className.contains("HashPalette")) {
            for (int i = 0; i < vp.getSize(); i++) {
                BlockState state = null;
                int blockId = -1;
                try { state = vp.valueFor(i); } catch (Exception e) {}
                if (state != null) {
                    blockId = blockCache.getOrDefault(state, -1);
                    if (blockId == -1) {
                        blockId = mapper.getIdForBlockState(state);
                        blockCache.put(state, blockId);
                    }
                }
                pc[i] = blockId;
            }
            return true;
        }
        return false;
    }
    private static void setupLocalPalette(Palette<BlockState> vp, Reference2IntOpenHashMap<BlockState> blockCache, Mapper mapper, int[] pc) {
        if (vp instanceof LinearPalette<BlockState>) {
            for (int i = 0; i < vp.getSize(); i++) {
                var state = vp.valueFor(i);
                int blockId = -1;
                if (state != null) {
                    blockId = blockCache.getOrDefault(state, -1);
                    if (blockId == -1) {
                        blockId = mapper.getIdForBlockState(state);
                        blockCache.put(state, blockId);
                    }
                }
                pc[i] = blockId;
            }
        } else if (vp instanceof HashMapPalette<BlockState> pal) {
            //var map = pal.map;
            //TODO: heavily optimize this by reading the map directly

            for (int i = 0; i < vp.getSize(); i++) {
                BlockState state = null;
                int blockId = -1;
                try { state = vp.valueFor(i); } catch (Exception e) {}
                if (state != null) {
                    blockId = blockCache.getOrDefault(state, -1);
                    if (blockId == -1) {
                        blockId = mapper.getIdForBlockState(state);
                        blockCache.put(state, blockId);
                    }
                }
                pc[i] = blockId;
            }

        } else if (vp instanceof SingleValuePalette<BlockState>) {
            int blockId = -1;
            var state = vp.valueFor(0);
            if (state != null) {
                blockId = blockCache.getOrDefault(state, -1);
                if (blockId == -1) {
                    blockId = mapper.getIdForBlockState(state);
                    blockCache.put(state, blockId);
                }
            }
            pc[0] = blockId;
        } else {
            // 尝试Lithium调色板
            boolean handled = LITHIUM_INSTALLED && setupLithiumLocalPallet(vp, blockCache, mapper, pc);
            if (!handled) {
                handled = CANARY_INSTALLED && setupCanaryLocalPallet(vp, blockCache, mapper, pc);
            }
            if (!handled) {
                handled = setupCanaryLocalPallet(vp, blockCache, mapper, pc);
            }
            // 实在处理不了算了喵
            if (!handled) {
                throw new IllegalStateException("Unknown palette type: " + vp + " (class: " + vp.getClass().getName() + ")");
            }
        }
    }

    private static int unpackLightNibble(byte[] packedLight, int index) {
        if (packedLight == null) {
            return 0;
        }
        int packed = packedLight[index >> 1] & 0xFF;
        return (index & 1) == 0 ? (packed & 0xF) : (packed >> 4);
    }

    private static byte sampleLight(ILightingSupplier lightSupplier, byte[] blockLight, byte[] skyLight, boolean useArrayLight, int index) {
        if (useArrayLight) {
            int block = unpackLightNibble(blockLight, index);
            int sky = unpackLightNibble(skyLight, index);
            return (byte) (sky | (block << 4));
        }
        if (lightSupplier == null) {
            return 0;
        }
        int x = index & 0xF;
        int y = (index >> 8) & 0xF;
        int z = (index >> 4) & 0xF;
        return lightSupplier.supply(x, y, z);
    }

    public static VoxelizedSection convert(VoxelizedSection section,
                                           Mapper stateMapper,
                                           PalettedContainer<BlockState> blockContainer,
                                           PalettedContainerRO<Holder<Biome>> biomeContainer,
                                           ILightingSupplier lightSupplier) {
        byte[] blockLight = lightSupplier == null ? null : lightSupplier.getBlockLight();
        byte[] skyLight = lightSupplier == null ? null : lightSupplier.getSkyLight();
        boolean useArrayLight = blockLight != null || skyLight != null;

        //Cheat by creating a local pallet then read the data directly
        var cache = THREAD_LOCAL.get();
        var blockCache = cache.getLocalMapping(stateMapper);

        var biomes = cache.biomeCache;
        var data = section.section;

        {
            int i = 0;
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 4; z++) {
                    for (int x = 0; x < 4; x++) {
                        biomes[i++] = stateMapper.getIdForBiome(biomeContainer.get(x, y, z));
                    }
                }
            }
        }

        if (blockContainer == null) {
            for (int i = 0; i <= 0xFFF; i++) {
                data[i] = Mapper.airWithLight(sampleLight(lightSupplier, blockLight, skyLight, useArrayLight, i));
            }
            section.lvl0NonAirCount = 0;
            return section;
        }

        var vp = blockContainer.data.palette;
        var pc = cache.getPaletteCache(vp.getSize());
        GlobalPalette<BlockState> bps = null;

        if (blockContainer.data.palette instanceof GlobalPalette<BlockState> _bps) {
            bps = _bps;
        } else {
            setupLocalPalette(vp, blockCache, stateMapper, pc);
        }

        int nonZeroCnt = 0;
        if (blockContainer.data.storage instanceof SimpleBitStorage bStor) {
            var bDat = bStor.getRaw();
            int iterPerLong = (64 / bStor.getBits()) - 1;
            int paletteSize = vp.getSize();

            int MSK = (1 << bStor.getBits()) - 1;
            int eBits = bStor.getBits();

            long sample = 0;
            int c = 0;
            int dec = 0;
            for (int i = 0; i <= 0xFFF; i++) {
                if (dec-- == 0) {
                    sample = bDat[c++];
                    dec = iterPerLong;
                }
                int bId;
                if (bps == null) {
                    int paletteIndex = (int) (sample & MSK);
                    if (paletteIndex >= paletteSize) {
                        paletteIndex = Math.max(0, paletteSize - 1);
                    }
                    bId = pc[paletteIndex];
                } else {
                    bId = stateMapper.getIdForBlockState(bps.valueFor((int) (sample&MSK)));
                }
                sample >>>= eBits;

                byte light = sampleLight(lightSupplier, blockLight, skyLight, useArrayLight, i);

                nonZeroCnt += (bId != 0)?1:0;
                data[i] = Mapper.composeMappingId(light, bId, biomes[ExpansionUtil.compress(i,0b1100_1100_1100)]);
            }
        } else {
            if (!(blockContainer.data.storage instanceof ZeroBitStorage)) {
                throw new IllegalStateException();
            }
            int bId = pc[0];
            if (bId == 0) {//Its air
                for (int i = 0; i <= 0xFFF; i++) {
                    data[i] = Mapper.airWithLight(sampleLight(lightSupplier, blockLight, skyLight, useArrayLight, i));
                }
            } else {
                nonZeroCnt = 4096;
                for (int i = 0; i <= 0xFFF; i++) {
                    byte light = sampleLight(lightSupplier, blockLight, skyLight, useArrayLight, i);
                    data[i] = Mapper.composeMappingId(light, bId, biomes[ExpansionUtil.compress(i,0b1100_1100_1100)]);
                }
            }
        }
        section.lvl0NonAirCount = nonZeroCnt;
        return section;
    }

    //Support for other mods etc that use this entry point
    @Deprecated(forRemoval = true)
    public static void mipSection(VoxelizedSection section, Mapper mapper) {
        WorldVoxilizedSectionMipper.mipSection(section, mapper);
    }
}
