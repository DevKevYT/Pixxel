package com.mygdx.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Disposable;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.items.Item;
import com.mygdx.items.ItemValues;
import com.mygdx.objects.Game;
import com.mygdx.objects.GameValues;
import com.mygdx.ui.ItemInfo;

public class PlayerInventory {

    public ItemInfo itemInfo;
    private Game game;

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
                    if(hotbarLocation == -1 && getActions().size == 0) {
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

        inventory.setBounds(hotbar.getX(), stage.getHeight() / 2, hotbar.getWidth(), inventory.getHeight());
        inventory.setY(inventory.getY() - inventory.getHeight() / 2);
        inv.addActor(inventory);

        stage.addListener(new InputListener() {

            InputProcessor prev = null;
            @Override
            public boolean keyUp(InputEvent event, int keycode) {
                if(keycode == Input.Keys.TAB && isVisible && !Main.debug.debugMode) {
                    itemInfo.setDisplay(null);
                    inventory.setVisible(false);
                    inventoryBackground.setVisible(false);
                    inventory.clearActions();
                    Gdx.input.setInputProcessor(prev);
                }
                return true;
            }

            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if(keycode == Input.Keys.TAB && isVisible && !Main.debug.debugMode) {
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
    }

    public void update() {
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
                        return true;
                    }
            }
        }
        for(ItemCell cell : hotbarCells) {
            if(cell.itemRoot.data.id.equals(cellData.id) && cell.cellData.quantity + cellData.quantity <= cell.itemRoot.data.maxStack) {
                cell.cellData.quantity += cellData.quantity;
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
                    return true;
                }
            }
        }
        for(ItemCell cell : hotbarCells) {
            if(cell.itemRoot.data.id.equals("missing")) {
                cell.itemRoot = item;
                cell.cellData = ItemValues.ItemCellData.copy(cellData);
                cell.loadIcon();
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
    }

    public Item getEquippedItem() {
        return hotbarCells[3].itemRoot;
    }

    public ItemCell getEquippedCell() {
        return hotbarCells[3];
    }

    public void dispose() {
        inv.remove();
        hotbar.remove();
        healthbar.remove();
        hotbarTable.remove(); //Table of the hotbar cells beside the bix main weapon
        inventory.remove();
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
