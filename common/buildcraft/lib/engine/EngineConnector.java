package buildcraft.lib.engine;

import buildcraft.api.mj.IMjConnector;
import buildcraft.api.mj.IMjReceiver;
import buildcraft.api.mj.IMjRedstoneReceiver;

public class EngineConnector implements IMjConnector {
    public final boolean redstoneOnly;

    public EngineConnector(boolean redstoneOnly) {
        this.redstoneOnly = redstoneOnly;
    }

    @Override
    public boolean canConnect(IMjConnector other) {
        if (other instanceof IMjReceiver) {
            if (redstoneOnly) {
                return other instanceof IMjRedstoneReceiver;
            }
            return true;
        }
        return true;
    }
}
