package fr.frinn.custommachinery.common.init;

import dev.architectury.fluid.FluidStack;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.Registrar;
import dev.architectury.registry.registries.Registries;
import dev.architectury.registry.registries.RegistrySupplier;
import fr.frinn.custommachinery.CustomMachinery;
import fr.frinn.custommachinery.PlatformHelper;
import fr.frinn.custommachinery.api.codec.NamedCodec;
import fr.frinn.custommachinery.api.component.IMachineComponent;
import fr.frinn.custommachinery.api.component.MachineComponentType;
import fr.frinn.custommachinery.api.component.variant.RegisterComponentVariantEvent;
import fr.frinn.custommachinery.api.crafting.ProcessorType;
import fr.frinn.custommachinery.api.guielement.GuiElementType;
import fr.frinn.custommachinery.api.guielement.IGuiElement;
import fr.frinn.custommachinery.api.machine.MachineAppearanceProperty;
import fr.frinn.custommachinery.api.network.DataType;
import fr.frinn.custommachinery.api.requirement.IRequirement;
import fr.frinn.custommachinery.api.requirement.RequirementType;
import fr.frinn.custommachinery.common.component.BlockMachineComponent;
import fr.frinn.custommachinery.common.component.CommandMachineComponent;
import fr.frinn.custommachinery.common.component.DataMachineComponent;
import fr.frinn.custommachinery.common.component.DropMachineComponent;
import fr.frinn.custommachinery.common.component.EffectMachineComponent;
import fr.frinn.custommachinery.common.component.EnergyMachineComponent;
import fr.frinn.custommachinery.common.component.EntityMachineComponent;
import fr.frinn.custommachinery.common.component.FluidMachineComponent;
import fr.frinn.custommachinery.common.component.FuelMachineComponent;
import fr.frinn.custommachinery.common.component.FunctionMachineComponent;
import fr.frinn.custommachinery.common.component.ItemMachineComponent;
import fr.frinn.custommachinery.common.component.LightMachineComponent;
import fr.frinn.custommachinery.common.component.PositionMachineComponent;
import fr.frinn.custommachinery.common.component.RedstoneMachineComponent;
import fr.frinn.custommachinery.common.component.StructureMachineComponent;
import fr.frinn.custommachinery.common.component.TimeMachineComponent;
import fr.frinn.custommachinery.common.component.WeatherMachineComponent;
import fr.frinn.custommachinery.common.component.handler.FluidComponentHandler;
import fr.frinn.custommachinery.common.component.handler.ItemComponentHandler;
import fr.frinn.custommachinery.common.component.variant.item.DefaultItemComponentVariant;
import fr.frinn.custommachinery.common.component.variant.item.EnergyItemComponentVariant;
import fr.frinn.custommachinery.common.component.variant.item.FluidItemComponentVariant;
import fr.frinn.custommachinery.common.component.variant.item.FuelItemComponentVariant;
import fr.frinn.custommachinery.common.component.variant.item.ResultItemComponentVariant;
import fr.frinn.custommachinery.common.component.variant.item.UpgradeItemComponentVariant;
import fr.frinn.custommachinery.common.crafting.DummyProcessor;
import fr.frinn.custommachinery.common.crafting.craft.CraftProcessor;
import fr.frinn.custommachinery.common.crafting.craft.CustomCraftRecipe;
import fr.frinn.custommachinery.common.crafting.craft.CustomCraftRecipeSerializer;
import fr.frinn.custommachinery.common.crafting.machine.CustomMachineRecipe;
import fr.frinn.custommachinery.common.crafting.machine.CustomMachineRecipeSerializer;
import fr.frinn.custommachinery.common.crafting.machine.MachineProcessor;
import fr.frinn.custommachinery.common.guielement.ConfigGuiElement;
import fr.frinn.custommachinery.common.guielement.DumpGuiElement;
import fr.frinn.custommachinery.common.guielement.EnergyGuiElement;
import fr.frinn.custommachinery.common.guielement.FluidGuiElement;
import fr.frinn.custommachinery.common.guielement.FuelGuiElement;
import fr.frinn.custommachinery.common.guielement.PlayerInventoryGuiElement;
import fr.frinn.custommachinery.common.guielement.ProgressBarGuiElement;
import fr.frinn.custommachinery.common.guielement.ResetGuiElement;
import fr.frinn.custommachinery.common.guielement.SizeGuiElement;
import fr.frinn.custommachinery.common.guielement.SlotGuiElement;
import fr.frinn.custommachinery.common.guielement.StatusGuiElement;
import fr.frinn.custommachinery.common.guielement.TextGuiElement;
import fr.frinn.custommachinery.common.guielement.TextureGuiElement;
import fr.frinn.custommachinery.common.machine.CustomMachine;
import fr.frinn.custommachinery.common.machine.builder.component.EnergyComponentBuilder;
import fr.frinn.custommachinery.common.machine.builder.component.FluidComponentBuilder;
import fr.frinn.custommachinery.common.machine.builder.component.ItemComponentBuilder;
import fr.frinn.custommachinery.common.network.data.BooleanData;
import fr.frinn.custommachinery.common.network.data.DoubleData;
import fr.frinn.custommachinery.common.network.data.FluidStackData;
import fr.frinn.custommachinery.common.network.data.IntegerData;
import fr.frinn.custommachinery.common.network.data.ItemStackData;
import fr.frinn.custommachinery.common.network.data.LongData;
import fr.frinn.custommachinery.common.network.data.SideConfigData;
import fr.frinn.custommachinery.common.network.data.StringData;
import fr.frinn.custommachinery.common.network.syncable.BooleanSyncable;
import fr.frinn.custommachinery.common.network.syncable.DoubleSyncable;
import fr.frinn.custommachinery.common.network.syncable.FluidStackSyncable;
import fr.frinn.custommachinery.common.network.syncable.IntegerSyncable;
import fr.frinn.custommachinery.common.network.syncable.ItemStackSyncable;
import fr.frinn.custommachinery.common.network.syncable.LongSyncable;
import fr.frinn.custommachinery.common.network.syncable.SideConfigSyncable;
import fr.frinn.custommachinery.common.network.syncable.StringSyncable;
import fr.frinn.custommachinery.common.requirement.BiomeRequirement;
import fr.frinn.custommachinery.common.requirement.BlockRequirement;
import fr.frinn.custommachinery.common.requirement.CommandRequirement;
import fr.frinn.custommachinery.common.requirement.DimensionRequirement;
import fr.frinn.custommachinery.common.requirement.DropRequirement;
import fr.frinn.custommachinery.common.requirement.DurabilityRequirement;
import fr.frinn.custommachinery.common.requirement.EffectRequirement;
import fr.frinn.custommachinery.common.requirement.EnergyPerTickRequirement;
import fr.frinn.custommachinery.common.requirement.EnergyRequirement;
import fr.frinn.custommachinery.common.requirement.EntityRequirement;
import fr.frinn.custommachinery.common.requirement.FluidPerTickRequirement;
import fr.frinn.custommachinery.common.requirement.FluidRequirement;
import fr.frinn.custommachinery.common.requirement.FuelRequirement;
import fr.frinn.custommachinery.common.requirement.FunctionRequirement;
import fr.frinn.custommachinery.common.requirement.ItemRequirement;
import fr.frinn.custommachinery.common.requirement.ItemTransformRequirement;
import fr.frinn.custommachinery.common.requirement.LightRequirement;
import fr.frinn.custommachinery.common.requirement.LootTableRequirement;
import fr.frinn.custommachinery.common.requirement.PositionRequirement;
import fr.frinn.custommachinery.common.requirement.RedstoneRequirement;
import fr.frinn.custommachinery.common.requirement.SpeedRequirement;
import fr.frinn.custommachinery.common.requirement.StructureRequirement;
import fr.frinn.custommachinery.common.requirement.TimeRequirement;
import fr.frinn.custommachinery.common.requirement.WeatherRequirement;
import fr.frinn.custommachinery.common.util.CMSoundType;
import fr.frinn.custommachinery.common.util.Codecs;
import fr.frinn.custommachinery.common.util.MachineShape;
import fr.frinn.custommachinery.impl.codec.DefaultCodecs;
import fr.frinn.custommachinery.impl.component.config.SideConfig;
import fr.frinn.custommachinery.impl.util.ModelLocation;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import java.util.Collections;
import java.util.List;

