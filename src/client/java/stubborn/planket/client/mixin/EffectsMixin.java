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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import stubborn.planket.client.util.ScreenInterface;

import java.util.Collection;

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
    public void render(GuiGraphics guiGraphics, int i, int j, CallbackInfo ci) {
        Collection<MobEffectInstance> collection = this.minecraft.player.getActiveEffects();
        if (collection.isEmpty()) return;

        if (!(this.screen instanceof CreativeModeInventoryScreen) || !(this.screen instanceof ScreenInterface Pl)) {
            return;
        }

        // 获取起始坐标（右侧背包下方）
        int startX = this.screen.leftPos + this.screen.imageWidth + 2;
        int startY = Pl.planket$getInventoryTopPos() + Pl.planket$getInventoryHeight() + 3;

        // 计算可用总宽度（到屏幕右边缘的距离）
        int maxWidth = this.screen.width - startX;
        if (maxWidth < 32) return; // 极小空间不渲染

        this.stubborn$renderHorizontalEffects(guiGraphics, collection, startX, startY, i, j, maxWidth);

        ci.cancel();
    }

    @Unique
    private void stubborn$renderHorizontalEffects(GuiGraphics guiGraphics, Collection<MobEffectInstance> collection, int startX, int startY, int mouseX, int mouseY, int totalWidth) {
        var sortedEffects = Ordering.natural().sortedCopy(collection);
        Font font = this.screen.getFont();
        int GAP = 2;
        int HEIGHT_STEP = 33;

        // 假设可用的最大高度，可以根据你的 Planket 底部边缘动态计算
        // 比如：this.screen.height - startY - 5
        int availableHeight = this.screen.height - startY - 5;

        // --- 第一步：预计算动态宽度并评估所需高度 ---
        int totalExpectedWidth = 0;
        int[] cachedWidths = new int[sortedEffects.size()];
        int estimatedRows = 1;
        int currentLineOccupied = 0;

        for (int i = 0; i < sortedEffects.size(); i++) {
            MobEffectInstance effect = sortedEffects.get(i);
            int textW = 0;
            if (this.getEffectName(effect) != null) {
                textW = Math.max(font.width(this.getEffectName(effect)),
                        font.width(MobEffectUtil.formatDuration(effect, 1.0F, this.minecraft.level.tickRateManager().tickrate())));
            }
            int w = Math.min(120, 32 + textW + 7);
            cachedWidths[i] = w;
            totalExpectedWidth += w + GAP;

            // 模拟换行逻辑来估算总高度
            if (currentLineOccupied + w > totalWidth) {
                estimatedRows++;
                currentLineOccupied = w + GAP;
            } else {
                currentLineOccupied += w + GAP;
            }
        }

        // --- 第二步：核心决策逻辑 ---
        boolean isCompact = totalExpectedWidth > totalWidth;
        boolean forceSingleRow = (estimatedRows * HEIGHT_STEP) > availableHeight;

        int currentX = startX;
        int currentY = startY;

        // 如果高度不够，哪怕本该换行，我们也强行挤在一行（类似原版 logic）
        if (forceSingleRow) {
            // 计算极致紧凑下的间距（可能会重叠图标，类似原版 5 个以上效果的处理）
            int n = 33;
            if (sortedEffects.size() > 1) {
                // totalWidth 减去最后一个图标的宽度(32)，平摊剩下的空间
                n = (totalWidth - 32) / (sortedEffects.size() - 1);
                n = Math.min(n, 33); // 最大间距不超过原版宽度
            }

            Iterable<MobEffectInstance> iterable = Ordering.natural().sortedCopy(collection);

            // 极致紧凑渲染循环
            for (MobEffectInstance effect : iterable) {
                //MobEffectInstance effect = sortedEffects.get(i);
                //int x = startX + i * n;
                // 极致紧凑模式 m 传 32（只画图标）
                Component time = MobEffectUtil.formatDuration(effect, 1.0F, this.minecraft.level.tickRateManager().tickrate());

                int actualWidth = this.renderBackground(guiGraphics, font, this.getEffectName(effect),
                        time, currentX, startY, effect.isAmbient(), 32);

                this.renderText(guiGraphics, this.getEffectName(effect), time,
                        font, currentX, startY, actualWidth, HEIGHT_STEP, mouseX, mouseY);

                guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()), currentX + 7, startY + 7, 18, 18);

                currentX += n;
            }
            return; // 处理完极致紧凑模式直接返回
        }

        // --- 第三步：正常的换行排列渲染（高度足够时） ---
        int m = isCompact ? 32 : 120;

        for (int i = 0; i < sortedEffects.size(); i++) {
            MobEffectInstance effect = sortedEffects.get(i);
            int effectWidth = isCompact ? 32 : cachedWidths[i];

            if (currentX + effectWidth > startX + totalWidth) {
                currentX = startX;
                currentY += HEIGHT_STEP + GAP;
            }

            Component time = MobEffectUtil.formatDuration(effect, 1.0F, this.minecraft.level.tickRateManager().tickrate());;

            int actualWidth = this.renderBackground(guiGraphics, font, this.getEffectName(effect),
                    time, currentX, currentY, effect.isAmbient(), m);

            this.renderText(guiGraphics, this.getEffectName(effect), time,
                    font, currentX, currentY, actualWidth, HEIGHT_STEP, mouseX, mouseY);

            guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, Gui.getMobEffectSprite(effect.getEffect()), currentX + 7, currentY + 7, 18, 18);
            currentX += actualWidth + GAP;
        }
    }
}
