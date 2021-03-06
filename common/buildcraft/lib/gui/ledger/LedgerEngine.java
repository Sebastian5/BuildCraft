package buildcraft.lib.gui.ledger;

import buildcraft.lib.client.sprite.ISprite;
import buildcraft.lib.client.sprite.LibSprites;
import buildcraft.lib.engine.TileEngineBase_BC8;
import buildcraft.lib.gui.GuiIcon;
import buildcraft.lib.misc.LocaleUtil;

public class LedgerEngine extends Ledger_Neptune {
    private static final int OVERLAY_COLOUR = 0xFF_D4_6C_1F;// 0xFF_FF_55_11;// TEMP!
    private static final int HEADER_COLOUR = 0xFF_E1_C9_2F;
    private static final int SUB_HEADER_COLOUR = 0xFF_AA_AF_b8;
    private static final int TEXT_COLOUR = 0xFF_00_00_00;

    public final TileEngineBase_BC8 engine;

    public LedgerEngine(LedgerManager_Neptune manager, TileEngineBase_BC8 engine) {
        super(manager);
        this.engine = engine;
        this.title = "gui.power";

        appendText(LocaleUtil.localize("gui.currentOutput") + ":", SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> LocaleUtil.localizeMjFlow(engine.currentOutput), TEXT_COLOUR);
        appendText(LocaleUtil.localize("gui.stored") + ":", SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> LocaleUtil.localizeMj(engine.getEnergyStored()), TEXT_COLOUR);
        appendText(LocaleUtil.localize("gui.heat") + ":", SUB_HEADER_COLOUR).setDropShadow(true);
        appendText(() -> LocaleUtil.localizeHeat(engine.getHeat()), TEXT_COLOUR);
        calculateMaxSize();
    }

    @Override
    public int getColour() {
        return OVERLAY_COLOUR;
    }

    @Override
    public int getTitleColour() {
        return HEADER_COLOUR;
    }

    @Override
    protected void drawIcon(int x, int y) {
        ISprite sprite;
        switch (engine.getPowerStage()) {
            case OVERHEAT:
                sprite = LibSprites.ENGINE_OVERHEAT;
                break;
            case RED:
            case YELLOW:
                sprite = LibSprites.ENGINE_WARM;
                break;
            default:
                sprite = engine.isEngineOn() ? LibSprites.ENGINE_ACTIVE : LibSprites.ENGINE_INACTIVE;
        }
        GuiIcon.draw(sprite, x, y, x + 16, y + 16);
    }
}
