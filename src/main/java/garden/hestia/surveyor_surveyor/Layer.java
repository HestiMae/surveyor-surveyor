package garden.hestia.surveyor_surveyor;

import java.util.Arrays;

import static garden.hestia.surveyor_surveyor.SurveyorSurveyor.*;

public class Layer {
    public int[][] depth;
    public int[][] block;
    public int[][] biome;
    public int[][] light;
    public int[][] water;
    public final int width;
    public final int height;
    public final int y;

    public Layer(int width, int height, int y) {
        depth = new int[width][height];
        block = new int[width][height];
        biome = new int[width][height];
        light = new int[width][height];
        water = new int[width][height];
        this.width = width;
        this.height = height;
        this.y = y;
    }

    public void putChunk(int chunkX, int chunkZ, int[] cDepth, int[] cBlock, int[] cBiome, int[] cLight, int[] cWater) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                depth[chunkX * 16 + x][chunkZ * 16 + z] = cDepth[x * 16 + z];
                block[chunkX * 16 + x][chunkZ * 16 + z] = cBlock[x * 16 + z];
                biome[chunkX * 16 + x][chunkZ * 16 + z] = cBiome[x * 16 + z];
                light[chunkX * 16 + x][chunkZ * 16 + z] = cLight[x * 16 + z];
                water[chunkX * 16 + x][chunkZ * 16 + z] = cWater[x * 16 + z];
            }
        }
    }

    boolean isEmpty(int x, int z) {
        return depth[x][z] == -1;
    }

    int getHeight(int x, int z) {
        return y - depth[x][z];
    }

    int[][] getARGB(int[] blockColors, int[] biomeWater, int[] biomeGrass, int[] biomeFoliage, String[] blocks) {
        int[][] colors = new int[width][height];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (isEmpty(x, z)) continue;
                int color = blockColors[block[x][z]];

                SurveyorSurveyor.Brightness brightness = SurveyorSurveyor.Brightness.NORMAL;
                if (water[x][z] > 0) {
                    int waterColor = BIOME_WATER ? tint(WATER_TEXTURE_COLOR, biomeWater[biome[x][z]]) : applyBrightnessRGB(Brightness.LOWEST, WATER_MAP_COLOR);
                    if (TRANSPARENT_WATER) {
                        color = blend(color, waterColor, Math.min(0.7F + water[x][z] / 53.0F, 1.0F));
                    } else {
                        color = waterColor;
                        brightness = getBrightnessFromDepth(water[x][z], x, z);

                    }
                } else {
                    if (BIOME_GRASS && GRASS_BLOCKS.contains(blocks[block[x][z]])) {
                        color = SurveyorSurveyor.tint(GRASS_TEXTURE_COLOR, biomeGrass[biome[x][z]]);
                    } else if (BIOME_GRASS && GRASS_BLOCK_BLOCKS.contains(blocks[block[x][z]])) {
                        color = SurveyorSurveyor.tint(GRASS_BLOCK_TEXTURE_COLOR, biomeGrass[biome[x][z]]);
                    } else if (BIOME_FOLIAGE && FOLIAGE_BLOCKS.contains(blocks[block[x][z]])) {
                        color = SurveyorSurveyor.tint(FOLIAGE_TEXTURE_COLOR, biomeFoliage[biome[x][z]]);
                    } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:birch_leaves")) {
                        color = tint(FOLIAGE_TEXTURE_COLOR, BIRCH_MAP_COLOR);
                    } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:spruce_leaves")) {
                        color = tint(FOLIAGE_TEXTURE_COLOR, SPRUCE_MAP_COLOR);
                    } else if (BIOME_FOLIAGE && blocks[block[x][z]].equals("minecraft:mangrove_leaves")) {
                        color = tint(FOLIAGE_TEXTURE_COLOR, MANGROVE_MAP_COLOR);
                    } else if (STONE_BLOCKS.contains(blocks[block[x][z]])) {
                        color = STONE_MAP_COLOR;
                    }
                    if (z > 0) {
                        if (depth[x][z - 1] < depth[x][z]) brightness = SurveyorSurveyor.Brightness.LOW;
                        if (depth[x][z - 1] > depth[x][z]) brightness = SurveyorSurveyor.Brightness.HIGH;
                    }
                }
                colors[x][z] = 0xFF000000 | applyBrightnessRGB(brightness, color);
            }
        }
        return colors;
    }

    public void fillEmptyFloors(int depthOffset, int minDepth, int maxDepth, Layer layer) {
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < height; z++) {
                if (this.depth[x][z] == -1 && layer.depth[x][z] != -1 && layer.depth[x][z] <= maxDepth && layer.depth[x][z] >= minDepth) {
                    this.depth[x][z] = layer.depth[x][z] + depthOffset;
                    this.block[x][z] = layer.block[x][z];
                    this.biome[x][z] = layer.biome[x][z];
                    this.light[x][z] = layer.light[x][z];
                    this.water[x][z] = layer.water[x][z];
                }
            }
        }
    }

    public Layer copy() {
        Layer newLayer = new Layer(width, height, y);
        newLayer.depth = Arrays.stream(depth).map(int[]::clone).toArray(int[][]::new);
        newLayer.block = Arrays.stream(block).map(int[]::clone).toArray(int[][]::new);
        newLayer.biome = Arrays.stream(biome).map(int[]::clone).toArray(int[][]::new);
        newLayer.light = Arrays.stream(light).map(int[]::clone).toArray(int[][]::new);
        newLayer.water = Arrays.stream(water).map(int[]::clone).toArray(int[][]::new);
        return newLayer;
    }
}
