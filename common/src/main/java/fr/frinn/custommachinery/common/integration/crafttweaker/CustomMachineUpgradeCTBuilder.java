package fr.frinn.custommachinery.common.integration.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.base.IRuntimeAction;
import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.platform.Services;
import com.google.common.collect.ImmutableList;
import fr.frinn.custommachinery.CustomMachinery;
import fr.frinn.custommachinery.api.requirement.RequirementIOMode;
import fr.frinn.custommachinery.api.requirement.RequirementType;
import fr.frinn.custommachinery.common.integration.crafttweaker.RequirementTypeCTBrackets.CTRequirementType;
import fr.frinn.custommachinery.common.upgrade.MachineUpgrade;
import fr.frinn.custommachinery.common.upgrade.RecipeModifier;
import net.minecraft.ChatFormatting;
import net.minecraft.ResourceLocationException;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.openzen.zencode.java.ZenCodeType.Method;
import org.openzen.zencode.java.ZenCodeType.Name;
import org.openzen.zencode.java.ZenCodeType.OptionalInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ZenRegister
@Name(CTConstants.UPGRADE_BUILDER)
public class CustomMachineUpgradeCTBuilder {

    private final Item item;
    private List<Component> tooltips;
    private final List<ResourceLocation> machines;
    private final List<RecipeModifier> modifiers;
    private final int maxAmount;

    public CustomMachineUpgradeCTBuilder(Item item, int maxAmount) {
        this.item = item;
        this.tooltips = Collections.singletonList(new TranslatableComponent("custommachinery.upgrade.tooltip").withStyle(ChatFormatting.AQUA));
        this.maxAmount = maxAmount;
        this.machines = new ArrayList<>();
        this.modifiers = new ArrayList<>();
    }

    @Method
    public static CustomMachineUpgradeCTBuilder create(Item item, @OptionalInt(64) int maxAmount) {
        return new CustomMachineUpgradeCTBuilder(item, maxAmount);
    }

    @Method
    public void build() {
        if(this.machines.isEmpty())
            throw new IllegalArgumentException("You must specify at least 1 machine for machine upgrade item: " + Services.REGISTRY.getRegistryKey(this.item));
        if(this.modifiers.isEmpty())
            throw new IllegalArgumentException("You must specify at least 1 recipe modifier for machine upgrade item: " + Services.REGISTRY.getRegistryKey(this.item));
        MachineUpgrade upgrade = new MachineUpgrade(this.item, this.machines, this.modifiers, this.tooltips, this.maxAmount);
        CraftTweakerAPI.apply(new AddMachineUpgradeAction(upgrade));
    }

    @Method
    public CustomMachineUpgradeCTBuilder machine(String... string) {
        for(String s : string) {
            final ResourceLocation machine;
            try {
                machine = new ResourceLocation(s);
            } catch (ResourceLocationException e) {
                throw new IllegalArgumentException("Invalid Machine ID: " + s + "\n" + e.getMessage());
            }
            this.machines.add(machine);
        }
        return this;
    }

    @Method
    public CustomMachineUpgradeCTBuilder tooltip(String... strings) {
        ImmutableList.Builder<Component> tooltips = ImmutableList.builder();
        for(String tooltip : strings) {
            try {
                Component component = Component.Serializer.fromJson(tooltip);
                if(component == null)
                    throw new IllegalArgumentException("");
                tooltips.add(component);
            } catch (Exception e) {
                tooltips.add(new TranslatableComponent(tooltip));
            }
        }
        this.tooltips = tooltips.build();
        return this;
    }

    @Method
    public CustomMachineUpgradeCTBuilder tooltip(Component... components) {
        this.tooltips = ImmutableList.copyOf(components);
        return this;
    }

    @Method
    public CustomMachineUpgradeCTBuilder modifier(CTRecipeModifierBuilder builder) {
        this.modifiers.add(builder.build());
        return this;
    }

    @ZenRegister
    @Name(CTConstants.MODIFIER_BUILDER)
    public static class CTRecipeModifierBuilder {

        private final RequirementType<?> requirementType;
        private final RequirementIOMode mode;
        private final RecipeModifier.OPERATION operation;
        private final double modifier;
        private String target = "";
        private double chance = 1.0D;
        private double max = Double.POSITIVE_INFINITY;
        private double min = Double.NEGATIVE_INFINITY;
        private Component tooltip = null;

        private CTRecipeModifierBuilder(RequirementType<?> type, RequirementIOMode mode, RecipeModifier.OPERATION operation, double modifier) {
            this.requirementType = type;
            this.mode = mode;
            this.operation = operation;
            this.modifier = modifier;
        }

        @Method
        public static CTRecipeModifierBuilder addInput(CTRequirementType type, double modifier) {
            return new CTRecipeModifierBuilder(type.getType(), RequirementIOMode.INPUT, RecipeModifier.OPERATION.ADDITION, modifier);
        }

        @Method
        public static CTRecipeModifierBuilder mulInput(CTRequirementType type, double modifier) {
            return new CTRecipeModifierBuilder(type.getType(), RequirementIOMode.INPUT, RecipeModifier.OPERATION.MULTIPLICATION, modifier);
        }

        @Method
        public static CTRecipeModifierBuilder addOutput(CTRequirementType type, double modifier) {
            return new CTRecipeModifierBuilder(type.getType(), RequirementIOMode.OUTPUT, RecipeModifier.OPERATION.ADDITION, modifier);
        }

        @Method
        public static CTRecipeModifierBuilder mulOutput(CTRequirementType type, double modifier) {
            return new CTRecipeModifierBuilder(type.getType(), RequirementIOMode.OUTPUT, RecipeModifier.OPERATION.MULTIPLICATION, modifier);
        }

        @Method
        public CTRecipeModifierBuilder target(String target) {
            this.target = target;
            return this;
        }

        @Method
        public CTRecipeModifierBuilder chance(double chance) {
            this.chance = chance;
            return this;
        }

        @Method
        public CTRecipeModifierBuilder max(double max) {
            this.max = max;
            return this;
        }

        @Method
        public CTRecipeModifierBuilder min(double min) {
            this.min = min;
            return this;
        }

        @Method
        public CTRecipeModifierBuilder tooltip(String tooltip) {
            try {
                this.tooltip = Component.Serializer.fromJson(tooltip);
            } catch (Exception e) {
                this.tooltip = new TranslatableComponent(tooltip);
            }
            return this;
        }

        @Method
        public CTRecipeModifierBuilder tooltip(Component tooltip) {
            this.tooltip = tooltip;
            return this;
        }

        private RecipeModifier build() {
            return new RecipeModifier(this.requirementType, this.mode, this.operation, this.modifier, this.target, this.chance, this.max, this.min, this.tooltip);
        }
    }

    public static class AddMachineUpgradeAction implements IRuntimeAction {

        private final MachineUpgrade upgrade;

        public AddMachineUpgradeAction(MachineUpgrade upgrade) {
            this.upgrade = upgrade;
        }

        @Override
        public void apply() {
            CustomMachinery.UPGRADES.addUpgrade(this.upgrade);
        }

        @Override
        public String describe() {
            return "Add a custom machine upgrade for item: " + Services.REGISTRY.getRegistryKey(this.upgrade.getItem());
        }
    }
}
