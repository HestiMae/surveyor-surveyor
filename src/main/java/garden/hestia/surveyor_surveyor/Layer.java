package garden.hestia.surveyor_surveyor;

import static garden.hestia.surveyor_surveyor.SurveyorSurveyor.*;

public class Layer {
    public int[][] depth;
    public int[][] block;
    public int[][] biome;
    public int[][] light;
    public int[][] water;
    public final int y;

    public Layer(int size, int y) {
        depth = new int[size][size];
        block = new int[size][size];
        biome = new int[size][size];
        light = new int[size][size];
        water = new int[size][size];
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
        int[][] colors = new int[512][512];
        for (int x = 0; x < 512; x++) {
            for (int z = 0; z < 512; z++) {
                if (isEmpty(x, z)) continue;
                int color = blockColors[block[x][z]];
                SurveyorSurveyor.Brightness brightness = SurveyorSurveyor.Brightness.NORMAL;
                if (water[x][z] > 0) {
                    if (TRANSPARENT_WATER) {
                        color = blend(color, applyBrightnessRGB(Brightness.LOWEST, BIOME_WATER ? biomeWater[biome[x][z]] : WATER_MAP_COLOR), Math.min(0.5F + water[x][z] / 32.0F, 1.0F));
                    } else {
                        color = BIOME_WATER ? biomeWater[biome[x][z]] : WATER_MAP_COLOR;
                        brightness = getBrightnessFromDepth(water[x][z], x, z);

                    }
                } else {
                    if (BIOME_GRASS && GRASS_BLOCKS.contains(blocks[block[x][z]])) {
                        color = SurveyorSurveyor.tintColor(color, biomeGrass[biome[x][z]]);
                    } else if (BIOME_FOLIAGE && FOLIAGE_BLOCKS.contains(blocks[block[x][z]])) {
                        color = SurveyorSurveyor.tintColor(color, biomeFoliage[biome[x][z]]);
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
        for (int i = 0; i < 512; i++) {
            for (int j = 0; j < 512; j++) {
                if (this.depth[i][j] == -1 && layer.depth[i][j] != -1 && layer.depth[i][j] <= maxDepth && layer.depth[i][j] >= minDepth) {
                    this.depth[i][j] = layer.depth[i][j] + depthOffset;
                    this.block[i][j] = layer.block[i][j];
                    this.biome[i][j] = layer.biome[i][j];
                    this.light[i][j] = layer.light[i][j];
                    this.water[i][j] = layer.water[i][j];
                }
            }
        }
    }
}