@SuppressWarnings("unused")
public class Registration {

    public static final Registries REGISTRIES = Registries.get(CustomMachinery.MODID);

    public static final CreativeModeTab GROUP = CreativeTabRegistry.create(new ResourceLocation(CustomMachinery.MODID, "group"), () -> CustomMachineItem.makeMachineItem(CustomMachine.DUMMY.getId()));

    public static final LootContextParamSet CUSTOM_MACHINE_LOOT_PARAMETER_SET = LootContextParamSets.register("custom_machine", builder ->
            builder.optional(LootContextParams.ORIGIN).optional(LootContextParams.BLOCK_ENTITY)
    );

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(CustomMachinery.MODID, Registry.BLOCK_REGISTRY);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(CustomMachinery.MODID, Registry.ITEM_REGISTRY);
    public static final DeferredRegister<BlockEntityType<?>> TILE_ENTITIES = DeferredRegister.create(CustomMachinery.MODID, Registry.BLOCK_ENTITY_TYPE_REGISTRY);
    public static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(CustomMachinery.MODID, Registry.MENU_REGISTRY);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(CustomMachinery.MODID, Registry.RECIPE_SERIALIZER_REGISTRY);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(CustomMachinery.MODID, Registry.RECIPE_TYPE_REGISTRY);
    public static final DeferredRegister<GuiElementType<? extends IGuiElement>> GUI_ELEMENTS = DeferredRegister.create(CustomMachinery.MODID, GuiElementType.REGISTRY_KEY);
    public static final DeferredRegister<MachineComponentType<? extends IMachineComponent>> MACHINE_COMPONENTS = DeferredRegister.create(CustomMachinery.MODID, MachineComponentType.REGISTRY_KEY);
    public static final DeferredRegister<RequirementType<? extends IRequirement<?>>> REQUIREMENTS = DeferredRegister.create(CustomMachinery.MODID, RequirementType.REGISTRY_KEY);
    public static final DeferredRegister<MachineAppearanceProperty<?>> APPEARANCE_PROPERTIES = DeferredRegister.create(CustomMachinery.MODID, MachineAppearanceProperty.REGISTRY_KEY);
    public static final DeferredRegister<DataType<?, ?>> DATAS = DeferredRegister.create(CustomMachinery.MODID, DataType.REGISTRY_KEY);
    public static final DeferredRegister<ProcessorType<?>> PROCESSORS = DeferredRegister.create(CustomMachinery.MODID, ProcessorType.REGISTRY_KEY);

