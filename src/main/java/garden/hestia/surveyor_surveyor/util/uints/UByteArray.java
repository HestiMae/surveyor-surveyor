package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.*;

public record UByteArray(byte[] value) implements ArrayUInts {

    public static UInts ofInts(int[] ints) {
        byte[] value = new byte[ints.length];
        for (int i = 0; i < ints.length; i++) {
            value[i] = (byte) ints[i];
        }
        return new UByteArray(value);
    }

    public static UInts fromNbt(ByteArrayTag nbt, int cardinality) {
        byte[] value = nbt.getValue();
        return value.length == cardinality ? new UByteArray(value) : new UNibbleArray(value);
    }

    @Override
    public int get(int i) {
        return value[i] & BYTE_MASK;
    }

}
