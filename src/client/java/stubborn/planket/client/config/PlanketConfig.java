package stubborn.planket.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
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
    //slot num
    public static final int DEFAULT_CREATIVE_ROWS = 6;
    public static final boolean DEFAULT_SCROLL_MEMORY = true;
    public static final int DEFAULT_MAX_CREATIVE_ROWS = 15;
    public static final int MAX_CREATIVE_ROWS_LIMIT = 100;
    public static final boolean DEFAULT_IGNORE_RIGHT_TABS = false;

    // 默认不启用 Inventory 标签页（即隐藏它）
    //为了适配可变的行数，inventory实际上已经不可用了
    //public boolean enableInventoryTab = DEFAULT_INVENTORY_TAB;
    public int creativeRows = DEFAULT_CREATIVE_ROWS;
    public boolean enableScrollMemory = DEFAULT_SCROLL_MEMORY;
    public static int maxCreativeRows = DEFAULT_MAX_CREATIVE_ROWS;
    public boolean ignoreRightTabs = DEFAULT_IGNORE_RIGHT_TABS;

    @SerializedName("maxCreativeRows")
    private int serializedMaxCreativeRows = DEFAULT_MAX_CREATIVE_ROWS;

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

                if (INSTANCE == null) {
                    INSTANCE = new PlanketConfig();
                }

                maxCreativeRows = INSTANCE.serializedMaxCreativeRows;

                if (INSTANCE.normalize()) {
                    INSTANCE.serializedMaxCreativeRows = maxCreativeRows;
                    INSTANCE.save();
                }

            } catch (IOException | JsonParseException e) {
                LOGGER.error("Errors when trying to load config: ", e);
                INSTANCE = new PlanketConfig();
                maxCreativeRows = DEFAULT_MAX_CREATIVE_ROWS;
            }
        } else {
            INSTANCE = new PlanketConfig();
            maxCreativeRows = DEFAULT_MAX_CREATIVE_ROWS;
            INSTANCE.save();
        }
    }

    public void save() {
        normalize();

        serializedMaxCreativeRows = maxCreativeRows;

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            LOGGER.error("Errors when trying to save config: ", e);
        }
    }

    private boolean normalize() {
        int oldMaxCreativeRows = maxCreativeRows;
        int oldCreativeRows = creativeRows;

        maxCreativeRows = Math.clamp(maxCreativeRows,1 , MAX_CREATIVE_ROWS_LIMIT);
        creativeRows = Math.clamp(creativeRows, 1, maxCreativeRows);

        return oldMaxCreativeRows != maxCreativeRows
                || oldCreativeRows != creativeRows
                ;
    }
}