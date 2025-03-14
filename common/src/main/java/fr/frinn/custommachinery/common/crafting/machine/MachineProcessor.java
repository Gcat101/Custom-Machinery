package fr.frinn.custommachinery.common.crafting.machine;

import fr.frinn.custommachinery.api.codec.NamedCodec;
import fr.frinn.custommachinery.api.component.IMachineComponent;
import fr.frinn.custommachinery.api.crafting.ComponentNotFoundException;
import fr.frinn.custommachinery.api.crafting.CraftingResult;
import fr.frinn.custommachinery.api.crafting.ICraftingContext;
import fr.frinn.custommachinery.api.crafting.IProcessor;
import fr.frinn.custommachinery.api.crafting.IProcessorTemplate;
import fr.frinn.custommachinery.api.crafting.ProcessorType;
import fr.frinn.custommachinery.api.machine.MachineStatus;
import fr.frinn.custommachinery.api.machine.MachineTile;
import fr.frinn.custommachinery.api.network.ISyncable;
import fr.frinn.custommachinery.api.network.ISyncableStuff;
import fr.frinn.custommachinery.api.requirement.IChanceableRequirement;
import fr.frinn.custommachinery.api.requirement.IDelayedRequirement;
import fr.frinn.custommachinery.api.requirement.IRequirement;
import fr.frinn.custommachinery.api.requirement.ITickableRequirement;
import fr.frinn.custommachinery.common.crafting.CraftingContext;
import fr.frinn.custommachinery.common.init.Registration;
import fr.frinn.custommachinery.common.network.syncable.DoubleSyncable;
import fr.frinn.custommachinery.common.network.syncable.IntegerSyncable;
import fr.frinn.custommachinery.common.util.Utils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Consumer;

public class MachineProcessor implements IProcessor, ISyncableStuff {

    private final MachineTile tile;
    private final Random rand = Utils.RAND;
    private final List<IRequirement<?>> processedRequirements;
    //Use only for recipe searching, not recipe processing
    private final CraftingContext.Mutable mutableCraftingContext;
    private final int amount;
    private final int recipeCheckCooldown;
    private CustomMachineRecipe currentRecipe;
    //Recipe that was processed when the machine was unloaded, and we need to resume
    private ResourceLocation futureRecipeID;
    private double recipeProgressTime = 0;
    private int recipeTotalTime = 0;
    private CraftingContext context;
    private boolean initialized = false;

    private List<ITickableRequirement<IMachineComponent>> tickableRequirements;
    private List<IDelayedRequirement<IMachineComponent>> delayedRequirements;

    private PHASE phase = PHASE.STARTING;
    private final MachineRecipeFinder recipeFinder;

    public MachineProcessor(MachineTile tile, int amount, int recipeCheckCooldown) {
        this.tile = tile;
        this.amount = amount;
        this.recipeCheckCooldown = recipeCheckCooldown;
        this.mutableCraftingContext = new CraftingContext.Mutable(this, tile.getUpgradeManager());
        this.processedRequirements = new ArrayList<>();
        this.recipeFinder = new MachineRecipeFinder(tile, recipeCheckCooldown);
    }

    @Override
    public void tick() {
        if(!this.initialized)
            this.init();

        if(this.currentRecipe == null)
            this.searchForRecipe(false);

        if(this.currentRecipe != null) {
            if(this.phase == PHASE.STARTING)
                this.startProcess();

            if(this.phase == PHASE.CRAFTING_TICKABLE)
                this.processTickable();

            if(this.phase == PHASE.CRAFTING_DELAYED)
                this.processDelayed();

            if(this.phase == PHASE.ENDING)
                this.endProcess();
        }
        else this.tile.setStatus(MachineStatus.IDLE);
    }

    private void init() {
        this.initialized = true;
        this.recipeFinder.init();
        if(this.futureRecipeID != null && this.tile.getLevel() != null) {
            this.tile.getLevel().getRecipeManager()
                    .byKey(this.futureRecipeID)
                    .filter(recipe -> recipe instanceof CustomMachineRecipe)
                    .map(recipe -> (CustomMachineRecipe)recipe)
                    .ifPresent(this::setOldRecipe);
            this.futureRecipeID = null;
        }
    }

    private void searchForRecipe(boolean immediately) {
        if(this.currentRecipe == null)
            this.recipeFinder.findRecipe(this.mutableCraftingContext, immediately).ifPresent(this::setRecipe);
    }

