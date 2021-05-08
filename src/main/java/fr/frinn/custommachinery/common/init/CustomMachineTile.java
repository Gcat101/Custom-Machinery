package fr.frinn.custommachinery.common.init;

import fr.frinn.custommachinery.CustomMachinery;
import fr.frinn.custommachinery.client.ClientHandler;
import fr.frinn.custommachinery.client.DummyBakedModel;
import fr.frinn.custommachinery.common.crafting.CraftingManager;
import fr.frinn.custommachinery.common.data.CustomMachine;
import fr.frinn.custommachinery.common.data.MachineAppearance;
import fr.frinn.custommachinery.common.data.component.ICapabilityMachineComponent;
import fr.frinn.custommachinery.common.data.component.ITickableMachineComponent;
import fr.frinn.custommachinery.common.data.component.MachineComponentManager;
import fr.frinn.custommachinery.common.network.NetworkManager;
import fr.frinn.custommachinery.common.network.SUpdateCustomTileLightPacket;
import fr.frinn.custommachinery.common.network.sync.ISyncable;
import fr.frinn.custommachinery.common.network.sync.ISyncableStuff;
import fr.frinn.custommachinery.common.util.FuelManager;
import fr.frinn.custommachinery.common.util.SoundManager;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelDataMap;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.network.PacketDistributor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Consumer;

public class CustomMachineTile extends TileEntity implements ITickableTileEntity, INamedContainerProvider, ISyncableStuff {

    public static final ResourceLocation DUMMY = new ResourceLocation(CustomMachinery.MODID, "dummy");

    private ResourceLocation id = DUMMY;

    public CraftingManager craftingManager;
    public MachineComponentManager componentManager;
    public SoundManager soundManager;
    public FuelManager fuelManager = new FuelManager(this);

    public CustomMachineTile() {
        super(Registration.CUSTOM_MACHINE_TILE.get());
    }

    public ResourceLocation getId() {
        return id;
    }

    public void setId(ResourceLocation id) {
        this.id = id;
        this.craftingManager = new CraftingManager(this);
        this.componentManager = new MachineComponentManager(getMachine().getComponentTemplates(), this);
    }

    public CustomMachine getMachine() {
        CustomMachine machine = CustomMachinery.MACHINES.get(getId());
        if(machine != null)
            return machine;
        else
            return CustomMachine.DUMMY;
    }

    @Override
    public void tick() {
        if(this.world == null || this.componentManager == null || this.craftingManager == null)
            return;

        if(!this.world.isRemote()) {

            this.fuelManager.consume();
            this.componentManager.getTickableComponents().forEach(ITickableMachineComponent::tick);
            this.craftingManager.tick();

            if(this.needRefreshLightning())
                this.refreshLightning();

        } else {
            if(this.modelDataMap == null)
                this.setupModelData();
            if(this.soundManager == null)
                this.soundManager = new SoundManager(this.pos);
            if(getMachine().getAppearance().getSound() != MachineAppearance.DEFAULT_SOUND && !getMachine().getAppearance().getSound().getName().equals(this.soundManager.getSound()))
                this.soundManager.setSound(getMachine().getAppearance().getSound());

            if (this.craftingManager.getStatus() == CraftingManager.STATUS.RUNNING && !this.soundManager.isPlaying())
                this.soundManager.play();
            else if(this.craftingManager.getStatus() != CraftingManager.STATUS.RUNNING && this.soundManager.isPlaying())
                this.soundManager.stop();
        }
    }

    @Override
    protected void invalidateCaps() {
        super.invalidateCaps();
        if(this.componentManager != null)
            this.componentManager.getCapabilityComponents().forEach(ICapabilityMachineComponent::invalidateCapability);
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        for (ICapabilityMachineComponent component : this.componentManager.getCapabilityComponents()) {
            LazyOptional<T> capability = component.getCapability(cap, side);
            if(capability != LazyOptional.empty())
                return capability;
        }
        return LazyOptional.empty();
    }

    @ParametersAreNonnullByDefault
    @Override
    public CompoundNBT write(CompoundNBT nbt) {
        super.write(nbt);
        nbt.putString("machineID", this.id.toString());
        nbt.put("craftingManager", this.craftingManager.serializeNBT());
        nbt.put("fuelManager", this.fuelManager.serializeNBT());
        nbt.put("componentManager", this.componentManager.serializeNBT());
        return nbt;
    }

    @ParametersAreNonnullByDefault
    @Override
    public void read(BlockState state, CompoundNBT nbt) {
        super.read(state, nbt);
        if(nbt.contains("machineID", Constants.NBT.TAG_STRING) && getMachine() == CustomMachine.DUMMY)
            this.setId(new ResourceLocation(nbt.getString("machineID")));

        if(nbt.contains("craftingManager", Constants.NBT.TAG_COMPOUND))
            this.craftingManager.deserializeNBT(nbt.getCompound("craftingManager"));

        if(nbt.contains("fuelManager", Constants.NBT.TAG_COMPOUND))
            this.fuelManager.deserializeNBT(nbt.getCompound("fuelManager"));

        if(nbt.contains("componentManager", Constants.NBT.TAG_COMPOUND))
            this.componentManager.deserializeNBT(nbt.getCompound("componentManager"));
    }

    @Override
    public CompoundNBT getUpdateTag() {
        CompoundNBT nbt = super.getUpdateTag();
        nbt.putString("machineID", this.id.toString());
        return nbt;
    }

    /**LIGHTNING STUFF**/

    private void refreshLightning() {
        if(world != null && !world.isRemote) {
            this.changeLightState();
            NetworkManager.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> world.getChunk(pos.getX() / 16, pos.getZ() / 16)), new SUpdateCustomTileLightPacket(pos));
        }

    }

    private boolean emmitLight = false;
    public void changeLightState() {
        this.emmitLight = !this.emmitLight;
        if(world != null)world.getChunkProvider().getLightManager().checkBlock(pos);
    }

    private boolean needRefreshLightning() {
        return (getMachine().getAppearance().getLightMode() == MachineAppearance.LightMode.ALWAYS || getMachine().getAppearance().getLightMode().toString().equals(this.craftingManager.getStatus().toString())) != emmitLight;
    }

    public int getLightValue() {
        if(this.emmitLight)
            return getMachine().getAppearance().getLightLevel();
        else return 0;
    }

    /**CONTAINER STUFF**/

    @Override
    public ITextComponent getDisplayName() {
        return new StringTextComponent(getMachine().getName());
    }

    @ParametersAreNonnullByDefault
    @Nullable
    @Override
    public Container createMenu(int id, PlayerInventory inv, PlayerEntity player) {
        return new CustomMachineContainer(id, inv, this);
    }

    @Override
    public void getStuffToSync(Consumer<ISyncable<?, ?>> container) {
        this.craftingManager.getStuffToSync(container);
        this.componentManager.getStuffToSync(container);
        this.fuelManager.getStuffToSync(container);
    }

    /**CLIENT STUFF**/

    private ModelDataMap modelDataMap;
    private boolean needRefresh = false;

    private void setupModelData() {
        this.modelDataMap = new ModelDataMap.Builder().withInitial(DummyBakedModel.PARTICLE_TEXTURE, ClientHandler.getParticleTexture(getMachine().getAppearance())).build();
        this.needRefresh = true;
    }

    @Nonnull
    @Override
    public IModelData getModelData() {
        if(this.needRefresh) {
            ModelDataManager.requestModelDataRefresh(this);
            this.needRefresh = false;
        }
        return this.modelDataMap == null ? EmptyModelData.INSTANCE : this.modelDataMap;
    }
}
