package tchojnacki.mcpcb.logic;

import com.google.common.collect.ImmutableList;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.ParametersAreNonnullByDefault;
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
    public static ImmutableList<RelDir> bytesToDirList(byte[] bytes) {
        final var builder = ImmutableList.<RelDir>builder();
        for (byte b : bytes) {
            if (0 <= b && b <= 3) {
                builder.add(RelDir.values()[b]);
            }
        }

        return builder.build();
    }

    public TranslatableComponent translationComponent() {
        String key = "util.mcpcb.direction.";

        switch (this) {
            case FRONT -> key += "front";
            case RIGHT -> key += "right";
            case BACK -> key += "back";
            case LEFT -> key += "left";
            default -> throw new AssertionError("Illegal relative direction.");
        }

        return new TranslatableComponent(key);
    }

    /**
     * Use built in {@link Direction} methods to get an absolute direction offset
     * from another absolute direction by a given relative direction.
     *
     * @param initialDirection direction from which we are offsetting
     * @return direction offset from {@code initialDirection} by value of this enum
     */
    public Direction offsetFrom(Direction initialDirection) {
        return switch (this) {
            case FRONT -> initialDirection;
            case RIGHT -> initialDirection.getClockWise();
            case BACK -> initialDirection.getOpposite();
            case LEFT -> initialDirection.getCounterClockWise();
        };
    }

    /**
     * Gets a relative direction placed clockwise from the current relative direction.
     *
     * @return clockwise transform of this enum value
     */
    public RelDir getClockWise() {
        return switch (this) {
            case FRONT -> RIGHT;
            case RIGHT -> BACK;
            case BACK -> LEFT;
            case LEFT -> FRONT;
        };
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
