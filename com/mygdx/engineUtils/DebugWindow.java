package com.mygdx.engineUtils;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextArea;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter;
import com.badlogic.gdx.scenes.scene2d.ui.Tree;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ActorGestureListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.SpriteDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Scaling;
import com.mygdx.ai.Connection;
import com.mygdx.darkdawn.Logger;
import com.mygdx.darkdawn.Main;
import com.mygdx.files.JsonHandler;
import com.mygdx.objects.*;
import com.mygdx.objects.RootValues.RootObjectValues;
import com.mygdx.objects.WorldValues.TriggerValues;
import com.mygdx.objects.WorldValues.WorldObjectValues;
import com.mygdx.ui.ObjectHolder;
import com.mygdx.utils.Tools;
import com.badlogic.gdx.scenes.scene2d.ui.Tree.Node;

public class DebugWindow implements Disposable {

	public boolean debugMode = false;
	private WorldObject moving = null;
	public Texture dot;
	private TextureRegion resizeIcon;
	private TextureRegion rotateIcon;
	private Game game;
	
	public final Stage stage;
	private Window window;
	private Skin skin;

	//Shape rendering
	private Box2DDebugRenderer debug;
	private ShapeRenderer shape;

	private TextButton tabMinimize;
	private byte activeTab = 0;

	private float viewportZoom = 1;
	private Vector3 viewportPos = new Vector3();
	private float debugZoom = 1;
	private Vector3 debugPos = new Vector3();

	private Vector2 pointer = new Vector2();
	private Label info;
	private float offX = 0, offY = 0;
	private OrthographicCamera stageView = new OrthographicCamera();
	private float initZoom = 1;

	//onUpdate
	private float time = 0;
	private Vector2 originPos = new Vector2();
	private float dist = 0;
	private Vector2 camOrigin = new Vector2();
	private float camDist = 0;
	private float camTime = 0;
	private Vector2 addMousePos = new Vector2();
	private boolean yOffMovin = false;
	private CheckBox fixed;
	private CheckBox visible;
	private boolean groupIndexListener = true;
	private SelectBox<String> groupIndex;
	private boolean maskListener = true;
	private ButtonGroup<CheckBox> radioButtons;
	private boolean  categoryListener = true;
	private SelectBox<String> category;
	private Table leftclickMenue;
	public boolean showDebug = false;
	private TextField address;
	private CheckBox fixedRot;

	public DebugWindow(Game game, ArrayList<RootObject> preloaded, Skin skin) {
		debug = new Box2DDebugRenderer();
		Pixmap dotPx = new Pixmap(20, 20, Format.RGBA4444);
		dotPx.setColor(Color.WHITE);
		dotPx.fillCircle(10, 10, 10);
		dot = new Texture(dotPx);
		dotPx.dispose();

		Pixmap resIcon = new Pixmap(10, 10, Format.RGBA4444);
		resIcon.setColor(Color.YELLOW);
		resIcon.fillRectangle(0, 0, 3, 10);
		resIcon.fillRectangle(0, 7, 10, 3);
		resizeIcon = new TextureRegion(new Texture(resIcon));
		resIcon.dispose();

		Pixmap rotIcon = new Pixmap(10, 10, Format.RGBA4444);
		rotIcon.setColor(Color.YELLOW);
		rotIcon.fillCircle(0, 0, 8);
		rotateIcon = new TextureRegion(new Texture(rotIcon));
		rotIcon.dispose();

		this.game = game;
		this.skin = skin;
		//Scale the script font//
		skin.getFont("script-font").getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		skin.getFont("script-font").getData().setScale(0.7f);
		//skin.getFont("script-font").getData().
		shape = new ShapeRenderer();
		shape.setAutoShapeType(true);
		this.preloaded = preloaded;

		stageView = new OrthographicCamera();
		stage = new Stage();

		viewportZoom = game.stage.getPPM();
		info = new Label("", skin);
		info.setFontScale(0.8f);
		info.setAlignment(Align.topLeft);
		stage.addActor(info);
		init();
	}

	public void updateViewport(int width, int height, Camera cam) {
	    stage.getViewport().update(width, height);
	    stage.getViewport().apply();
		if(Gdx.app.getType() == Application.ApplicationType.Android) {
			window.setScale((float) width / 800f, (float) height / 800f *( (float) width/ (float) height));
		}
	}
	
