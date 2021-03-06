package buildcraft.transport;

import net.minecraft.util.ResourceLocation;

import buildcraft.api.transport.neptune.PipeAPI;
import buildcraft.api.transport.neptune.PluggableDefinition;
import buildcraft.api.transport.neptune.PluggableDefinition.IPluggableCreator;
import buildcraft.api.transport.neptune.PluggableDefinition.IPluggableNbtReader;
import buildcraft.api.transport.neptune.PluggableDefinition.IPluggableNetLoader;

import buildcraft.transport.plug.PluggableBlocker;
import buildcraft.transport.plug.PluggableGate;
import buildcraft.transport.plug.PluggablePulsar;

public class BCTransportPlugs {

    public static PluggableDefinition blocker;
    public static PluggableDefinition gate;
    public static PluggableDefinition pulsar;

    public static void preInit() {
        blocker = register("blocker", PluggableBlocker::new);
        gate = register("gate", PluggableGate::new, PluggableGate::new);
        pulsar = register("pulsar", PluggablePulsar::new, PluggablePulsar::new);
    }

    private static PluggableDefinition register(String name, IPluggableCreator creator) {
        return register(new PluggableDefinition(idFor(name), creator));
    }

    private static PluggableDefinition register(String name, IPluggableNbtReader reader, IPluggableNetLoader loader) {
        return register(new PluggableDefinition(idFor(name), reader, loader));
    }

    private static PluggableDefinition register(PluggableDefinition def) {
        // TODO: Add config for enabling/disabling
        PipeAPI.pluggableRegistry.registerPluggable(def);
        return def;
    }

    private static ResourceLocation idFor(String name) {
        return new ResourceLocation("buildcrafttransport", name);
    }

}
