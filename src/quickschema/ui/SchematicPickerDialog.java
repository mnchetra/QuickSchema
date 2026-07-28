package quickschema.ui;

import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import quickschema.QuickSlotManager;

public class SchematicPickerDialog extends BaseDialog {
    private final int targetSlot;
    private final Runnable onSelect;
    private String filter = "";

    public SchematicPickerDialog(int targetSlot, Runnable onSelect) {
        super(Vars.state.isMenu() ? "Select Schematic" : "Assign to Quick Slot " + (targetSlot + 1));
        this.targetSlot = targetSlot;
        this.onSelect = onSelect;

        setup();
    }

    private void setup() {
        cont.clear();
        buttons.clear();

        // Search Field Header
        Table searchTable = new Table();
        searchTable.add("Search: ").padRight(8f);
        TextField field = searchTable.field("", text -> {
            filter = text.toLowerCase();
            rebuildList();
        }).growX().get();
        searchTable.button(Icon.cancel, Styles.clearNonei, () -> {
            field.setText("");
            filter = "";
            rebuildList();
        }).size(40f);
        cont.add(searchTable).growX().pad(8f).row();

        // Schematic Grid Scroll Pane - Fits Full Screen Width
        Table listTable = new Table();
        listTable.top();
        ScrollPane pane = new ScrollPane(listTable);
        pane.setFadeScrollBars(false);

        cont.add(pane).grow().pad(4f).row();

        // Close Button
        addCloseButton();

        // Populate initially
        rebuildList(listTable);
    }

    private void rebuildList() {
        if (cont.getChildren().size < 2) return;
        ScrollPane pane = (ScrollPane) cont.getChildren().get(1);
        Table listTable = (Table) pane.getWidget();
        rebuildList(listTable);
    }

    private void rebuildList(Table listTable) {
        listTable.clear();

        Seq<Schematic> all = Vars.schematics.all();
        if (all == null || all.isEmpty()) {
            listTable.add("No schematics found.").pad(20f).color(Color.gray);
            return;
        }

        // Filter items
        Seq<Schematic> filtered = new Seq<>();
        for (Schematic s : all) {
            if (s == null) continue;
            String name = s.name() == null ? "" : s.name().toLowerCase();
            if (filter.isEmpty() || name.contains(filter)) {
                filtered.add(s);
            }
        }

        if (filtered.isEmpty()) {
            listTable.add("No matching schematics found.").pad(20f).color(Color.gray);
            return;
        }

        // Responsive grid columns across screen width
        int cols = Vars.mobile ? 4 : 5;
        int colCount = 0;

        for (Schematic s : filtered) {
            Table card = new Table();
            card.background(Tex.pane).margin(4f);

            // Card Header Bar (Info button & Schematic Name)
            Table header = new Table();
            header.background(Tex.button);
            header.margin(3f);

            header.button(Icon.info, Styles.clearNonei, () -> {
                new SchematicPreviewDialog(s, -1, onSelect).show();
            }).size(26f).padRight(4f);

            String displayName = s.name();
            if (displayName.length() > 12) {
                displayName = displayName.substring(0, 10) + "..";
            }
            header.add(displayName).ellipsis(true).color(Color.white).fontScale(0.8f).growX().center();

            int existingSlot = QuickSlotManager.findSlot(s);
            if (existingSlot != -1) {
                header.add("⭐ S" + (existingSlot + 1)).color(Color.gold).fontScale(0.75f).padLeft(2f);
            }

            card.add(header).growX().row();

            // Card Icon & Block Composition (Fast, 100% Mobile Texture Compatible)
            Table iconHolder = new Table();
            iconHolder.background(Tex.button).margin(6f);
            iconHolder.image(QuickSlotManager.getSchematicIcon(s)).size(48f).row();

            // Top blocks preview row inside schematic
            Table blocksRow = new Table();
            blocksRow.left();
            Seq<Block> uniqueBlocks = new Seq<>();
            if (s.tiles != null) {
                for (var tile : s.tiles) {
                    if (tile.block != null && !uniqueBlocks.contains(tile.block)) {
                        uniqueBlocks.add(tile.block);
                        if (uniqueBlocks.size >= 4) break;
                    }
                }
            }
            for (Block b : uniqueBlocks) {
                blocksRow.image(b.uiIcon).size(16f).padRight(2f);
            }
            iconHolder.add(blocksRow).padTop(4f);

            card.add(iconHolder).growX().pad(4f).row();

            // Card Footer: Dimensions & Tile count
            Table footer = new Table();
            footer.add(s.width + "x" + s.height + " | " + (s.tiles != null ? s.tiles.size : 0) + "b")
                .color(Color.lightGray).fontScale(0.75f).center();
            card.add(footer).pad(2f).row();

            // Click card button (proper dynamic sizing to avoid card overlapping)
            listTable.button(btn -> btn.add(card).growX(), Styles.cleart, () -> {
                boolean success = QuickSlotManager.setSlot(targetSlot, s);
                if (success) {
                    Vars.ui.showInfoToast("Assigned [" + s.name() + "] to Quick Slot " + (targetSlot + 1), 2f);
                    hide();
                    if (onSelect != null) onSelect.run();
                }
            }).pad(3f).growX();

            colCount++;
            if (colCount % cols == 0) {
                listTable.row();
            }
        }
    }
}
