package quickschema.ui;

import arc.graphics.Color;
import arc.graphics.Texture;
import arc.scene.ui.Image;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextField;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
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

        // Schematic Grid Scroll Pane
        Table listTable = new Table();
        listTable.top();
        ScrollPane pane = new ScrollPane(listTable);
        pane.setFadeScrollBars(false);

        cont.add(pane).grow().pad(4f).row();

        // Close Button (Only added once)
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

        // Determine column count based on screen width (matching native Mindustry Schematics Dialog grid)
        int cols = Vars.mobile ? 3 : 5;
        int colCount = 0;

        for (Schematic s : filtered) {
            Table card = new Table();
            card.background(Tex.pane);

            // Card Header Bar (Top buttons & Schematic Name)
            Table header = new Table();
            header.background(Tex.button);
            header.margin(2f);

            // Info button
            header.button(Icon.info, Styles.clearNonei, () -> {
                Vars.ui.schematics.showInfo(s);
            }).size(28f).padRight(4f);

            // Schematic Name label (truncated if long)
            String displayName = s.name();
            if (displayName.length() > 14) {
                displayName = displayName.substring(0, 12) + "..";
            }
            header.add(displayName).ellipsis(true).color(Color.white).fontScale(0.85f).growX().center();

            // Slot badge or assign indicator
            int existingSlot = QuickSlotManager.findSlot(s);
            if (existingSlot != -1) {
                header.add("⭐ S" + (existingSlot + 1)).color(Color.gold).fontScale(0.75f).padLeft(4f);
            }

            card.add(header).growX().row();

            // Card Schematic Preview Image (Native Mindustry Schematic Rendering)
            Table imageHolder = new Table();
            imageHolder.background(Tex.pane);

            try {
                Texture previewTex = Vars.schematics.getPreview(s);
                Image img = new Image(previewTex);
                img.setScaling(Scaling.fit);
                imageHolder.add(img).size(110f).pad(4f);
            } catch (Throwable t) {
                imageHolder.image(QuickSlotManager.getSchematicIcon(s)).size(60f).pad(20f);
            }

            card.add(imageHolder).size(125f).row();

            // Card Footer: Dimensions & Tile count
            Table footer = new Table();
            footer.add(s.width + "x" + s.height + " | " + (s.tiles != null ? s.tiles.size : 0) + "b")
                .color(Color.lightGray).fontScale(0.75f).center();
            card.add(footer).pad(2f).row();

            // Click card to select schematic for quick slot
            listTable.button(btn -> btn.add(card), Styles.cleart, () -> {
                boolean success = QuickSlotManager.setSlot(targetSlot, s);
                if (success) {
                    Vars.ui.showInfoToast("Assigned [" + s.name() + "] to Quick Slot " + (targetSlot + 1), 2f);
                    hide();
                    if (onSelect != null) onSelect.run();
                }
            }).size(130f, 165f).pad(4f);

            colCount++;
            if (colCount % cols == 0) {
                listTable.row();
            }
        }
    }
}
