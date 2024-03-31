package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.*;

public record UInt(int value) implements SingleUInts {
    public static UInts ofInt(int value) {
        return new UInt(value);
    }

    public static UInts fromNbt(IntTag nbt) {
        return new UInt(nbt.getValue());
    }

    @Override
    public int get() {
        return value;
    }

}
