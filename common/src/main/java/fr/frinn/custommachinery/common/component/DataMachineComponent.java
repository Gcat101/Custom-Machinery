package fr.frinn.custommachinery.common.component;

import fr.frinn.custommachinery.api.component.ComponentIOMode;
import fr.frinn.custommachinery.api.component.IMachineComponentManager;
import fr.frinn.custommachinery.api.component.ISerializableComponent;
import fr.frinn.custommachinery.api.component.MachineComponentType;
import fr.frinn.custommachinery.common.init.Registration;
import fr.frinn.custommachinery.impl.component.AbstractMachineComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

public class DataMachineComponent extends AbstractMachineComponent implements ISerializableComponent {

    private CompoundTag nbt = new CompoundTag();

    public DataMachineComponent(IMachineComponentManager manager) {
        super(manager, ComponentIOMode.NONE);
    }

    public CompoundTag getData() {
        return this.nbt;
    }

    public void setData(CompoundTag nbt) {
        this.nbt = nbt;
    }

    @Override
    public MachineComponentType<DataMachineComponent> getType() {
        return Registration.DATA_MACHINE_COMPONENT.get();
    }

    @Override
    public void serialize(CompoundTag nbt) {
        nbt.put("data_component", this.nbt);
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        if(nbt.contains("data_component", Tag.TAG_COMPOUND))
            this.nbt = nbt.getCompound("data_component");
    }
}
