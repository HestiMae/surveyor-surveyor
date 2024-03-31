package garden.hestia.surveyor_surveyor.util.uints;


public record UNibbleArray(byte[] value) implements ArrayUInts {

    public static UInts ofInts(int[] value) {
        byte[] packed = new byte[value.length / 2 + (value.length & 1)];
        for (int i = 0; i < value.length; i+= 2) {
            packed[i / 2] |= (byte) (value[i] << NIBBLE_SIZE);
        }
        for (int i = 1; i < value.length; i+= 2) {
            packed[i / 2] |= (byte) value[i];
        }
        return new UNibbleArray(packed);
    }

    @Override
    public int get(int i) {
        return ((i & 1) == 0 ? value[i / 2] >>> NIBBLE_SIZE : value[i / 2]) & NIBBLE_MASK;
    }

}
