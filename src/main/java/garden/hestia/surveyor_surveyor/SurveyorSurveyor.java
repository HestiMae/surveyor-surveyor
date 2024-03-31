package garden.hestia.surveyor_surveyor;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;
import garden.hestia.surveyor_surveyor.util.ArrayUtil;
import garden.hestia.surveyor_surveyor.util.uints.UInt;
import garden.hestia.surveyor_surveyor.util.uints.UInts;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

public class SurveyorSurveyor {
    public static final int UINT_OFFSET = 128;
    static final boolean BIOME_WATER = true;
    static final boolean TRANSPARENT_WATER = true;
    static final int WATER_MAP_COLOR = 0x4040ff;
    static final int BIRCH_MAP_COLOR = 0x80a755;
    static final int SPRUCE_MAP_COLOR = 0x619961;
    static final int MANGROVE_MAP_COLOR = 0x92c648;
    static final int STONE_MAP_COLOR = 0x808080;
    static final int WATER_TEXTURE_COLOR = 0x909090;
    static final int FOLIAGE_TEXTURE_COLOR = 0x949594;
    static final int GRASS_TEXTURE_COLOR = 0x949494;
    static final int GRASS_BLOCK_TEXTURE_COLOR = 0x959595;
    static final boolean BIOME_GRASS = true;
    static final boolean BIOME_FOLIAGE = true;
    static final int[] DEFAULT_ARRAY = ArrayUtil.ofSingle(0, 256);
    static final List<String> FOLIAGE_BLOCKS = List.of("minecraft:oak_leaves", "minecraft:jungle_leaves",
            "minecraft:acacia_leaves", "minecraft:dark_oak_leaves", "minecraft:mangrove_leaves", "minecraft:vine");
    static final List<String> GRASS_BLOCKS = List.of("minecraft:grass", "minecraft:tall_grass",
            "minecraft:fern", "minecraft:potted_fern", "minecraft:large_fern", "minecraft:sugar_cane");
    static final List<String> GRASS_BLOCK_BLOCKS = List.of("minecraft:grass_block");
    static final List<String> STONE_BLOCKS = List.of("minecraft:stone", "minecraft:andesite");

