package tchojnacki.mcpcb.common.block;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraftforge.common.ToolType;
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
                        .harvestLevel(0) // no pickaxe required
                        .harvestTool(ToolType.PICKAXE)
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
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
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
