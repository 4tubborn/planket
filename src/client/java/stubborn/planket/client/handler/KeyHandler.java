package stubborn.planket.client.handler;

import net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import org.lwjgl.glfw.GLFW;

import  net.fabricmc.fabric.api.client.itemgroup.v1.FabricCreativeInventoryScreen;


import java.util.List;

public class KeyHandler {

    /**
     * 核心快捷键路由方法
     * @return 如果触发了自定义快捷键并消耗了事件，返回 true；否则返回 false 让原版继续处理
     */
    public static boolean handleKeyPressed(CreativeModeInventoryScreen screen, KeyEvent key) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return false;
        }

        FabricCreativeInventoryScreen ext = (FabricCreativeInventoryScreen) screen;

        int currentPage = ext.getCurrentPage();
        int pageCount = ext.getPageCount();

        List<CreativeModeTab> pageTabs = ext.getItemGroupsOnPage(currentPage);
        if (pageTabs.isEmpty()) {
            return false;
        }

        CreativeModeTab currentTab = ext.getSelectedItemGroup();

        int keyCode = key.key();
        int modifiers = key.modifiers();

        boolean ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;

        if (!ctrl) {
            return false;
        }

        // Ctrl + Tab / Ctrl + Shift + Tab
        if (keyCode == GLFW.GLFW_KEY_TAB) {

            int index = pageTabs.indexOf(currentTab);
            if (index < 0) {
                index = 0;
            }

            int next;

            if (shift) {
                next = (index - 1 + pageTabs.size()) % pageTabs.size();
            } else {
                next = (index + 1) % pageTabs.size();
            }

            switchToTab(screen, pageTabs.get(next));
            return true;
        }

        // Ctrl + 1~9
        if (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9) {

            if (keyCode == GLFW.GLFW_KEY_9) {
                switchToTab(screen, pageTabs.getLast());
            } else {
                int target = keyCode - GLFW.GLFW_KEY_1;

                if (target >= pageTabs.size()) {target = pageTabs.size() - 1;}

                switchToTab(screen, pageTabs.get(target));
            }

            return true;
        }

        // Ctrl + F
        if (keyCode == GLFW.GLFW_KEY_F) {
            //若不存在search tab原版会直接throw
            CreativeModeTab searchTab = CreativeModeTabs.searchTab();

            if (searchTab.shouldDisplay()) {
                switchToTab(screen, searchTab);
            }

            return true;
        }

        // Ctrl + L
        if (keyCode == GLFW.GLFW_KEY_L) {

            if (CreativeScrollManager.lastSelectedTab != null) {

                int targetPage = ext.getPage(CreativeScrollManager.lastSelectedTab);

                if (pageCount > 1 && targetPage != currentPage) {
                    ext.switchToPage(targetPage);
                    //screen.updateLayout();
                }

                switchToTab(screen, CreativeScrollManager.lastSelectedTab);
            }

            return true;
        }

        // Ctrl + ↑ / ↓
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {

            boolean changed;

            if (keyCode == GLFW.GLFW_KEY_UP) {
                changed = ext.switchToPreviousPage();
            } else {
                changed = ext.switchToNextPage();
            }

            if (changed) {

                //screen.updateLayout();

                List<CreativeModeTab> newTabs =
                        ext.getItemGroupsOnPage(ext.getCurrentPage());

                if (!newTabs.isEmpty()) {
                    switchToTab(screen, newTabs.getFirst());
                }
            }

            return true;
        }

        return false;
    }

    // 切换标签页
    private static void switchToTab(CreativeModeInventoryScreen screen, CreativeModeTab targetTab) {

        if (targetTab == null) {
            return;
        }

        CreativeModeTab current = screen.getSelectedItemGroup();

        if (current == targetTab) {
            return;
        }

        CreativeScrollManager.lastSelectedTab = current;

        screen.setSelectedItemGroup(targetTab);

        screen.selectTab(targetTab);
    }

}