package garden.hestia.surveyor_surveyor;

import java.util.Arrays;
import java.util.BitSet;

import static garden.hestia.surveyor_surveyor.SurveyorSurveyor.*;

public class Layer {
    BitSet found;
    public int[][] depth;
    public int[][] block;
    public int[][] biome;
    public int[][] light;
    public int[][] water;
    public int[][] glint;
    public final int width;
    public final int height;
    public final int y;

    public Layer(int width, int height, int y) {
        found = new BitSet(width * height);
        depth = new int[width][height];
        block = new int[width][height];
        biome = new int[width][height];
        light = new int[width][height];
        water = new int[width][height];
        glint = new int[width][height];
        this.width = width;
        this.height = height;
        this.y = y;
    }

    public void putChunk(int chunkX, int chunkZ, BitSet cFound, int[] cDepth, int[] cBlock, int[] cBiome, int[] cLight, int[] cWater, int[] cGlint) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                found.set((chunkX * 16 + x) * height + chunkZ * 16 + z, cFound.get(x * 16 + z));
                depth[chunkX * 16 + x][chunkZ * 16 + z] = cDepth[x * 16 + z];
                block[chunkX * 16 + x][chunkZ * 16 + z] = cBlock[x * 16 + z];
                biome[chunkX * 16 + x][chunkZ * 16 + z] = cBiome[x * 16 + z];
                light[chunkX * 16 + x][chunkZ * 16 + z] = cLight[x * 16 + z];
                water[chunkX * 16 + x][chunkZ * 16 + z] = cWater[x * 16 + z];
                glint[chunkX * 16 + x][chunkZ * 16 + z] = cGlint[x * 16 + z];
            }
        }
    }

    boolean isEmpty(int x, int z) {
        return !found.get(x * height + z);
    }

    int getHeight(int x, int z) {
        return y - depth[x][z];
    }

    int[][] getARGB(int[] blockColors, int[] biomeWater, int[] biomeGrass, int[] biomeFoliage, String[] blocks) {
        int[][] colors = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (isEmpty(x, z)) continue;
                // base block colour
                int color;
                if (!TRANSPARENT_WATER && water[x][z] > 0) {
                    color = getWaterColor(biomeWater, x, z);
                } else if (BIOME_GRASS && GRASS_BLOCKS.contains(blocks[block[x][z]])) {
                    color = SurveyorSurveyor.tint(GRASS_TEXTURE_COLOR, getBlendedBiomeColor(biomeGrass, x, z, BLEND_RADIUS));
                } else if (BIOME_GRASS && GRASS_BLOCK_BLOCKS.contains(blocks[block[x][z]])) {
                    color = SurveyorSurveyor.tint(GRASS_BLOCK_TEXTURE_COLOR, getBlendedBiomeColor(biomeGrass, x, z, BLEND_RADIUS));
                } else if (BIOME_FOLIAGE && FOLIAGE_BLOCKS.contains(blocks[block[x][z]])) {
                    color = SurveyorSurveyor.tint(FOLIAGE_TEXTURE_COLOR, getBlendedBiomeColor(biomeFoliage, x, z, BLEND_RADIUS));
                } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:birch_leaves")) {
                    color = tint(FOLIAGE_TEXTURE_COLOR, BIRCH_MAP_COLOR);
                } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:spruce_leaves")) {
                    color = tint(FOLIAGE_TEXTURE_COLOR, SPRUCE_MAP_COLOR);
                } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:mangrove_leaves")) {
                    color = tint(FOLIAGE_TEXTURE_COLOR, MANGROVE_MAP_COLOR);
                } else if (STONE_BLOCKS.contains(blocks[block[x][z]])) {
                    color = STONE_MAP_COLOR;
                } else {
                    color = blockColors[block[x][z]];
                }

                if (TOPOGRAPHY) {
                    SurveyorSurveyor.Brightness brightness = SurveyorSurveyor.Brightness.NORMAL;
                    if (!TRANSPARENT_WATER && water[x][z] > 0) {
                        brightness = getBrightnessFromDepth(water[x][z], x, z);
                    }
                    if (z > 0) {
                        if (depth[x][z - 1] < depth[x][z]) brightness = SurveyorSurveyor.Brightness.LOW;
                        if (depth[x][z - 1] > depth[x][z]) brightness = SurveyorSurveyor.Brightness.HIGH;
                    }
                    color = applyBrightnessRGB(brightness, color);
                }

                if (LIGHTING) {
                    int blockLight = light[x][z];
                    int skyLight = Math.max(SurveyorSurveyor.SKY_LIGHT - water[x][z], 0);
                    color = tint(color, LIGHTMAP[skyLight][blockLight]);
                }
                if (water[x][z] > 0 && TRANSPARENT_WATER) {
                    int waterColor = getWaterColor(biomeWater, x, z);
                    if (LIGHTING) {
                        int blockLight = glint[x][z];
                        int skyLight = SurveyorSurveyor.SKY_LIGHT;
                        waterColor = tint(waterColor, LIGHTMAP[skyLight][blockLight]);
                    }
                    color = blend(color, waterColor, 0.6F);
                }
                colors[x][z] = 0xFF000000 | color;
            }
        }
        return colors;
    }

    private int getWaterColor(int[] biomeWater, int x, int z) {
        if (BLEND_RADIUS != 0)
        {
            return tint(WATER_TEXTURE_COLOR, getBlendedBiomeColor(biomeWater, x, z, BLEND_RADIUS));
        }
        return BIOME_WATER ? tint(WATER_TEXTURE_COLOR, biomeWater[biome[x][z]]) : applyBrightnessRGB(Brightness.LOWEST, WATER_MAP_COLOR);
    }

    private int getBlendedBiomeColor(int[] array , int x, int z, int radius)
    {
        if (radius == 0) return array[biome[x][z]];

        long r = 0;
        long g = 0;
        long b = 0;
        int num = 0;
        for (int i = x - radius; i < x + radius; i++) {
            for (int j = z - radius; j < z + radius; j++) {
                if (i < 0 || j < 0 || i >= biome.length || j >= biome[0].length || (x-i) * (x-i) + (z-j) * (z-j) > radius * radius) continue;
                r += ((array[biome[i][j]] >> 16) & 0xFF) * (array[biome[i][j]] >> 16) & 0xFF;
                g += ((array[biome[i][j]] >> 8) & 0xFF) * ((array[biome[i][j]] >> 8) & 0xFF);
                b += ((array[biome[i][j]]) & 0xFF) * ((array[biome[i][j]]) & 0xFF);
                num++;
            }
        }
        return (int) Math.sqrt((double) r /num) << 16 | (int) Math.sqrt((double) g /num) << 8 | (int) Math.sqrt((double) b /num);
    }

    public void fillEmptyFloors(int depthOffset, int minDepth, int maxDepth, Layer layer) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (this.isEmpty(x, z) && !layer.isEmpty(x, z) && layer.depth[x][z] <= maxDepth && layer.depth[x][z] >= minDepth) {
                    found.set(x * height + z);
                    this.depth[x][z] = layer.depth[x][z] + depthOffset;
                    this.block[x][z] = layer.block[x][z];
                    this.biome[x][z] = layer.biome[x][z];
                    this.light[x][z] = layer.light[x][z];
                    this.water[x][z] = layer.water[x][z];
                    this.glint[x][z] = layer.glint[x][z];
                }
            }
        }
    }

    public Layer copy() {
        Layer newLayer = new Layer(width, height, y);
        newLayer.found = (BitSet) this.found.clone();
        newLayer.depth = Arrays.stream(depth).map(int[]::clone).toArray(int[][]::new);
        newLayer.block = Arrays.stream(block).map(int[]::clone).toArray(int[][]::new);
        newLayer.biome = Arrays.stream(biome).map(int[]::clone).toArray(int[][]::new);
        newLayer.light = Arrays.stream(light).map(int[]::clone).toArray(int[][]::new);
        newLayer.water = Arrays.stream(water).map(int[]::clone).toArray(int[][]::new);
        newLayer.glint = Arrays.stream(glint).map(int[]::clone).toArray(int[][]::new);
        return newLayer;
    }
}
