package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.ByteTag;

public record UByte(byte value) implements SingleUInts {
    public static UInts ofInt(int value) {
        return new UByte((byte) value);
    }

    public static UInts fromNbt(ByteTag nbt) {
        return new UByte(nbt.getValue());
    }

    @Override
    public int get() {
        return value & BYTE_MASK;
    }

}
