package garden.hestia.surveyor_surveyor;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

public class SurveyorSurveyor {
    public static final int UINT_OFFSET = 127;
    static final boolean BIOME_WATER = false;
    static final int WATER_MAP_COLOR = 0x4040ff;

    public static void main(String[] args) throws IOException {
        String filename = args[0];
        int heightLimit = Integer.parseInt(args[1]);
        File file = new File(filename);
        CompoundTag nbt = NBTIO.readFile(file, true, false);
        BufferedImage combinedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Map<Integer, BufferedImage> seenLayers = new HashMap<>();
        Map<Integer, Layer> layerData = new HashMap<>();
        int[] blockColors = (int[]) nbt.get("blockColors").getValue();
        int[] biomeWater = (int[]) nbt.get("biomeWater").getValue();
        CompoundTag chunksCompound = nbt.get("chunks");
        for (String worldChunkPosString : chunksCompound.keySet()) {
            int worldChunkX = Integer.parseInt(worldChunkPosString.split(",")[0]);
            int worldChunkZ = Integer.parseInt(worldChunkPosString.split(",")[1]);
            int regionChunkX = worldChunkX & 31; //mod 32
            int regionChunkZ = worldChunkZ & 31;
            CompoundTag chunkCompound = chunksCompound.get(worldChunkPosString);
            CompoundTag layersCompound = chunkCompound.get("layers");
            for (String layer : layersCompound.keySet().stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList()) {
                layerData.putIfAbsent(Integer.parseInt(layer), new Layer(512, Integer.parseInt(layer)));
                CompoundTag layerCompound = layersCompound.get(layer);
                int[] depth = readUInts(layerCompound.get("depth"), -1);
                int[] block = unmaskUInts(readUInts(layerCompound.get("block"), 0), depth);
                int[] biome = unmaskUInts(readUInts(layerCompound.get("biome"), 0), depth);
                int[] light = unmaskUInts(readUInts(layerCompound.get("light"), 0), depth);
                int[] water = unmaskUInts(readUInts(layerCompound.get("water"), 0), depth);
                layerData.get(Integer.parseInt(layer)).putChunk(regionChunkX, regionChunkZ, depth, block, biome, light, water);
            }
        }
        for (Integer layerHeight : layerData.keySet().stream().sorted(Comparator.comparingInt(i -> i)).toList()) {
            seenLayers.put(layerHeight, new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB));
            Layer layer = layerData.get(layerHeight);
            int[][] colors = layer.getARGB(blockColors, biomeWater);
            for (int x = 0; x < colors.length; x++) {
                for (int z = 0; z < colors[x].length; z++) {
                    if (colors[x][z] == 0) continue;
                    combinedImage.setRGB(x, z, colors[x][z]);
                    seenLayers.get(layerHeight).setRGB(x, z, colors[x][z]);
                }
            }
        }
        seenLayers.forEach((l, bi) -> {
            try {
                ImageIO.write(bi, "png", new File(file.getPath().replace(".dat", "-" + l + ".png")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        ImageIO.write(combinedImage, "png", new File(file.getPath().replace(".dat", "-combined.png")));
        System.out.println(Arrays.toString(blockColors));
    }

    public static int[] unmaskUInts(int[] array, int[] mask) {
        if (array.length == mask.length) return array;
        int[] newArray = new int[mask.length];
        int maskedIndex = 0;
        for (int i = 0; i < mask.length; i++) {
            if (mask[i] != -1) {
                newArray[i] = array[maskedIndex];
                maskedIndex++;
            }
        }
        return newArray;
    }

    public static int[] readUInts(Tag nbt, int defaultValue) {
        if (nbt == null) return Collections.nCopies(256, defaultValue).stream().mapToInt(i -> i).toArray();
        if (nbt.getClass().equals(ByteTag.class)) {
            return Collections.nCopies(256, ((ByteTag) nbt).getValue().intValue() + UINT_OFFSET).stream().mapToInt(i -> i).toArray();
        } else if (nbt.getClass().equals(ByteArrayTag.class)) {
            byte[] bytes = ((ByteArrayTag) nbt).getValue();
            return IntStream.range(0, bytes.length).map(i -> bytes[i] + UINT_OFFSET).toArray();
        } else if (nbt.getClass().equals(IntTag.class)) {
            return Collections.nCopies(256, ((IntTag) nbt).getValue()).stream().mapToInt(i -> i).toArray();
        } else if (nbt.getClass().equals(IntArrayTag.class)) {
            return ((IntArrayTag) nbt).getValue();
        }
        throw new IllegalStateException("Unexpected value: " + nbt.getClass());
    }

    public static int getRenderColor(Brightness brightness, int color) {
        int i = brightness.brightness;
        int r = (color >> 16 & 0xFF) * i / 255;
        int g = (color >> 8 & 0xFF) * i / 255;
        int b = (color & 0xFF) * i / 255;
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    /**
     * @author ampflower
     */
    static Brightness getBrightnessFromDepth(int depth, int x, int z) {
        if (depth == 6) { // Emulate floating point error in vanilla code
            depth = 7;
        }
        int ditheredDepth = depth + (((x ^ z) & 1) << 1);
        if (ditheredDepth > 8) {
            return Brightness.LOW;
        } else if (ditheredDepth >= 4) {
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
}