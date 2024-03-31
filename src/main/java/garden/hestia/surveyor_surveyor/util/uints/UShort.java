package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.*;

public record UShort(short value) implements SingleUInts {

    public static UInts ofInt(int value) {
        return new UShort((short) value);
    }

    public static UInts fromNbt(ShortTag nbt) {
        return new UShort(nbt.getValue());
    }

    @Override
    public int get() {
        return value & SHORT_MASK;
    }

}
