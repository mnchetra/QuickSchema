package quickschema;

import arc.*;
import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.*;
import mindustry.*;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Schematic;
import mindustry.gen.Icon;

public class QuickSlotManager {
    public static final int SLOTS_PER_PAGE = 6;
    public static final int MAX_SLOTS = 36;

    private static final Seq<Schematic> slots = new Seq<>();
    private static final Seq<String> slotIcons = new Seq<>();

    public static void load() {
        slots.clear();
        slotIcons.clear();
        int count = Core.settings.getInt("quickschema_slot_count_saved", 0);
        for (int i = 0; i < count && i < MAX_SLOTS; i++) {
            String data = Core.settings.getString("quickschema_slot_" + i, "");
            String iconKey = Core.settings.getString("quickschema_slot_icon_" + i, "");
            if (!data.isEmpty()) {
                try {
                    Schematic s = null;
                    if (data.startsWith("file:")) {
                        String filename = data.substring(5);
                        s = Vars.schematics.all().find(sc -> sc.file != null && sc.file.name().equals(filename));
                    } else if (data.startsWith("name:")) {
                        String name = data.substring(5);
                        s = Vars.schematics.all().find(sc -> sc.name().equals(name));
                    } else if (data.startsWith("b64:")) {
                        String b64 = data.substring(4);
                        s = Vars.schematics.readBase64(b64);
                    }
                    if (s != null && !contains(s)) {
                        slots.add(s);
                        slotIcons.add(iconKey);
                    }
                } catch (Throwable t) {
                    Log.err("Failed to load QuickSchema slot @", i, t);
                }
            }
        }
    }

