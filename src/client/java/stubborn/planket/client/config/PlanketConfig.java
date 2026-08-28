package stubborn.planket.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import static stubborn.planket.client.PlanketClient.LOGGER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PlanketConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("planket.json");
    private static PlanketConfig INSTANCE;

    public static final boolean DEFAULT_INVENTORY_TAB = false;
    public static final int DEFAULT_CREATIVE_ROWS = 6;
    public static final boolean DEFAULT_SCROLL_MEMORY = true;

    // 默认不启用 Inventory 标签页（即隐藏它）
    //为了适配可变的行数，inventory实际上已经不可用了
    public boolean enableInventoryTab = DEFAULT_INVENTORY_TAB;
    public int creativeRows = DEFAULT_CREATIVE_ROWS;
    public boolean enableScrollMemory = DEFAULT_SCROLL_MEMORY;
    //slot num
    public static final int slotMaxCapacity = 135;

    public static PlanketConfig getInstance() {
        if (INSTANCE == null) {
            load();
        }
        return INSTANCE;
    }

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                INSTANCE = GSON.fromJson(json, PlanketConfig.class);
            } catch (IOException e) {
                LOGGER.error("Errors when trying to load config: ", e);
                INSTANCE = new PlanketConfig();
            }
        } else {
            INSTANCE = new PlanketConfig();
            INSTANCE.save();    // 通过实例调用
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Errors when trying to save config: ", e);
        }
    }
}