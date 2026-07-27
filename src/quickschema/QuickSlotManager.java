package quickschema;

import arc.*;
import arc.graphics.g2d.TextureRegion;
import arc.struct.Seq;
import arc.util.*;
import mindustry.*;
import mindustry.game.Schematic;
import mindustry.gen.Icon;

public class QuickSlotManager {
    public static final int SLOTS_PER_PAGE = 6;
    public static final int MAX_SLOTS = 36;

    private static final Seq<Schematic> slots = new Seq<>();

    public static void load() {
        slots.clear();
        int count = Core.settings.getInt("quickschema_slot_count_saved", 0);
        for (int i = 0; i < count && i < MAX_SLOTS; i++) {
            String data = Core.settings.getString("quickschema_slot_" + i, "");
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
            } else {
                Core.settings.put("quickschema_slot_" + i, "");
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
        }
        save();
        return true;
    }

    public static void clearSlot(int index) {
        if (index >= 0 && index < slots.size) {
            slots.remove(index);
            save();
        }
    }

    public static void clearAll() {
        slots.clear();
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

    public static TextureRegion getSchematicIcon(Schematic s) {
        if (s == null) return Icon.copy.getRegion();
        try {
            if (s.tiles != null && s.tiles.size > 0 && s.tiles.first().block != null) {
                return s.tiles.first().block.uiIcon;
            }
        } catch (Throwable t) {
            Log.err("Error getting schematic icon", t);
        }
        return Icon.copy.getRegion();
    }
}
