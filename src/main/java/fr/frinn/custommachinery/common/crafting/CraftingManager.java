package fr.frinn.custommachinery.common.crafting;

import fr.frinn.custommachinery.api.component.IMachineComponent;
import fr.frinn.custommachinery.api.crafting.ComponentNotFoundException;
import fr.frinn.custommachinery.api.crafting.CraftingResult;
import fr.frinn.custommachinery.api.machine.MachineStatus;
import fr.frinn.custommachinery.api.network.ISyncable;
import fr.frinn.custommachinery.api.requirement.IChanceableRequirement;
import fr.frinn.custommachinery.api.requirement.IDelayedRequirement;
import fr.frinn.custommachinery.api.requirement.IRequirement;
import fr.frinn.custommachinery.api.requirement.ITickableRequirement;
import fr.frinn.custommachinery.apiimpl.network.syncable.DoubleSyncable;
import fr.frinn.custommachinery.apiimpl.network.syncable.IntegerSyncable;
import fr.frinn.custommachinery.apiimpl.network.syncable.StringSyncable;
import fr.frinn.custommachinery.common.init.CustomMachineTile;
import fr.frinn.custommachinery.common.network.NetworkManager;
import fr.frinn.custommachinery.common.network.SCraftingManagerStatusChangedPacket;
import fr.frinn.custommachinery.common.util.TextComponentUtils;
import fr.frinn.custommachinery.common.util.Utils;
import mcjty.theoneprobe.api.IProbeInfo;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fml.network.PacketDistributor;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CraftingManager implements INBTSerializable<CompoundNBT> {

    private final CustomMachineTile tile;
    private final Random rand = Utils.RAND;
    private final List<IRequirement<?>> processedRequirements;
    //Use only for recipe searching, not recipe processing
    private final CraftingContext.Mutable mutableCraftingContext;
    private CustomMachineRecipe currentRecipe;
    //Recipe that was processed when the machine was unloaded, and we need to resume
    private CustomMachineRecipe lastRecipe;
    private ResourceLocation futureRecipeID;
    public double recipeProgressTime = 0;
    public int recipeTotalTime = 0;
    private CraftingContext context;
    private boolean initialized = false;

    private List<ITickableRequirement<IMachineComponent>> tickableRequirements;
    private List<IDelayedRequirement<IMachineComponent>> delayedRequirements;

    private MachineStatus status;
    private MachineStatus prevStatus;
    private PHASE phase = PHASE.STARTING;

    private ITextComponent errorMessage = StringTextComponent.EMPTY;

    private final RecipeFinder recipeFinder;

    public CraftingManager(CustomMachineTile tile) {
        this.tile = tile;
        this.mutableCraftingContext = new CraftingContext.Mutable(this);
        this.status = MachineStatus.IDLE;
        this.prevStatus = this.status;
        this.processedRequirements = new ArrayList<>();
        this.recipeFinder = new RecipeFinder(tile);
    }

    public void tick() {
        if(!this.initialized)
            this.init();

        if(this.checkPause())
            return;

        if(this.currentRecipe == null)
            this.searchForRecipe();

        if(this.currentRecipe != null) {
            this.context.tickModifiers();

            if(this.phase == PHASE.STARTING)
                this.startProcess();

            if(this.phase == PHASE.CRAFTING_TICKABLE)
                this.processTickable();

            if(this.phase == PHASE.CRAFTING_DELAYED)
                this.processDelayed();

            if(this.phase == PHASE.ENDING)
                this.endProcess();
        }
        else this.setStatus(MachineStatus.IDLE);
    }

    private void init() {
        this.initialized = true;
        if(this.futureRecipeID != null && this.tile.getWorld() != null) {
            CustomMachineRecipe recipe = (CustomMachineRecipe) this.tile.getWorld().getRecipeManager().getRecipe(this.futureRecipeID).orElse(null);
            if(recipe != null) {
                this.setRecipe(recipe);
            }
            this.futureRecipeID = null;
        }
    }

    private boolean checkPause() {
        if(this.tile.isPaused() && this.status != MachineStatus.PAUSED) {
            this.prevStatus = this.status;
            this.status = MachineStatus.PAUSED;
            notifyStatusChanged();
        }
        if(!this.tile.isPaused() && this.status == MachineStatus.PAUSED) {
            this.status = this.prevStatus;
            notifyStatusChanged();
        }
        return this.status == MachineStatus.PAUSED;
    }

    private void searchForRecipe() {
        if(this.lastRecipe != null && this.lastRecipe.matches(this.tile, this.mutableCraftingContext.setRecipe(this.lastRecipe))) {
            this.setRecipe(this.lastRecipe);
            this.lastRecipe = null;
        }
        if(this.currentRecipe == null)
            this.recipeFinder.findRecipe(this.mutableCraftingContext, this.status == MachineStatus.RUNNING).ifPresent(this::setRecipe);
    }

    private void startProcess() {
        for (IRequirement<?> requirement : this.currentRecipe.getRequirements()) {
            if (!this.processedRequirements.contains(requirement)) {
                IMachineComponent component = this.tile.componentManager.getComponent(requirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), requirement.getType()));
                if (requirement instanceof IChanceableRequirement && ((IChanceableRequirement<IMachineComponent>) requirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(requirement);
                    continue;
                }
                CraftingResult result = ((IRequirement<IMachineComponent>)requirement).processStart(component, this.context);
                if (!result.isSuccess()) {
                    this.setStatus(MachineStatus.ERRORED, result.getMessage());
                    break;
                } else this.processedRequirements.add(requirement);
            }
        }

        if (this.processedRequirements.size() == this.currentRecipe.getRequirements().size()) {
            this.setStatus(MachineStatus.RUNNING);
            this.phase = PHASE.CRAFTING_TICKABLE;
            this.processedRequirements.clear();
        }
    }

    private void processTickable() {
        for (ITickableRequirement<IMachineComponent> tickableRequirement : this.tickableRequirements) {
            if (!this.processedRequirements.contains(tickableRequirement)) {
                IMachineComponent component = this.tile.componentManager.getComponent(tickableRequirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), tickableRequirement.getType()));
                if (tickableRequirement instanceof IChanceableRequirement && ((IChanceableRequirement<IMachineComponent>) tickableRequirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(tickableRequirement);
                    continue;
                }
                CraftingResult result = tickableRequirement.processTick(component, this.context);
                if (!result.isSuccess()) {
                    this.setStatus(MachineStatus.ERRORED, result.getMessage());
                    break;
                } else this.processedRequirements.add(tickableRequirement);
            }
        }

        if (this.processedRequirements.size() == this.tickableRequirements.size()) {
            this.recipeProgressTime += this.context.getModifiedSpeed();
            this.setStatus(MachineStatus.RUNNING);
            this.processedRequirements.clear();
        }
        this.phase = PHASE.CRAFTING_DELAYED;
    }

    private void processDelayed() {
        for(Iterator<IDelayedRequirement<IMachineComponent>> iterator = this.delayedRequirements.iterator(); iterator.hasNext(); ) {
            IDelayedRequirement<IMachineComponent> delayedRequirement = iterator.next();
            if(this.recipeProgressTime / this.recipeTotalTime >= delayedRequirement.getDelay()) {
                IMachineComponent component = this.tile.componentManager.getComponent(delayedRequirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), delayedRequirement.getType()));
                CraftingResult result = delayedRequirement.execute(component, this.context);
                if(!result.isSuccess()) {
                    this.setStatus(MachineStatus.ERRORED, result.getMessage());
                    break;
                } else iterator.remove();
            }
        }

        if(this.delayedRequirements.stream().allMatch(delayedRequirement -> this.recipeProgressTime / this.recipeTotalTime < delayedRequirement.getDelay()))
            if (this.recipeProgressTime >= this.recipeTotalTime)
                this.phase = PHASE.ENDING;
            else this.phase = PHASE.CRAFTING_TICKABLE;
    }

    private void endProcess() {
        for(IRequirement<?> requirement : this.currentRecipe.getRequirements()) {
            if(!this.processedRequirements.contains(requirement)) {
                IMachineComponent component = this.tile.componentManager.getComponent(requirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), requirement.getType()));
                if(requirement instanceof IChanceableRequirement && ((IChanceableRequirement) requirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(requirement);
                    continue;
                }
                CraftingResult result = ((IRequirement)requirement).processEnd(component, this.context);
                if(!result.isSuccess()) {
                    this.setStatus(MachineStatus.ERRORED, result.getMessage());
                    break;
                }
                else this.processedRequirements.add(requirement);
            }
        }

        if(this.processedRequirements.size() == this.currentRecipe.getRequirements().size()) {
            this.lastRecipe = this.currentRecipe;
            this.currentRecipe = null;
            this.recipeProgressTime = 0;
            this.context = null;
            this.processedRequirements.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private void setRecipe(CustomMachineRecipe recipe) {
        this.currentRecipe = recipe;
        this.context = new CraftingContext(this, recipe);
        this.tickableRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof ITickableRequirement)
                .map(requirement -> (ITickableRequirement<IMachineComponent>)requirement)
                .collect(Collectors.toList());
        this.delayedRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof IDelayedRequirement)
                .map(requirement -> (IDelayedRequirement<IMachineComponent>)requirement)
                .filter(requirement -> requirement.getDelay() > 0 && requirement.getDelay() < 1.0)
                .collect(Collectors.toList());
        this.recipeTotalTime = this.currentRecipe.getRecipeTime();
        this.phase = PHASE.STARTING;
        this.setStatus(MachineStatus.RUNNING);
    }

    public ITextComponent getErrorMessage() {
        return this.errorMessage;
    }

    public void setStatus(MachineStatus status) {
        this.setStatus(status, StringTextComponent.EMPTY);
    }

    public void setStatus(MachineStatus status, ITextComponent mesage) {
        if(this.status != status) {
            this.status = status;
            this.errorMessage = mesage;
            this.tile.markDirty();
            notifyStatusChanged();
        }
    }

    private void notifyStatusChanged() {
        if(this.tile.getWorld() != null && !this.tile.getWorld().isRemote()) {
            BlockPos pos = this.tile.getPos();
            NetworkManager.CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> this.tile.getWorld().getChunkAt(pos)), new SCraftingManagerStatusChangedPacket(pos, this.status));
        }

    }

    public MachineStatus getStatus() {
        return this.status;
    }

    public void reset() {
        this.currentRecipe = null;
        this.futureRecipeID = null;
        this.setStatus(MachineStatus.IDLE);
        this.prevStatus = MachineStatus.IDLE;
        this.recipeProgressTime = 0;
        this.recipeTotalTime = 0;
        this.processedRequirements.clear();
        this.context = null;
        this.errorMessage = StringTextComponent.EMPTY;
    }

    public CustomMachineTile getTile() {
        return this.tile;
    };

    public CustomMachineRecipe getCurrentRecipe() {
        return this.currentRecipe;
    }

    public void addProbeInfo(IProbeInfo info) {
        TranslationTextComponent status = this.status.getTranslatedName();
        switch (this.status) {
            case ERRORED:
                status.mergeStyle(TextFormatting.RED);
                break;
            case RUNNING:
                status.mergeStyle(TextFormatting.GREEN);
                break;
            case PAUSED:
                status.mergeStyle(TextFormatting.GOLD);
                break;
        }
        info.mcText(status);
        if(this.currentRecipe != null)
            info.progress((int)this.recipeProgressTime, this.recipeTotalTime, info.defaultProgressStyle().suffix("/" + this.recipeTotalTime));
        if(this.status == MachineStatus.ERRORED)
            info.text(this.errorMessage);
    }

    @Override
    public CompoundNBT serializeNBT() {
        CompoundNBT nbt = new CompoundNBT();
        if(this.currentRecipe != null)
            nbt.putString("recipe", this.currentRecipe.getId().toString());
        nbt.putString("phase", this.phase.toString());
        nbt.putString("status", this.status.toString());
        nbt.putString("message", TextComponentUtils.toJsonString(this.errorMessage));
        nbt.putDouble("recipeProgressTime", this.recipeProgressTime);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        if(nbt.contains("recipe", Constants.NBT.TAG_STRING))
            this.futureRecipeID = new ResourceLocation(nbt.getString("recipe"));
        if(nbt.contains("phase", Constants.NBT.TAG_STRING))
            this.phase = PHASE.value(nbt.getString("phase"));
        if(nbt.contains("status", Constants.NBT.TAG_STRING))
            this.setStatus(MachineStatus.value(nbt.getString("status")));
        if(nbt.contains("message", Constants.NBT.TAG_STRING))
            this.errorMessage = TextComponentUtils.fromJsonString(nbt.getString("message"));
        if(nbt.contains("recipeProgressTime", Constants.NBT.TAG_DOUBLE))
            this.recipeProgressTime = nbt.getDouble("recipeProgressTime");
    }

    public void getStuffToSync(Consumer<ISyncable<?, ?>> container) {
        container.accept(DoubleSyncable.create(() -> this.recipeProgressTime, recipeProgressTime -> this.recipeProgressTime = recipeProgressTime));
        container.accept(IntegerSyncable.create(() -> this.recipeTotalTime, recipeTotalTime -> this.recipeTotalTime = recipeTotalTime));
        container.accept(StringSyncable.create(() -> this.status.toString(), status -> this.status = MachineStatus.value(status)));
        container.accept(StringSyncable.create(() -> TextComponentUtils.toJsonString(this.errorMessage), errorMessage -> this.errorMessage = TextComponentUtils.fromJsonString(errorMessage)));
    }

    public void setMachineInventoryChanged() {
        this.recipeFinder.setInventoryChanged();
    }

    public enum PHASE {
        STARTING,
        CRAFTING_TICKABLE,
        CRAFTING_DELAYED,
        ENDING;

        public static PHASE value(String string) {
            return valueOf(string.toUpperCase(Locale.ENGLISH));
        }
    }

}