    private void startProcess() {
        for (IRequirement<?> requirement : this.currentRecipe.getRequirements()) {
            if (!this.processedRequirements.contains(requirement)) {
                IMachineComponent component = this.tile.getComponentManager().getComponent(requirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), requirement.getType()));
                if (requirement instanceof IChanceableRequirement && ((IChanceableRequirement<IMachineComponent>) requirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(requirement);
                    continue;
                }
                CraftingResult result = ((IRequirement<IMachineComponent>)requirement).processStart(component, this.context);
                if (!result.isSuccess()) {
                    if(this.currentRecipe.shouldResetOnError()) {
                        reset();
                        return;
                    } else {
                        this.tile.setStatus(MachineStatus.ERRORED, result.getMessage());
                        break;
                    }
                } else this.processedRequirements.add(requirement);
            }
        }

        if (this.processedRequirements.size() == this.currentRecipe.getRequirements().size()) {
            this.tile.setStatus(MachineStatus.RUNNING);
            this.phase = PHASE.CRAFTING_TICKABLE;
            this.processedRequirements.clear();
        }
    }

    private void processTickable() {
        for (ITickableRequirement<IMachineComponent> tickableRequirement : this.tickableRequirements) {
            if (!this.processedRequirements.contains(tickableRequirement)) {
                IMachineComponent component = this.tile.getComponentManager().getComponent(tickableRequirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), tickableRequirement.getType()));
                if (tickableRequirement instanceof IChanceableRequirement && ((IChanceableRequirement<IMachineComponent>) tickableRequirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(tickableRequirement);
                    continue;
                }
                CraftingResult result = tickableRequirement.processTick(component, this.context);
                if (!result.isSuccess()) {
                    if(this.currentRecipe.shouldResetOnError()) {
                        reset();
                        return;
                    } else {
                        this.tile.setStatus(MachineStatus.ERRORED, result.getMessage());
                        break;
                    }
                } else this.processedRequirements.add(tickableRequirement);
            }
        }

        if (this.processedRequirements.size() == this.tickableRequirements.size()) {
            this.recipeProgressTime += this.context.getModifiedSpeed();
            this.tile.setStatus(MachineStatus.RUNNING);
            this.processedRequirements.clear();
        }
        this.phase = PHASE.CRAFTING_DELAYED;
    }

    private void processDelayed() {
        for(Iterator<IDelayedRequirement<IMachineComponent>> iterator = this.delayedRequirements.iterator(); iterator.hasNext(); ) {
            IDelayedRequirement<IMachineComponent> delayedRequirement = iterator.next();
            if(this.recipeProgressTime / this.recipeTotalTime >= delayedRequirement.getDelay()) {
                IMachineComponent component = this.tile.getComponentManager().getComponent(delayedRequirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), delayedRequirement.getType()));
                CraftingResult result = delayedRequirement.execute(component, this.context);
                if(!result.isSuccess()) {
                    if(this.currentRecipe.shouldResetOnError()) {
                        reset();
                        return;
                    } else {
                        this.tile.setStatus(MachineStatus.ERRORED, result.getMessage());
                        break;
                    }
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
                IMachineComponent component = this.tile.getComponentManager().getComponent(requirement.getComponentType()).orElseThrow(() -> new ComponentNotFoundException(this.currentRecipe, this.tile.getMachine(), requirement.getType()));
                if(requirement instanceof IChanceableRequirement && ((IChanceableRequirement) requirement).shouldSkip(component, this.rand, this.context)) {
                    this.processedRequirements.add(requirement);
                    continue;
                }
                CraftingResult result = ((IRequirement)requirement).processEnd(component, this.context);
                if(!result.isSuccess()) {
                    if(this.currentRecipe.shouldResetOnError()) {
                        reset();
                        return;
                    } else {
                        this.tile.setStatus(MachineStatus.ERRORED, result.getMessage());
                        break;
                    }
                }
                else this.processedRequirements.add(requirement);
            }
        }

        if(this.processedRequirements.size() == this.currentRecipe.getRequirements().size()) {
            this.currentRecipe = null;
            this.recipeProgressTime = 0;
            this.context = null;
            this.processedRequirements.clear();

            this.recipeFinder.setInventoryChanged(true);
            this.searchForRecipe(true);
        }
    }

    @SuppressWarnings("unchecked")
    private void setRecipe(CustomMachineRecipe recipe) {
        this.currentRecipe = recipe;
        this.context = new CraftingContext(this, this.tile.getUpgradeManager(), recipe);
        this.tickableRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof ITickableRequirement)
                .map(requirement -> (ITickableRequirement<IMachineComponent>)requirement)
                .toList();
        this.delayedRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof IDelayedRequirement)
                .map(requirement -> (IDelayedRequirement<IMachineComponent>)requirement)
                .filter(requirement -> requirement.getDelay() > 0 && requirement.getDelay() < 1.0)
                .toList();
        this.recipeTotalTime = this.currentRecipe.getRecipeTime();
        this.phase = PHASE.STARTING;
        this.tile.setStatus(MachineStatus.RUNNING);
    }

    //Set the recipe the machine was processing before being unloaded. Save the current progress time and phase.
    private void setOldRecipe(CustomMachineRecipe recipe) {
        this.currentRecipe = recipe;
        this.context = new CraftingContext(this, this.tile.getUpgradeManager(), recipe);
        this.tickableRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof ITickableRequirement)
                .map(requirement -> (ITickableRequirement<IMachineComponent>)requirement)
                .toList();
        this.delayedRequirements = this.currentRecipe.getRequirements()
                .stream()
                .filter(requirement -> requirement instanceof IDelayedRequirement)
                .map(requirement -> (IDelayedRequirement<IMachineComponent>)requirement)
                .filter(requirement -> requirement.getDelay() > 0 && requirement.getDelay() < 1.0)
                .toList();
        this.recipeTotalTime = this.currentRecipe.getRecipeTime();
        this.tile.setStatus(MachineStatus.RUNNING);
    }

    @Override
    public void reset() {
        this.currentRecipe = null;
        this.futureRecipeID = null;
        this.tile.setStatus(MachineStatus.IDLE);
        this.recipeProgressTime = 0;
        this.recipeTotalTime = 0;
        this.processedRequirements.clear();
        this.context = null;
    }

    public MachineTile getTile() {
        return this.tile;
    };

    public CustomMachineRecipe getCurrentRecipe() {
        return this.currentRecipe;
    }

    public double getRecipeProgressTime() {
        return this.recipeProgressTime;
    }

    public int getRecipeTotalTime() {
        return this.recipeTotalTime;
    }

    @Nullable
    @Override
    public ICraftingContext getCurrentContext() {
        return this.context;
    }

    @Override
    public ProcessorType<MachineProcessor> getType() {
        return Registration.MACHINE_PROCESSOR.get();
    }

    @Override
    public CompoundTag serialize() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("type", getType().getId().toString());
        if(this.currentRecipe != null)
            nbt.putString("recipe", this.currentRecipe.getId().toString());
        nbt.putString("phase", this.phase.toString());
        nbt.putDouble("recipeProgressTime", this.recipeProgressTime);
        return nbt;
    }

    @Override
    public void deserialize(CompoundTag nbt) {
        if(nbt.contains("type", Tag.TAG_STRING) && !nbt.getString("type").equals(getType().getId().toString()))
            return;
        if(nbt.contains("recipe", Tag.TAG_STRING))
            this.futureRecipeID = new ResourceLocation(nbt.getString("recipe"));
        if(nbt.contains("phase", Tag.TAG_STRING))
            this.phase = PHASE.value(nbt.getString("phase"));
        if(nbt.contains("recipeProgressTime", Tag.TAG_DOUBLE))
            this.recipeProgressTime = nbt.getDouble("recipeProgressTime");
    }

    @Override
    public void getStuffToSync(Consumer<ISyncable<?, ?>> container) {
        container.accept(DoubleSyncable.create(() -> this.recipeProgressTime, recipeProgressTime -> this.recipeProgressTime = recipeProgressTime));
        container.accept(IntegerSyncable.create(() -> this.recipeTotalTime, recipeTotalTime -> this.recipeTotalTime = recipeTotalTime));
    }

    @Override
    public void setMachineInventoryChanged() {
        this.recipeFinder.setInventoryChanged(true);
    }

    public enum PHASE {
        STARTING,
        CRAFTING_TICKABLE,
        CRAFTING_DELAYED,
        ENDING;

        public static final NamedCodec<PHASE> CODEC = NamedCodec.enumCodec(PHASE.class);

        public static PHASE value(String string) {
            return valueOf(string.toUpperCase(Locale.ENGLISH));
        }
    }

    public static class Template implements IProcessorTemplate<MachineProcessor> {

        public static final NamedCodec<Template> CODEC = NamedCodec.record(templateInstance ->
                templateInstance.group(
                        NamedCodec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("amount", 1).forGetter(template -> template.amount),
                        NamedCodec.intRange(1, Integer.MAX_VALUE).optionalFieldOf("cooldown", 20).forGetter(template -> template.recipeCheckCooldown)
                ).apply(templateInstance, Template::new), "Machine processor"
        );

        public static final Template DEFAULT = new Template(1, 20);

        private final int amount;
        private final int recipeCheckCooldown;

        private Template(int amount, int cooldown) {
            this.amount = amount;
            this.recipeCheckCooldown = cooldown;
        }

        @Override
        public ProcessorType<MachineProcessor> getType() {
            return Registration.MACHINE_PROCESSOR.get();
        }

        @Override
        public MachineProcessor build(MachineTile tile) {
            return new MachineProcessor(tile, this.amount, this.recipeCheckCooldown);
        }
    }
}
