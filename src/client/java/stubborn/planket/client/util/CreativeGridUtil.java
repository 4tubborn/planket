package stubborn.planket.client.util;

import net.minecraft.util.Mth;
import stubborn.planket.client.config.PlanketConfig;

public final class CreativeGridUtil {
    private CreativeGridUtil() {}

    public static final int COLUMNS = 9;

    public static int getVisibleRows() {
        return PlanketConfig.getInstance().creativeRows;
    }

    public static int getVisibleSlotCount() {
        return getVisibleRows() * COLUMNS;
    }

    public static int getHotbarStartIndex() {
        return getVisibleSlotCount();
    }

    public static int getHotbarEndIndexExclusive() {
        //hotbar count: 9
        return getHotbarStartIndex() + 9;
    }
}