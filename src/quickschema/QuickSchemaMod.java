package quickschema;

import arc.*;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Table;
import arc.util.*;
import mindustry.Vars;
import mindustry.game.EventType.*;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.mod.*;
import quickschema.ui.QuickAccessSlotsDialog;
import quickschema.ui.QuickBarUI;

public class QuickSchemaMod extends Mod {
    private QuickBarUI quickBarUI;
    private Table hudContainer;

    public QuickSchemaMod() {
        Log.info("Initialized QuickSchemaMod.");

        Events.on(ClientLoadEvent.class, e -> {
            Time.runTask(10f, () -> {
                // Load saved slot configurations
                QuickSlotManager.load();

                // Create container table
                hudContainer = new Table();
                hudContainer.setFillParent(true);
                hudContainer.touchable = Touchable.childrenOnly;

                // Create HUD Quick Bar with position listener
                quickBarUI = new QuickBarUI(this::applyPosition);

                // Add to HUD Group
                applyPosition();

                // Schematics Dialog Integration
                if (Vars.ui != null && Vars.ui.schematics != null) {
                    try {
                        Vars.ui.schematics.buttons.button("⭐ Quick Access", Icon.star, () -> {
                            Schematic held = QuickSlotManager.getCurrentlyHeldSchematic();
                            if (held != null) {
                                boolean added = QuickSlotManager.addSchematic(held);
                                if (added) {
                                    Vars.ui.showInfoToast("Added [" + held.name() + "] to Quick Access!", 2f);
                                    if (quickBarUI != null) quickBarUI.rebuild();
                                }
                            } else {
                                new QuickAccessSlotsDialog(() -> {
                                    if (quickBarUI != null) quickBarUI.rebuild();
                                }).show();
                            }
                        }).size(160f, 64f).pad(4f);
                    } catch (Throwable err) {
                        Log.err("Failed to hook into SchematicsDialog", err);
                    }
                }
            });
        });

        // Re-apply position and rebuild UI on world load
        Events.on(WorldLoadEvent.class, e -> {
            if (quickBarUI != null) {
                quickBarUI.rebuild();
                applyPosition();
            }
        });
    }

    private void applyPosition() {
        if (Vars.ui == null || Vars.ui.hudGroup == null || quickBarUI == null) return;
        
        hudContainer.remove();
        hudContainer.clear();

        int pos = Core.settings.getInt("quickschema_position", 0);
        if (pos == 0) {
            // Position 0 (DEFAULT): Top-Right (safely UNDER top-right minimap)
            hudContainer.top().right().margin(190f, 8f, 8f, 8f);
        } else if (pos == 1) {
            // Position 1: Left-Center (safely below Wave 1/FPS box, above bottom unit bar)
            hudContainer.left().margin(140f, 8f, 140f, 8f);
        } else {
            // Position 2: Bottom-Left Elevated (above unit command bar)
            hudContainer.bottom().left().margin(8f, 8f, 150f, 8f);
        }

        hudContainer.add(quickBarUI);
        hudContainer.visible(() -> Vars.ui != null && Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown && Vars.state != null && Vars.state.isGame());

        if (hudContainer.parent != Vars.ui.hudGroup) {
            Vars.ui.hudGroup.addChild(hudContainer);
        }
    }

    @Override
    public void loadContent() {
        Log.info("QuickSchema content loaded.");
    }
}