    public static void main(String[] args) throws IOException {
        File path = new File(args[0]);
        int heightLimit = Integer.parseInt(args[1]);
        List<File> files = new ArrayList<>();
        if (path.isDirectory()) {
            files.addAll(Arrays.stream(Objects.requireNonNull(path.listFiles(f -> f.getName().endsWith(".dat")))).toList());
        } else {
            files.add(path);
        }
        Map<RegionPos, CompoundTag> regionNbt = new HashMap<>();
        for (File file : files) {
            String[] split = file.getName().split("\\.");
            if (split.length == 4 && split[0].equals("c")) {
                try {
                    int regionX = Integer.parseInt(split[1]);
                    int regionZ = Integer.parseInt(split[2]);
                    CompoundTag nbt = NBTIO.readFile(file, true, false);
                    regionNbt.put(new RegionPos(regionX, regionZ), nbt);
                } catch (NumberFormatException ignored) {

                }
            }
        }
        int minRegionX = regionNbt.keySet().stream().mapToInt(RegionPos::x).min().orElseThrow();
        int maxRegionX = regionNbt.keySet().stream().mapToInt(RegionPos::x).max().orElseThrow();
        int minRegionZ = regionNbt.keySet().stream().mapToInt(RegionPos::z).min().orElseThrow();
        int maxRegionZ = regionNbt.keySet().stream().mapToInt(RegionPos::z).max().orElseThrow();

        int imageWidth = (maxRegionX + 1 - minRegionX) * 512;
        int imageHeight = (maxRegionZ + 1 - minRegionZ) * 512;
        Map<Integer, LayerImage> layers = new HashMap<>();
        List<Integer> blockColors = new ArrayList<>();
        List<Integer> biomeWater = new ArrayList<>();
        List<Integer> biomeGrass = new ArrayList<>();
        List<Integer> biomeFoliage = new ArrayList<>();
        List<String> blocks = new ArrayList<>();
        List<String> biomes = new ArrayList<>();
        for (RegionPos regionPos : regionNbt.keySet()) {
            CompoundTag nbt = regionNbt.get(regionPos);

            String[] regionBlocks = ((List<Tag>) nbt.get("blocks").getValue()).stream().map(tag -> (String) tag.getValue()).toArray(String[]::new);
            int[] regionBlockColors = (int[]) nbt.get("blockColors").getValue();
            for (int i = 0; i < regionBlocks.length; i++) {
                if (!blocks.contains(regionBlocks[i])) {
                    blocks.add(regionBlocks[i]);
                    blockColors.add(regionBlockColors[i]);
                }
            }

            String[] regionBiomes = ((List<Tag>) nbt.get("biomes").getValue()).stream().map(tag -> (String) tag.getValue()).toArray(String[]::new);
            int[] regionBiomeWater = (int[]) nbt.get("biomeWater").getValue();
            int[] regionBiomeGrass = (int[]) nbt.get("biomeGrass").getValue();
            int[] regionBiomeFoliage = (int[]) nbt.get("biomeFoliage").getValue();
            for (int i = 0; i < regionBiomes.length; i++) {
                if (!biomes.contains(regionBiomes[i])) {
                    biomes.add(regionBiomes[i]);
                    biomeWater.add(regionBiomeWater[i]);
                    biomeGrass.add(regionBiomeGrass[i]);
                    biomeFoliage.add(regionBiomeFoliage[i]);
                }
            }

            CompoundTag chunksCompound = nbt.get("chunks");
            for (String worldChunkPosString : chunksCompound.keySet()) {
                int worldChunkX = Integer.parseInt(worldChunkPosString.split(",")[0]);
                int worldChunkZ = Integer.parseInt(worldChunkPosString.split(",")[1]);
                int regionChunkX = worldChunkX & 31; //mod 32
                int regionChunkZ = worldChunkZ & 31;
                CompoundTag chunkCompound = chunksCompound.get(worldChunkPosString);
                CompoundTag layersCompound = chunkCompound.get("layers");
                for (String layer : layersCompound.keySet()) {
                    layers.computeIfAbsent(Integer.parseInt(layer), k -> new LayerImage(new Layer(imageWidth, imageHeight, Integer.parseInt(layer)), new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)));
                    CompoundTag layerCompound = layersCompound.get(layer);
                    if (!layerCompound.contains("found")) continue;
                    BitSet found = new BitSet(256);
                    found.or(BitSet.valueOf(((LongArrayTag) layerCompound.get("found")).getValue()));
                    int cardinality = found.cardinality();
                    int[] depth = layerCompound.contains("depth") ? UInts.readNbt(layerCompound.get("depth"), cardinality).getUnmasked(found) : DEFAULT_ARRAY;
                    int[] block = Objects.requireNonNullElse(UInts.remap(UInts.readNbt(layerCompound.get("block"), cardinality), (i) -> blocks.indexOf(regionBlocks[i]), 0, cardinality), new UInt(0)).getUnmasked(found);
                    int[] biome = Objects.requireNonNullElse(UInts.remap(UInts.readNbt(layerCompound.get("biome"), cardinality), (i) -> biomes.indexOf(regionBiomes[i]), 0, cardinality), new UInt(0)).getUnmasked(found);
                    int[] light = layerCompound.contains("light") ? UInts.readNbt(layerCompound.get("light"), cardinality).getUnmasked(found) : DEFAULT_ARRAY;
                    int[] water = layerCompound.contains("water") ? UInts.readNbt(layerCompound.get("water"), cardinality).getUnmasked(found) : DEFAULT_ARRAY;
                    layers.get(Integer.parseInt(layer)).layer().putChunk((regionPos.x - minRegionX) * 32 + regionChunkX, (regionPos.z - minRegionZ) * 32 + regionChunkZ, found, depth, block, biome, light, water);
                }
            }
        }

