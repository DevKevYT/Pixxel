package com.mygdx.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.mygdx.darkdawn.Logger;
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
    ProgressBar progress;

    public ImageButton dismantle;

    PlayerInventory.ItemCell cell;
    Game game;
    Stage stage;
    Skin skin;
    Vector2 stagePos = new Vector2();
    boolean graterThanHeight = false;

    public ItemInfo(Game game, Stage stage, Skin skin) {
        this.stage = stage;
        this.skin = skin;
        this.game = game;
        setSize(120, 180);

        windowTable = new Table();
        windowTable.setSize(super.getWidth(), super.getHeight());
        windowTable.setDebug(true, true);
        windowTable.align(Align.topLeft);

        super.setVisible(false);

        iconImage = new Image();
        iconSlotBackground = new Image(skin.getDrawable("item-slot-common"));
        windowTable.add(iconImage).align(Align.bottomLeft).width(50).height(50).padTop(5).padLeft(5).padRight(5);
        itemName = new Label("", skin, "itemInfo");
        itemName.setFontScale(0.4f);
       // itemName.setFontScale(0.5f);
        windowTable.add(itemName).height(50).padTop(10).padRight(5).align(Align.left).row();
        itemDescription = new Label("", skin, "itemInfo");
        itemDescription.setFontScale(0.3f, 0.3f);
        windowTable.add(itemDescription).colspan(2).align(Align.left).pad(5).padTop(10).padBottom(10).row();
        stats = new Table(skin);
        windowTable.add(stats).pad(10).align(Align.left).colspan(2);

        background = new Image(skin.getDrawable("item-info-common"));
        background.setScale(2.5f);
        windowTable.pack();
        windowTable.setDebug(true, true);
        windowTable.pad(10);

        dismantle = new ImageButton(skin.getDrawable("dismantle-icon"), skin.getDrawable("dismantle-icon"));
        dismantle.addAction(new Action() {
            @Override
            public boolean act(float delta) {
                if(dismantle.getClickListener().isOver() && Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                    if(cell != null) cell.dismantle();
                }
                return false;
            }
        });
        dismantle.setVisible(false);
        stage.addActor(dismantle);
        stage.addActor(this);
    }

    /**Use null to set the display to invisible*/
    public void setDisplay(PlayerInventory.ItemCell cell) {
        if(cell == null) {
            super.setVisible(false);
            dismantle.setVisible(false);
            this.cell = null;
            return;
        }

        iconImage.setDrawable(cell.itemSprite.getDrawable());
        iconImage.setSize(50, 50);
        Vector2 pos = cell.localToStageCoordinates(new Vector2(0, 0));
        dismantle.setPosition(pos.x + PlayerInventory.ItemCell.scale*2, pos.y + PlayerInventory.ItemCell.scale*2);
        dismantle.setVisible(true);

        if(this.cell != null) {
            if (this.cell.equals(cell)) return;
        }
        this.cell = cell;

        itemName.setText(cell.itemRoot.data.name + (cell.cellData.quantity > 1 ? " x"+cell.cellData.quantity : "") + (cell.itemRoot.data.useType != ItemValues.UseType.HANDHELD ? "\nLevel: " + (cell.cellData.level + 1) : "") + "\n" + cell.itemRoot.data.rarity.name());
        itemDescription.setText(cell.itemRoot.data.description);
        stats.clearChildren();

        ItemValues.LevelStats levelStat = cell.itemRoot.data.levelStats.get(cell.cellData.level);
        if(cell.itemRoot.data.useType == ItemValues.UseType.BOW) {
            addStatRow("damage-icon", levelStat.base_damage);
            addStatRow("bow-min-speed-icon", levelStat.arrow_speed_min);
            addStatRow("bow-max-speed-icon", levelStat.arrow_speed_max);
            addStatRow("tensing-icon", levelStat.arrow_tensing_time + "s");
        } else if(cell.itemRoot.data.useType == ItemValues.UseType.SWORD) {
            addStatRow("damage-icon", levelStat.base_damage);
            if(levelStat.crit_damage > 0) addStatRow("crit-damage-icon", levelStat.crit_damage);
            if(levelStat.shield_protection > 0) addStatRow("shield-protection-icon", levelStat.shield_protection + "%");
        }
        this.cell = cell;
        windowTable.setSize(windowTable.getPrefWidth(), windowTable.getPrefHeight());
        background.setSize(windowTable.getWidth(), windowTable.getHeight());
        super.setSize(windowTable.getWidth(), windowTable.getHeight());

        if(cell.itemRoot.data.rarity == ItemValues.Rarity.COMMON) background.setColor(Color.WHITE);
        else if(cell.itemRoot.data.rarity == ItemValues.Rarity.RARE) background.setColor(Color.BLUE);
        else if(cell.itemRoot.data.rarity == ItemValues.Rarity.LEGENDARY) background.setColor(Color.PURPLE);
        else if(cell.itemRoot.data.rarity == ItemValues.Rarity.MYSTIC) background.setColor(Color.GOLD);

        super.setVisible(true);
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

            windowTable.setPosition(super.getX() + 10, super.getY());
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
