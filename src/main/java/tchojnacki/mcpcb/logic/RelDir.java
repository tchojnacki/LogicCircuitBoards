package tchojnacki.mcpcb.logic;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.Direction;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An enum representing the relative direction of two absolute directions.
 * For instance, WEST is LEFT of NORTH.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public enum RelDir {
    FRONT,
    RIGHT,
    BACK,
    LEFT;

    /**
     * Serialize a list of {@code RelDir} into a byte array.
     * Useful for NBT tag serialization.
     *
     * @param list list of relative directions to get serialized
     * @return resulting byte array
     */
    public static byte[] dirListToBytes(List<RelDir> list) {
        // TODO: Check for nulls within the list

        byte[] bytes = new byte[list.size()];
        for (int i = 0; i < list.size(); i++) {
            bytes[i] = (byte) Arrays.asList(RelDir.values()).indexOf(list.get(i));
        }
        return bytes;
    }

    /**
     * Read an {@code ArrayList} of relative directions from their serialized byte array form.
     * Incorrect values get skipped.
     *
     * @param bytes serialized relative direction list
     * @return list of relative dirs deserialized from the byte array
     */
    public static ArrayList<RelDir> bytesToDirList(byte[] bytes) {
        // TODO: Possibly use an immutable list

        ArrayList<RelDir> list = new ArrayList<>();
        for (byte b : bytes) {
            if (0 <= b && b <= 3) {
                list.add(RelDir.values()[b]);
            }
        }

        return list;
    }

    public TranslationTextComponent translationComponent() {
        String key = "util.mcpcb.direction.";

        switch (this) {
            case FRONT:
                key += "front";
                break;
            case RIGHT:
                key += "right";
                break;
            case BACK:
                key += "back";
                break;
            case LEFT:
                key += "left";
                break;
            default:
                throw new AssertionError("Illegal relative direction.");
        }

        return new TranslationTextComponent(key);
    }

    /**
     * Use built in {@link Direction} methods to get an absolute direction offset
     * from another absolute direction by a given relative direction.
     *
     * @param initialDirection direction from which we are offsetting
     * @return direction offset from {@code initialDirection} by value of this enum
     */
    public Direction offsetFrom(Direction initialDirection) {
        switch (this) {
            case FRONT:
                return initialDirection;
            case RIGHT:
                return initialDirection.getClockWise();
            case BACK:
                return initialDirection.getOpposite();
            case LEFT:
                return initialDirection.getCounterClockWise();
            default:
                throw new AssertionError("Illegal relative direction.");
        }
    }

    /**
     * Gets a relative direction placed clockwise from the current relative direction.
     *
     * @return clockwise transform of this enum value
     */
    public RelDir getClockWise() {
        switch (this) {
            case FRONT:
                return RIGHT;
            case RIGHT:
                return BACK;
            case BACK:
                return LEFT;
            case LEFT:
                return FRONT;
            default:
                throw new AssertionError("Illegal relative direction.");
        }
    }

    /**
     * Get the relative direction from {@code from} to {@code to}.
     *
     * @param from initial direction
     * @param to   target direction
     * @return offset between {@code from} and {@code to}
     */
    public static RelDir getOffset(Direction from, Direction to) {
        Direction currentAbs = from;
        RelDir currentRel = FRONT;

        while (currentAbs != to) {
            currentAbs = currentAbs.getClockWise();
            currentRel = currentRel.getClockWise();
        }

        return currentRel;
    }
}
