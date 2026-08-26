package stubborn.planket.client.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.gui.handlers.IGuiProperties;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public class PlanketJEIPlugin {

}

/*@JeiPlugin
public class PlanketJEIPlugin implements IModPlugin {

    private static final Identifier PLUGIN_ID = Identifier.fromNamespaceAndPath("planket", "jei_plugin");

    @Override
    public @NotNull Identifier getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiScreenHandler(CreativeModeInventoryScreen.class, (screen) -> {
            return new IGuiProperties() {
                @Override
                public @NotNull Class<? extends Screen> screenClass() {
                    return CreativeModeInventoryScreen.class;
                }

                @Override
                public int guiLeft() {
                    return 0; // 撑满左边
                }

                @Override
                public int guiTop() {
                    return 0;
                }

                @Override
                public int guiXSize() {
                    return screen.width; // 独占整宽
                }

                @Override
                public int guiYSize() {
                    return screen.height;
                }

                @Override
                public int screenWidth() {
                    return screen.width;
                }

                @Override
                public int screenHeight() {
                    return screen.height;
                }
            };
        });
    }
}*/