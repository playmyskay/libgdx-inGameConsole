/**
 * Copyright 2015 StrongJoshua (strongjoshua@hotmail.com)
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.strongjoshua.console.gui;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Array;
import com.strongjoshua.console.AbstractConsole;
import com.strongjoshua.console.Console;
import com.strongjoshua.console.gui.ConsoleDisplay.ConsoleSettings;
import com.strongjoshua.console.log.LogLevel;

/**
 * A simple console that allows live logging, and live execution of methods,
 * from within an application. Please see the
 * <a href="https://github.com/StrongJoshua/libgdx-inGameConsole">GitHub
 * Repository</a> for more information.
 *
 * @author StrongJoshua
 */
public class GUIConsole extends AbstractConsole {

	private int keyID;

	private ConsoleDisplay display;
	private boolean visibile = true;
	private boolean focus = false;
	private boolean handleFocus = false;
	private boolean captured = false;
	private boolean usesMultiplexer = false;
	private InputProcessor appInput;
	private InputMultiplexer multiplexer;
	private Stage stage;
	private Window consoleWindow;
	private final ReadWriteLock rwl = new ReentrantReadWriteLock();
	private boolean refreshFlag = true;

	private float foregroundTransparency = 1f;
	private float backgroundTransparency = 1f;

	/**
	 * Creates the console.<br>
	 * <b>***IMPORTANT***</b> Call {@link Console#dispose()} to make your
	 * {@link InputProcessor} the default processor again (this console uses a
	 * multiplexer to circumvent it).
	 *
	 * @param skin           Uses skins for Label, TextField, and Table. Skin
	 *                       <b>must</b> contain a font called 'default-font'.
	 * @param useMultiplexer If internal multiplexer should be used
	 * @param keyID          Sets the key used to open/close the console
	 * @see Console#dispose()
	 */
	public GUIConsole(ConsoleSettings settings) {
		this.keyID = settings.getKeyID();
		stage = new Stage();
		display = new ConsoleDisplay(stage, log, this, settings);
		logToSystem = false;

		usesMultiplexer = settings.isUseMultiplexer();
		if (usesMultiplexer) {
			resetInputProcessing();
		}

		// display.pad(4);
		// display.padTop(22);
		display.setFillParent(true);

		consoleWindow = new Dialog("Console", settings.getSkin());
		consoleWindow.setModal(false);
		consoleWindow.setMovable(true);
		consoleWindow.setResizable(true);
		consoleWindow.setKeepWithinStage(true);
		consoleWindow.addActor(display);
		consoleWindow.setTouchable(Touchable.enabled);
		stage.addListener(new InputListener() {

			@Override
			public boolean mouseMoved(InputEvent event, float x, float y) {
				Vector2 consoleWindowStageCoords = new Vector2();
				consoleWindow.localToStageCoordinates(consoleWindowStageCoords);
				float x1 = consoleWindowStageCoords.x;
				float y1 = consoleWindowStageCoords.y;
				float x2 = x1 + consoleWindow.getWidth();
				float y2 = y1 + consoleWindow.getHeight();
				if (x >= x1 && x <= x2 && y >= y1 && y <= y2) {
					if (!captured) {
						captured = true;
						updateTransparency();
					}

					display.updateLabelBackground();
				} else {
					if (captured) {
						captured = false;
						updateTransparency();
					}
				}

				return false;
			}
		});

		stage.addListener(new DisplayListener(display));
		stage.addActor(consoleWindow);
		stage.setKeyboardFocus(display);

		setSizePercent(50, 50);
		setPositionPercent(50, 50);
		updateTransparency();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setMaxEntries(int)
	 */
	@Override
	public void setMaxEntries(int numEntries) {
		if (numEntries > 0 || numEntries == UNLIMITED_ENTRIES) {
			log.setMaxEntries(numEntries);
		} else {
			throw new IllegalArgumentException(
					"Maximum entries must be greater than 0 or use Console.UNLIMITED_ENTRIES.");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#clear()
	 */
	@Override
	public void clear() {
		rwl.writeLock().lock();
		try {
			log.getLogEntries().clear();
			refreshFlag = true;
		} finally {
			rwl.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setSize(int, int)
	 */
	@Override
	public void setSize(int width, int height) {
		if (width <= 0 || height <= 0) {
			throw new IllegalArgumentException("Pixel size must be greater than 0.");
		}
		consoleWindow.setSize(width, height);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setSizePercent(float, float)
	 */
	@Override
	public void setSizePercent(float wPct, float hPct) {
		if (wPct <= 0 || hPct <= 0) {
			throw new IllegalArgumentException("Size percentage must be greater than 0.");
		}
		if (wPct > 100 || hPct > 100) {
			throw new IllegalArgumentException("Size percentage cannot be greater than 100.");
		}
		float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
		consoleWindow.setSize(w * wPct / 100.0f, h * hPct / 100.0f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setPosition(int, int)
	 */
	@Override
	public void setPosition(int x, int y) {
		consoleWindow.setPosition(x, y);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setPositionPercent(float, float)
	 */
	@Override
	public void setPositionPercent(float xPosPct, float yPosPct) {
		if (xPosPct > 100 || yPosPct > 100) {
			throw new IllegalArgumentException(
					"Error: The console would be drawn outside of the screen.");
		}
		float w = Gdx.graphics.getWidth(), h = Gdx.graphics.getHeight();
		consoleWindow.setPosition(w * xPosPct / 100.0f, h * yPosPct / 100.0f);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#resetInputProcessing()
	 */
	@Override
	public void resetInputProcessing() {
		usesMultiplexer = true;
		appInput = Gdx.input.getInputProcessor();
		if (appInput != null) {
			if (hasStage(appInput)) {
				log("Console already added to input processor!", LogLevel.ERROR);
				Gdx.app.log("Console", "Already added to input processor!");
				return;
			}
			multiplexer = new InputMultiplexer();
			multiplexer.addProcessor(stage);
			multiplexer.addProcessor(appInput);
			Gdx.input.setInputProcessor(multiplexer);
		} else {
			Gdx.input.setInputProcessor(stage);
		}
	}

	/**
	 * Compares the given processor to the console's stage. If given a
	 * multiplexer, it is iterated through recursively to check all of the
	 * multiplexer's processors for comparison.
	 *
	 * @param processor
	 * @return processor == this.stage
	 */
	private boolean hasStage(InputProcessor processor) {
		if (!(processor instanceof InputMultiplexer)) {
			return processor == stage;
		}
		InputMultiplexer im = (InputMultiplexer) processor;
		Array<InputProcessor> ips = im.getProcessors();
		for (InputProcessor ip : ips) {
			if (hasStage(ip)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#getInputProcessor()
	 */
	@Override
	public InputProcessor getInputProcessor() {
		return stage;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#draw()
	 */
	@Override
	public void draw() {
		if (disabled) {
			return;
		}
		stage.act();

		if (!visibile) {
			return;
		}
		if (refreshFlag) {
			if (rwl.writeLock().tryLock()) {
				try {
					display.refresh();
					refreshFlag = false;
				} finally {
					rwl.writeLock().unlock();
				}
			}
		}
		stage.draw();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#refresh()
	 */
	@Override
	public void refresh() {
		this.refresh(true);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#refresh(boolean)
	 */
	@Override
	public void refresh(boolean retain) {
		float oldWPct = 0, oldHPct = 0, oldXPosPct = 0, oldYPosPct = 0;
		if (retain) {
			oldWPct = consoleWindow.getWidth() / stage.getWidth() * 100;
			oldHPct = consoleWindow.getHeight() / stage.getHeight() * 100;
			oldXPosPct = consoleWindow.getX() / stage.getWidth() * 100;
			oldYPosPct = consoleWindow.getY() / stage.getHeight() * 100;
		}
		int width = Gdx.graphics.getWidth(), height = Gdx.graphics.getHeight();
		stage.getViewport().setWorldSize(width, height);
		stage.getViewport().update(width, height, true);
		if (retain) {
			this.setSizePercent(oldWPct, oldHPct);
			this.setPositionPercent(oldXPosPct, oldYPosPct);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#log(java.lang.String, com.strongjoshua.console.GUIConsole.LogLevel)
	 */
	@Override
	public void log(String msg, LogLevel level) {
		rwl.writeLock().lock();
		try {
			super.log(msg, level);
			refreshFlag = true;
		} finally {
			rwl.writeLock().unlock();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setDisabled(boolean)
	 */
	@Override
	public void setDisabled(boolean disabled) {
		if (disabled) {
			display.setHidden(true);
		}
		this.disabled = disabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#getKeyID()
	 */
	@Override
	public int getDisplayKeyID() {
		return keyID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setKeyID(int)
	 */
	@Override
	public void setDisplayKeyID(int code) {
		if (code == Keys.ENTER) {
			return;
		}
		keyID = code;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#hitsConsole(float, float)
	 */
	@Override
	public boolean hitsConsole(float screenX, float screenY) {
		if (disabled || !visibile) {
			return false;
		}

		Vector3 stageCoords = stage.getCamera().unproject(new Vector3(screenX, screenY, 0));
		float x = stageCoords.x;
		float y = stageCoords.y;

		float x1 = consoleWindow.getX();
		float y1 = consoleWindow.getY();
		float x2 = x1 + consoleWindow.getWidth();
		float y2 = y1 + consoleWindow.getHeight();
		return (x >= x1 && x <= x2 && y >= y1 && y <= y2);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#dispose()
	 */
	@Override
	public void dispose() {
		if (usesMultiplexer && appInput != null) {
			Gdx.input.setInputProcessor(appInput);
		}
		stage.dispose();
	}

	@Override
	public boolean isVisible() {
		return visibile;
	}

	@Override
	public void setVisible(boolean visible) {
		this.visibile = visible;
		setFocus(visible);
		consoleWindow.setTouchable(visible ? Touchable.childrenOnly : Touchable.disabled);
		display.setHidden(!visible);
	}

	public void setHandleFocus(boolean handleFocus) {
		this.handleFocus = handleFocus;
		updateTransparency();
	}

	@Override
	public boolean hasFocus() {
		if (!handleFocus) {
			return visibile;
		}
		return focus;
	}

	@Override
	public void setFocus(boolean focus) {
		if (!handleFocus)
			return;
		this.focus = focus;
		if (this.focus) {
			display.select();
		} else {
			display.deselect();
			updateTransparency();
		}
	}

	@Override
	public void select() {
		display.select();
	}

	@Override
	public void deselect() {
		display.deselect();
	}

	public void setTransparency(float foreTransparency, float backgroundTransparency) {
		this.foregroundTransparency = foreTransparency;
		this.backgroundTransparency = backgroundTransparency;
	}

	public boolean isCaptured() {
		return this.captured;
	}

	public void updateTransparency() {
		if (!isVisible())
			return;

		Color color = consoleWindow.getColor();
		if (hasFocus() || isCaptured()) {
			color.a = foregroundTransparency;
		} else {
			color.a = backgroundTransparency;
		}
		consoleWindow.setColor(color);
	}

	public void setMouseHoverDrawble(Drawable mouseHoverDrawable) {
		display.setMouseHoverDrawable(mouseHoverDrawable);
	}

	public void setSelectedDrawble(Drawable selectedDrawable) {
		display.setSelectedDrawable(selectedDrawable);
	}

	public void setDebug(boolean enabled, boolean recursively) {
		consoleWindow.setDebug(enabled, recursively);
	}

	public Window getWindow() {
		return consoleWindow;
	}
}
