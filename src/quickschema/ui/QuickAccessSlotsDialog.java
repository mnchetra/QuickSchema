package quickschema.ui;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import quickschema.QuickSlotManager;

public class QuickAccessSlotsDialog extends BaseDialog {
    private int currentPage = 0;
    private final Runnable onUpdate;

    public QuickAccessSlotsDialog(Runnable onUpdate) {
        super("⭐ Quick Access Slots");
        this.onUpdate = onUpdate;
        setup();
    }

    private void setup() {
        cont.clear();
        buttons.clear();

        int totalPages = QuickSlotManager.getTotalPages();
        if (currentPage >= totalPages) currentPage = totalPages - 1;

        // Top Navigation Bar (Only show if > 1 page)
        if (totalPages > 1) {
            Table navTable = new Table();
            navTable.button(Icon.left, Styles.clearNonei, () -> {
                currentPage = (currentPage - 1 + totalPages) % totalPages;
                setup();
            }).size(36f);

            navTable.add("Page " + (currentPage + 1) + " of " + totalPages)
                .color(Color.white).padLeft(12f).padRight(12f);

            navTable.button(Icon.right, Styles.clearNonei, () -> {
                currentPage = (currentPage + 1) % totalPages;
                setup();
            }).size(36f);

            cont.add(navTable).pad(8f).row();
        }

        // Grid for slots on the current page
        Table grid = new Table();
        int totalDisplay = QuickSlotManager.getTotalDisplaySlots();
        int startIdx = currentPage * QuickSlotManager.SLOTS_PER_PAGE;
        int endIdx = Math.min(totalDisplay, startIdx + QuickSlotManager.SLOTS_PER_PAGE);

        int countOnPage = 0;
        for (int slotIdx = startIdx; slotIdx < endIdx; slotIdx++) {
            final int index = slotIdx;
            Schematic s = QuickSlotManager.getSlot(index);

            Table card = new Table();
            card.background(Tex.pane).margin(6f);

            // Slot Header
            Table header = new Table();
            header.add("Slot " + (index + 1)).color(Color.gold).fontScale(0.85f).left().growX();

            if (s != null) {
                header.button(Icon.cancel, Styles.clearNonei, () -> {
                    QuickSlotManager.clearSlot(index);
                    if (onUpdate != null) onUpdate.run();
                    setup();
                }).size(24f).right();
            }
            card.add(header).growX().row();

            // Slot Content
            Table body = new Table();
            if (s != null) {
                body.image(QuickSlotManager.getSlotIcon(index)).size(40f).padRight(8f);
                Table info = new Table();
                info.left();
                String name = s.name();
                if (name.length() > 14) name = name.substring(0, 12) + "..";
                info.add(name).color(Color.white).fontScale(0.85f).left().row();
                info.add(s.width + "x" + s.height).color(Color.lightGray).fontScale(0.75f).left();
                body.add(info).growX();
            } else {
                body.image(Icon.add).size(28f).padRight(8f);
                body.add("[gray]+ Add Schematic[-]").fontScale(0.85f).growX();
            }
            card.add(body).pad(6f).growX().row();

            // Click card to manage/assign slot
            grid.button(btn -> btn.add(card).growX(), Styles.flatt, () -> {
                if (s != null) {
                    new SchematicPreviewDialog(s, index, () -> {
                        if (onUpdate != null) onUpdate.run();
                        setup();
                    }).show();
                } else {
                    new SchematicPickerDialog(index, () -> {
                        if (onUpdate != null) onUpdate.run();
                        setup();
                    }).show();
                }
            }).size(180f, 90f).pad(6f);

            countOnPage++;
            if (countOnPage % 3 == 0) {
                grid.row();
            }
        }

        cont.add(grid).pad(8f).row();

        // Clear All Button (Only shown if at least 1 schematic is saved)
        if (QuickSlotManager.getFilledCount() > 0) {
            buttons.button("Clear All", Icon.trash, () -> {
                Vars.ui.showConfirm("Clear All Slots", "Are you sure you want to remove all quick access schematics?", () -> {
                    QuickSlotManager.clearAll();
                    if (onUpdate != null) onUpdate.run();
                    setup();
                });
            }).size(160f, 60f).pad(4f);
        }

        // Add Close / Back Button ONCE
        addCloseButton();
    }
}
