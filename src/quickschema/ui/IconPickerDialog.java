package quickschema.ui;

import arc.graphics.Color;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import quickschema.QuickSlotManager;

public class IconPickerDialog extends BaseDialog {
    private final int targetSlot;
    private final Runnable onUpdate;

    public IconPickerDialog(int targetSlot, Runnable onUpdate) {
        super("🎨 Select Slot Icon");
        this.targetSlot = targetSlot;
        this.onUpdate = onUpdate;
        setup();
    }

    private void setup() {
        cont.clear();
        buttons.clear();

        Schematic schematic = QuickSlotManager.getSlot(targetSlot);
        String currentKey = QuickSlotManager.getSlotIconKey(targetSlot);

        Table main = new Table();
        main.top();

        // Automatic Icon Button
        Table autoTable = new Table();
        autoTable.background(Tex.pane).margin(6f);
        autoTable.add("Current Setting: " + (currentKey.isEmpty() || currentKey.equals("auto") ? "[accent]Automatic (Default)[-]" : "[gold]" + currentKey + "[-]"))
            .growX().left().pad(4f);

        autoTable.button("Reset to Auto", Icon.refresh, Styles.cleart, () -> {
            QuickSlotManager.setSlotIconKey(targetSlot, "auto");
            Vars.ui.showInfoToast("Icon reset to Automatic", 2f);
            if (onUpdate != null) onUpdate.run();
            hide();
        }).size(150f, 38f).right();

        main.add(autoTable).growX().pad(6f).row();

        // 1. Blocks present inside schematic
        if (schematic != null && schematic.tiles != null && schematic.tiles.size > 0) {
            main.add("Blocks in Schematic").color(Pal.accent).fontScale(0.9f).left().pad(6f).row();

            Table blockGrid = new Table();
            blockGrid.left();

            Seq<Block> uniqueBlocks = new Seq<>();
            for (var tile : schematic.tiles) {
                if (tile.block != null && !uniqueBlocks.contains(tile.block)) {
                    uniqueBlocks.add(tile.block);
                }
            }

            int count = 0;
            for (Block b : uniqueBlocks) {
                String key = "block:" + b.name;
                boolean isSelected = key.equals(currentKey);

                blockGrid.button(btn -> {
                    btn.image(b.uiIcon).size(36f).row();
                    btn.add(b.localizedName).fontScale(0.7f).ellipsis(true).color(isSelected ? Color.gold : Color.white);
                }, Styles.cleari, () -> {
                    QuickSlotManager.setSlotIconKey(targetSlot, key);
                    Vars.ui.showInfoToast("Icon set to [" + b.localizedName + "]", 2f);
                    if (onUpdate != null) onUpdate.run();
                    hide();
                }).size(64f, 64f).pad(3f);

                count++;
                if (count % 5 == 0) blockGrid.row();
            }

            main.add(blockGrid).growX().pad(4f).row();
        }

        // 2. Predefined Mindustry System Icons
        main.add("System Icons").color(Pal.accent).fontScale(0.9f).left().pad(6f).row();

        String[] iconNames = {
            "star", "power", "hammer", "wrench", "crafting", "units", 
            "defense", "liquid", "production", "distribution", "logic", 
            "target", "box", "grid", "light", "layers", "book", "refresh", "copy", "add"
        };

        Table iconGrid = new Table();
        iconGrid.left();
        int iconCount = 0;
        for (String name : iconNames) {
            var region = QuickSlotManager.getIconByName(name);
            if (region == null) continue;

            String key = "icon:" + name;
            boolean isSelected = key.equals(currentKey);

            iconGrid.button(btn -> {
                btn.image(region).size(32f).row();
                btn.add(name).fontScale(0.65f).color(isSelected ? Color.gold : Color.lightGray);
            }, Styles.cleari, () -> {
                QuickSlotManager.setSlotIconKey(targetSlot, key);
                Vars.ui.showInfoToast("Icon set to [" + name + "]", 2f);
                if (onUpdate != null) onUpdate.run();
                hide();
            }).size(56f, 56f).pad(3f);

            iconCount++;
            if (iconCount % 6 == 0) iconGrid.row();
        }

        main.add(iconGrid).growX().pad(4f).row();

        ScrollPane pane = new ScrollPane(main);
        pane.setFadeScrollBars(false);
        cont.add(pane).grow().pad(4f).row();

        addCloseButton();
    }
}
