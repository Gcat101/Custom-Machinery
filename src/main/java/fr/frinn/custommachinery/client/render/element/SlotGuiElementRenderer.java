package fr.frinn.custommachinery.client.render.element;

import com.mojang.blaze3d.matrix.MatrixStack;
import fr.frinn.custommachinery.client.TextureSizeHelper;
import fr.frinn.custommachinery.client.screen.CustomMachineScreen;
import fr.frinn.custommachinery.common.data.gui.SlotGuiElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;

public class SlotGuiElementRenderer implements IGuiElementRenderer<SlotGuiElement> {

    @Override
    public void renderElement(MatrixStack matrix, SlotGuiElement element, CustomMachineScreen screen) {
        int posX = element.getX();
        int posY = element.getY();
        int width = element.getWidth();
        if(width < 0)
            width = TextureSizeHelper.getTextureWidth(element.getTexture());
        int height = element.getHeight();
        if(height < 0)
            height = TextureSizeHelper.getTextureHeight(element.getTexture());
        Minecraft.getInstance().getTextureManager().bindTexture(element.getTexture());
        AbstractGui.blit(matrix, posX, posY, 0, 0, width, height, width, height);
    }

    @Override
    public void renderTooltip(MatrixStack matrix, SlotGuiElement element, CustomMachineScreen screen, int mouseX, int mouseY) {

    }

    @Override
    public boolean isHovered(SlotGuiElement element, CustomMachineScreen screen, int mouseX, int mouseY) {
        int posX = element.getX();
        int posY = element.getY();
        int width = element.getWidth() > 0 ? element.getWidth() : TextureSizeHelper.getTextureWidth(element.getTexture());
        int height = element.getHeight() > 0 ? element.getHeight() : TextureSizeHelper.getTextureHeight(element.getTexture());
        return mouseX >= posX && mouseX <= posX + width && mouseY >= posY && mouseY <= posY + height;
    }
}
