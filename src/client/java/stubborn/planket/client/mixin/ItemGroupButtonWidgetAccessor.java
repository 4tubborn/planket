package stubborn.planket.client.mixin;

import net.fabricmc.fabric.impl.client.itemgroup.FabricCreativeGuiComponents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FabricCreativeGuiComponents.ItemGroupButtonWidget.class)
public interface ItemGroupButtonWidgetAccessor {

    @Accessor("type")
    FabricCreativeGuiComponents.Type planket$getType();
}