	private void init() {
		window = new Window("Debug", skin);
		window.setKeepWithinStage(false);
		window.setResizable(true);
		window.setSize(Gdx.graphics.getWidth() * 0.25f, 250);
		window.setPosition(0, 0);
		window.align(Align.topLeft);
		stage.addActor(window);
		window.clearChildren();
		window.setVisible(false);

		tabMinimize = new TextButton("X", skin);
		window.row();

		tabMinimize.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				deselectAllWorldObjects();
				setDebug(false);
			}
		});

		Table edit = initTab1(window.getWidth(), window.getHeight());
		edit.setPosition(5, 0);
		edit.align(Align.topLeft);
		final ScrollPane paneTab1 = new ScrollPane(edit, skin);
		paneTab1.setScrollingDisabled(true, false);
		paneTab1.setPosition(5, -20);
		window.addActor(paneTab1);

		Table edit2 = initTab12(window.getWidth(), window.getHeight());
		edit2.align(Align.topLeft);
		edit2.setY(30);
		final ScrollPane paneTab12 = new ScrollPane(edit2, skin);
		paneTab12.setScrollingDisabled(true, false);
		paneTab12.setScrollbarsVisible(false);
		window.add(paneTab12);

		window.addListener(new EventListener() {
			public boolean handle(Event event) {
				paneTab1.setSize(window.getWidth()*.5f, window.getHeight());
				paneTab12.setSize(window.getWidth() * .5f, window.getHeight());
				paneTab12.setPosition(paneTab1.getX() + paneTab1.getWidth() + 5, 2.5f);
				return false;
			}
		});
		window.setSize(edit.getPrefWidth() + edit2.getPrefWidth(), edit.getPrefHeight() + 40);

		//Zoom listener, for Desktop and Phone
		stage.addListener(new ActorGestureListener() {
			@Override
			public void zoom(InputEvent event, float initialDistance, float distance) {
				if(Gdx.app.getType() != Application.ApplicationType.Android || !debugMode || game.stage == null) return;

				if(game.stage.getViewport().zoom >= .1f) {
					game.stage.getViewport().zoom = (initialDistance / distance) * initZoom;
				}
				if(game.stage.getViewport().zoom < .1f) game.stage.getViewport().zoom = .1f;
			}

			@Override
			public void touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if(Gdx.app.getType() == Application.ApplicationType.Android || !debugMode || game.stage != null)
					initZoom = game.stage.getViewport().zoom;
			}
		});

		stage.addListener(new InputListener() {
			@Override
			public boolean scrolled(InputEvent event, float x, float y, int amount) {
				if(Gdx.app.getType() != Application.ApplicationType.Desktop || game.stage == null) return false;
				if(debugMode && !onWindow(Gdx.input.getX(), Gdx.input.getY())) {
					stage.unfocusAll();
					if(amount > 0) game.stage.getViewport().zoom += .25f*(game.stage.getViewport().zoom);
					if(amount < 0 && game.stage.getViewport().zoom >= .1f) game.stage.getViewport().zoom -= .25f*(game.stage.getViewport().zoom);
					//if(game.stage.getViewport().zoom >= 3) game.stage.getViewport().zoom = 3;
					if(game.stage.getViewport().zoom < .1f) game.stage.getViewport().zoom = .1f;
					debugZoom = game.stage.getViewport().zoom;
					game.stage.getViewport().update();
				} else if(debugMode && onWindow(Gdx.input.getX(), Gdx.input.getY())) {
					Vector2 pos = paneTab12.screenToLocalCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
					if(paneTab12.hit(pos.x, pos.y, true) != null) {
						stage.setScrollFocus(paneTab12);
						return false;
					}

					Vector2 pos2 = paneTab1.screenToLocalCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
					if(paneTab1.hit(pos2.x, pos2.y, true) != null) {
					    stage.setScrollFocus(paneTab1);
						return false;
					}
				}
				return super.scrolled(event, x, y, amount);
			}
		});

		//Init leftclick menue
		leftclickMenue = new Table(skin);
		leftclickMenue.setVisible(false);
		leftclickMenue.align(Align.topLeft);
		Pixmap background = new Pixmap(50, 50, Format.RGBA4444);
		background.setColor(Color.DARK_GRAY);
		background.fill();
		leftclickMenue.setBackground("leftclick-menue");
		TextButton copy = new TextButton("Copy", skin);
		copy.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(selectedObjects.isEmpty()) return; //Nothing to copy
				addObjectData.clear();
				Vector2 rootPos = selectedObjects.get(0).object.getPosition();
				for(SelectedObject s : selectedObjects) {
					s.object.updateBehaviorVariables();
					AddObject a = new AddObject(s.object.worldObjectValues.copy(),
							s.object.getRootValues().copy(), new Vector2(rootPos.x - s.object.getPosition().x, rootPos.y - s.object.getPosition().y));
					a.offset.scl(game.stage.getScale());
					a.addObjectRoot.setScale(a.addObjectRoot.getScale() / game.stage.getScale());
					addObjectData.add(a);
				}
				leftclickMenue.setVisible(false);
			}
		});
		leftclickMenue.add(copy).width(100).padLeft(10).padRight(10).height(20).colspan(6).padTop(5).align(Align.left).row();
		TextButton delete = new TextButton("Delete", skin);
		delete.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(selectedObjects.isEmpty()) return;
				for(SelectedObject s : selectedObjects) game.stage.removeObject(s.object);
				deselectAllWorldObjects();
				leftclickMenue.setVisible(false);
			}
		});
		leftclickMenue.add(delete).width(100).padLeft(10).padRight(10).height(20).padTop(2).colspan(6).align(Align.left).row();
		TextButton editTrigger = new TextButton("Edit Trigger", skin);
		editTrigger.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(selectedObjects.size() != 1) return;
				WorldObject worldObjectedit = selectedObjects.get(0).object;
				if(worldObjectedit.worldObjectValues.change.trigger != null) {
					compileInfo.setText("Last output:\n" + worldObjectedit.getTrigger().getErrors());
					messageText.setText(worldObjectedit.worldObjectValues.change.trigger.messageText);
					script = worldObjectedit.worldObjectValues.change.trigger.scr;
					triggerScript.setText(script);
					checkTiles.setChecked(worldObjectedit.getTrigger().values.tileCheck);
					base.removeAll();
					for(int k :  worldObjectedit.worldObjectValues.change.trigger.keys) {
						if(k > 0) base.add(new Node(new Label(Keys.toString(k), skin)));
						else if(k < 0) base.add(new Node(new Label(String.valueOf(k), skin)));
					}
				} else {
					script = "";
					triggerScript.setText("");
					compileInfo.setText("");
					base.removeAll();
				}
				DebugWindow.this.editTrigger.setPosition(0, 0);
				DebugWindow.this.editTrigger.setVisible(true);
				DebugWindow.this.editTrigger.toFront();
				leftclickMenue.setVisible(false);
			}
		});
		leftclickMenue.add(editTrigger).width(110).padLeft(10).padRight(10).padTop(2).height(20).colspan(6).align(Align.left).row();
		TextButton editBehavior = new TextButton("Edit Behavior", skin);
		editBehavior.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if(selectedObjects.size() != 1) return;
				DebugWindow.this.editBehavior.setVisible(true);
				setBehaviorTabs(selectedObjects.get(0).object);
				DebugWindow.this.editBehavior.toFront();
				DebugWindow.this.editBehavior.setPosition(stage.getWidth() / 2f - editBehavior.getWidth() / 2f, stage.getHeight() / 2f - editBehavior.getHeight() / 2f);
				leftclickMenue.setVisible(false);
			}
		});
		leftclickMenue.add(editBehavior).width(110).padLeft(10).padTop(2).padRight(10).height(20).align(Align.left).colspan(6).row();

		category = new SelectBox<String>(skin);
		category.setItems("1", "2", "3", "4", "5");
		category.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(!categoryListener) return;
				for(SelectedObject s : selectedObjects) {
					s.object.setCategoryBit(Integer.valueOf(category.getSelected()));
				}
			}});

		groupIndex = new SelectBox<String>(skin);
		groupIndex.setItems("-5", "-4", "-3", "-2", "-1", "0", "1", "2", "3", "4", "5");
		groupIndex.setSelected("0");
		groupIndex.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(!groupIndexListener) return;
				for(SelectedObject s : selectedObjects) {
					s.object.setGroupIndex(Integer.valueOf(groupIndex.getSelected()));
					Logger.logInfo("", "Setting group index...");
				}
			}});
		leftclickMenue.add("Group").maxWidth(60).padTop(2).padLeft(10).height(20).colspan(3).align(Align.left);
		leftclickMenue.add(groupIndex).width(40).padTop(2).padRight(10).height(20).colspan(3).align(Align.left).row();

		leftclickMenue.add("Category").maxWidth(70).padTop(2).padLeft(10).height(20).colspan(3).align(Align.left);
		leftclickMenue.add(category).width(30).padTop(2).padRight(10).height(20).colspan(3).align(Align.left).row();

		radioButtons = new ButtonGroup<>();
		radioButtons.add(new CheckBox("1", skin));
		radioButtons.add(new CheckBox("2", skin));
		radioButtons.add(new CheckBox("3", skin));
		radioButtons.add(new CheckBox("4", skin));
		radioButtons.add(new CheckBox("5", skin));
		radioButtons.setMaxCheckCount(5);
		radioButtons.setMinCheckCount(0);
		leftclickMenue.add("Mask").maxWidth(35).padTop(2).padLeft(10).height(20).align(Align.left);
		for(int i = 0; i < radioButtons.getButtons().size; i++) {
			leftclickMenue.add(radioButtons.getButtons().get(i)).width(23).maxWidth(23).padLeft(i == 0 ? 10 : 0).padRight(i == radioButtons.getButtons().size-1 ? 10 : 0).align(Align.center);
			radioButtons.getButtons().get(i).setChecked(true);
			radioButtons.getButtons().get(i).addListener(new ChangeListener() {
				public void changed(ChangeEvent event, Actor actor) {
					if(!maskListener) return;
					for(SelectedObject s : selectedObjects) {
						ArrayList<Integer> maskBits = new ArrayList<>();
						for(CheckBox c : radioButtons.getButtons()) {
							if(c.isChecked()) maskBits.add(Integer.valueOf(c.getLabel().getText().toString()));
						}
						s.object.setMaskBits(maskBits);
					}
				}});
			leftclickMenue.validate();
		}

		leftclickMenue.row();
		address = new TextField("", skin);
		address.setMessageText("Address");
		address.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					s.object.worldObjectValues.addr = address.getText();

					if (s.object.getAddress().equals(WorldObject.NO_ADDR) || s.object.getAddress().equals(WorldObject.TILE_ADDR)) {
						game.stage.getObjectWithAddress().remove(s.object);
					} else {
						boolean found = false;
						for(WorldObject obj : game.stage.getObjectWithAddress()) {
							if(obj.equals(s.object)) {
								found = true; break;
							}
						}
						if(!found) game.stage.getObjectWithAddress().add(s.object);
					}
				}
			}});
		leftclickMenue.add(address).width(100).padLeft(10).padRight(10).padTop(2).height(25).align(Align.left).colspan(6).row();

		fixed = new CheckBox("Moveable", skin);
		fixed.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					s.object.setFixed(!fixed.isChecked());
				}
			}
		});
		leftclickMenue.add(fixed).width(110).padLeft(10).padTop(2).padRight(10).height(20).align(Align.left).colspan(6).row();
		background.dispose();
		fixedRot = new CheckBox("Fixed Rot.", skin);
		fixedRot.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					if (s.object.hasHitbox()) {
						s.object.getHitboxBody().setFixedRotation(fixedRot.isChecked());
						s.object.worldObjectValues.change.fixedRotation = fixedRot.isChecked();
					}
				}
			}});
		visible = new CheckBox("Visible", skin);
		visible.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					s.object.worldObjectValues.visible = visible.isChecked();
				}
			}});
		leftclickMenue.add(visible).padLeft(10).padRight(10).padTop(2).height(20).width(110).align(Align.left).colspan(6).row();
		leftclickMenue.add(fixedRot).padLeft(10).padRight(10).padTop(2).height(20).width(110).align(Align.left).colspan(6).padBottom(5).row();
		//leftclickMenue.setDebug(true);
		leftclickMenue.pack();
		stage.addActor(leftclickMenue);

		window.removeActor(paneTab1);
		window.removeActor(paneTab12);

		window.addActor(paneTab1);
		window.addActor(paneTab12);
		activeTab = 0;
	}

	/**Its assumed, the matrix has been set and begin was called.*/
	public void drawVertices(RootObjectValues object, float x, float y, ShapeRenderer renderer) {
		if(game.stage == null) return;
		float s = game.stage.getScale();
		Color prev = new Color(renderer.getColor());
		renderer.setColor(Color.YELLOW);
		renderer.rect(x - object.size.x * object.scale * s * .5f, y - object.size.y * object.scale * s * .5f, object.size.x * object.scale * s, object.size.y * object.scale * s);
		if(object.fixtures != null) {
			for (RootValues.Fixture f : object.fixtures) {
				if (f.isCircle) {
					renderer.setColor(Color.GREEN);
					renderer.ellipse(x + f.xOff * s * object.scale * .5f - f.width/2*object.scale*game.stage.getScale(), y + f.yOff * s * object.scale * .5f - f.width/2*object.scale*game.stage.getScale(), f.width * s * object.scale, f.width * s * object.scale, 10);
				} else {
					renderer.setColor(Color.GREEN);
					renderer.rect(x + f.xOff * object.scale * s - f.width * object.scale * s * .5f, y + f.yOff * object.scale * s - f.height * object.scale * s * .5f, f.width * object.scale * s, f.height * object.scale * s);
				}
			}
		}
		renderer.setColor(prev);
	}

	boolean resizing = false;
	boolean rotating = false;
	private Vector2 resizeRotatePointer = new Vector2();
	private int startRotation = 0;
	private Vector2 startSize = new Vector2();
	private Vector2 rubberbandPointer = new Vector2();
	private boolean rubberbandActive = false;
	private boolean nodeTouched = false;
	private com.mygdx.ai.Node connectMount = null;

	public void update() {
		if(!debugMode || game.stage == null) return;

		boolean onWindow = onWindow(Gdx.input.getX(), Gdx.input.getY());

		//Visual and tools to add objects like tile perfect and neighbot perfect
		//by modifying the adMousePos
		addMousePos.set(game.stage.getMousePos());
		if((tile.isChecked() || tilePosition.isChecked()) && game.stage.getTouchedTile() != null) addMousePos.set(game.stage.getTouchedTile().groundTile.getPosition().scl(game.stage.getScale()));
		if(neighborPerfect.isChecked() && selectedObjects.size() == 1 && selected != null) {
			WorldObject obj = selectedObjects.get(0).object;
			Vector2 normPos = game.stage.getMousePos().scl(game.stage.getPPM());
			float y = obj.getPosition().y;
			float x = obj.getPosition().x;
			float sY = obj.getSize().y*obj.getScale()*game.stage.getPPM();
			float sX = obj.getSize().x*obj.getScale()*game.stage.getPPM();
			float addSX = selected.getSize().x*selected.getScale()*game.stage.getPPM();
			float addSY = selected.getSize().y*selected.getScale()*game.stage.getPPM();

			if(normPos.x > x-sX*.5f && normPos.x < x+sX*.5f) {
				if(normPos.y > y+sY*.5) addMousePos.set(x*game.stage.getScale(), (y+sY)*game.stage.getScale());
				else if(normPos.y < y-sY*.5f) addMousePos.set(x*game.stage.getScale(), (y-sY)*game.stage.getScale());
			} else if(normPos.y < y+sY*.5f && normPos.y > y-sY*.5f) {
				if(normPos.x > x+sX*.5) addMousePos.set((x+sX)*game.stage.getScale(), y*game.stage.getScale());
				else if(normPos.x < x-sX*.5f) addMousePos.set((x-sX)*game.stage.getScale(), y*game.stage.getScale());
			}
			if(moving != null) moving.setPosition(addMousePos.scl(game.stage.getPPM()));
		}
		if(showHitboxes.isChecked()) {
			if(Gdx.graphics.getFramesPerSecond() / 10 != 0) {
				if (Gdx.graphics.getFrameId() % (int) (Gdx.graphics.getFramesPerSecond() / 10) == 0) {
					info.setText("");
					info.setPosition(10, stage.getHeight() - 10);
					info.setText("FPS: " + Gdx.graphics.getFramesPerSecond() + "\nMouse pos: " + game.stage.getMousePos().scl(game.stage.getPPM()));
					info.setText(info.getText() + "\nHeap: " + (Gdx.app.getJavaHeap() / 1000000f) + " MB");
					if (game.stage.selected == null)
						info.setText(info.getText() + "\nTouched object:\n none");
					else {
						info.setText(info.getText() + "\nTouched object:\n ID:" + game.stage.selected.getID() + "\n addr:" + game.stage.selected.getAddress()
								+ "\n x:" + game.stage.selected.getPosition().x + "\n y:" + game.stage.selected.getPosition().y +
								"\n Group Index: " + (game.stage.selected.hasHitbox() ? game.stage.selected.getHitboxBody().getFixtureList().first().getFilterData().groupIndex : "-") +
								"\n Category Bit: " + (game.stage.selected.hasHitbox() ? game.stage.selected.getHitboxBody().getFixtureList().first().getFilterData().categoryBits : "-"));
					}
					info.setText(info.getText() + "\n");
					if (game.stage.getTouchedTile() == null)
						info.setText(info.getText() + "\nTouched tile:\n none");
					else {
						info.setText(info.getText() + "\nTouched tile: " + game.stage.getTouchedTile().getX() + " " + game.stage.getTouchedTile().getY() + "\n  ID = " + game.stage.getTouchedTile().groundTile.getID());
						info.setText(info.getText() + "\n  Total objects = " + game.stage.getWorldObjects().size());
					}
				}
			}
		} else info.setText("");

		//Shortcuts to be more efficient
		if(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Keys.D)) {
			if(game.stage.selected != null && !addGraphNode) game.stage.removeObject(game.stage.selected);
			else if(addGraphNode) {
				for(com.mygdx.ai.Node n : game.stage.getGraph().getNodes()) {
					if(game.stage.getMousePos().dst(n.data.x * game.stage.getScale(), n.data.y * game.stage.getScale()) < game.stage.objectHitbox * game.stage.getScale()) {
						game.stage.getGraph().removeNode(n);
						break;
					}
				}
			}
			deselectAllWorldObjects();
		}

		if(Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isKeyJustPressed(Keys.N)) neighborPerfect.setChecked(!neighborPerfect.isChecked());

		if((Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) && Gdx.input.isKeyJustPressed(Keys.S)) {
			if(game.stage.selected != null) {
				for (RootObject r : list) {
					if (game.stage.selected.getID().equals(r.getID())) {
						tile.setChecked(true);
						setSelected(r);
					}
				}
			} else if(game.stage.selected == null && game.stage.getTouchedTile() != null) { //Take the tile
				for (RootObject r : list) {
					if (game.stage.getTouchedTile().groundTile.getID().equals(r.getID())) {
						tile.setChecked(true);
						setSelected(r);
					}
				}
			}
		}

		//Rubberband
		if(Gdx.input.isButtonPressed(Buttons.RIGHT) && !rubberbandActive && !addGraphNode) {
			rubberbandActive = true;
			rubberbandPointer = game.stage.getMousePos();
		} else if(!Gdx.input.isTouched() && rubberbandActive) {
			//Select all objects that are inside the hitbox of the rubberband
			if(rubberbandPointer.dst(game.stage.getMousePos()) > 10*game.stage.getScale()) {
				if (!Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) deselectAllWorldObjects();
				for (WorldObject w : game.stage.getRenderedObjects()) {
					if (w.getPosition().x * game.stage.getScale() > rubberbandPointer.x && w.getPosition().y * game.stage.getScale() < rubberbandPointer.y
							&& w.getPosition().x * game.stage.getScale() < game.stage.getMousePos().x && w.getPosition().y * game.stage.getScale() > game.stage.getMousePos().y) {
						selectWorldObject(w);
					}
				}
			}
			rubberbandActive = false;
			rubberbandPointer.setZero();
		}

		//Connect nodes
		if(Gdx.input.isButtonPressed(Buttons.RIGHT) && addGraphNode && Gdx.input.isTouched() && connectMount == null) {
			if(connectMount == null) {
				for (com.mygdx.ai.Node n : game.stage.getGraph().getNodes()) {
					if (game.stage.getMousePos().dst(n.data.x * game.stage.getScale(), n.data.y * game.stage.getScale()) < game.stage.objectHitbox * game.stage.getScale()) {
						connectMount = n;
						break;
					}
				}
			}
		}
		if((!Gdx.input.isTouched() || !addGraphNode) && connectMount != null) {
			for (com.mygdx.ai.Node n : game.stage.getGraph().getNodes()) {
				if (game.stage.getMousePos().dst(n.data.x * game.stage.getScale(), n.data.y* game.stage.getScale()) < game.stage.objectHitbox * game.stage.getScale() && !connectMount.equals(n)) {
					WorldValues.ConnectionData connectionData = new WorldValues.ConnectionData();
					connectionData.cost = 1;
					connectionData.i1 = connectMount.data.index;
					connectionData.i2 = n.data.index;
					game.stage.getGraph().addConnection(connectionData);
					break;
				}
			}
			connectMount = null;
		}

		//Camera movement by mouse and adding of objecs
		if(!rubberbandActive && !yOffMovin && Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.LEFT) && !onWindow && game.stage.selected == null && !Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && !nodeTouched && connectMount == null) {
			pointer.set(game.stage.getMousePos());
			camOrigin.set(Gdx.input.getX(), Gdx.input.getY());
		}
		if(!rubberbandActive && Gdx.input.isTouched() && Gdx.input.isButtonPressed(Buttons.LEFT) && !pointer.isZero() && !yOffMovin && !resizing && !nodeTouched && connectMount == null) {
			Vector2 current = new Vector2(game.stage.getMousePos());
			leftclickMenue.setVisible(false);
			game.stage.getViewport().position.set(
					game.stage.getViewport().position.x-((current.x-pointer.x)),
					game.stage.getViewport().position.y-((current.y-pointer.y)), 0);
			game.stage.getViewport().update();
			debugPos.set(game.stage.getViewport().position);
			camDist = camOrigin.dst(Gdx.input.getX(), Gdx.input.getY());
			camTime++;
		} else if(!Gdx.input.isTouched() && !pointer.isZero() && !yOffMovin && !resizing && !game.chat.isShowed()) {
			pointer.setZero();
			if(camDist < game.stage.objectHitbox*.25f && camTime < Gdx.graphics.getFramesPerSecond()*.2f) {
				if(game.stage.selected == null && activeTab == 0) {
					if(!onWindow) {
						deselectAllWorldObjects();
						if(!addGraphNode) {
							for (AddObject a : addObjectData) {
								if (a.addObjectRoot != null) {
									if (!tile.isChecked()) {
										a.addObjectValues.x = (addMousePos.x - a.offset.x) * game.stage.getPPM();
										a.addObjectValues.y = (addMousePos.y - a.offset.y) * game.stage.getPPM();
										if (randomRot.isChecked())
											a.addObjectValues.rotation = MathUtils.random(364);
										WorldObject obj = game.stage.addObject(a.addObjectValues, a.addObjectRoot);
										if (tileSize.isChecked())
											obj.setSize(new Vector2(game.stage.getTileSizeNORM(), game.stage.getTileSizeNORM()));
										if (addObjectData.size() == 1) selectWorldObject(obj);
									} else {
										if (game.stage.getTouchedTile() != null) {
											if (!game.stage.getTouchedTile().groundTile.getID().equals(a.addObjectRoot.getID())) {
												if (!Gdx.input.isKeyPressed(Keys.ALT_LEFT)) {
													game.stage.changeTile(a.addObjectRoot, game.stage.getTouchedChunk(), game.stage.touchedTileX(), game.stage.touchedTileY());
												} else {
													for (int i = 0; i < WorldValues.GroundValues.SQUARE; i++) {
														for (int j = 0; j < WorldValues.GroundValues.SQUARE; j++) {
															if (!game.stage.getTouchedChunk().ground[i][j].groundTile.getID().equals(a.addObjectRoot.getID()))
																game.stage.changeTile(a.addObjectRoot, game.stage.getTouchedChunk(), i, j);
														}
													}
												}
											}
										}
										break;
									}
								}
							}
						} else {
							WorldValues.NodeData n = new WorldValues.NodeData();
							n.x = addMousePos.x * game.stage.getPPM();
							n.y = addMousePos.y * game.stage.getPPM();
							game.stage.getGraph().addNode(n);
						}
					}
				}
			}
			camDist = 0;
			camTime = 0;
		}

		if(!leftclickMenue.isVisible() && Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.RIGHT)) {
			boolean anyTouched = false;
			for(SelectedObject s : selectedObjects) {
				if(s.object.touched(game.stage.getMousePos().scl(game.stage.getPPM()))) {
					anyTouched = true;
					break;
				}
			}
			if(anyTouched) {
				Vector2 translate = game.stage.translateTo(game.stage.getMousePos(), false, stage.getCamera());
				leftclickMenue.setPosition(translate.x, translate.y + (translate.y + leftclickMenue.getHeight() > stage.getCamera().viewportHeight ? -leftclickMenue.getHeight() : 0));
				leftclickMenue.setVisible(!leftclickMenue.isVisible());
				if(leftclickMenue.isVisible()) {
					leftclickMenue.toFront();
					if(selectedObjects.size() == 1) {
						fixed.setChecked(!selectedObjects.get(0).object.getRootValues().values.fixed);
						address.setText(selectedObjects.get(0).object.getAddress());
						fixedRot.setChecked(selectedObjects.get(0).object.getRootValues().values.fixedRotation);
						visible.setChecked(selectedObjects.get(0).object.worldObjectValues.visible);

						boolean found = false;  //Handling category bits
						for(String s : category.getItems()) {
							if(String.valueOf(selectedObjects.get(0).object.worldObjectValues.categoryBits).equals(s)) {
								categoryListener = false;
								category.setSelected(s);
								categoryListener = true;
								found = true;
								break;
							}
						}
						if(!found) {
							selectedObjects.get(0).object.worldObjectValues.categoryBits = 1;
							category.setSelected("1");
						}
						//Handling mask bits
						maskListener = false;
						radioButtons.uncheckAll();
						maskListener = true;
						for(int i = 0;  i < selectedObjects.get(0).object.worldObjectValues.maskBits.size(); i++) {
							found = false;
							for (CheckBox c : radioButtons.getButtons()) {
								if(c.getLabel().getText().toString().equals(String.valueOf(selectedObjects.get(0).object.worldObjectValues.maskBits.get(i)))) {
									maskListener = false;
									c.setChecked(true);
									maskListener = true;
									found = true;
									break;
								}
							}
							if(!found) {
								selectedObjects.get(0).object.worldObjectValues.maskBits.remove(i);
								i--;
							}
						}
						found = false;
						//Handle group index
						for(String s : groupIndex.getItems()) {
							if(s.equals(String.valueOf(selectedObjects.get(0).object.worldObjectValues.groupIndex))) {
								groupIndexListener = false;
								groupIndex.setSelected(s);
								groupIndexListener = true;
								found = true;
								break;
							}
						}
						if(!found) selectedObjects.get(0).object.setGroupIndex(0);
					}
				}
			} else leftclickMenue.setVisible(false);
		}

		//Editing Magic y
		if(!rubberbandActive && moving == null && !yOffMovin && Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
			if(Gdx.input.justTouched()) {
				for(SelectedObject s : selectedObjects) {
					s.yOff.set(s.object.getPosition());
					s.yOff.y -= s.object.getYOff();
					s.yOff.scl(game.stage.getScale());
					//Move all magic Y positions, if only one is selected
					if (s.yOff.dst(game.stage.getMousePos()) < game.stage.objectHitbox * game.stage.getScale()) {
						yOffMovin = true;
					}
				}
			}
		}
		if(Gdx.input.isTouched() && yOffMovin && Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)) {
			for(SelectedObject s : selectedObjects) {
				s.object.setYOff(s.object.getPosition().y-game.stage.getMousePos().y*game.stage.getPPM());
				s.object.getWorld().objectMoved = true;
			}
		} else if(!Gdx.input.isTouched()) {
			pointer.setZero(); //To prevent that objects get added the frame after.
			yOffMovin = false;
		}

		//Object moving and Selecting Object Selecting
		if(!rubberbandActive && !yOffMovin && Gdx.input.justTouched() && Gdx.input.isButtonPressed(Buttons.LEFT) && game.stage.selected != null  && !onWindow && activeTab == 0 && !addGraphNode) {
			moving = game.stage.selected;
			originPos = moving.getPosition();
			Vector2 off = moving.getPosition().sub(game.stage.getMousePos().scl(game.stage.getPPM()));
			offX = off.x;
			offY = off.y;
			for(SelectedObject s : selectedObjects) {
				if(!s.object.equals(moving)) s.offset.set(moving.getPosition().sub(s.object.getPosition()));
			}
		}
		if(Gdx.input.isTouched() && moving != null && activeTab == 0 && !yOffMovin) {
			if(!Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) {
				moving.setPosition(game.stage.getMousePos().scl(game.stage.getPPM()).add(offX, offY));
				boolean partOfSelected = false;
				for(SelectedObject s : selectedObjects) {
					if(s.object.equals(moving)) partOfSelected = true;
				}
				if(partOfSelected) {
					for (SelectedObject s : selectedObjects) {
						if (!s.object.equals(moving))
							s.object.setPosition(moving.getPosition().sub(s.offset));
					}
				}
				dist = moving.getPosition().dst(originPos);
				time++;
				moving.getWorld().objectMoved = true;
			}
		} else if(!Gdx.input.isTouched() && moving != null) {
			moving = null;
			if(time <= Gdx.graphics.getFramesPerSecond()*.125 && dist < game.stage.objectHitbox*.25f) {
				boolean wasSelected = false;
				if(!Gdx.input.isKeyPressed(Keys.CONTROL_LEFT)) deselectAllWorldObjects();

				if (game.stage.selected != null) {
					for (SelectedObject s : selectedObjects) {
							if (s.object.equals(game.stage.selected)) {
								deselectWorldObject(s);
								wasSelected = true;
								break;
							}
						}
					}
					if (!wasSelected) selectWorldObject(game.stage.selected);
			}
			time = 0;
			dist = 0;
		}

		//Node moving (simple)
		nodeTouched = false;
		if(Gdx.input.isButtonPressed(Buttons.LEFT) && Gdx.input.isTouched() && addGraphNode) {
			for(com.mygdx.ai.Node n : game.stage.getGraph().getNodes()) {
				if(game.stage.getMousePos().dst(n.data.x * game.stage.getScale(), n.data.y * game.stage.getScale()) < game.stage.objectHitbox * game.stage.getScale()) {
					nodeTouched = true;
					n.data.x = game.stage.getMousePos().x * game.stage.getPPM();
					n.data.y = game.stage.getMousePos().y * game.stage.getPPM();
				}
			}
		}

		//Interface drawing
		debug.render(game.stage.b2dWorld, game.stage.getViewport().combined);
		game.stage.batch.begin();
		game.stage.batch.setColor(1, 1, 1, .6f);

		boolean onResize = false; //If the mouse is on the bottom left of any object
		boolean onRotate = false; //If the mouse is on the bottom right of any object

		if(addGraphNode) {
			game.stage.batch.setColor(Color.BLUE);
			for (com.mygdx.ai.Node n : game.stage.getGraph().getNodes()) {
				game.stage.batch.draw(dot, n.data.x * game.stage.getScale() - game.stage.objectHitbox * game.stage.getScale() * .5f, n.data.y * game.stage.getScale() - game.stage.objectHitbox * game.stage.getScale() * .5f,
						game.stage.objectHitbox * game.stage.getScale(), game.stage.objectHitbox * game.stage.getScale());
			}
		}

		for(SelectedObject s : selectedObjects) {  //Draw YOFF and Dots for selected objects
			if(!showHitboxes.isChecked()) {
				game.stage.batch.setColor(1, 0, 0, 0.5f); //Draw the dots of the selected objects, If debug view is turned off
				game.stage.batch.draw(dot, s.object.getScaledX(game.stage) - game.stage.objectHitbox * .5f * game.stage.getScale(), s.object.getScaledY(game.stage) - game.stage.objectHitbox * .5f * game.stage.getScale(),
						game.stage.objectHitbox * game.stage.getScale(), game.stage.objectHitbox * game.stage.getScale());
				//
			}

			if (s.object.getYOff() != 0) {  //Draw YOFF Dot in yellow
				game.stage.batch.setColor(Color.YELLOW);
				game.stage.batch.draw(dot, s.object.getPosition().x*game.stage.getScale()-game.stage.objectHitbox*game.stage.getScale()*.5f,
						s.object.getPosition().y*game.stage.getScale()-game.stage.objectHitbox*game.stage.getScale()*.5f - s.object.getYOff() * game.stage.getScale(),
						game.stage.objectHitbox * game.stage.getScale(),
						game.stage.objectHitbox * game.stage.getScale());
			}
		}

		//Check resize
		if(selectedObjects.size() == 1) {
			 WorldObject worldObjectedit = selectedObjects.get(0).object;
			 if(!rubberbandActive && !resizing && !rotating && !yOffMovin && game.stage.getMousePos().dst(worldObjectedit.cornerBottomLeft(game.stage.getScale())) <= game.stage.objectHitbox*game.stage.getScale()) {
				game.stage.batch.draw(resizeIcon, game.stage.getMousePos().x, game.stage.getMousePos().y, game.stage.objectHitbox*game.stage.getScale()*.5f, game.stage.objectHitbox*game.stage.getScale()*.5f, game.stage.objectHitbox*game.stage.getScale(), game.stage.objectHitbox*game.stage.getScale(), 1, 1, 0);
				onResize = true;
			}
			if(!rubberbandActive && !onResize && !resizing && !yOffMovin && !rotating) { //Check Rotation
				if (game.stage.getMousePos().dst(worldObjectedit.cornerBottomRight(game.stage.getScale())) <= game.stage.objectHitbox * game.stage.getScale()) { //Resize has a higher priority
					game.stage.batch.draw(rotateIcon, game.stage.getMousePos().x, game.stage.getMousePos().y,game.stage.objectHitbox*game.stage.getScale()*.5f, game.stage.objectHitbox*game.stage.getScale()*.5f, game.stage.objectHitbox*game.stage.getScale(), game.stage.objectHitbox*game.stage.getScale(), 1, 1, 0);
					onRotate = true;
				}
			}
			//Handle resizing
			if((onResize || resizing) && !rotating && Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isButtonPressed(Buttons.LEFT)) {
				if(!resizing){
					startSize.set(worldObjectedit.getSize());
					resizeRotatePointer.set(game.stage.getMousePos());
				}
				resizing = true;
				worldObjectedit.setSize(new Vector2(
						 startSize.x + (resizeRotatePointer.x-game.stage.getMousePos().x) * game.stage.getPPM(),
						 startSize.y + (resizeRotatePointer.y-game.stage.getMousePos().y) * game.stage.getPPM()
				));
			} else if(resizing && !Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && !Gdx.input.isButtonPressed(Buttons.LEFT)) resizing = false;
			//Handle rotating
			if((onRotate || rotating) && !resizing && Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && Gdx.input.isButtonPressed(Buttons.LEFT)) {
				if(!rotating) {
					startRotation = (int) (worldObjectedit.getRotation() - game.stage.getMousePos().scl(game.stage.getPPM()).sub(worldObjectedit.getPosition()).angle());
					resizeRotatePointer.set(game.stage.getMousePos());
				}
				rotating = true;
				worldObjectedit.setRotation((int) game.stage.getMousePos().scl(game.stage.getPPM()).sub(worldObjectedit.getPosition()).angle() + startRotation);
			} else if(rotating && !Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && !Gdx.input.isButtonPressed(Buttons.LEFT)) rotating = false;
		}
		if(!rubberbandActive && game.stage.selected != null && !onWindow) {
			game.stage.batch.setColor(Color.RED);
			game.stage.batch.draw(dot, game.stage.selected.getScaledX(game.stage)-game.stage.objectHitbox*.5f*game.stage.getScale(), game.stage.selected.getScaledY(game.stage)-game.stage.objectHitbox*.5f*game.stage.getScale(),
					game.stage.objectHitbox * game.stage.getScale(), game.stage.objectHitbox * game.stage.getScale());
		}

		if(showHitboxes.isChecked()) {
			game.stage.batch.setColor(1, 0, 0, 0.4f);
			for (WorldObject obj : game.stage.getRenderedObjects()) {
				Vector2 unproj = game.stage.translateToWindow(obj.getX() * game.stage.getScale(), obj.getY() * game.stage.getScale());
				if (!onWindow(unproj.x, unproj.y)) {
					float x = obj.getPosition().x * game.stage.getScale() - game.stage.objectHitbox * game.stage.getScale() * .5f;
					float y = obj.getPosition().y * game.stage.getScale() - game.stage.objectHitbox * game.stage.getScale() * .5f;
					game.stage.batch.draw(dot, x, y,
							game.stage.objectHitbox * game.stage.getScale(), game.stage.objectHitbox * game.stage.getScale());
				}
			}
		}
		if(!rubberbandActive && Gdx.app.getType() == Application.ApplicationType.Desktop && !onResize) {
			for(AddObject a : addObjectData) {
				if (!Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && game.stage.selected == null && moving == null && activeTab == 0 && !onWindow && !game.chat.isShowed()) {
					Vector2 pos = addMousePos;
					Vector2 size = a.addObjectRoot.getSize();
					if (a.addObjectRoot.texture != null) {
						game.stage.batch.setColor(1, 1, 1, .5f);
						game.stage.batch.draw(a.addObjectRoot.texture,   //Dude this was pain...
								pos.x - size.x * .5f * game.stage.getScale() * (a.addObjectRoot.values.scale) - a.addObjectRoot.values.textureOffset.x * game.stage.getScale() - a.offset.x,
								pos.y - size.y * .5f * game.stage.getScale() * (a.addObjectRoot.values.scale) - a.addObjectRoot.values.textureOffset.y * game.stage.getScale() - a.offset.y,
								size.x * .5f * game.stage.getScale() * (a.addObjectRoot.values.scale) + a.addObjectRoot.values.textureOffset.x * game.stage.getScale(),
								size.y * .5f * game.stage.getScale() * (a.addObjectRoot.values.scale) + a.addObjectRoot.values.textureOffset.y * game.stage.getScale(),
								size.x * game.stage.getScale() * a.addObjectRoot.values.scale, size.y * game.stage.getScale() * a.addObjectRoot.values.scale,
								1, 1,
								a.addObjectValues.rotation);
					}
				}
			}
		}

		game.stage.batch.end();
		shape.begin();
		shape.setProjectionMatrix(game.stage.batch.getProjectionMatrix());
		shape.set(ShapeType.Line);

		if(rubberbandActive) {
			shape.setColor(Color.GOLD);
			shape.rect(rubberbandPointer.x, rubberbandPointer.y, game.stage.getMousePos().x - rubberbandPointer.x, game.stage.getMousePos().y - rubberbandPointer.y);
		}

		if(addGraphNode) {
			shape.setColor(Color.WHITE);
			if (connectMount != null) shape.line(connectMount.data.x * game.stage.getScale(), connectMount.data.y * game.stage.getScale(), addMousePos.x, addMousePos.y);
			for (Connection c : game.stage.getGraph().getNodeConnections()) shape.line(c.fromNode.data.x * game.stage.getScale(), c.fromNode.data.y * game.stage.getScale(), c.toNode.data.x * game.stage.getScale(), c.toNode.data.y * game.stage.getScale());
		}

		if(!onResize && game.stage.selected == null && !onWindow && !game.chat.isShowed() && !rubberbandActive && !Gdx.input.isKeyPressed(Keys.CONTROL_LEFT) && !addGraphNode) {
			for (AddObject a : addObjectData)
				drawVertices(a.addObjectRoot.values, addMousePos.x - a.offset.x, addMousePos.y - a.offset.y, shape);
		}
		if(game.stage.selected != null) {
			Vector2 p = game.stage.selected.getPosition().scl(game.stage.getScale());
			Vector2 s = game.stage.selected.getSize();
			shape.setColor(Color.GOLD);
			shape.rect(p.x-s.x*.5f*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM()-game.stage.selected.getTextureOffset().x,
					p.y-s.y*.5f*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM()-game.stage.selected.getTextureOffset().y,
					s.x*.5f*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM()+game.stage.selected.getTextureOffset().x,
					s.y*.5f*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM()+game.stage.selected.getTextureOffset().y,
					s.x*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM(),
					s.y*game.stage.getScale()*game.stage.selected.getScale()*game.stage.getPPM(),
					1,
					1, game.stage.selected.getRotation());
		}
		for(SelectedObject sel : selectedObjects) {
			Vector2 p = sel.object.getPosition().scl(game.stage.getScale());
			Vector2 s = sel.object.getSize();
			shape.setColor(Color.YELLOW);
			shape.rect(p.x-s.x*.5f*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM()-sel.object.getTextureOffset().x,
					p.y-s.y*.5f*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM()-sel.object.getTextureOffset().y,
					s.x*.5f*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM()+sel.object.getTextureOffset().x,
					s.y*.5f*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM()+sel.object.getTextureOffset().y,
					s.x*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM(),
					s.y*game.stage.getScale()*sel.object.getScale()*game.stage.getPPM(),
					1,
					1, sel.object.getRotation());

			if(sel.object.getYOff() != 0) {
				shape.setColor(Color.YELLOW);
				shape.line(sel.object.getPosition().x*game.stage.getScale(),
							sel.object.getPosition().y*game.stage.getScale(),sel.object.getPosition().x*game.stage.getScale(), sel.object.getPosition().y*game.stage.getScale()-sel.object.getYOff()*game.stage.getScale());
			}
		}

		if(showHitboxes.isChecked()) {
			shape.setColor(Color.RED);
			for (Chunk c : game.stage.loadedChunks) {
				shape.rect(c.xpos * game.stage.getScale(), c.ypos * game.stage.getScale(),
						c.width * game.stage.getScale(),
						c.height * game.stage.getScale());
			}
		}
		shape.setProjectionMatrix(stage.getViewport().getCamera().combined);
		shape.end();

		stage.act(Gdx.graphics.getDeltaTime());
		stage.act();
		stage.draw();
	}

	private void selectWorldObject(WorldObject object) {
		if(object == null) return;
		deselect.setVisible(true);
		leftclickMenue.setVisible(false);
		editTrigger.setVisible(false);
		editBehavior.setVisible(false);
		for(SelectedObject w : selectedObjects) {
			if(w.object.equals(object)) return;
		}
		selectedObjects.add(new SelectedObject(new Vector2(), object));
	}

	private void deselectWorldObject(SelectedObject selected) {
		selectedObjects.remove(selected);
		if(selectedObjects.isEmpty()) deselect.setVisible(false);
		leftclickMenue.setVisible(false);
		editTrigger.setVisible(false);
		editBehavior.setVisible(false);
		editTrigger.setVisible(false);
	}

	private void deselectAllWorldObjects() {
		deselect.setVisible(false);
		selectedObjects.clear();
		leftclickMenue.setVisible(false);
		editTrigger.setVisible(false);
		editBehavior.setVisible(false);
		editTrigger.setVisible(false);
	}

	public boolean onWindow(float x, float y) {
		if(!window.isVisible()) return false;
		Vector2 stl = window.screenToLocalCoordinates(new Vector2(x, y));
		Vector2 stl2 = editTrigger.screenToLocalCoordinates(new Vector2(x, y));
		Vector2 stl3 = leftclickMenue.screenToLocalCoordinates(new Vector2(x, y));
		Vector2 stl4 = editBehavior.screenToLocalCoordinates(new Vector2(x, y));
		Vector2 stl5 = setVar.screenToLocalCoordinates(new Vector2(x, y));
		return window.hit(stl.x, stl.y, false) != null || editTrigger.hit(stl2.x, stl2.y, false) != null || leftclickMenue.hit(stl3.x, stl3.y, false) != null
				|| editBehavior.hit(stl4.x, stl4.y, false) != null || setVar.hit(stl5.x, stl5.y,false) != null;
	}

	public void setSelected(RootObject root) {
		main.getSelection().removeAll(main.getNodes());  //First remove all possible selected items
		for(Node n : main.getNodes()) {
			Actor a = ((Table) n.getActor()).findActor("holder");
			if(a instanceof ObjectHolder) {
				if(((ObjectHolder) a).getObject() instanceof RootObject) {
					if(((RootObject) ((ObjectHolder) a).getObject()).getID().equals(root.getID())) {
						main.getSelection().add(n);
						selected = root;
						return;
					}
				}
			}
		}
	}

	public void addObject(RootObject object) {
		Table prev = new Table(skin);
		ObjectHolder<RootObject> holder = new ObjectHolder<>(object);
		holder.setName("holder");
		prev.add(holder);

		Label l = new Label(object.getID(), skin);
		l.setSize(main.getWidth() - 50, l.getGlyphLayout().height);

		Image image = new Image();
		if(object.texture != null) image.setDrawable(new TextureRegionDrawable(object.texture));
		image.setAlign(Align.right);
		image.setScaling(Scaling.fit);
		prev.add(image).width(20).height(20).padRight(5);
		prev.add(l);
		Node n = new Node(prev);
		main.add(n);
	}

	//public WorldObject worldObjectedit = null;
	public class SelectedObject {
		Vector2 offset;
		Vector2 yOff = new Vector2();
		public WorldObject object;

		SelectedObject(Vector2 offset, WorldObject object) {
			this.offset = offset;
			this.object = object;
		}
	}

	class AddObject {
		WorldObjectValues addObjectValues = null;
		RootObject addObjectRoot = null;
		Vector2 offset;

		AddObject(WorldObjectValues addObjectValues, RootObject addObjectRoot, Vector2 offset) {
			this.addObjectValues = addObjectValues;
			this.addObjectRoot = addObjectRoot;
			this.offset = offset;
		}
	}

	public ArrayList<SelectedObject> selectedObjects = new ArrayList<>();
	private ArrayList<AddObject> addObjectData = new ArrayList<>();
	private TextButton deselect;
	private CheckBox tileSize;
	private CheckBox tile;
	private CheckBox tilePosition;
	private CheckBox showHitboxes;
	private String script;
	private Tree keys;
	private Node base;
	private CheckBox randomRot;
	private ArrayList<RootObject> preloaded;
	private CheckBox neighborPerfect;
	private RootObject selected;
	private ArrayList<RootObject> list;
	private TextButton loadObject;
	private TextButton editScript;

	private Table initTab1(float initWidth, float initHeight) {
		Table table = new Table(skin);
		table.setSize(initWidth / 2, initHeight);
		table.align(Align.topLeft);
		loadObject = new TextButton("Load", skin);
		table.add(loadObject).colspan(2).fillX();

		table.row();
		deselect = new TextButton("Deselect", skin);
		deselect.setVisible(false);
		table.add(deselect).align(Align.left);
		table.row();

		tile = new CheckBox("Is tile", skin);
		table.add(tile).colspan(2).align(Align.left);
		table.row();

		tileSize = new CheckBox("Tile size", skin);
		table.add(tileSize).colspan(2).align(Align.left);
		table.row();

		tilePosition = new CheckBox("Tile Position", skin);
		table.add(tilePosition).colspan(2).align(Align.left);
		table.row();

		randomRot = new CheckBox("Random Rotation", skin);
		table.add(randomRot).colspan(2).align(Align.left);
		table.row();

		neighborPerfect = new CheckBox("Aligned", skin);
		table.add(neighborPerfect).colspan(2).align(Align.left);
		table.row();

		showHitboxes = new CheckBox("Show Debug", skin);
		showHitboxes.setChecked(false);
		table.add(showHitboxes).colspan(2).align(Align.left).padBottom(5);
		table.row();

		loadObject.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(Gdx.app.getType() != Application.ApplicationType.Desktop) {
					info.setText("This feature is disabled on " + Gdx.app.getType());
					return;
				}
				new Thread(new Runnable() {
				    @Override
				    public void run() {
						JFileChooser chooser = new JFileChooser();

						FileFilter filter = new FileFilter() {
							public String getDescription() {
								return "Objects";
							}

							public boolean accept(File f) {
								return f.getName().endsWith(".json") || f.isDirectory();
							}
						};
						chooser.setFileFilter(filter);
						chooser.setCurrentDirectory(Gdx.files.local("/").file());
						int res = chooser.showDialog(null, "Open Object or directrory");
						if (res == JFileChooser.APPROVE_OPTION) {
							try {
								JsonHandler jh = new JsonHandler();
								RootObjectValues values = jh.readJSON(chooser.getSelectedFile().getAbsolutePath(), RootObjectValues.class);
								RootObject r = new RootObject(values);

								Gdx.app.postRunnable(new Runnable() {
									public void run() {
										r.loadTexture();
									}
								});
								addObject(r);
							} catch(Exception e) {
								Logger.logError("DebugWindow", "Error while loading object: " + e.toString());
								e.printStackTrace();
							}
						}
					}
				}).start();
			}
		});

		deselect.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(game.stage != null) deselectAllWorldObjects();
			}
		});
		intitEditTriggerWindow();
		initEditBehaviorWindow();
		return table;
	}

	private Window editTrigger;
	private TextArea triggerScript;
	private Label compileInfo;
	private TextField messageText;
	private CheckBox checkTiles;

	private void intitEditTriggerWindow() {
		editTrigger = new Window("Edit trigger", skin);
		editTrigger.setResizable(true);
		editTrigger.align(Align.topLeft);
		editTrigger.setKeepWithinStage(false);
		TextButton close = new TextButton("X", skin);
		close.setColor(Color.RED);
		close.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(triggerScript.getText().isEmpty()) {
					editTrigger.setVisible(false);
					for(SelectedObject s : selectedObjects) {
						s.object.removeTrigger();
					}
					return;
				}

				script = triggerScript.getText();
				compileInfo.setText("");

				RootObjectValues changed = new RootObjectValues();
				changed.trigger = new TriggerValues();
				changed.trigger.scr = script;
				changed.trigger.tileCheck = checkTiles.isChecked();
				changed.trigger.messageText = messageText.getText();

				for(Node n : base.getChildren()) {
					Label l = ((Label) (n.getActor()));
					if(Keys.valueOf(l.getText().toString()) != -1) {
						int key = Keys.valueOf(((Label) (n.getActor())).getText().toString());
						changed.trigger.keys.add(key);
					} else changed.trigger.keys.add(Integer.valueOf(l.getText().toString()));
				}
				for(SelectedObject s : selectedObjects) s.object.setTrigger(changed.trigger);
				compileInfo.setText("Saved");
				editTrigger.setVisible(false);
			}});
		editTrigger.getTitleTable().add(close).align(Align.right).height(15).padRight(5);

		triggerScript = new TextArea("", skin, "script");
		triggerScript.setCursorPosition(0);
		triggerScript.setFocusTraversal(false);
		triggerScript.addListener(new InputListener() {
			@Override
			public boolean keyDown(InputEvent event, int keycode) {
				if(keycode == Keys.TAB) {
					int prevCursor = triggerScript.getCursorPosition();
					triggerScript.setText(triggerScript.getText().substring(0, triggerScript.getCursorPosition()) + "   " + triggerScript.getText().substring(triggerScript.getCursorPosition()));
					triggerScript.setCursorPosition(prevCursor + 3);
				}
				return true;
			}
		});
		editTrigger.add(triggerScript).colspan(2).fillX().align(Align.topLeft).expandY().fillY().padTop(5).height(200);
		editTrigger.row();
		editTrigger.setVisible(false);
		compileInfo = new Label("", skin);
		compileInfo.setAlignment(Align.topLeft);
		ScrollPane p = new ScrollPane(compileInfo);
		editTrigger.add(p).colspan(2).height(40).align(Align.left).row();

		editTrigger.add("Message Text:");
		messageText = new TextField("", skin);
		editTrigger.add(messageText).fillX().align(Align.left).row();

		checkTiles = new CheckBox("Join Trigger", skin);
		checkTiles.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					if(s.object.getTrigger() != null) {
						s.object.getTrigger().values.tileCheck = checkTiles.isChecked();
					}
				}
			}});
		editTrigger.add(checkTiles).colspan(2).align(Align.left).row();

		keys = new Tree(skin);
		base = new Node(new Label("Added keys", skin));
		keys.add(base);
		TextButton addkey = new TextButton("Add Key", skin);
		editTrigger.add(addkey).align(Align.center);

		TextButton delnode = new TextButton("Delete Key", skin);
		editTrigger.add(delnode).align(Align.center).padBottom(5);
		editTrigger.row();
		editTrigger.add(keys).align(Align.left).colspan(2);

		addkey.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				if(String.valueOf(addkey.getText()).contains("Add")) {
					addkey.setText("Press/Cancel");
					stage.unfocusAll();
				} else addkey.setText("Add Key");
			}
		});
		stage.addListener(new InputListener() {
			public boolean keyDown(InputEvent event, int keycode) {
				if(String.valueOf(addkey.getText()).contains("Press/Cancel")) {
					stage.unfocusAll();
					addkey.setText("Add Key");
					for(Node n : base.getChildren()) {
						if(((Label) (n.getActor())).getText().toString().contains(Keys.toString(keycode))) return false;
					}
					base.add(new Node(new Label(Keys.toString(keycode), skin)));
				}
				return super.keyDown(event, keycode);
			}
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if(String.valueOf(addkey.getText()).contains("Press/Cancel")) {
					stage.unfocusAll();
					addkey.setText("Add Key");
					for(Node n : base.getChildren()) {
						if(((Label) (n.getActor())).getText().toString().contains(-button-1+"")) return false;
					}
					base.add(new Node(new Label(-button-1+"", skin)));
				}
				return super.touchDown(event, x, y, pointer, button);
			}

			@Override
			public boolean scrolled(InputEvent event, float x, float y, int amount) {
				if(stage.getKeyboardFocus() == null) return false;
				if(stage.getKeyboardFocus().equals(triggerScript)) {
					triggerScript.moveCursorLine(triggerScript.getCursorLine() + amount);
				}
				return true;
			}
		});
		delnode.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(Node n : keys.getSelection()) {
					if(!n.equals(base)) keys.remove(n);
				}
			}
		});
		stage.addActor(editTrigger);
		editTrigger.row();

		TextButton interrupt = new TextButton("Interrupt", skin);
		interrupt.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					if (s.object.getTrigger() != null) {
						s.object.getTrigger().process.kill(s.object.getTrigger().process.getMain(), "Interrupted by command");
						compileInfo.setText("Interrupted");
					}
				}
			}});
		editTrigger.add(interrupt).align(Align.left).pad(5).row();

		editTrigger.addListener(new EventListener() {
			public boolean handle(Event event) {
				editTrigger.getCell(triggerScript).width(editTrigger.getWidth()-10);
				editTrigger.getCell(triggerScript).height(editTrigger.getCell(triggerScript).getMaxHeight());
				return true;
			}
		});
		editTrigger.setSize(editTrigger.getPrefWidth(), editTrigger.getPrefHeight());
	}

	private Window editBehavior;
	private Window setVar;
	private TextField valueInput;
	private Behavior selectedBehavior;
	private Field setField;
	private Label selectedValue;
	SelectBox<String> classes;
	private Table tabs;
	private TextField id;

	private void initEditBehaviorWindow() {
		editBehavior = new Window("EDIT BEHAVIOR", skin);
		editBehavior.setVisible(false);
		stage.addActor(editBehavior);
		tabs = new Table(skin);

		setVar = new Window("SET VARIABLE", skin);
		setVar.addAction(new Action() {
			public boolean act(float delta) {
				if(!editBehavior.isVisible()) {
					setVar.setVisible(false);
					valueInput.setText("");
				}
				return false;
			}
		});
		valueInput = new TextField("", skin);
		setVar.add(valueInput).colspan(2).row();
		TextButton cancel = new TextButton("Close", skin);
		setVar.add(cancel).align(Align.center).padTop(20);
		cancel.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				valueInput.setText("");
				setVar.setVisible(false);
			}
		});
		TextButton set = new TextButton("Set", skin);
		set.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				try {
					if(setField.getType().toString().equals("float")) {
						if(Tools.Convert.testForFloat(valueInput.getText())) Behavior.setVariable(selectedBehavior, setField.getName(), Float.valueOf(valueInput.getText()));
						else return;
					} else if(setField.getType().toString().equals("int")) {
						if(Tools.Convert.testForInteger(valueInput.getText())) Behavior.setVariable(selectedBehavior, setField.getName(), Integer.valueOf(valueInput.getText()));
						else return;
					} else if(setField.getType().toString().equals("boolean")) {
						if(Tools.Convert.testForBoolean(valueInput.getText())) Behavior.setVariable(selectedBehavior, setField.getName(), Boolean.valueOf(valueInput.getText()));
						else return;
					} else Behavior.setVariable(selectedBehavior, setField.getName(), valueInput.getText());
						selectedValue.setText(valueInput.getText());
					valueInput.setText("");
				} catch (Exception e) {
					e.printStackTrace();
				}
				setVar.setVisible(false);
			}
		});
		setVar.add(set).align(Align.center).padTop(20);
		stage.addActor(setVar);

		id = new TextField("", skin);
		id.setMaxLength(10);
		classes = new SelectBox<>(skin);
		Array<String> a = new Array<>();
		for (Class<?> c : Behavior.BEHAVIOR_CLASSES) {
			a.add(c.getName());
		}
		if(!classes.getItems().isEmpty()) id.setText(classes.getItems().get(0));
		classes.setItems(a);
		classes.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				id.setText(Behavior.BEHAVIOR_CLASSES.get(classes.getSelectedIndex()).getSimpleName());
			}
		});

		TextButton close = new TextButton("X", skin);
		close.addListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				editBehavior.setVisible(false);
			}
		});
		close.toFront();
		editBehavior.getTitleTable().add(close).padRight(5).height(15).align(Align.right).getActor().setColor(Color.RED);
	}

	public void setBehaviorTabs(WorldObject object) {
		Vector2 prevPos = new Vector2(editBehavior.getX(), editBehavior.getY());
		if (editBehavior != null) editBehavior.remove();
		editBehavior = new Window("EDIT BEHAVIOR", skin);
		stage.addActor(editBehavior);
		tabs.clearChildren();
		editBehavior.add("ID:").align(Align.left).padRight(5);
		editBehavior.add(id).align(Align.left).width(100).padRight(5);
		editBehavior.add("Classpath:").align(Align.left).padRight(5);
		//TextField classPath = new TextField("", skin);
		//editBehavior.add(classPath).width(200).align(Align.left).fillX();

		editBehavior.add(classes).width(280).align(Align.left).fillX();
		TextButton add = new TextButton("Add", skin);
		add.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				for(SelectedObject s : selectedObjects) {
					String selected = classes.getSelected();
					if (!selected.isEmpty() && !id.getText().isEmpty()) {
						WorldValues.BehaviorValues values = new WorldValues.BehaviorValues();
						values.classPath = selected.trim();
						values.id = id.getText().trim();

						try {
							s.object.loadBehavior(values);
						} catch (Exception e) {
							e.printStackTrace();
						}
						setBehaviorTabs(s.object);
					} else if (s.object == null) editBehavior.setVisible(false);
					if(selectedObjects.size() != 1) editBehavior.setVisible(false);
				}
			}
		});
		editBehavior.add(add).height(25).row();
		editBehavior.setSize(editBehavior.getPrefWidth(), editBehavior.getPrefHeight());
		editBehavior.align(Align.topLeft);
		editBehavior.setResizable(true);
		editBehavior.setKeepWithinStage(false);
		if(object.getBehavior().isEmpty()) return;

		for(Behavior b : object.getBehavior()) {
			tabs.add(b.getID()).width(100).align(Align.left);
			tabs.add(b.getClassPath());
			TextButton remove = new TextButton("Remove", skin);
			remove.addListener(new ChangeListener() {
				public void changed(ChangeEvent event, Actor actor) {
					for(SelectedObject s : selectedObjects) {
						s.object.removeBehavior(b.getID());
						if(selectedObjects.size() == 1) setBehaviorTabs(s.object);
						else {
							editBehavior.setVisible(false);
							break;
						}
					}
				}
			});
			tabs.add(remove).colspan(2).align(Align.right);
			tabs.row().padBottom(5);

			try {
				Field[] validFields = Behavior.getFields(b);
				for (Field f : validFields) {
					tabs.add(f.getName() + " = ").align(Align.right);
					Label varValue = new Label(f.get(b).toString(), skin);
					tabs.add(varValue).align(Align.left).maxHeight(100).getActor().setWrap(true);
					TextButton set = new TextButton("change", skin);
					set.addListener(new ChangeListener() {
						@Override
						public void changed(ChangeEvent event, Actor actor) {
							setVar.setPosition(stage.getWidth() / 2f - setVar.getWidth() / 2f, stage.getHeight() / 2f - setVar.getHeight()/2f);
							setVar.toFront();
							selectedBehavior = b;
							setField = f;
							selectedValue = varValue;
							try {
								valueInput.setText(f.get(b).toString());
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							}
							setVar.getTitleLabel().setText("SET VAR (" + f.getType().getSimpleName() + ")");
							stage.setKeyboardFocus(valueInput);
							setVar.setVisible(true);
						}
					});
					tabs.add(set).height(25);
					tabs.row();
				}
			} catch (IllegalAccessException e) {
				tabs.add("Failed to fetch Variables :(").colspan(4);
			}
		}

		ScrollPane pane = new ScrollPane(tabs);
		editBehavior.add(pane).colspan(4).width(editBehavior.getPrefWidth()).height(100).align(Align.top);
		editBehavior.setWidth(editBehavior.getPrefWidth());
		editBehavior.setHeight(pane.getHeight());
		editBehavior.setPosition(prevPos.x, prevPos.y);
	}

	private Tree main;
	private boolean addGraphNode = false;
	private Table initTab12(float initWidth, float initHeight) {
		Table table = new Table();

		main = new Tree(skin);
		list = new ArrayList<>();
		for(RootObject r : preloaded) {
			r.loadTexture();
			list.add(r);
		}

		for(RootObject obj : preloaded)	addObject(obj);
		main.addCaptureListener(new ChangeListener() {
			public void changed(ChangeEvent event, Actor actor) {
				addGraphNode = false;
				if(!main.getSelection().isEmpty()) {
					deselect.toggle();
					addObjectData.clear();

					if(main.getSelection().first().getActor() instanceof Label) {
						addGraphNode = true;
						return;
					} else {
						Actor a = ((Table) main.getSelection().first().getActor()).findActor("holder");
						if (a instanceof ObjectHolder) {
							if (((ObjectHolder) a).getObject() instanceof RootObject)
								selected = (RootObject) ((ObjectHolder) a).getObject();
							else return;  //Selected stays null
						} else return;
					}

					addObjectData.add(new AddObject(new WorldObjectValues(), selected, new Vector2()));

					selected.loadTexture(); //Just to make sure
					if(selected.getSize().x == -1 && selected.getSize().y == -1) selected.setSize(new Vector2(game.stage.getTileSizeNORM(), game.stage.getTileSizeNORM()));
				} else {
					deselectAllWorldObjects();
					addObjectData.clear();
				}
				if(main.getSelection().size() > 1) {
					Node selected = main.getSelection().first();
					main.getSelection().removeAll(main.getNodes());
					main.getSelection().add(selected);
				}
			}
		});
		Node n = new Node(new Label("GraphNode", skin));
		main.add(n);
		table.add(main);
		return table;
	}

	InputProcessor prev;
	public void  setDebug(boolean debug) {
		debugMode = debug;
		this.window.setVisible(debug);

		if(debug) {
			prev = Gdx.input.getInputProcessor();
			Gdx.input.setInputProcessor(stage);
			game.stage.getWorldData().viewport.center = false;
			viewportZoom = game.stage.getViewport().zoom;
			viewportPos = game.stage.getViewport().position;
			//window.setHeight(stage.getHeight());
			window.setPosition(stage.getWidth() - window.getWidth(), stage.getHeight() - window.getHeight());
		} else {
			game.stage.getWorldData().viewport.center = true;
			stage.unfocusAll();
			Gdx.input.setInputProcessor(prev);
			debugPos.set(game.stage.getViewport().position);
			debugZoom = game.stage.getViewport().zoom;
			game.stage.getViewport().position.set(viewportPos);
			game.stage.getViewport().zoom = viewportZoom;
		}
	}

	@Override
	public void dispose() {
		if(game.stage != null) game.stage.dispose();
	}
}
