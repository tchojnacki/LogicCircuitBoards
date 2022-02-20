package tchojnacki.mcpcb.logic;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.TranslatableComponent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Simple exception providing translation for common breadboard errors.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoardManagerException extends RuntimeException {
    private final TranslatableComponent translationTextComponent;

    public BoardManagerException(String translationString) {
        translationTextComponent = new TranslatableComponent("util.mcpcb.board_manager.error." + translationString);
    }

    public TranslatableComponent getTranslationTextComponent() {
        return translationTextComponent;
    }
}
