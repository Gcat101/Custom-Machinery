package fr.frinn.custommachinery.common.machine;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import fr.frinn.custommachinery.api.codec.NamedCodec;
import fr.frinn.custommachinery.api.machine.MachineAppearanceProperty;
import fr.frinn.custommachinery.api.machine.MachineStatus;
import fr.frinn.custommachinery.common.init.Registration;

import java.util.Map;

public class MachineAppearanceManager {

    public static final NamedCodec<MachineAppearanceManager> CODEC = NamedCodec.record(builder ->
            builder.group(
                    MachineAppearance.CODEC.forGetter(manager -> manager.defaultProperties),
                    MachineAppearance.CODEC.optionalFieldOf("idle", Maps.newHashMap()).forGetter(manager -> manager.idle.getProperties()),
                    MachineAppearance.CODEC.optionalFieldOf("running", Maps.newHashMap()).forGetter(manager -> manager.running.getProperties()),
                    MachineAppearance.CODEC.optionalFieldOf("errored", Maps.newHashMap()).forGetter(manager -> manager.errored.getProperties()),
                    MachineAppearance.CODEC.optionalFieldOf("paused", Maps.newHashMap()).forGetter(manager -> manager.paused.getProperties())
            ).apply(builder, (defaults, idle, running, errored, paused) -> {
                MachineAppearance idleAppearance = buildAppearance(defaults, idle);
                MachineAppearance runningAppearance = buildAppearance(defaults, running);
                MachineAppearance erroredAppearance = buildAppearance(defaults, errored);
                MachineAppearance pausedAppearance = buildAppearance(defaults, paused);
                return new MachineAppearanceManager(defaults, idleAppearance, runningAppearance, erroredAppearance, pausedAppearance);
            }),
            "Machine appearance"
    );

    private final Map<MachineAppearanceProperty<?>, Object> defaultProperties;
    private final MachineAppearance idle;
    private final MachineAppearance running;
    private final MachineAppearance errored;
    private final MachineAppearance paused;

    public MachineAppearanceManager(Map<MachineAppearanceProperty<?>, Object> defaultProperties, MachineAppearance idle, MachineAppearance running, MachineAppearance errored, MachineAppearance paused) {
        this.defaultProperties = defaultProperties;
        this.idle = idle;
        this.running = running;
        this.errored = errored;
        this.paused = paused;
    }

    public MachineAppearance getAppearance(MachineStatus status) {
        switch (status) {
            case IDLE:
                return this.idle;
            case RUNNING:
                return this.running;
            case ERRORED:
                return this.errored;
            case PAUSED:
                return this.paused;
        }
        throw new IllegalArgumentException("Invalid machine status: " + status);
    }

    private static MachineAppearance buildAppearance(Map<MachineAppearanceProperty<?>, Object> defaults, Map<MachineAppearanceProperty<?>, Object> specifics) {
        ImmutableMap.Builder<MachineAppearanceProperty<?>, Object> properties = ImmutableMap.builder();
        for(MachineAppearanceProperty<?> property : Registration.APPEARANCE_PROPERTY_REGISTRY) {
            Object value = specifics.get(property);
            if(value == null || value == property.getDefaultValue())
                properties.put(property, defaults.get(property));
            else
                properties.put(property, value);
        }
        return new MachineAppearance(properties.build());
    }
}