    public static void save() {
        Core.settings.put("quickschema_slot_count_saved", slots.size);
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (i < slots.size) {
                Schematic s = slots.get(i);
                String iconKey = i < slotIcons.size ? slotIcons.get(i) : "";
                if (s.file != null) {
                    Core.settings.put("quickschema_slot_" + i, "file:" + s.file.name());
                } else if (s.name() != null && !s.name().isEmpty()) {
                    Core.settings.put("quickschema_slot_" + i, "name:" + s.name());
                } else {
                    try {
                        String b64 = Vars.schematics.writeBase64(s);
                        Core.settings.put("quickschema_slot_" + i, "b64:" + b64);
                    } catch (Throwable t) {
                        Core.settings.put("quickschema_slot_" + i, "");
                    }
                }
                Core.settings.put("quickschema_slot_icon_" + i, iconKey);
            } else {
                Core.settings.put("quickschema_slot_" + i, "");
                Core.settings.put("quickschema_slot_icon_" + i, "");
            }
        }
    }

    public static int getFilledCount() {
        return slots.size;
    }

    public static int getTotalDisplaySlots() {
        if (slots.size >= MAX_SLOTS) return MAX_SLOTS;
        return slots.size + 1; // Filled slots + 1 empty '+' slot to extend
    }

    public static int getTotalPages() {
        int display = getTotalDisplaySlots();
        return Math.max(1, (int) Math.ceil((double) display / SLOTS_PER_PAGE));
    }

    public static Schematic getSlot(int index) {
        if (index < 0 || index >= slots.size) return null;
        return slots.get(index);
    }

    public static boolean setSlot(int index, Schematic schematic) {
        if (schematic == null) {
            clearSlot(index);
            return true;
        }

        int existingSlot = findSlot(schematic);
        if (existingSlot != -1 && existingSlot != index) {
            Vars.ui.showInfoToast("⚠️ Schematic [" + schematic.name() + "] is already in Slot " + (existingSlot + 1) + "!", 3f);
            return false;
        }

        if (index >= 0 && index < slots.size) {
            slots.set(index, schematic);
        } else if (index >= slots.size && slots.size < MAX_SLOTS) {
            slots.add(schematic);
            while (slotIcons.size < slots.size) {
                slotIcons.add("");
            }
        }
        save();
        return true;
    }

    public static void clearSlot(int index) {
        if (index >= 0 && index < slots.size) {
            slots.remove(index);
            if (index < slotIcons.size) {
                slotIcons.remove(index);
            }
            save();
        }
    }

    public static void clearAll() {
        slots.clear();
        slotIcons.clear();
        save();
    }

    public static boolean addSchematic(Schematic schematic) {
        if (schematic == null) return false;

        int existing = findSlot(schematic);
        if (existing != -1) {
            Vars.ui.showInfoToast("⚠️ Schematic [" + schematic.name() + "] is already in Slot " + (existing + 1) + "!", 3f);
            return false;
        }

        if (slots.size >= MAX_SLOTS) {
            Vars.ui.showInfoToast("⚠️ Quick Access slots are full!", 2.5f);
            return false;
        }

        slots.add(schematic);
        slotIcons.add("");
        save();
        return true;
    }

    public static int findSlot(Schematic schematic) {
        if (schematic == null) return -1;
        for (int i = 0; i < slots.size; i++) {
            Schematic s = slots.get(i);
            if (s == null) continue;
            if (s == schematic) return i;
            if (s.file != null && schematic.file != null && s.file.name().equals(schematic.file.name())) return i;
            if (s.name() != null && s.name().equals(schematic.name()) && s.width == schematic.width && s.height == schematic.height) return i;
        }
        return -1;
    }

    public static boolean contains(Schematic schematic) {
        return findSlot(schematic) != -1;
    }

    public static Schematic getCurrentlyHeldSchematic() {
        if (Vars.control == null || Vars.control.input == null) return null;

        // 1. Explicit lastSchematic stored by Mindustry
        if (Vars.control.input.lastSchematic != null) {
            return Vars.control.input.lastSchematic;
        }

        // 2. Active build plans selected/held in hand
        try {
            Seq<BuildPlan> plans = Vars.control.input.selectPlans;
            if (plans != null && plans.size > 0) {
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

                for (BuildPlan plan : plans) {
                    if (plan == null || plan.block == null) continue;
                    minX = Math.min(minX, plan.x);
                    minY = Math.min(minY, plan.y);
                    maxX = Math.max(maxX, plan.x + plan.block.size - 1);
                    maxY = Math.max(maxY, plan.y + plan.block.size - 1);
                }

                if (minX <= maxX && minY <= maxY) {
                    Seq<Schematic.Stile> stiles = new Seq<>();
                    for (BuildPlan plan : plans) {
                        if (plan == null || plan.block == null) continue;
                        stiles.add(new Schematic.Stile(plan.block, plan.x - minX, plan.y - minY, plan.config, (byte) plan.rotation));
                    }
                    int width = maxX - minX + 1;
                    int height = maxY - minY + 1;
                    StringMap tags = new StringMap();
                    tags.put("name", "Copied " + stiles.size + "b (" + width + "x" + height + ")");

                    Schematic created = new Schematic(stiles, tags, width, height);
                    Vars.control.input.lastSchematic = created;
                    return created;
                }
            }
        } catch (Throwable t) {
            Log.err("Error getting currently held schematic", t);
        }

        return null;
    }

    public static void setSlotIconKey(int index, String iconKey) {
        while (slotIcons.size <= index) {
            slotIcons.add("");
        }
        slotIcons.set(index, iconKey == null ? "" : iconKey);
        save();
    }

    public static String getSlotIconKey(int index) {
        if (index >= 0 && index < slotIcons.size) {
            return slotIcons.get(index);
        }
        return "";
    }

    public static TextureRegion getSlotIcon(int index) {
        Schematic s = getSlot(index);
        if (s == null) return Icon.copy.getRegion();

        String key = getSlotIconKey(index);
        if (!key.isEmpty()) {
            TextureRegion custom = parseIconKey(key);
            if (custom != null && custom.found()) {
                return custom;
            }
        }

        return getSchematicIcon(s);
    }

    public static TextureRegion parseIconKey(String key) {
        if (key == null || key.isEmpty() || key.equals("auto")) return null;
        try {
            if (key.startsWith("block:")) {
                var b = Vars.content.block(key.substring(6));
                if (b != null) return b.uiIcon;
            } else if (key.startsWith("item:")) {
                var item = Vars.content.item(key.substring(5));
                if (item != null) return item.uiIcon;
            } else if (key.startsWith("liquid:")) {
                var liq = Vars.content.liquid(key.substring(7));
                if (liq != null) return liq.uiIcon;
            } else if (key.startsWith("icon:")) {
                String iconName = key.substring(5);
                return getIconByName(iconName);
            }
        } catch (Throwable t) {
            Log.err("Error parsing icon key: " + key, t);
        }
        return null;
    }

    public static TextureRegion getIconByName(String name) {
        try {
            var field = Icon.class.getField(name);
            Object val = field.get(null);
            if (val instanceof TextureRegionDrawable) {
                return ((TextureRegionDrawable) val).getRegion();
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static TextureRegion getSchematicIcon(Schematic s) {
        if (s == null) return Icon.copy.getRegion();
        try {
            if (s.tiles != null && s.tiles.size > 0) {
                mindustry.world.Block bestBlock = null;
                for (var tile : s.tiles) {
                    if (tile == null || tile.block == null) continue;
                    mindustry.world.Block b = tile.block;
                    if (bestBlock == null) bestBlock = b;
                    if (b instanceof mindustry.world.blocks.storage.CoreBlock || b.hasItems || b.hasPower || b.synthetic()) {
                        bestBlock = b;
                    }
                }
                if (bestBlock != null && bestBlock.uiIcon != null && bestBlock.uiIcon.found()) {
                    return bestBlock.uiIcon;
                }
            }
        } catch (Throwable t) {
            Log.err("Error getting schematic icon", t);
        }
        return Icon.copy.getRegion();
    }
}
