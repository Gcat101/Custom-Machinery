package fr.frinn.custommachinery.common.init;

import dev.architectury.platform.Platform;
import fr.frinn.custommachinery.client.ClientHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MachineCreatorItem extends Item {

    public MachineCreatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        if(world.isClientSide() && Platform.isDevelopmentEnvironment())
            ClientHandler.openMachineLoadingScreen();
        return super.use(world, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level worldIn, List<Component> tooltips, TooltipFlag flagIn) {
        tooltips.add(new TranslatableComponent("custommachinery.machine_creator.warning").withStyle(ChatFormatting.DARK_RED));
    }
}
