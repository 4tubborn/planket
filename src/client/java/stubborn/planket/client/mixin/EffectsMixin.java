package stubborn.planket.client.mixin;

import com.google.common.collect.Ordering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stubborn.planket.client.util.ScreenInterface;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

//修改effect的渲染
@Mixin(EffectsInInventory.class)
public class EffectsMixin {
    @Mutable
    @Final
    @Shadow
    private final AbstractContainerScreen<?> screen;
    @Mutable
    @Final
    @Shadow
    private final Minecraft minecraft;


    @Shadow
    private int renderBackground(GuiGraphics guiGraphics, Font font, net.minecraft.network.chat.Component component, net.minecraft.network.chat.Component component2, int i, int j, boolean bl, int k) {
        return 0;
    }

    @Shadow
    private void renderText(GuiGraphics guiGraphics, net.minecraft.network.chat.Component component, net.minecraft.network.chat.Component component2, Font font, int i, int j, int k, int l, int m, int n) {

    }

    @Shadow
    private net.minecraft.network.chat.Component getEffectName(MobEffectInstance mobEffectInstance) {
        return null;
    }

    public EffectsMixin(AbstractContainerScreen<?> screen, Minecraft minecraft) {
        this.screen = screen;
        this.minecraft = minecraft;
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void stubborn$render(GuiGraphics guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (this.minecraft.player == null) return;

        Collection<MobEffectInstance> effects = this.minecraft.player.getActiveEffects();
        if (effects.isEmpty()) return;

        if (!(this.screen instanceof CreativeModeInventoryScreen)
                || !(this.screen instanceof ScreenInterface planketScreen)) {
            return;
        }

        int startX = this.screen.leftPos + this.screen.imageWidth + 2;
        int startY = planketScreen.planket$getInventoryTopPos()
                + planketScreen.planket$getInventoryHeight() + 3;

        int availableWidth = this.screen.width - startX;
        int availableHeight = this.screen.height - startY - 5;

        if (availableWidth < 32 || availableHeight < 32) return;

        this.stubborn$renderHorizontalEffects(
                guiGraphics, effects, startX, startY, mouseX, mouseY,
                availableWidth, availableHeight);

        ci.cancel();
    }

    @Unique
    private void stubborn$renderHorizontalEffects(
            GuiGraphics guiGraphics, Collection<MobEffectInstance> effects,
            int startX, int startY, int mouseX, int mouseY,
            int availableWidth, int availableHeight
    ) {
        List<MobEffectInstance> sortedEffects = Ordering.natural().sortedCopy(effects);
        Font font = this.screen.getFont();

        final int gap = 2;
        final int effectHeight = 32;
        final int rowStep = effectHeight + gap;

        int[] fullWidths = new int[sortedEffects.size()];

        for (int i = 0; i < sortedEffects.size(); i++) {
            fullWidths[i] = this.stubborn$getFullEffectWidth(sortedEffects.get(i), font);
        }

        int fullRows = this.stubborn$calculateRows(fullWidths, availableWidth, gap);
        int compactRows = this.stubborn$calculateFixedRows(
                sortedEffects.size(), 32, availableWidth, gap
        );

        int fullHeight = fullRows * effectHeight + Math.max(0, fullRows - 1) * gap;
        int compactHeight = compactRows * effectHeight + Math.max(0, compactRows - 1) * gap;

        if (fullHeight <= availableHeight) {
            this.stubborn$renderWrappedEffects(
                    guiGraphics, sortedEffects, fullWidths,
                    startX, startY, mouseX, mouseY,
                    availableWidth, gap, rowStep
            );
        } else if (compactHeight <= availableHeight) {
            this.stubborn$renderCompactEffects(
                    guiGraphics, sortedEffects,
                    startX, startY, mouseX, mouseY,
                    availableWidth, gap, rowStep
            );
        } else {
            this.stubborn$renderCompressedEffects(
                    guiGraphics, sortedEffects,
                    startX, startY, mouseX, mouseY,
                    availableWidth
            );
        }
    }

    @Unique
    private int stubborn$getFullEffectWidth(MobEffectInstance effect, Font font) {
        Component name = this.getEffectName(effect);
        Component duration = MobEffectUtil.formatDuration(
                effect, 1.0F, this.minecraft.level.tickRateManager().tickrate()
        );

        return Math.min(
                120,
                32 + Math.max(font.width(name), font.width(duration)) + 7
        );
    }

    @Unique
    private int stubborn$calculateRows(int[] widths, int availableWidth, int gap) {
        if (widths.length == 0) return 0;

        int rows = 1;
        int occupied = 0;

        for (int width : widths) {
            if (occupied == 0) {
                occupied = width;
            } else if (occupied + gap + width > availableWidth) {
                rows++;
                occupied = width;
            } else {
                occupied += gap + width;
            }
        }

        return rows;
    }

    @Unique
    private int stubborn$calculateFixedRows(int count, int width, int availableWidth, int gap) {
        if (count == 0) return 0;

        int rows = 1;
        int occupied = 0;

        for (int i = 0; i < count; i++) {
            if (occupied == 0) {
                occupied = width;
            } else if (occupied + gap + width > availableWidth) {
                rows++;
                occupied = width;
            } else {
                occupied += gap + width;
            }
        }

        return rows;
    }

    @Unique
    private void stubborn$renderWrappedEffects(
            GuiGraphics guiGraphics, List<MobEffectInstance> effects, int[] widths,
            int startX, int startY, int mouseX, int mouseY,
            int availableWidth, int gap, int rowStep
    ) {
        Font font = this.screen.getFont();
        int x = startX;
        int y = startY;

        for (int i = 0; i < effects.size(); i++) {
            int width = widths[i];

            if (x != startX && x + width > startX + availableWidth) {
                x = startX;
                y += rowStep;
            }

            int actualWidth = this.stubborn$renderSingleEffect(
                    guiGraphics, effects.get(i), font,
                    x, y, width, rowStep,
                    mouseX, mouseY
            );

            x += actualWidth + gap;
        }
    }

    @Unique
    private void stubborn$renderCompactEffects(
            GuiGraphics guiGraphics, List<MobEffectInstance> effects,
            int startX, int startY, int mouseX, int mouseY,
            int availableWidth, int gap, int rowStep
    ) {
        Font font = this.screen.getFont();
        int x = startX;
        int y = startY;

        for (MobEffectInstance effect : effects) {
            if (x != startX && x + 32 > startX + availableWidth) {
                x = startX;
                y += rowStep;
            }

            this.stubborn$renderSingleEffect(
                    guiGraphics, effect, font,
                    x, y, 32, rowStep,
                    mouseX, mouseY
            );

            x += 32 + gap;
        }
    }

    @Unique
    private void stubborn$renderCompressedEffects(
            GuiGraphics guiGraphics, List<MobEffectInstance> effects,
            int startX, int startY, int mouseX, int mouseY,
            int availableWidth
    ) {
        Font font = this.screen.getFont();
        int count = effects.size();

        int step = 33;
        if (count > 1) {
            step = Mth.clamp((availableWidth - 32) / (count - 1), 1, 33);
        }

        int x = startX;

        for (MobEffectInstance effect : effects) {
            this.stubborn$renderSingleEffect(
                    guiGraphics, effect, font,
                    x, startY, 32, 33,
                    mouseX, mouseY
            );

            x += step;
        }
    }

    @Unique
    private int stubborn$renderSingleEffect(
            GuiGraphics guiGraphics, MobEffectInstance effect, Font font,
            int x, int y, int maxWidth, int verticalSpacing,
            int mouseX, int mouseY
    ) {
        Component name = this.getEffectName(effect);
        Component duration = MobEffectUtil.formatDuration(
                effect, 1.0F, this.minecraft.level.tickRateManager().tickrate()
        );

        int actualWidth = this.renderBackground(
                guiGraphics, font, name, duration,
                x, y, effect.isAmbient(), maxWidth
        );

        this.renderText(
                guiGraphics, name, duration, font,
                x, y, actualWidth, verticalSpacing,
                mouseX, mouseY
        );

        guiGraphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                Gui.getMobEffectSprite(effect.getEffect()),
                x + 7, y + 7, 18, 18
        );

        return actualWidth;
    }
}