    public static final Registrar<GuiElementType<? extends IGuiElement>> GUI_ELEMENT_TYPE_REGISTRY = REGISTRIES.builder(GuiElementType.REGISTRY_KEY.location(), new GuiElementType<?>[]{}).build();
    public static final Registrar<MachineComponentType<? extends IMachineComponent>> MACHINE_COMPONENT_TYPE_REGISTRY = REGISTRIES.builder(MachineComponentType.REGISTRY_KEY.location(), new MachineComponentType<?>[]{}).build();
    public static final Registrar<RequirementType<? extends IRequirement<?>>> REQUIREMENT_TYPE_REGISTRY = REGISTRIES.builder(RequirementType.REGISTRY_KEY.location(), new RequirementType<?>[]{}).build();
    public static final Registrar<MachineAppearanceProperty<?>> APPEARANCE_PROPERTY_REGISTRY = REGISTRIES.builder(MachineAppearanceProperty.REGISTRY_KEY.location(), new MachineAppearanceProperty<?>[]{}).build();
    public static final Registrar<DataType<?, ?>> DATA_REGISTRY = REGISTRIES.builder(DataType.REGISTRY_KEY.location(), new DataType<?, ?>[]{}).build();
    public static final Registrar<ProcessorType<?>> PROCESSOR_REGISTRY = REGISTRIES.builder(ProcessorType.REGISTRY_KEY.location(), new ProcessorType<?>[]{}).build();

    public static final RegistrySupplier<CustomMachineBlock> CUSTOM_MACHINE_BLOCK = BLOCKS.register("custom_machine_block", PlatformHelper::createMachineBlock);

    public static final RegistrySupplier<CustomMachineItem> CUSTOM_MACHINE_ITEM = ITEMS.register("custom_machine_item", () -> new CustomMachineItem(CUSTOM_MACHINE_BLOCK.get(), new Item.Properties().tab(GROUP)));
    public static final RegistrySupplier<MachineCreatorItem> MACHINE_CREATOR_ITEM = ITEMS.register("machine_creator_item", () ->  new MachineCreatorItem(new Item.Properties().tab(GROUP).stacksTo(1)));
    public static final RegistrySupplier<BoxCreatorItem> BOX_CREATOR_ITEM = ITEMS.register("box_creator_item", () -> new BoxCreatorItem(new Item.Properties().tab(GROUP).stacksTo(1)));
    public static final RegistrySupplier<StructureCreatorItem> STRUCTURE_CREATOR_ITEM = ITEMS.register("structure_creator", () -> new StructureCreatorItem(new Item.Properties().tab(GROUP).stacksTo(1)));

