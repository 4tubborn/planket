package stubborn.planket.client.handler;

import net.minecraft.world.item.CreativeModeTab;

import java.util.IdentityHashMap;
import java.util.Map;

public final class CreativeScrollManager {

    private CreativeScrollManager() {
    }

    private static final Map<CreativeModeTab, Float> SCROLL_POSITIONS =
            new IdentityHashMap<>();

    // 记录上一次选中的标签页，用于 Ctrl + L 回滚和记忆
    public static CreativeModeTab lastSelectedTab = null;

    /**
     * 当标签页发生改变时调用，用来追踪历史记录
     */
    public static void updateLastTab(CreativeModeTab currentTab) {
        if (currentTab != null && currentTab != CreativeScrollManager.lastSelectedTab) {
            CreativeScrollManager.lastSelectedTab = currentTab;
        }
    }

    public static float get(CreativeModeTab tab) {
        return SCROLL_POSITIONS.getOrDefault(tab, 0.0F);
    }

    public static void set(CreativeModeTab tab, float scroll) {
        SCROLL_POSITIONS.put(tab, scroll);
    }
}