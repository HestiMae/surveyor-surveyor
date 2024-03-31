package garden.hestia.surveyor_surveyor.util.uints;

import com.github.steveice10.opennbt.tag.builtin.*;

public record UIntArray(int[] value) implements ArrayUInts {

    public static UInts ofInts(int[] ints) {
        return new UIntArray(ints);
    }

    public static UInts fromNbt(IntArrayTag nbt, int cardinality) {
        int[] value = nbt.getValue();
        return value.length == cardinality ? new UIntArray(value) : UShortArray.ofPacked(value, cardinality);
    }

    @Override
    public int get(int i) {
        return value[i];
    }

}
