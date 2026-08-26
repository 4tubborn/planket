package stubborn.planket.client.util;

public interface ScreenInterface {
    int planket$getInventoryTopPos();
    int planket$getInventoryWidth();
    int planket$getInventoryHeight();

    // 注入到 keyPressed 方法的首部
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
}