        int[] blockColorsArray = blockColors.stream().mapToInt(i -> i).toArray();
        int[] biomeWaterArray = biomeWater.stream().mapToInt(i -> i).toArray();
        int[] biomeGrassArray = biomeGrass.stream().mapToInt(i -> i).toArray();
        int[] biomeFoliageArray = biomeFoliage.stream().mapToInt(i -> i).toArray();
        String[] blocksArray = blocks.toArray(new String[]{});

        List<Integer> sortedKeys = new ArrayList<>(layers.keySet().stream().sorted(Comparator.reverseOrder()).toList());
        Layer combinedLayer = new Layer(imageWidth, imageHeight, sortedKeys.get(0));
        for (Integer layer : sortedKeys) {
            combinedLayer.fillEmptyFloors(combinedLayer.y - layers.get(layer).layer.y, layers.get(layer).layer.y - heightLimit, Integer.MAX_VALUE, layers.get(layer).layer);
        }
        layers.put(null, new LayerImage(combinedLayer, new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB)));
        layers.forEach((height, layerImage) -> {
            Layer layer = layerImage.layer();
            int[][] colors = layer.getARGB(blockColorsArray, biomeWaterArray, biomeGrassArray, biomeFoliageArray, blocksArray);
            for (int x = 0; x < colors.length; x++) {
                for (int z = 0; z < colors[x].length; z++) {
                    if (colors[x][z] == 0) continue;
                    layerImage.image().setRGB(x, z, colors[x][z]);
                }
            }
            try {
                ImageIO.write(layerImage.image(), "png", (path.isDirectory() ? path : path.getParentFile()).toPath().resolve("surveyor-%s.png".formatted(Objects.toString(height, "combined"))).toFile());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static int applyBrightnessRGB(Brightness brightness, int color) {
        int i = brightness.brightness;
        int r = (color >> 16 & 0xFF) * i / 255;
        int g = (color >> 8 & 0xFF) * i / 255;
        int b = (color & 0xFF) * i / 255;
        return r << 16 | g << 8 | b;
    }

    public static int tint(int base, int tint) {
        int a1 = (base >>> 24);
        int r1 = ((base & 0xff0000) >> 16);
        int g1 = ((base & 0xff00) >> 8);
        int b1 = (base & 0xff);

        int a2 = (tint >>> 24);
        int r2 = ((tint & 0xff0000) >> 16);
        int g2 = ((tint & 0xff00) >> 8);
        int b2 = (tint & 0xff);

        int a = (a1 * a2) / 256;
        int r = (r1 * r2) / 256;
        int g = (g1 * g2) / 256;
        int b = (b1 * b2) / 256;

        return a << 24 | r << 16 | g << 8 | b;
    }

    static int blend(int c1, int c2, float ratio) {
        float iRatio = 1.0f - ratio;

        int a1 = (c1 >>> 24);
        int r1 = ((c1 & 0xff0000) >> 16);
        int g1 = ((c1 & 0xff00) >> 8);
        int b1 = (c1 & 0xff);

        int a2 = (c2 >>> 24);
        int r2 = ((c2 & 0xff0000) >> 16);
        int g2 = ((c2 & 0xff00) >> 8);
        int b2 = (c2 & 0xff);

        int a = (int) ((a1 * iRatio) + (a2 * ratio));
        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return a << 24 | r << 16 | g << 8 | b;
    }

    /**
     * @author ampflower
     */
    static Brightness getBrightnessFromDepth(int depth, int x, int z) {
        if (depth == 7) { // Emulate floating point error in vanilla code
            depth = 8;
        }
        int ditheredDepth = depth + (((x ^ z) & 1) << 1);
        if (ditheredDepth > 9) {
            return Brightness.LOW;
        } else if (ditheredDepth >= 5) {
            return Brightness.NORMAL;
        } else {
            return Brightness.HIGH;
        }
    }

    public enum Brightness {
        LOW(180),
        NORMAL(220),
        HIGH(255),
        LOWEST(135);

        public final int brightness;

        Brightness(int brightness) {
            this.brightness = brightness;

        }
    }

    record RegionPos(int x, int z) {
    }

    record LayerImage(Layer layer, BufferedImage image) {
    }
}