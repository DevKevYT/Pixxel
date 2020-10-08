package com.mygdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.mygdx.entities.PlayerInventory;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Game;


public class ItemInfo extends Actor {
    Image background;
    Table windowTable;

    Image iconImage;
    Image iconSlotBackground;

    Label itemName;
    Label itemDescription;
    Label itemType;
    Table stats;
    Label level;
    Label upgradeDescription;
    Table upgradeRequirements;
    Label upgradeKeyDisplay;
    public ProgressBar progessBar;

    public ImageButton dismantle;

    PlayerInventory.ItemCell cell;
    Game game;
    Stage stage;
    Skin skin;
    Vector2 stagePos = new Vector2();
    boolean graterThanHeight = false;
    public static final float FONT_SCALE = 0.4f;
    public static final float PIXELART_SCALE = 2f;

    public ItemInfo(Game game, Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        this.game = game;
        setSize(120, 180);

        windowTable = new Table();
        windowTable.setSize(super.getWidth(), super.getHeight());
        windowTable.align(Align.topLeft);
        windowTable.setDebug(true, true);
        super.setVisible(false);

        iconImage = new Image();
        iconSlotBackground = new Image(skin.getDrawable("item-slot-common"));
        windowTable.add(iconImage).align(Align.bottomLeft).width(50).height(50).pad(5);
        itemName = new Label("", skin, "itemInfo");
        itemName.setFontScale(FONT_SCALE);
       // itemName.setFontScale(0.5f);
        windowTable.add(itemName).height(50).padTop(10).align(Align.left).row();
        itemDescription = new Label("", skin, "itemInfo");
        itemDescription.setFontScale(0.3f, 0.3f);
        windowTable.add(itemDescription).colspan(2).align(Align.left).padTop(10).padBottom(10).row();
        stats = new Table(skin);
        windowTable.add(stats).pad(10).align(Align.left).colspan(2);
        windowTable.row();
        progessBar = new ProgressBar(0, 1, 0.01f, false, skin, "destroyitem");
        progessBar.setVisible(false);
        progessBar.setColor(Color.RED);
        windowTable.add(progessBar).colspan(2).expandX().fillX().width(windowTable.getPrefWidth() - (windowTable.getPadLeft() + windowTable.getPadRight()));

        background = new Image(skin.getDrawable("item-info-common"));
        background.setScale(PIXELART_SCALE);
        windowTable.pack();
        windowTable.pad(10);

        dismantle = new ImageButton(skin.getDrawable("dismantle-icon"), skin.getDrawable("dismantle-icon"));
        dismantle.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                if(dismantle.getClickListener().isOver() && Gdx.input.isButtonPressed(Input.Buttons.LEFT) && cell != null) {
                        progessBar.setVisible(true);
                        progessBar.setValue(progessBar.getValue() + (delta / cell.itemRoot.data.destroyTime));
                        if (progessBar.getValue() == progessBar.getMaxValue()) {
                            cell.dismantle();
                            progessBar.setValue(0);
                            progessBar.setVisible(false);
                        }
                } else if (cell != null) {
                    progessBar.setValue(0);
                    progessBar.setVisible(false);
                }
                return false;
            }
        });
        dismantle.setVisible(false);
        stage.addActor(dismantle);
        stage.addActor(this);
    }

    public void setDisplay(PlayerInventory.ItemCell cell) {
        if(cell != null) {
            if(cell.itemRoot != null) setDisplay(cell.itemRoot, cell);
            else setDisplay(null, null);
        } else setDisplay(null, null);
    }

    /**Use null to set the display to invisible
     * To disable display, set both arguments as null. cell is only used for iventory data. Item data is priorized.
     * cell also may be null*/
    public void setDisplay(Item item, PlayerInventory.ItemCell cell) {
        if(cell == null && item == null) {
            super.setVisible(false);
            dismantle.setVisible(false);
            this.cell = null;
            return;
        }

        iconImage.setDrawable(item.loadDrawableIcon());
        iconImage.setSize(50, 50);

        if(cell != null) {
            Vector2 pos = cell.localToStageCoordinates(new Vector2(0, 0));
            dismantle.setPosition(pos.x + PlayerInventory.ItemCell.scale * 2, pos.y + PlayerInventory.ItemCell.scale * 2);
            dismantle.setVisible(true);
        }
        if(this.cell != null) {
            if (this.cell.equals(cell)) return;
        }
        this.cell = cell;

        itemName.setText(item.data.name + (cell != null ? (cell.cellData.quantity > 1 ? " x"+cell.cellData.quantity : "") : "") /*+ (item.data.useType != ItemValues.UseType.HANDHELD ? "\nLevel: " + (cell.cellData.level + 1) : "") */+ "\n" + item.data.rarity.name());
        itemDescription.setText(item.data.description);
        stats.clearChildren();

        ItemValues.LevelStats levelStat = item.data.levelStats.get(cell != null ? cell.cellData.level : 0);
        if(item.data.useType == ItemValues.UseType.BOW) {
            addStatRow("damage-icon", levelStat.base_damage);
            addStatRow("bow-min-speed-icon", levelStat.arrow_speed_min);
            addStatRow("bow-max-speed-icon", levelStat.arrow_speed_max);
            addStatRow("tensing-icon", levelStat.arrow_tensing_time + "s");
        } else if(item.data.useType == ItemValues.UseType.SWORD) {
            addStatRow("damage-icon", levelStat.base_damage);
            if (levelStat.crit_damage > 0) addStatRow("crit-damage-icon", levelStat.crit_damage);
            if (levelStat.shield_protection > 0)
                addStatRow("shield-protection-icon", levelStat.shield_protection + "%");
        }
        windowTable.setSize(windowTable.getPrefWidth(), windowTable.getPrefHeight());
        background.setSize(windowTable.getWidth(), windowTable.getHeight());
        super.setSize(windowTable.getWidth(), windowTable.getHeight());
        windowTable.getCell(progessBar).expand();

        background.setColor(getRarityColor(item.data.rarity));

        super.setVisible(true);
    }

    public static Color getRarityColor(ItemValues.Rarity rarity) {
        if(rarity == ItemValues.Rarity.COMMON) return Color.WHITE;
        else if(rarity == ItemValues.Rarity.RARE) return new Color(0.1f, 0.3f, 0.9f, 1);
        else if(rarity == ItemValues.Rarity.LEGENDARY) return Color.PURPLE;
        else if(rarity == ItemValues.Rarity.MYSTIC) return Color.GOLD;
        else return Color.WHITE;
    }

    private void addStatRow(String drawableIcon, Object value) {
        if(drawableIcon != null) stats.add(new Image(skin.getDrawable(drawableIcon))).align(Align.center).width(15).height(15);
        else stats.add("x").align(Align.center).width(15).height(15);
        stats.add(addStatLabel(value)).align(Align.left).padLeft(3).row();
    }

    private Label addStatLabel(Object value) {
        Label l = new Label(String.format("%10s", value.toString()).replace(' ', '.'), skin, "itemInfo");
        l.setFontScale(0.5f);
        return l;
    }

    @Override
    public void act(float delta) {
        windowTable.act(delta);

        if(super.isVisible()) {
            stagePos.set(game.stage.translateTo(game.stage.getMousePos(), false, stage.getViewport().getCamera()));

            if(windowTable.getY() + windowTable.getHeight() > stage.getHeight()) graterThanHeight = true;
            else if(windowTable.getY()  < 0) graterThanHeight = false;

            windowTable.setPosition(super.getX(), super.getY());
            setPosition(stagePos.x + 1, stagePos.y - (graterThanHeight ? windowTable.getHeight() : 0));

            background.setSize(super.getWidth() / background.getScaleX(), super.getHeight() / background.getScaleY());
            background.setPosition(super.getX(), super.getY());
            iconSlotBackground.setBounds(windowTable.getX() + iconImage.getX()- 2, windowTable.getY() + iconImage.getY() - 2, iconImage.getWidth() + 4, iconImage.getHeight() + 4);
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        background.draw(batch, parentAlpha);
        iconSlotBackground.draw(batch, parentAlpha);
        windowTable.draw(batch, parentAlpha);
    }
}
