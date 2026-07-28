package quickschema.ui;

import arc.*;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.event.ElementGestureListener;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import quickschema.QuickSlotManager;

public class QuickBarUI extends Table {
    private boolean collapsed = false;
    private int currentPage = 0;
    private final Table slotsTable = new Table();
    private Runnable onPositionChange;

    public QuickBarUI() {
        this(null);
    }

    public QuickBarUI(Runnable onPositionChange) {
        this.onPositionChange = onPositionChange;
        collapsed = Core.settings.getBool("quickschema_collapsed", false);
        currentPage = Core.settings.getInt("quickschema_page", 0);
        setup();
    }

    public void setOnPositionChange(Runnable listener) {
        this.onPositionChange = listener;
    }

    public void rebuild() {
        setup();
    }

    private void setup() {
        clear();
        background(Tex.pane);

        // Toggle Collapse Button
        button(collapsed ? Icon.rightOpen : Icon.leftOpen, Styles.clearNonei, () -> {
            collapsed = !collapsed;
            Core.settings.put("quickschema_collapsed", collapsed);
            setup();
        }).size(40f).pad(2f);

        if (!collapsed) {
            slotsTable.clear();
            slotsTable.defaults().pad(2f);

            int totalPages = QuickSlotManager.getTotalPages();
            if (currentPage >= totalPages) {
                currentPage = totalPages - 1;
            }

            // Only show Page Navigation arrows if there is more than 1 page
            if (totalPages > 1) {
                Button prevBtn = slotsTable.button(Icon.left, Styles.clearNonei, () -> {
                    currentPage = (currentPage - 1 + totalPages) % totalPages;
                    Core.settings.put("quickschema_page", currentPage);
                    setup();
                }).size(36f).get();
                prevBtn.addListener(new Tooltip(t -> t.background(Tex.pane).add("Previous Page")));

                slotsTable.add((currentPage + 1) + "/" + totalPages)
                    .color(arc.graphics.Color.lightGray)
                    .fontScale(0.85f)
                    .padLeft(2f).padRight(2f);

                Button nextBtn = slotsTable.button(Icon.right, Styles.clearNonei, () -> {
                    currentPage = (currentPage + 1) % totalPages;
                    Core.settings.put("quickschema_page", currentPage);
                    setup();
                }).size(36f).get();
                nextBtn.addListener(new Tooltip(t -> t.background(Tex.pane).add("Next Page")));
            }

            // Render slots for current page based on actual added schematics
            int totalDisplay = QuickSlotManager.getTotalDisplaySlots();
            int startIdx = currentPage * QuickSlotManager.SLOTS_PER_PAGE;
            int endIdx = Math.min(totalDisplay, startIdx + QuickSlotManager.SLOTS_PER_PAGE);

            for (int i = startIdx; i < endIdx; i++) {
                final int index = i;
                Schematic s = QuickSlotManager.getSlot(index);

                if (s != null) {
                    // Filled slot
                    Button btn = slotsTable.button(Icon.copy, Styles.cleari, () -> {
                        selectSchematic(s);
                    }).size(44f).get();

                    if (btn instanceof ImageButton) {
                        ImageButton imgBtn = (ImageButton) btn;
                        imgBtn.getStyle().imageUp = new TextureRegionDrawable(QuickSlotManager.getSlotIcon(index));
                    }

                    // Rich Tooltip with Schematic Image Preview
                    btn.addListener(new Tooltip(t -> {
                        t.background(Tex.pane).margin(6f);
                        t.add("#" + (index + 1) + ": " + s.name()).color(arc.graphics.Color.gold).padBottom(2f).row();
                        try {
                            t.add(new mindustry.ui.dialogs.SchematicsDialog.SchematicImage(s)).size(150f).pad(2f).row();
                        } catch (Throwable ignored) {}
                        t.add(s.width + "x" + s.height + " | " + (s.tiles != null ? s.tiles.size : 0) + " tiles")
                         .color(arc.graphics.Color.lightGray).fontScale(0.85f);
                    }));
                    
                    // Mouse Right Click (PC)
                    btn.clicked(KeyCode.mouseRight, () -> showSlotMenu(index, s));
                    
                    // Touch Long Press / Hold Gesture (Mobile)
                    btn.addListener(new ElementGestureListener() {
                        @Override
                        public boolean longPress(Element element, float x, float y) {
                            showSlotMenu(index, s);
                            return true;
                        }
                    });
                } else {
                    // Single '+' Empty slot to add the next schematic
                    Button btn = slotsTable.button(Icon.add, Styles.clearNonei, () -> {
                        handleEmptySlotClick(index);
                    }).size(44f).get();

                    btn.addListener(new Tooltip(t -> t.background(Tex.pane).add("Click to add schematic to Slot " + (index + 1))));
                }
            }

            // Quick "+ Held" button
            Button addCurrentBtn = slotsTable.button(Icon.star, Styles.clearNonei, () -> {
                Schematic held = Vars.control.input.lastSchematic;
                if (held != null) {
                    boolean added = QuickSlotManager.addSchematic(held);
                    if (added) {
                        Vars.ui.showInfoToast("Added to Quick Access!", 2f);
                        setup();
                    }
                } else {
                    Vars.ui.showInfoToast("No schematic currently copied/held", 2f);
                }
            }).size(44f).get();
            addCurrentBtn.addListener(new Tooltip(t -> t.background(Tex.pane).add("Add Currently Copied Schematic")));

            // Position Switcher Button (0: Top-Right (Under Minimap), 1: Left-Center, 2: Bottom-Left)
            Button posBtn = slotsTable.button(Icon.refresh, Styles.clearNonei, () -> {
                int pos = Core.settings.getInt("quickschema_position", 0);
                pos = (pos + 1) % 3;
                Core.settings.put("quickschema_position", pos);
                String posName = pos == 0 ? "Top-Right (Under Minimap)" : pos == 1 ? "Left-Center" : "Bottom-Left (Elevated)";
                Vars.ui.showInfoToast("Position: " + posName, 2f);
                if (onPositionChange != null) onPositionChange.run();
                setup();
            }).size(44f).get();
            posBtn.addListener(new Tooltip(t -> t.background(Tex.pane).add("Change Bar Position")));

            add(slotsTable).pad(2f);
        }

        pack();
    }

