package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.*;
import garden.hestia.surveyor_surveyor.util.ArrayUtil;
import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Function;

/**
 * A compressed representation of an int array for holding unsigned ints.
 * Keeps size down in memory, as well as in NBT and packets.
 * Designed to be paired with a bitset as a mask, so it can represent nullable values within a static-sized array.
 *
 * @author Sisby folk
 * @author Falkreon
 * @author Ampflower
 */
public interface UInts {
    int MAX_SHORT = Short.MAX_VALUE - Short.MIN_VALUE;
    int MAX_BYTE = Byte.MAX_VALUE - Byte.MIN_VALUE;
    int MAX_NIBBLE = MAX_BYTE >> 4;
    int NIBBLE_SIZE = Byte.SIZE >> 1;
    int SHORT_MASK = 0xFFFF;
    int BYTE_MASK = 0xFF;
    int NIBBLE_MASK = 0xF;

    int[] getUnmasked(BitSet mask);

    int get(int i);

    static UInts readNbt(Tag nbt, int cardinality) {
        if (nbt == null) return null;
        return switch (nbt) {
            case ByteTag tag -> UByte.fromNbt(tag);
            case ShortTag tag -> UShort.fromNbt(tag);
            case IntTag tag -> UInt.fromNbt(tag);
            case ByteArrayTag tag -> UByteArray.fromNbt(tag, cardinality);
            case IntArrayTag tag -> UIntArray.fromNbt(tag, cardinality);
            default -> throw new IllegalStateException("UIntArray encountered unexpected NBT type: " + nbt.getClass());
        };
    }

    static UInts fromUInts(int[] uints, int defaultValue) {
        return ArrayUtil.isSingle(uints) ? ofSingle(uints[0], defaultValue) : ofMany(uints);
    }

    static UInts ofMany(int[] uints) {
        int max = Arrays.stream(uints).max().orElseThrow();
        if (max <= MAX_NIBBLE) return UNibbleArray.ofInts(uints);
        if (max <= MAX_BYTE) return UByteArray.ofInts(uints);
        if (max <= MAX_SHORT) return UShortArray.ofInts(uints);
        return UIntArray.ofInts(uints);
    }

    static UInts ofSingle(int uint, int defaultValue) {
        if (uint == defaultValue) return null;
        if (uint <= MAX_BYTE) return UByte.ofInt(uint);
        if (uint <= MAX_SHORT) return UShort.ofInt(uint);
        return UInt.ofInt(uint);
    }

    UInts remap(Function<Integer, Integer> remapping, int defaultValue, int cardinality);

    static UInts remap(UInts input, Function<Integer, Integer> remapping, int defaultValue, int cardinality) {
        return (input == null ? new UInt(defaultValue) : input).remap(remapping, defaultValue, cardinality);
    }
}
