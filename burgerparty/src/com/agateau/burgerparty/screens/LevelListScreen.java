package com.agateau.burgerparty.screens;

import com.agateau.burgerparty.BurgerPartyGame;
import com.agateau.burgerparty.model.LevelWorld;
import com.agateau.burgerparty.utils.Anchor;
import com.agateau.burgerparty.utils.AnchorGroup;
import com.agateau.burgerparty.utils.GridGroup;
import com.agateau.burgerparty.utils.TiledImage;
import com.agateau.burgerparty.utils.UiUtils;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;

public class LevelListScreen extends BaseScreen {
	public LevelListScreen(BurgerPartyGame game, TextureAtlas atlas, Skin skin) {
		super(game, skin);
		TiledImage bgImage = new TiledImage(atlas.findRegion("ui/menu-bg"));
		setBackgroundActor(bgImage);

		mStarOff = atlas.findRegion("ui/star-off");
		mStarOn = atlas.findRegion("ui/star-on");
		mLock = atlas.findRegion("ui/lock");
		setupWidgets(skin);
	}

	private void setupWidgets(Skin skin) {
		mAnchorGroup.setSpacing(UiUtils.SPACING);
		getStage().addActor(mAnchorGroup);
		mAnchorGroup.setFillParent(true);

		TextButton backButton = new TextButton("<- Back", skin);
		backButton.setSize(backButton.getPrefWidth(), UiUtils.BUTTON_HEIGHT);
		mAnchorGroup.addRule(backButton, Anchor.BOTTOM_LEFT, mAnchorGroup, Anchor.BOTTOM_LEFT, 1, 1);
		backButton.addListener(new ChangeListener() {
			public void changed(ChangeListener.ChangeEvent Event, Actor actor) {
				getGame().showMenu();
			}
		});

		mPreviousButton = new TextButton("<", skin);
		mPreviousButton.setSize(UiUtils.BUTTON_HEIGHT, UiUtils.BUTTON_HEIGHT);
		mPreviousButton.addListener(new ChangeListener() {
			public void changed(ChangeListener.ChangeEvent Event, Actor actor) {
				scrollTo(mGroupIndex - 1);
			}
		});

		mNextButton = new TextButton(">", skin);
		mNextButton.setSize(UiUtils.BUTTON_HEIGHT, UiUtils.BUTTON_HEIGHT);
		mNextButton.addListener(new ChangeListener() {
			public void changed(ChangeListener.ChangeEvent Event, Actor actor) {
				scrollTo(mGroupIndex + 1);
			}
		});

		for (int levelWorldIndex = 0; levelWorldIndex < getGame().getLevelWorldCount(); ++levelWorldIndex) {
			GridGroup gridGroup = createLevelButtonGridGroup(levelWorldIndex, skin);
			gridGroup.setVisible(false);
			mAnchorGroup.addActor(gridGroup);
			mGridGroups.add(gridGroup);
		}

		// Add buttons after creating the grids so that buttons are above grids
		mAnchorGroup.addRule(mPreviousButton, Anchor.CENTER_LEFT, mAnchorGroup, Anchor.CENTER_LEFT);
		mAnchorGroup.addRule(mNextButton, Anchor.CENTER_RIGHT, mAnchorGroup, Anchor.CENTER_RIGHT);

		scrollTo(0);
	}

	private GridGroup createLevelButtonGridGroup(int levelWorldIndex, Skin skin) {
		GridGroup gridGroup = new GridGroup();
		gridGroup.setSpacing(UiUtils.SPACING);
		gridGroup.setColumnCount(COL_COUNT);
		gridGroup.setCellSize(150, 150);

		LevelWorld levelWorld = getGame().getLevelWorld(levelWorldIndex);
		for (int idx=0; idx < levelWorld.getLevelCount(); idx++) {
			Actor levelButton = createLevelButton(levelWorldIndex, idx, skin);
			gridGroup.addActor(levelButton);
		}
		return gridGroup;
	}

