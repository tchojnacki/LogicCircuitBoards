package tchojnacki.mcpcb.common.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.PushReaction;
import tchojnacki.mcpcb.logic.BreadboardKindEnum;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BreadboardBlock extends Block {
    public final static String ID = "breadboard_block";

    public final static EnumProperty<BreadboardKindEnum> KIND = EnumProperty.create("kind", BreadboardKindEnum.class);

    public BreadboardBlock() {
        super(
                Properties
                        .of(Material.CLAY)
                        .strength(0.1f, 5) // fast breaking
                        .isRedstoneConductor((_blockState, _blockReader, _blockPos) -> false) // doesn't conduct power
        );

        this.registerDefaultState(
                this.defaultBlockState()
                        .setValue(KIND, BreadboardKindEnum.NORMAL)
        );
    }

    /**
     * Adds {@link #KIND} to block state's definition.
     *
     * @param builder state container builder
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND);
    }

    /**
     * Blocks pushing the block by pistons.
     *
     * @param _blockState unused
     * @return blocked piston push reaction
     */
    @SuppressWarnings("deprecation")
    @Override
    public PushReaction getPistonPushReaction(BlockState _blockState) {
        return PushReaction.BLOCK;
    }
}
