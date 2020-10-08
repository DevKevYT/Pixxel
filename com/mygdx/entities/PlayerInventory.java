package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.items.Blueprint;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Game;
import com.mygdx.ui.ItemInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class PlayerInventory {

    public ItemInfo itemInfo;
    private Game game;
    private boolean inventoryOpened = false;
    private Table xpDisplay;
    public int xp = 0;
    private Label xpCount;

    private Stage stage;
    private Skin skin;
    public float x, y;
    public final float cellSize = 40;

    private Group inv;
    private Group hotbar;
    public ProgressBar healthbar;
    private Table hotbarTable; //Table of the hotbar cells beside the bix main weapon
    private Table inventory;
    private Image inventoryBackground;
    private boolean isVisible = true;

    private float recipesPadding = 10;
    private Table recipesDisplay;
    private ScrollPane recipesScrollpane;

    private final static String[] hotbar_cellNames = new String[]{"cell1", "cell2", "cell3"};
    private final int inventorySizeX = 4;
    private final int inventorySizeY = 5;
    private ItemCell[][] inventoryCells = new ItemCell[inventorySizeX][inventorySizeY];
    private ItemCell[] hotbarCells = new ItemCell[4]; //Main weapon = hotbarCells[hotbarCells.length-1]
    private EquipListener equipListener = new EquipListener() {
        public void equip(ItemCell itemCell, int cell) {}
    };

    private static Sprite swapForeground;
    static {
        Pixmap whiteBackground = new Pixmap(1, 1, Pixmap.Format.RGBA4444);
        whiteBackground.setColor(Color.WHITE);
        whiteBackground.fill();
        swapForeground = new Sprite(new Texture(whiteBackground));
    }

    interface EquipListener {
        void equip(ItemCell itemCell, int cell);
    }

    public class ItemCell extends ImageButton {
        private final float animationSpeed = 4; //1/4 = 0.25 = 0.25 seconds
        public Image itemSprite;
        private float itemSpriteOffsetX = 0;
        private float itemSpriteOffsetY = 0;
        public Item itemRoot = new Item(new ItemValues.ItemData()); //Default
        public ItemValues.ItemCellData cellData = new ItemValues.ItemCellData();
        public Label quantity;
        public int hotbarLocation = -1; //0, 1, 2, 3 (3=main weapon) use -1 for inventory
        public static final float scale = 2.5f;
        private float animationFrameYOffset = 0;
        private float animationFrameHeight = 2*scale;
        private volatile boolean animationDone = true;

        public Action animation = new Action() {
            boolean up = false;

            @Override
            public boolean act(float delta) {
                animationDone = false;
                float speed = getHeight()*delta*animationSpeed;
                if(!up && !animationDone) animationFrameHeight += speed; //1 second
                else {
                    animationFrameYOffset += speed;
                    animationFrameHeight -= speed;
                }

                if(animationFrameHeight > getHeight()-2*scale-speed && !up) {
                    up = true;
                    loadIcon();
                } else if(up) {
                    if(animationFrameHeight-speed <= speed) {
                        animationFrameYOffset = 0;
                        animationFrameHeight = 2*scale;
                        animationDone = true;
                        up = false;
                        return true;
                    }
                }
                return false;
            }
        };

        public ItemCell(float width, float height, int hotbarLocation) {
            this(width, height);
            this.hotbarLocation = hotbarLocation;
        }

        Vector2 localMouse = new Vector2();
        Vector2 local = new Vector2();
        public boolean mouseOver() {
            localMouse.setZero();
            local.setZero();
            local.set(Gdx.input.getX(), Gdx.input.getY());
            localMouse.set(screenToLocalCoordinates(local));
            return localMouse.x >= 0 && localMouse.x <= getWidth() && localMouse.y >= 0 && localMouse.y <= getHeight();
        }

        public ItemCell(float width, float height) {
            super(new ImageButtonStyle());
            itemSprite = new Image();
            itemSprite.setBounds(getX(), getY(), width-4*scale, height-4*scale);
            super.getStyle().imageUp = skin.getDrawable("cell-background");
            super.getImageCell().getActor().setScale(scale);
            super.getImageCell().width(width/scale).height(height/scale);
            super.getImageCell().align(Align.bottomLeft);
            super.align(Align.bottomLeft);

            quantity = new Label("", skin, "itemInfo");
            quantity.setAlignment(Align.bottomRight);
            quantity.setFontScale(0.3f);
            super.addListener(new ChangeListener() {
                public void changed(ChangeEvent event, Actor actor) {
                    if(hotbarLocation == -1 && getActions().size == 0 && itemRoot.data.targetSlot >= 0 && itemRoot.data.targetSlot <= 3) {
                        swapItem(hotbarCells[itemRoot.data.targetSlot]);
                    }
                }
            });

        }

       public void swapItem(ItemCell other) { //TODO optimize? (Mainly the extra icon loading)
            if(other.itemRoot.data.id.equals("missing")) {
                other.itemRoot = this.itemRoot.copy();
                other.cellData = ItemValues.ItemCellData.copy(this.cellData);
                this.cellData = new ItemValues.ItemCellData();
                this.itemRoot = new Item(new ItemValues.ItemData());
            } else {
                Item tempRoot = this.itemRoot.copy();
                itemRoot = other.itemRoot;
                other.itemRoot = tempRoot;

               ItemValues.ItemCellData tempCellData = ItemValues.ItemCellData.copy(other.cellData);
               other.cellData = ItemValues.ItemCellData.copy(this.cellData);
               this.cellData = tempCellData;
            }

            equipListener.equip(other, other.itemRoot.data.targetSlot);
            other.addAction(other.animation);
            addAction(animation);
        }

        public void loadIcon() {
            if(!itemRoot.data.id.equals("missing")) {
                Gdx.app.postRunnable(new Runnable() {
                    public void run() {
                        itemSprite.setDrawable(new TextureRegionDrawable(itemRoot.loadIcon()));
                    }
                });
            } else itemSprite.setDrawable(null);
        }

        public void dismantle() {
            cellData = new ItemValues.ItemCellData();
            itemRoot = new Item(new ItemValues.ItemData());
            if(hotbarLocation == 3) equipListener.equip(null, 3);
            loadIcon();
        }

        public void draw(Batch batch, float parentAlpha) {
            super.draw(batch, parentAlpha);
            itemSprite.setPosition(getX()  + itemSpriteOffsetX + 2*scale, getY() + itemSpriteOffsetY + 2*scale);

            if(cellData != null && !itemRoot.data.id.equals("missing")) {
                itemSprite.draw(batch, parentAlpha);

                if(animationDone && cellData.quantity > 1) {
                    quantity.setText(cellData.quantity);
                    quantity.setSize(getWidth()*.5f, 5*scale);
                    quantity.setPosition(getX() + getWidth()*.5f - 3*scale, getY()+2*scale);
                    quantity.draw(batch, 1);
               }
            }
            if(!animationDone)  batch.draw(swapForeground, getX()+2*scale, getY()+2*scale + animationFrameYOffset, getWidth()-4*scale, animationFrameHeight-4);
        }

        public void clear() {
            itemSprite.setDrawable(null);
            cellData = null;
            itemRoot = new Item(new ItemValues.ItemData()); //Reload default "missing" item
            quantity.setText("");
        }
    }

    static class ItemWrapper {
        public ItemCell cell;
        public ItemWrapper(ItemCell cell) {
            this.cell = cell;
        }
    }

    /**
     * There need to be some drawables registered in the skin file:
     * cell-background
     * healthbar*/
    public PlayerInventory(Game game, float x, float y) {
        this.skin = game.resource;
        this.game = game;
        this.stage = game.gui;
        this.x = x;
        this.y = y;
        init();
    }

    //Initialize the widgets and place them
    private void init() {
        inv = new Group();
        hotbar = new Group();
        hotbar.setBounds(x, y, 200, 80);

        hotbarTable = new Table(skin);

        hotbarTable.setBounds(cellSize*1.5f, 0, hotbar.getWidth() - cellSize * 1.5f, hotbar.getHeight() * 0.5f);
        for(int i = 0; i < hotbarCells.length-1; i++) {
            hotbarCells[i] = new ItemCell(cellSize, cellSize, i);
            hotbarTable.add(hotbarCells[i]).width(cellSize).height(cellSize).expandX();
        }
        hotbarCells[hotbarCells.length-1] = new ItemCell(cellSize * 1.5f, cellSize * 1.5f, hotbarCells.length);
        hotbarCells[hotbarCells.length-1].setBounds(0, 0, cellSize * 1.5f, cellSize * 1.5f);

        hotbar.addActor(hotbarTable);
        hotbar.addActor(hotbarCells[hotbarCells.length-1]);

        ProgressBar.ProgressBarStyle healthbarStyle = new ProgressBar.ProgressBarStyle();
        healthbarStyle.background = skin.getDrawable("hotbar-healthbar-background");
        healthbarStyle.knobBefore = skin.getDrawable("hotbar-healthbar");
        healthbarStyle.knobAfter = skin.getDrawable("hotbar-healthbar-after");
        healthbar = new ProgressBar(0, 1, 0.01f, false, healthbarStyle);
        healthbar.setAnimateDuration(0.2f);
        healthbar.setBounds(hotbar.getX() + cellSize*1.5f + 3, hotbar.getY() + cellSize*1.5f - (cellSize*1.5f - cellSize), hotbar.getWidth() - cellSize*1.5f-3, cellSize*1.5f - cellSize);
        stage.addActor(healthbar);

        inv.addActor(hotbar);

        inventory = new Table(skin);
        inventory.pack();

        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                inventoryCells[i][j] = new ItemCell(cellSize, cellSize);
                inventory.add(inventoryCells[i][j]).width(cellSize).height(cellSize).pad(3).expandX();
            }
            inventory.row();
        }
        inventoryBackground = new Image(skin.getDrawable("item-info-common"));
        inventoryBackground.setHeight(200);
        inventoryBackground.setPosition(0, stage.getHeight() / 2 - 100);
        inventoryBackground.setWidth(stage.getWidth());
        inventoryBackground.setVisible(false);
        inv.addActor(inventoryBackground);

        recipesDisplay = new Table(skin);
        recipesDisplay.align(Align.left);
        recipesScrollpane = new ScrollPane(recipesDisplay);
        inv.addActor(recipesScrollpane);

        inventory.setBounds(hotbar.getX(), stage.getHeight() / 2, hotbar.getWidth(), inventory.getHeight());
        inventory.setY(inventory.getY() - inventory.getHeight() / 2);
        inv.addActor(inventory);

        stage.addListener(new InputListener() {
            InputProcessor prev = null;
            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if(keycode == Input.Keys.TAB && isVisible && !Main.debug.debugMode) {
                    inventoryOpened = false;
                    itemInfo.setDisplay(null);
                    inventory.setVisible(false);
                    inventoryBackground.setVisible(false);
                    inventory.clearActions();
                    recipesDisplay.setVisible(false);
                    itemInfo.setDisplay(null);
                    Gdx.input.setInputProcessor(prev);
                }
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(keycode == Input.Keys.TAB && isVisible && !Main.debug.debugMode) {
                    inventoryOpened = true;
                    inventory.setVisible(true);
                    inventoryBackground.setVisible(true);
                    inventory.addAction(new Action() {
                        float x = -inventory.getWidth();
                        float bgx = -stage.getWidth();
                        @Override
                        public boolean act(float delta) {
                            inventory.setX(x);
                            inventoryBackground.setX(bgx);
                            float speed = (PlayerInventory.this.x  - x)*0.2f;
                            float bgSpeed = (-bgx)*0.2f;
                            x += speed;
                            bgx += bgSpeed;
                            if(x > PlayerInventory.this.x || speed < 0.1f) {
                                inventory.setX(PlayerInventory.this.x);
                                inventoryBackground.setBounds(0, inventory.getY() - 100, stage.getWidth(), 200);
                                recipesDisplay.setBounds(inventory.getWidth() + inventory.getX() + recipesPadding*2, inventoryBackground.getY()+recipesPadding, inventoryBackground.getWidth() - inventory.getWidth() - inventory.getX(), inventoryBackground.getHeight()-recipesPadding*4);
                                recipesScrollpane.setBounds(recipesDisplay.getX(), inventoryBackground.getY(), inventoryBackground.getWidth() - recipesDisplay.getX() - recipesPadding, inventoryBackground.getHeight());
                                displayRecipes(Blueprint.blueprintLibrary);
                                return true;
                            } return false;
                        }
                    });
                    prev = Gdx.input.getInputProcessor();
                    Gdx.input.setInputProcessor(stage);
                }
                return true;
            }
        });
        inventory.setVisible(false);
        stage.addActor(inv);

        itemInfo = new ItemInfo(game, stage, skin);

        xpDisplay = new Table(skin);
        xpDisplay.align(Align.topLeft);
        xpDisplay.setPosition(hotbar.getX(), stage.getHeight() - 10);
        Image icon = new Image(skin.getDrawable("xp-display-icon"));
        xpDisplay.add(icon).height(cellSize*0.7f).width(cellSize*0.7f);
        xpCount = new Label("", skin, "itemInfo");
        xpCount.setFontScale(0.5f);
        xpDisplay.add(xpCount).align(Align.center);
        stage.addActor(xpDisplay);
    }

    /**Displays the selected recipes beneath the inventory slots*/
    public void displayRecipes(ArrayList<Blueprint> recipes) {
        recipesDisplay.clearChildren();
        for(int i = 0; i < recipes.size(); i++) {
            final Item data = Item.getItem(recipes.get(i).recipe.targetID);
            final Blueprint blueprint = recipes.get(i);
            Table content = new Table(skin);
            Table itemFrame = new Table(skin);
            itemFrame.setBackground(skin.getDrawable("item-slot-common"));
            Image targetItem = new Image(Item.getItem(recipes.get(i).recipe.targetID).loadIcon());
            itemFrame.add(targetItem).width(cellSize).height(cellSize);
            itemFrame.pack();
            content.add(itemFrame).align(Align.topLeft).width(cellSize).height(cellSize).pad(3);
            Table xpRequirements = new Table(skin);
            xpRequirements.add(new Label("Need:", skin, "itemInfo")).align(Align.topLeft).colspan(2).getActor().setFontScale(ItemInfo.FONT_SCALE*1.2f);
            xpRequirements.row();
            Image xpImage = new Image(skin.getDrawable("xp-display-icon"));
            xpRequirements.add(xpImage).width(cellSize*.5f).height(cellSize*.5f).align(Align.left);
            xpRequirements.add(new Label("x " + recipes.get(i).recipe.xp, skin, "itemInfo")).getActor().setFontScale(ItemInfo.FONT_SCALE);
            content.add(xpRequirements).align(Align.topLeft);
            content.row();
            content.add(new Label(data.data.name, skin, "itemInfo")).colspan(2).getActor().setFontScale(ItemInfo.FONT_SCALE);
            content.row();
            Table requirements = new Table(skin);
            requirements.align(Align.left);
            ArrayList<Label> itemReq = new ArrayList<>(blueprint.recipe.requirements.size());
            for(int j = 0; j < recipes.get(i).recipe.requirements.size(); j++) {
                Table frame = new Table(skin);
                Image req = new Image(Item.getItem(recipes.get(i).recipe.requirements.get(j).itemID).loadIcon());
                frame.add(req).width(cellSize*.6f).height(cellSize*.6f);
                frame.setBackground(skin.getDrawable("item-slot-common"));
                frame.pack();
                requirements.add(frame).width(cellSize*.7f).height(cellSize*.7f).align(Align.left).padTop(2);
                itemReq.add(new Label("x " + recipes.get(i).recipe.requirements.get(j).amount, skin, "itemInfo"));
                requirements.add(itemReq.get(itemReq.size()-1)).padTop(2).align(Align.center).expandX().getActor().setFontScale(ItemInfo.FONT_SCALE);
                requirements.row();
            }
            content.add(requirements).align(Align.center).colspan(2).align(Align.center).fillX();
            content.align(Align.topLeft);
            content.setBackground(skin.getDrawable("item-info-common"));
            content.pad(10);
            content.row();
            content.setColor(ItemInfo.getRarityColor(data.data.rarity));
            Cell c = recipesDisplay.add(content).width(content.getPrefWidth()).height(inventoryBackground.getHeight()-recipesPadding*2).align(Align.center).padTop(recipesPadding).padBottom(recipesPadding).padLeft(5).padRight(5);
            content.addAction(new Action() {
                Vector2 mouse = new Vector2();
                Vector2 local = new Vector2();
                final Color originalColor = content.getColor().cpy();
                float cOffset = 0;
                boolean wasOn = false;
                boolean canCraft = false;
                @Override
                public boolean act(float delta) {
                    if(!inventoryOpened) return true;

                    mouse.set(Gdx.input.getX(), Gdx.input.getY());
                    local.set(content.screenToLocalCoordinates(mouse));
                    if(local.x > 0 && local.x < content.getWidth() && local.y > 0 && local.y < content.getHeight()) {
                        if(!wasOn) {
                            canCraft = xp >= blueprint.recipe.xp;
                            updateRecipe(blueprint, itemReq);
                            for(ItemValues.Requirements r : blueprint.recipe.requirements) {
                                if(getAmountOf(r.itemID) < r.amount) {
                                    canCraft = false;
                                    break;
                                }
                            }
                        }
                        wasOn = true;
                        if(canCraft) {
                            itemInfo.setDisplay(data, null);
                            if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                                itemInfo.progessBar.setVisible(true);
                                itemInfo.progessBar.setColor(Color.GREEN);
                                itemInfo.progessBar.setValue(itemInfo.progessBar.getValue() + (delta / (data.data.destroyTime*.5f)));
                                if (itemInfo.progessBar.getValue() == itemInfo.progessBar.getMaxValue()) {
                                    itemInfo.progessBar.setValue(0);
                                    itemInfo.progessBar.setVisible(false);
                                    ItemValues.ItemCellData newItem = new ItemValues.ItemCellData();
                                    newItem.id = data.data.id;
                                    newItem.level = 0;
                                    newItem.quantity = 1;
                                    give(newItem);
                                    giveXP(-blueprint.recipe.xp);
                                    wasOn = false;
                                    for(ItemValues.Requirements r : blueprint.recipe.requirements) {
                                        remove(r.itemID, r.amount);
                                    }
                                    updateRecipe(blueprint, itemReq);
                                }
                            } else {
                                itemInfo.progessBar.setValue(0);
                                itemInfo.progessBar.setVisible(false);
                                itemInfo.progessBar.setColor(Color.RED);
                            }
                            if (cOffset <= 0.2f) cOffset += delta;
                            else cOffset = 0.2f;
                            content.setColor(originalColor.r - cOffset, originalColor.g - cOffset, originalColor.b - cOffset, 1);
                        } else {
                            itemInfo.setDisplay(null, null);
                            itemInfo.progessBar.setColor(Color.RED);
                        }
                    } else {
                        canCraft = false;
                        if(wasOn) {
                            for(Label l : itemReq) {
                                for(ItemValues.Requirements r : blueprint.recipe.requirements) {
                                    l.setText("x " + r.amount);
                                    l.setColor(Color.WHITE);
                                }
                            }
                        }
                        wasOn = false;
                        content.setColor(originalColor);
                        cOffset = 0;
                    }
                    return false;
                }
            });
        }
        recipesDisplay.clearActions();
        recipesDisplay.setVisible(true);
        recipesDisplay.setColor(1, 1, 1, 0f);
        recipesDisplay.addAction(new Action() {
            float a = 0;
            @Override
            public boolean act(float delta) {
                a += delta*2;
                recipesDisplay.setColor(1, 1, 1, a);
                return a >= 1;
            }
        });
    }

    private void updateRecipe(Blueprint blueprint, ArrayList<Label> itemReq) {
        for (int i = 0; i < blueprint.recipe.requirements.size(); i++) {
            int amount = getAmountOf(blueprint.recipe.requirements.get(i).itemID);
            itemReq.get(i).setText(amount + " / " + blueprint.recipe.requirements.get(i).amount);
            if(blueprint.recipe.requirements.get(i).amount > amount) {
                itemReq.get(i).setColor(Color.RED);
            } else itemReq.get(i).setColor(Color.GREEN);
        }
    }

    public void update() {
        xpCount.setText("x" + xp);
        if(!inventory.isVisible()) return;

        itemInfo.act(Gdx.graphics.getDeltaTime());
        boolean anythingHit = false;
        a: for (int i = 0; i < inventorySizeX; i++) {
            for (int j = 0; j < inventorySizeY; j++) {
                if (!inventoryCells[i][j].itemRoot.data.id.equals("missing")) {
                    if (inventoryCells[i][j].mouseOver()) {
                        itemInfo.setDisplay(inventoryCells[i][j]);
                        anythingHit = true;
                        break a;
                    }
                }
            }
        }
        for(ItemCell cell : hotbarCells) {
            if(cell.mouseOver() && !cell.itemRoot.data.id.equals("missing")) {
                itemInfo.setDisplay(cell);
                anythingHit = true;
                break;
            }
        }
        if(!anythingHit) itemInfo.setDisplay(null);
    }

    public void remove(String id, int amount) {
        for(int i = 0; i < hotbarCells.length; i++) {
            if(hotbarCells[i].itemRoot != null) {
                if (hotbarCells[i].itemRoot.data.id.equals(id) && hotbarCells[i].cellData.quantity >= amount) {
                    hotbarCells[i].cellData.quantity -= amount;
                    if(hotbarCells[i].cellData.quantity <= 0) hotbarCells[i].dismantle();
                    return;
                }
            }
        }

        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                if(inventoryCells[i][j].itemRoot != null) {
                    if (inventoryCells[i][j].itemRoot.data.id.equals(id) && inventoryCells[i][j].cellData.quantity >= amount) {
                        inventoryCells[i][j].cellData.quantity -= amount;
                        if(inventoryCells[i][j].cellData.quantity <= 0) inventoryCells[i][j].dismantle();
                        return;
                    }
                }
            }
        }
    }

    public int getAmountOf(String itemID) {
        int amount = 0;
        ItemCell[] cells = getAllCells();
        for(ItemCell c : cells) {
            if(c.itemRoot.data.id.equals(itemID)) amount += c.cellData.quantity;
        }
        return amount;
    }

    public ItemCell[] getAllCells() {
        ArrayList<ItemCell> cells = new ArrayList<>(Arrays.asList(hotbarCells));
        for(int i = 0; i < inventorySizeX; i++) cells.addAll(Arrays.asList(inventoryCells[i]));
        return cells.toArray(new ItemCell[cells.size()]);
    }

    public boolean has(String id, int minCount) {
        for(int i = 0; i < hotbarCells.length; i++) {
            if(hotbarCells[i].itemRoot != null) {
                if (hotbarCells[i].itemRoot.data.id.equals(id) && hotbarCells[i].cellData.quantity >= minCount) return true;
            }
        }

        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                if(inventoryCells[i][j].itemRoot != null) {
                    if (inventoryCells[i][j].itemRoot.data.id.equals(id) && inventoryCells[i][j].cellData.quantity >= minCount) return true;
                }
            }
        }
        return false;
    }

    public void equip(String itemID) {
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                if(inventoryCells[i][j].itemRoot != null) {
                    if (inventoryCells[i][j].itemRoot.data.id.equals(itemID)) inventoryCells[i][j].getClickListener().clicked(null, 0, 0);
                }
            }
        }
    }

    public void clear() {
        equipListener.equip(null, -1);
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
               inventoryCells[i][j].clear();
            }
        }
        for(int i = 0; i < hotbarCells.length; i++) hotbarCells[i].clear();
    }

    public boolean give(ItemValues.ItemCellData cellData) {
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                    if (inventoryCells[i][j].itemRoot.data.id.equals(cellData.id) && inventoryCells[i][j].cellData.quantity + cellData.quantity <= inventoryCells[i][j].itemRoot.data.maxStack) {
                        inventoryCells[i][j].cellData.quantity += cellData.quantity;
                        inventoryCells[i][j].addAction( inventoryCells[i][j].animation);
                        return true;
                    }
            }
        }
        for(ItemCell cell : hotbarCells) {
            if(cell.itemRoot.data.id.equals(cellData.id) && cell.cellData.quantity + cellData.quantity <= cell.itemRoot.data.maxStack) {
                cell.cellData.quantity += cellData.quantity;
                cell.addAction(cell.animation);
                return true;
            }
        }

        Item item = Item.getItem(cellData.id);
        if(item.data.id.equals("missing")) return false;
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                if(inventoryCells[i][j].itemRoot.data.id.equals("missing")) {
                    inventoryCells[i][j].itemRoot = item;
                    inventoryCells[i][j].cellData = ItemValues.ItemCellData.copy(cellData);
                    inventoryCells[i][j].loadIcon();
                    inventoryCells[i][j].addAction(inventoryCells[i][j].animation);
                    return true;
                }
            }
        }
        for(ItemCell cell : hotbarCells) {
            if(cell.itemRoot.data.id.equals("missing")) {
                cell.itemRoot = item;
                cell.cellData = ItemValues.ItemCellData.copy(cellData);
                cell.loadIcon();
                cell.addAction(cell.animation);
                return true;
            }
        }

        return false;
    }

    public void setEquipListener(EquipListener equipListener) {
        this.equipListener = equipListener;
    }

    public boolean touches(float x, float y) {
        Vector2 temp1 = new Vector2(hotbar.screenToLocalCoordinates(new Vector2(x, y)));
        Vector2 temp2 = new Vector2(inventory.screenToLocalCoordinates(new Vector2(x, y)));
        return hotbar.hit(temp1.x, temp1.y, false) != null || inventory.hit(temp2.x, temp2.y, false) != null;
    }

    /**Loads the inventory from the game file custom save data. Saved names can be:
     * cell1, cell2, cell3, cell0-0, cell0-1 ...*/
    public void loadInventory(Game game) {
        for(int i = 0; i < hotbarCells.length; i++) {
            hotbarCells[i].cellData = game.getCutomData("cell" + i, ItemValues.ItemCellData.class);
            if(hotbarCells[i].cellData != null) {
                hotbarCells[i].itemRoot = Item.getItem(hotbarCells[i].cellData.id);
                hotbarCells[i].loadIcon();
                if (!hotbarCells[i].itemRoot.data.id.equals("missing")) equipListener.equip(hotbarCells[i], i);
            }
        }
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                inventoryCells[i][j].cellData = game.getCutomData("cell" + i + "-" + j, ItemValues.ItemCellData.class);
                if(inventoryCells[i][j].cellData != null) {
                    inventoryCells[i][j].itemRoot = Item.getItem(inventoryCells[i][j].cellData.id);
                    inventoryCells[i][j].loadIcon();
                }
            }
        }
        String amount = game.hashData.get("xp");
        xpCount.setText(amount == null ? "x 0" : "x " + amount);
        if(amount != null) xp = Integer.valueOf(amount);
        else xp = 0;
    }

    public void saveInventory(Game game) {
        for(int i = 0; i < hotbarCells.length; i++) {
            game.setCustomData("cell" + i, hotbarCells[i].cellData, ItemValues.ItemCellData.class);
        }
        for(int i = 0; i < inventorySizeX; i++) {
            for(int j = 0; j < inventorySizeY; j++) {
                game.setCustomData("cell" + i + "-" + j, inventoryCells[i][j].cellData, ItemValues.ItemCellData.class);
            }
        }
        game.hashData.set("xp", String.valueOf(xp));
    }

    public void giveXP(int amount) {
        xpCount.clearActions();
        xp += amount;
        xpCount.addAction(new Action() {
            float scale = 0.9f;
            public boolean act(float delta) {
                xpCount.setFontScale(scale);
                scale -= delta;
                if(scale <= 0.5f) {
                    xpCount.setFontScale(0.5f);
                    scale = 1f;
                    return true;
                }
                return false;
            }
        });
    }

    public Item getEquippedItem() {
        return hotbarCells[3].itemRoot;
    }

    public ItemCell getEquippedCell() {
        return hotbarCells[3];
    }

    public void dispose() {
        xpDisplay.remove();
        inv.remove();
        hotbar.remove();
        healthbar.remove();
        hotbarTable.remove(); //Table of the hotbar cells beside the bix main weapon
        inventory.remove();
    }

    public boolean isInventoryOpened() {
        return inventoryOpened;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
        inv.setVisible(visible);
        hotbar.setVisible(visible);
        healthbar.setVisible(visible);
        hotbarTable.setVisible(visible);
        if(!visible) inventory.setVisible(false);
    }
}
