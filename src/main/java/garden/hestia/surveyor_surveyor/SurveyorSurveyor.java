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

        BufferedImage image = new BufferedImage(512, 512, Image.SCALE_REPLICATE);
        for (int i = 0; i < blockColors.length; i++) {
            image.setRGB(i, 0, blockColors[i]);
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
