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

    public static void main(String[] args) throws IOException {
        String filename = args[0];
        int heightLimit = Integer.parseInt(args[1]);
        File file = new File(filename);
        CompoundTag nbt = NBTIO.readFile(file, true, false);
        BufferedImage combinedImage = new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB);
        Map<String, BufferedImage> seenLayers = new HashMap<>();
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
                seenLayers.putIfAbsent(layer, new BufferedImage(512, 512, BufferedImage.TYPE_INT_ARGB));
                CompoundTag layerCompound = layersCompound.get(layer);
                int[] depth = readUInts(layerCompound.get("depth"), -1);
                int[] block = unmaskUInts(readUInts(layerCompound.get("block"), 0), depth);
                int[] biome = unmaskUInts(readUInts(layerCompound.get("biome"), 0), depth);
                int[] water = unmaskUInts(readUInts(layerCompound.get("water"), 0), depth);
                for (int i = 0; i < block.length; i++) {
                    if (depth[i] == -1) continue;
                    int chunkBlockX = i >> 4; // divide 16
                    int chunkBlockZ = i & 15; //mod 16
                    int imageX = regionChunkX * 16 + chunkBlockX;
                    int imageZ = regionChunkZ * 16 + chunkBlockZ;
                    int color = blockColors[block[i]];
                    if (water[i] > 0) color = biomeWater[biome[i]];
                    seenLayers.get(layer).setRGB(imageX, imageZ, color | 0xFF000000);
                    if (Integer.parseInt(layer) <= heightLimit)
                        combinedImage.setRGB(imageX, imageZ, color | 0xFF000000);
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
        int j = (color >> 16 & 0xFF) * i / 255;
        int k = (color >> 8 & 0xFF) * i / 255;
        int l = (color & 0xFF) * i / 255;
        return 0xFF000000 | l << 16 | k << 8 | j;
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