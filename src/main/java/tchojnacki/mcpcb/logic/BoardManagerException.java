package tchojnacki.mcpcb.logic;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * Simple exception providing translation for common breadboard errors.
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class BoardManagerException extends RuntimeException {
    private final TranslationTextComponent translationTextComponent;

    public BoardManagerException(String translationString) {
        translationTextComponent = new TranslationTextComponent("util.mcpcb.board_manager.error." + translationString);
    }

    public TranslationTextComponent getTranslationTextComponent() {
        return translationTextComponent;
    }
}
