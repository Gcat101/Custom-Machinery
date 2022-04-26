package fr.frinn.custommachinery.client.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import fr.frinn.custommachinery.api.guielement.GuiElementType;
import fr.frinn.custommachinery.api.guielement.IMachineScreen;
import fr.frinn.custommachinery.common.data.CustomMachine;
import fr.frinn.custommachinery.common.data.gui.SizeGuiElement;
import fr.frinn.custommachinery.common.init.CustomMachineContainer;
import fr.frinn.custommachinery.common.init.CustomMachineTile;
import fr.frinn.custommachinery.common.network.CGuiElementClickPacket;
import fr.frinn.custommachinery.common.network.NetworkManager;
import fr.frinn.custommachinery.common.util.Comparators;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class CustomMachineScreen extends ContainerScreen<CustomMachineContainer> implements IMachineScreen {

    private final CustomMachineTile tile;
    private final CustomMachine machine;

    public CustomMachineScreen(CustomMachineContainer container, PlayerInventory inv, ITextComponent name) {
        super(container, inv, name);
        this.tile = container.tile;
        this.machine = container.tile.getMachine();
        this.xSize = 256;
        this.ySize = 192;
        this.machine.getGuiElements().stream()
                .filter(element -> element instanceof SizeGuiElement)
                .map(element -> (SizeGuiElement)element)
                .findFirst()
                .ifPresent(size -> {
                    this.xSize = size.getWidth();
                    this.ySize = size.getHeight();
                });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void drawGuiContainerBackgroundLayer(MatrixStack matrix, float partialTicks, int mouseX, int mouseY) {
        this.renderBackground(matrix);

        matrix.push();
        matrix.translate(this.guiLeft, this.guiTop, 0);
        this.machine.getGuiElements()
                .stream()
                .sorted(Comparators.GUI_ELEMENTS_COMPARATOR.reversed())
                .forEach(element -> ((GuiElementType)element.getType()).getRenderer().renderElement(matrix, element, this));
        matrix.pop();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    protected void drawGuiContainerForegroundLayer(MatrixStack matrix, int mouseX, int mouseY) {
        matrix.push();
        matrix.translate(-this.guiLeft, -this.guiTop, 0);
        this.renderHoveredTooltip(matrix, mouseX, mouseY);
        this.machine.getGuiElements()
                .stream()
                .filter(element -> ((GuiElementType)element.getType()).getRenderer().isHovered(element, this, mouseX - this.guiLeft, mouseY - this.guiTop))
                .max(Comparators.GUI_ELEMENTS_COMPARATOR)
                .ifPresent(element -> ((GuiElementType)element.getType()).getRenderer().renderTooltip(matrix, element, this, mouseX, mouseY));
        matrix.pop();
    }

    @Override
    public CustomMachine getMachine() {
        return this.machine;
    }

    @Override
    public CustomMachineTile getTile() {
        return this.tile;
    }

    @Override
    public CustomMachineScreen getScreen() {
        return this;
    }

    @SuppressWarnings("deprecation")
    public void renderTransparentItem(MatrixStack matrix, ItemStack stack, int posX, int posY) {
        RenderSystem.pushMatrix();
        RenderSystem.multMatrix(matrix.getLast().getMatrix());
        this.itemRenderer.renderItemAndEffectIntoGUI(stack, posX, posY);
        RenderSystem.popMatrix();
        RenderSystem.depthFunc(516);
        AbstractGui.fill(matrix, posX, posY, posX + 16, posY + 16, 822083583);
        RenderSystem.depthFunc(515);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        this.machine.getGuiElements().stream()
                .filter(element -> ((GuiElementType)element.getType()).getRenderer().isHovered(element, this, (int)mouseX - this.guiLeft, (int)mouseY - this.guiTop))
                .findFirst()
                .ifPresent(element -> NetworkManager.CHANNEL.sendToServer(new CGuiElementClickPacket(this.machine.getGuiElements().indexOf(element), (byte) button)));
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
