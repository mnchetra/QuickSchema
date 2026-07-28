package quickschema.ui;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.ItemStack;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustry.world.Block;
import quickschema.QuickSlotManager;

public class SchematicPreviewDialog extends BaseDialog {
    private final Schematic schematic;
    private final int slotIndex;
    private final Runnable onUpdate;

    public SchematicPreviewDialog(Schematic schematic, int slotIndex, Runnable onUpdate) {
        super(schematic == null ? "Schematic Preview" : "👁️ " + schematic.name());
        this.schematic = schematic;
        this.slotIndex = slotIndex;
        this.onUpdate = onUpdate;

        setup();
    }

    private void setup() {
        cont.clear();
        buttons.clear();

        if (schematic == null) {
            cont.add("No schematic selected.").color(Color.gray).pad(20f);
            addCloseButton();
            return;
        }

        Table contentTable = new Table();
        contentTable.top();

        // 1. Visual Schematic Image Preview
        try {
            SchematicImage image = new SchematicImage(schematic);
            Table imageHolder = new Table();
            imageHolder.background(Tex.pane).margin(6f);
            imageHolder.add(image).size(Math.min(Core.graphics.getWidth() * 0.7f, 320f)).row();
            contentTable.add(imageHolder).pad(6f).row();
        } catch (Throwable t) {
            // Fallback icon if image creation fails
            contentTable.image(QuickSlotManager.getSchematicIcon(schematic)).size(96f).pad(10f).row();
        }

        // 2. Info Bar (Dimensions, Tiles Count, Slot Number)
        Table infoBar = new Table();
        infoBar.background(Tex.button).margin(6f);
        infoBar.add("Size: " + schematic.width + "x" + schematic.height).color(Color.white).padRight(12f);
        infoBar.add("Tiles: " + (schematic.tiles != null ? schematic.tiles.size : 0)).color(Color.lightGray).padRight(12f);
        if (slotIndex >= 0) {
            infoBar.add("Slot: #" + (slotIndex + 1)).color(Color.gold);
        }
        contentTable.add(infoBar).growX().pad(4f).row();

        // 3. Description / Labels if available
        if (schematic.description() != null && !schematic.description().isEmpty()) {
            contentTable.add(schematic.description()).color(Color.lightGray).fontScale(0.85f).wrap().growX().pad(6f).row();
        }

        // 4. Requirements / Cost Breakdown
        try {
            var reqs = schematic.requirements();
            if (reqs != null && reqs.iterator().hasNext()) {
                contentTable.add("Build Cost").color(Pal.accent).fontScale(0.85f).left().pad(4f).row();
                Table reqTable = new Table();
                reqTable.left();
                int col = 0;
                for (ItemStack req : reqs) {
                    reqTable.image(req.item.uiIcon).size(20f).padRight(2f);
                    reqTable.add(req.amount + "").fontScale(0.8f).padRight(10f);
                    col++;
                    if (col % 4 == 0) reqTable.row();
                }
                contentTable.add(reqTable).growX().pad(4f).row();
            }
        } catch (Throwable ignored) {}

        // 5. Unique Blocks Breakdown
        if (schematic.tiles != null && schematic.tiles.size > 0) {
            contentTable.add("Key Components").color(Pal.accent).fontScale(0.85f).left().pad(4f).row();
            Table blocksTable = new Table();
            blocksTable.left();
            Seq<Block> uniqueBlocks = new Seq<>();
            for (var tile : schematic.tiles) {
                if (tile.block != null && !uniqueBlocks.contains(tile.block)) {
                    uniqueBlocks.add(tile.block);
                }
            }
            int bcol = 0;
            for (Block b : uniqueBlocks) {
                blocksTable.image(b.uiIcon).size(22f).padRight(2f);
                blocksTable.add(b.localizedName).fontScale(0.75f).ellipsis(true).color(Color.lightGray).padRight(10f);
                bcol++;
                if (bcol % 3 == 0) blocksTable.row();
            }
            contentTable.add(blocksTable).growX().pad(4f).row();
        }

        ScrollPane pane = new ScrollPane(contentTable);
        pane.setFadeScrollBars(false);
        cont.add(pane).grow().pad(4f).row();

        // Dialog Bottom Action Buttons
        buttons.button("⚡ Select & Use", Icon.ok, Styles.clearTogglet, () -> {
            try {
                Vars.control.input.useSchematic(schematic);
                Vars.ui.showInfoToast("Selected: " + schematic.name(), 1.5f);
            } catch (Throwable t) {
                Vars.ui.showInfoToast("Failed to select schematic", 2f);
            }
            hide();
        }).size(150f, 50f).pad(4f);

        if (slotIndex >= 0) {
            buttons.button("🎨 Icon", Icon.image, Styles.cleart, () -> {
                hide();
                new IconPickerDialog(slotIndex, () -> {
                    if (onUpdate != null) onUpdate.run();
                    new SchematicPreviewDialog(schematic, slotIndex, onUpdate).show();
                }).show();
            }).size(110f, 50f).pad(4f);

            buttons.button("❌ Clear", Icon.cancel, Styles.cleart, () -> {
                QuickSlotManager.clearSlot(slotIndex);
                if (onUpdate != null) onUpdate.run();
                hide();
            }).size(110f, 50f).pad(4f);
        }

        addCloseButton();
    }
}