    private void selectSchematic(Schematic s) {
        if (s == null) return;
        try {
            Vars.control.input.useSchematic(s);
            Vars.ui.showInfoToast("Selected: " + s.name(), 1.5f);
        } catch (Throwable t) {
            Log.err("Error selecting schematic", t);
        }
    }

    private void handleEmptySlotClick(int index) {
        Schematic held = Vars.control.input.lastSchematic;
        if (held != null) {
            BaseDialog choice = new BaseDialog("Add Quick Schematic");
            choice.cont.add("Choose schematic to add:").pad(10f).row();
            choice.cont.button("Assign Held: " + held.name(), () -> {
                QuickSlotManager.setSlot(index, held);
                choice.hide();
                setup();
            }).size(240f, 50f).pad(6f).row();

            choice.cont.button("Browse Library...", () -> {
                choice.hide();
                new SchematicPickerDialog(index, this::setup).show();
            }).size(240f, 50f).pad(6f).row();

            choice.addCloseButton();
            choice.show();
        } else {
            new SchematicPickerDialog(index, this::setup).show();
        }
    }

    private void showSlotMenu(int index, Schematic s) {
        BaseDialog dialog = new BaseDialog("Slot " + (index + 1) + ": " + s.name());

        dialog.cont.button("⚡ Select Schematic", () -> {
            selectSchematic(s);
            dialog.hide();
        }).size(220f, 48f).pad(4f).row();

        dialog.cont.button("👁️ Preview Schematic", () -> {
            dialog.hide();
            new SchematicPreviewDialog(s, index, this::setup).show();
        }).size(220f, 48f).pad(4f).row();

        dialog.cont.button("🎨 Change Icon", () -> {
            dialog.hide();
            new IconPickerDialog(index, this::setup).show();
        }).size(220f, 48f).pad(4f).row();

        dialog.cont.button("🔄 Replace Schematic", () -> {
            dialog.hide();
            new SchematicPickerDialog(index, this::setup).show();
        }).size(220f, 48f).pad(4f).row();

        dialog.cont.button("❌ Remove Slot", () -> {
            QuickSlotManager.clearSlot(index);
            dialog.hide();
            setup();
        }).size(220f, 48f).pad(4f).row();

        dialog.addCloseButton();
        dialog.show();
    }
}
