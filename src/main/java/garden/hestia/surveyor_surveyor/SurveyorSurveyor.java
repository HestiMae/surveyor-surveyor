package garden.hestia.surveyor_surveyor;

import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

public class SurveyorSurveyor {
    public static final int UINT_OFFSET = 127;
    public static void main(String[] args) throws IOException {

        String filename = args[0];
        File file =  new File(filename);
        CompoundTag nbt = NBTIO.readFile(file, true, false);

        int[] blockColors = (int[]) nbt.get("blockColors").getValue();
        CompoundTag chunksCompound = nbt.get("chunks");
        BufferedImage image = new BufferedImage(512, 512, Image.SCALE_REPLICATE);
        for (String worldChunkPosString : chunksCompound.keySet()) {
            int worldChunkX = Integer.parseInt(worldChunkPosString.split(",")[0]);
            int worldChunkZ = Integer.parseInt(worldChunkPosString.split(",")[1]);
            int regionChunkX = worldChunkX & 31; //mod 32
            int regionChunkZ = worldChunkZ & 31;
            CompoundTag chunkCompound = chunksCompound.get(worldChunkPosString);
            CompoundTag layersCompound = chunkCompound.get("layers");
            // rewrite for multiple images, hardcode 0 atm
            CompoundTag layerCompound = layersCompound.get("62");
            int[] block = readUInts(layerCompound.get("block"));
            int[] height = readUInts(layerCompound.get("height"));
            for (int i = 0; i < block.length; i++) {
                if (height[i] == -1) continue;
                int chunkBlockX = i >> 4; // divide 16
                int chunkBlockZ = i & 15; //mod 16
                int imageX = regionChunkX * 16 + chunkBlockX;
                int imageZ = regionChunkZ * 16 + chunkBlockZ;
                int color = blockColors[block[i]];
                image.setRGB(imageX, imageZ, color);
            }
        }
        ImageIO.write(image, "png", new File(file.getPath().replace(".dat", ".png")));

        System.out.println(Arrays.toString(blockColors));
    }

    public static int[] readUInts(Tag nbt) {
        if (nbt == null) return Collections.nCopies(255, -1).stream().mapToInt(i -> i).toArray();
        if (nbt.getClass().equals(ByteTag.class)) {
            return Collections.nCopies(255, ((ByteTag) nbt).getValue().intValue() + UINT_OFFSET).stream().mapToInt(i -> i).toArray();
        } else if (nbt.getClass().equals(ByteArrayTag.class)) {
            byte[] bytes = ((ByteArrayTag) nbt).getValue();
            return IntStream.range(0, bytes.length).map(i -> bytes[i] + UINT_OFFSET).toArray();
        } else if (nbt.getClass().equals(IntTag.class)) {
            return Collections.nCopies(255, ((IntTag) nbt).getValue()).stream().mapToInt(i -> i).toArray();
        } else if (nbt.getClass().equals(IntArrayTag.class)) {
            return ((IntArrayTag) nbt).getValue();
        }
        throw new IllegalStateException("Unexpected value: " + nbt.getClass());
    }
}