	class LevelButton extends TextButton {
		public LevelButton(int levelWorldIndex, int levelIndex, int score, Skin skin) {
			super("", skin);
			this.levelWorldIndex = levelWorldIndex;
			this.levelIndex = levelIndex;

			AnchorGroup group = new AnchorGroup();
			addActor(group);
			group.setFillParent(true);
			group.setSpacing(6);

			if (score >= 0) {
				setText(String.valueOf(levelWorldIndex + 1) + "-" + String.valueOf(levelIndex + 1));
				Table table = new Table();
				for (int x = 1; x <= 3; ++x) {
					Image image = new Image(x > score ? mStarOff : mStarOn);
					table.add(image);
				}
				group.addRule(table, Anchor.BOTTOM_CENTER, group, Anchor.BOTTOM_CENTER, 0, 2);
				table.setSize(mStarOff.getRegionWidth() * 3, mStarOff.getRegionHeight());
			} else {
				setDisabled(true);
				setColor(1f, 1f, 1f, 0.5f);
				Image image = new Image(mLock);
				group.addRule(image, Anchor.CENTER, group, Anchor.CENTER);
			}
		}

		public int levelWorldIndex;
		public int levelIndex;
	}

	private Actor createLevelButton(int levelWorldIndex, int levelIndex, Skin skin) {
		LevelWorld world = getGame().getLevelWorld(levelWorldIndex);
		int score = world.getLevel(levelIndex).stars;
		LevelButton button = new LevelButton(levelWorldIndex, levelIndex, score, skin);
		button.addListener(new ChangeListener() {
			public void changed(ChangeListener.ChangeEvent Event, Actor actor) {
				LevelButton button = (LevelButton)actor;
				getGame().startLevel(button.levelWorldIndex, button.levelIndex);
			}
		});

		return button;
	}

	private void scrollTo(int index) {
		assert(index >= 0);
		assert(index < getGame().getLevelWorldCount());

		GridGroup newGroup = mGridGroups.get(index);
		newGroup.setVisible(true);
		int oldIndex = mGroupIndex;
		mGroupIndex = index;
		if (oldIndex >= 0) {
			float deltaX = getStage().getWidth() * (mGroupIndex < oldIndex ? 1 : -1);

			GridGroup oldGroup = mGridGroups.get(oldIndex);
			mAnchorGroup.removeRulesForActor(oldGroup);
			oldGroup.addAction(Actions.moveBy(deltaX, 0, ANIMATION_DURATION, Interpolation.sineIn));

			newGroup.setPosition(oldGroup.getX() - deltaX, oldGroup.getY());
			newGroup.addAction(
				Actions.sequence(
					Actions.moveBy(deltaX, 0, ANIMATION_DURATION, Interpolation.sineIn),
					Actions.run(new Runnable() {
						@Override
						public void run() {
							setCurrentGridGroupAnchorRule();
						}
					})
				)
			);
		} else {
			setCurrentGridGroupAnchorRule();
		}
		updateButtons();
	}

	private void setCurrentGridGroupAnchorRule() {
		mAnchorGroup.addRule(mGridGroups.get(mGroupIndex), Anchor.TOP_CENTER, mAnchorGroup, Anchor.TOP_CENTER, 0, -1);
	}

	private void updateButtons() {
		mPreviousButton.setVisible(mGroupIndex > 0);
		mNextButton.setVisible(mGroupIndex < getGame().getLevelWorldCount() - 1);
	}

	static private float ANIMATION_DURATION = 0.4f;
	static private int COL_COUNT = 3;
	private TextureRegion mStarOff;
	private TextureRegion mStarOn;
	private TextureRegion mLock;

	private AnchorGroup mAnchorGroup = new AnchorGroup();
	private Array<GridGroup> mGridGroups = new Array<GridGroup>();

	private TextButton mPreviousButton;
	private TextButton mNextButton;

	private int mGroupIndex = -1;
}