    public static final RegistrySupplier<BlockEntityType<CustomMachineTile>> CUSTOM_MACHINE_TILE = TILE_ENTITIES.register("custom_machine_tile", () -> BlockEntityType.Builder.of(PlatformHelper::createMachineTile, CUSTOM_MACHINE_BLOCK.get()).build(null));

    public static final RegistrySupplier<MenuType<CustomMachineContainer>> CUSTOM_MACHINE_CONTAINER = CONTAINERS.register("custom_machine_container", () -> MenuRegistry.ofExtended(CustomMachineContainer::new));

    public static final RegistrySupplier<CustomMachineRecipeSerializer> CUSTOM_MACHINE_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("custom_machine", CustomMachineRecipeSerializer::new);
    public static final RegistrySupplier<CustomCraftRecipeSerializer> CUSTOM_CRAFT_RECIPE_SERIALIZER = RECIPE_SERIALIZERS.register("custom_craft", CustomCraftRecipeSerializer::new);

    public static final RegistrySupplier<RecipeType<CustomMachineRecipe>> CUSTOM_MACHINE_RECIPE = RECIPE_TYPES.register("custom_machine", () -> new RecipeType<>() {});
    public static final RegistrySupplier<RecipeType<CustomCraftRecipe>> CUSTOM_CRAFT_RECIPE = RECIPE_TYPES.register("custom_craft", () -> new RecipeType<>() {});

