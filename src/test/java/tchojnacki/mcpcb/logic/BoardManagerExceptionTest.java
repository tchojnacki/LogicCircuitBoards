package tchojnacki.mcpcb.logic;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoardManagerExceptionTest {
    @Test
    void getTranslationTextComponent() {
        BoardManagerException exception = new BoardManagerException("board_states_broken");

        assertEquals("util.mcpcb.board_manager.error.board_states_broken", exception.getTranslationTextComponent().getKey());
    }
}