    public static final RegistrySupplier<GuiElementType<EnergyGuiElement>> ENERGY_GUI_ELEMENT = GUI_ELEMENTS.register("energy", () -> GuiElementType.create(EnergyGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<FluidGuiElement>> FLUID_GUI_ELEMENT = GUI_ELEMENTS.register("fluid", () -> GuiElementType.create(FluidGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<PlayerInventoryGuiElement>> PLAYER_INVENTORY_GUI_ELEMENT = GUI_ELEMENTS.register("player_inventory", () -> GuiElementType.create(PlayerInventoryGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<ProgressBarGuiElement>> PROGRESS_GUI_ELEMENT = GUI_ELEMENTS.register("progress", () -> GuiElementType.create(ProgressBarGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<SlotGuiElement>> SLOT_GUI_ELEMENT = GUI_ELEMENTS.register("slot", () -> GuiElementType.create(SlotGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<StatusGuiElement>> STATUS_GUI_ELEMENT = GUI_ELEMENTS.register("status", () -> GuiElementType.create(StatusGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<TextureGuiElement>> TEXTURE_GUI_ELEMENT = GUI_ELEMENTS.register("texture", () -> GuiElementType.create(TextureGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<TextGuiElement>> TEXT_GUI_ELEMENT = GUI_ELEMENTS.register("text", () -> GuiElementType.create(TextGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<FuelGuiElement>> FUEL_GUI_ELEMENT = GUI_ELEMENTS.register("fuel", () -> GuiElementType.create(FuelGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<ResetGuiElement>> RESET_GUI_ELEMENT = GUI_ELEMENTS.register("reset", () -> GuiElementType.create(ResetGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<DumpGuiElement>> DUMP_GUI_ELEMENT = GUI_ELEMENTS.register("dump", () -> GuiElementType.create(DumpGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<SizeGuiElement>> SIZE_GUI_ELEMENT = GUI_ELEMENTS.register("size", () -> GuiElementType.create(SizeGuiElement.CODEC));
    public static final RegistrySupplier<GuiElementType<ConfigGuiElement>> CONFIG_GUI_ELEMENT = GUI_ELEMENTS.register("config", () -> GuiElementType.create(ConfigGuiElement.CODEC));

    public static final RegistrySupplier<MachineComponentType<EnergyMachineComponent>> ENERGY_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("energy", () -> MachineComponentType.create(EnergyMachineComponent.Template.CODEC).setGUIBuilder(EnergyComponentBuilder::new));
    public static final RegistrySupplier<MachineComponentType<FluidMachineComponent>> FLUID_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("fluid", () -> MachineComponentType.create(FluidMachineComponent.Template.CODEC).setNotSingle(FluidComponentHandler::new).setGUIBuilder(FluidComponentBuilder::new));
    public static final RegistrySupplier<MachineComponentType<ItemMachineComponent>> ITEM_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("item", () -> MachineComponentType.create(ItemMachineComponent.Template.CODEC).setNotSingle(ItemComponentHandler::new).setGUIBuilder(ItemComponentBuilder::new));
    public static final RegistrySupplier<MachineComponentType<PositionMachineComponent>> POSITION_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("position", () -> MachineComponentType.create(PositionMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<TimeMachineComponent>> TIME_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("time", () -> MachineComponentType.create(TimeMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<CommandMachineComponent>> COMMAND_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("command", () -> MachineComponentType.create(CommandMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<FuelMachineComponent>> FUEL_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("fuel", () -> MachineComponentType.create(FuelMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<EffectMachineComponent>> EFFECT_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("effect", () -> MachineComponentType.create(EffectMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<WeatherMachineComponent>> WEATHER_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("weather", () -> MachineComponentType.create(WeatherMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<RedstoneMachineComponent>> REDSTONE_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("redstone", () -> MachineComponentType.create(RedstoneMachineComponent.Template.CODEC, RedstoneMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<EntityMachineComponent>> ENTITY_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("entity", () -> MachineComponentType.create(EntityMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<LightMachineComponent>> LIGHT_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("light", () -> MachineComponentType.create(LightMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<BlockMachineComponent>> BLOCK_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("block", () -> MachineComponentType.create(BlockMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<StructureMachineComponent>> STRUCTURE_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("structure", () -> MachineComponentType.create(StructureMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<DropMachineComponent>> DROP_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("drop", () -> MachineComponentType.create(DropMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<FunctionMachineComponent>> FUNCTION_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("function", () -> MachineComponentType.create(FunctionMachineComponent::new));
    public static final RegistrySupplier<MachineComponentType<DataMachineComponent>> DATA_MACHINE_COMPONENT = MACHINE_COMPONENTS.register("data", () -> MachineComponentType.create(DataMachineComponent::new));

    public static final RegistrySupplier<RequirementType<ItemRequirement>> ITEM_REQUIREMENT = REQUIREMENTS.register("item", () -> RequirementType.inventory(ItemRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<EnergyRequirement>> ENERGY_REQUIREMENT = REQUIREMENTS.register("energy", () -> RequirementType.inventory(EnergyRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<EnergyPerTickRequirement>> ENERGY_PER_TICK_REQUIREMENT = REQUIREMENTS.register("energy_per_tick", () -> RequirementType.inventory(EnergyPerTickRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<FluidRequirement>> FLUID_REQUIREMENT = REQUIREMENTS.register("fluid", () -> RequirementType.inventory(FluidRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<FluidPerTickRequirement>> FLUID_PER_TICK_REQUIREMENT = REQUIREMENTS.register("fluid_per_tick", () -> RequirementType.inventory(FluidPerTickRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<PositionRequirement>> POSITION_REQUIREMENT = REQUIREMENTS.register("position", () -> RequirementType.world(PositionRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<TimeRequirement>> TIME_REQUIREMENT = REQUIREMENTS.register("time", () -> RequirementType.world(TimeRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<CommandRequirement>> COMMAND_REQUIREMENT = REQUIREMENTS.register("command", () -> RequirementType.world(CommandRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<FuelRequirement>> FUEL_REQUIREMENT = REQUIREMENTS.register("fuel", () -> RequirementType.inventory(FuelRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<EffectRequirement>> EFFECT_REQUIREMENT = REQUIREMENTS.register("effect", () -> RequirementType.world(EffectRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<WeatherRequirement>> WEATHER_REQUIREMENT = REQUIREMENTS.register("weather", () -> RequirementType.world(WeatherRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<RedstoneRequirement>> REDSTONE_REQUIREMENT = REQUIREMENTS.register("redstone", () -> RequirementType.world(RedstoneRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<EntityRequirement>> ENTITY_REQUIREMENT = REQUIREMENTS.register("entity", () -> RequirementType.world(EntityRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<LightRequirement>> LIGHT_REQUIREMENT = REQUIREMENTS.register("light", () -> RequirementType.world(LightRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<BlockRequirement>> BLOCK_REQUIREMENT = REQUIREMENTS.register("block", () -> RequirementType.world(BlockRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<SpeedRequirement>> SPEED_REQUIREMENT = REQUIREMENTS.register("speed", () -> RequirementType.inventory(SpeedRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<DurabilityRequirement>> DURABILITY_REQUIREMENT = REQUIREMENTS.register("durability", () -> RequirementType.inventory(DurabilityRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<StructureRequirement>> STRUCTURE_REQUIREMENT = REQUIREMENTS.register("structure", () -> RequirementType.world(StructureRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<LootTableRequirement>> LOOT_TABLE_REQUIREMENT = REQUIREMENTS.register("loot_table", () -> RequirementType.inventory(LootTableRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<BiomeRequirement>> BIOME_REQUIREMENT = REQUIREMENTS.register("biome", () -> RequirementType.world(BiomeRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<DimensionRequirement>> DIMENSION_REQUIREMENT = REQUIREMENTS.register("dimension", () -> RequirementType.world(DimensionRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<DropRequirement>> DROP_REQUIREMENT = REQUIREMENTS.register("drop", () -> RequirementType.world(DropRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<FunctionRequirement>> FUNCTION_REQUIREMENT = REQUIREMENTS.register("function", () -> RequirementType.world(FunctionRequirement.CODEC));
    public static final RegistrySupplier<RequirementType<ItemTransformRequirement>> ITEM_TRANSFORM_REQUIREMENT = REQUIREMENTS.register("item_transform", () -> RequirementType.inventory(ItemTransformRequirement.CODEC));

    public static final RegistrySupplier<MachineAppearanceProperty<ModelLocation>> BLOCK_MODEL_PROPERTY = APPEARANCE_PROPERTIES.register("block", () -> MachineAppearanceProperty.create(Codecs.BLOCK_MODEL_CODEC, ModelLocation.of(new ResourceLocation(CustomMachinery.MODID, "block/custom_machine_block"))));
    public static final RegistrySupplier<MachineAppearanceProperty<ModelLocation>> ITEM_MODEL_PROPERTY = APPEARANCE_PROPERTIES.register("item", () -> MachineAppearanceProperty.create(Codecs.ITEM_MODEL_CODEC, ModelLocation.of(new ResourceLocation(CustomMachinery.MODID, "block/custom_machine_block"))));
    public static final RegistrySupplier<MachineAppearanceProperty<SoundEvent>> AMBIENT_SOUND_PROPERTY = APPEARANCE_PROPERTIES.register("ambient_sound", () -> MachineAppearanceProperty.create(DefaultCodecs.SOUND_EVENT, new SoundEvent(new ResourceLocation(""))));
    public static final RegistrySupplier<MachineAppearanceProperty<CMSoundType>> INTERACTION_SOUND_PROPERTY = APPEARANCE_PROPERTIES.register("interaction_sound", () -> MachineAppearanceProperty.create(CMSoundType.CODEC, CMSoundType.DEFAULT));
    public static final RegistrySupplier<MachineAppearanceProperty<Integer>> LIGHT_PROPERTY = APPEARANCE_PROPERTIES.register("light", () -> MachineAppearanceProperty.create(NamedCodec.intRange(0, 15), 0));
    public static final RegistrySupplier<MachineAppearanceProperty<Integer>> COLOR_PROPERTY = APPEARANCE_PROPERTIES.register("color", () -> MachineAppearanceProperty.create(NamedCodec.INT, 0xFFFFFF));
    public static final RegistrySupplier<MachineAppearanceProperty<Float>> HARDNESS_PROPERTY = APPEARANCE_PROPERTIES.register("hardness", () -> MachineAppearanceProperty.create(NamedCodec.floatRange(-1.0F, Float.MAX_VALUE), 3.5F));
    public static final RegistrySupplier<MachineAppearanceProperty<Float>> RESISTANCE_PROPERTY = APPEARANCE_PROPERTIES.register("resistance", () -> MachineAppearanceProperty.create(NamedCodec.floatRange(0.0F, Float.MAX_VALUE), 3.5F));
    public static final RegistrySupplier<MachineAppearanceProperty<List<TagKey<Block>>>> TOOL_TYPE_PROPERTY = APPEARANCE_PROPERTIES.register("tool_type", () -> MachineAppearanceProperty.create(DefaultCodecs.tagKey(Registry.BLOCK_REGISTRY).listOf(), Collections.singletonList(BlockTags.MINEABLE_WITH_PICKAXE)));
    public static final RegistrySupplier<MachineAppearanceProperty<TagKey<Block>>> MINING_LEVEL_PROPERTY = APPEARANCE_PROPERTIES.register("mining_level", () -> MachineAppearanceProperty.create(DefaultCodecs.tagKey(Registry.BLOCK_REGISTRY), BlockTags.NEEDS_IRON_TOOL));
    public static final RegistrySupplier<MachineAppearanceProperty<Boolean>> REQUIRES_TOOL = APPEARANCE_PROPERTIES.register("requires_tool", () -> MachineAppearanceProperty.create(NamedCodec.BOOL, true));
    public static final RegistrySupplier<MachineAppearanceProperty<MachineShape>> SHAPE_PROPERTY = APPEARANCE_PROPERTIES.register("shape", () -> MachineAppearanceProperty.create(MachineShape.CODEC, MachineShape.DEFAULT));

    public static final RegistrySupplier<DataType<BooleanData, Boolean>> BOOLEAN_DATA = DATAS.register("boolean", () -> DataType.create(Boolean.class, BooleanSyncable::create, BooleanData::new));
    public static final RegistrySupplier<DataType<IntegerData, Integer>> INTEGER_DATA = DATAS.register("integer", () -> DataType.create(Integer.class, IntegerSyncable::create, IntegerData::new));
    public static final RegistrySupplier<DataType<DoubleData, Double>> DOUBLE_DATA = DATAS.register("double", () -> DataType.create(Double.class, DoubleSyncable::create, DoubleData::new));
    public static final RegistrySupplier<DataType<ItemStackData, ItemStack>> ITEMSTACK_DATA = DATAS.register("itemstack", () -> DataType.create(ItemStack.class, ItemStackSyncable::create, ItemStackData::new));
    public static final RegistrySupplier<DataType<FluidStackData, FluidStack>> FLUIDSTACK_DATA = DATAS.register("fluidstack", () -> DataType.create(FluidStack.class, FluidStackSyncable::create, FluidStackData::new));
    public static final RegistrySupplier<DataType<StringData, String>> STRING_DATA = DATAS.register("string", () -> DataType.create(String.class, StringSyncable::create, StringData::new));
    public static final RegistrySupplier<DataType<LongData, Long>> LONG_DATA = DATAS.register("long", () -> DataType.create(Long.class, LongSyncable::create, LongData::new));
    public static final RegistrySupplier<DataType<SideConfigData, SideConfig>> SIDE_CONFIG_DATA = DATAS.register("side_config", () -> DataType.create(SideConfig.class, SideConfigSyncable::create, SideConfigData::readData));

    public static final RegistrySupplier<ProcessorType<DummyProcessor>> DUMMY_PROCESSOR = PROCESSORS.register("dummy", () -> ProcessorType.create(DummyProcessor.Template.CODEC));
    public static final RegistrySupplier<ProcessorType<MachineProcessor>> MACHINE_PROCESSOR = PROCESSORS.register("machine", () -> ProcessorType.create(MachineProcessor.Template.CODEC));
    public static final RegistrySupplier<ProcessorType<CraftProcessor>> CRAFT_PROCESSOR = PROCESSORS.register("craft", () -> ProcessorType.create(CraftProcessor.Template.CODEC));

    public static void registerComponentVariants(RegisterComponentVariantEvent event) {
        event.register(ITEM_MACHINE_COMPONENT.get(), DefaultItemComponentVariant.ID, DefaultItemComponentVariant.CODEC);
        event.register(ITEM_MACHINE_COMPONENT.get(), EnergyItemComponentVariant.ID, EnergyItemComponentVariant.CODEC);
        event.register(ITEM_MACHINE_COMPONENT.get(), FluidItemComponentVariant.ID, FluidItemComponentVariant.CODEC);
        event.register(ITEM_MACHINE_COMPONENT.get(), FuelItemComponentVariant.ID, FuelItemComponentVariant.CODEC);
        event.register(ITEM_MACHINE_COMPONENT.get(), ResultItemComponentVariant.ID, ResultItemComponentVariant.CODEC);
        event.register(ITEM_MACHINE_COMPONENT.get(), UpgradeItemComponentVariant.ID, UpgradeItemComponentVariant.CODEC);
    }
}
