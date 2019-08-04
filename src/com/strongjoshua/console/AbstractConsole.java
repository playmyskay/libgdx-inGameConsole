/**
 *
 */

package com.strongjoshua.console;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.strongjoshua.console.log.Log;
import com.strongjoshua.console.log.LogConverter;
import com.strongjoshua.console.log.LogLevel;

/** @author Eric */
public abstract class AbstractConsole implements Console, Disposable {
	protected final Log log;
	protected boolean logToSystem;

	protected boolean disabled;

	protected boolean executeHiddenCommands = true;
	protected boolean displayHiddenCommands = false;
	protected boolean consoleTrace = false;
	protected Array<LogConverter> logConverters;

	private CommandManager commandManager;

	public AbstractConsole() {
		log = new Log();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setLoggingToSystem(java.lang .Boolean)
	 */
	@Override
	public void setLoggingToSystem(Boolean log) {
		this.logToSystem = log;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#log(java.lang.String, com .strongjoshua.console.GUIConsole.LogLevel)
	 */
	@Override
	public void log(String msg, LogLevel level) {
		if (logConverters != null && logConverters.size > 0) {
			for (LogConverter lc : logConverters) {
				msg = lc.convert(msg);
			}
		}
		log.addEntry(msg, level);

		if (logToSystem) {
			switch (level) {
			case ERROR:
				System.err.println("> " + msg);
				break;
			default:
				System.out.println("> " + msg);
				break;
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#log(java.lang.String)
	 */
	@Override
	public void log(String msg) {
		this.log(msg, LogLevel.DEFAULT);
	}

	/**
	 * Logs a new entry to the console using {@link LogLevel}.
	 *
	 * @param exception The exception to be logged
	 * @param level     The {@link LogLevel} of the log entry.
	 */
	@Override
	public void log(Exception exception, LogLevel level) {
		this.log(ConsoleUtils.exceptionToString(exception), level);
	}

	/**
	 * Logs a new entry to the console using {@link LogLevel#ERROR}.
	 *
	 * @param exception The exception to be logged
	 */
	@Override
	public void log(Exception exception) {
		this.log(exception, LogLevel.ERROR);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#printLogToFile(java.lang.String)
	 */
	@Override
	public void printLogToFile(String file) {
		this.printLogToFile(Gdx.files.local(file));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#printLogToFile(com.badlogic.gdx .files.FileHandle)
	 */
	@Override
	public void printLogToFile(FileHandle fh) {
		if (log.printToFile(fh)) {
			log("Successfully wrote logs to file.", LogLevel.SUCCESS);
		} else {
			log("Unable to write logs to file.", LogLevel.ERROR);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#isDisabled()
	 */
	@Override
	public boolean isDisabled() {
		return disabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setDisabled(boolean)
	 */
	@Override
	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#execCommand(java.lang.String)
	 */
	@Override
	public void execCommand(String commandStr) {
		if (disabled)
			return;

		log(commandStr, LogLevel.COMMAND);
		String[] parts = commandStr.split(" ");
		String name = "";
		String params = "";
		if (parts.length >= 1) {
			name = parts[0];
		}

		for (int i = 1; i < parts.length; ++i) {
			params += parts[i];
		}

		Command command = commandManager.getCommand(name);
		if (command == null) {
			log("Command does not exist.");
			return;
		}

		command.execute(params);
	}

	@Override
	public void printCommands() {
		StringBuilder sb = new StringBuilder();
		for (Command command : commandManager.getCommands()) {
			sb.append(command.getName() + ": ");
			for (CommandParameter parameter : command.getParameters()) {
				sb.append(parameter.getName());
			}
			sb.append("\n");
		}

		log(sb.toString());
	}

	@Override
	public void printHelp(String name) {
		Command command = commandManager.getCommand(name);
		if (command == null) {
			log("Command does not exist.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append(command.getName() + ": " + command.getDescription());
		sb.append("\n");
		for (CommandParameter parameter : command.getParameters()) {
			sb.append(parameter.getName() + ": " + parameter.getDescription());
			sb.append("\n");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setExecuteHiddenCommands(boolean)
	 */
	@Override
	public void setExecuteHiddenCommands(boolean enabled) {
		executeHiddenCommands = enabled;
	}

	@Override
	public boolean isExecuteHiddenCommandsEnabled() {
		return executeHiddenCommands;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.strongjoshua.console.Console#setDisplayHiddenCommands(boolean)
	 */
	@Override
	public void setDisplayHiddenCommands(boolean enabled) {
		displayHiddenCommands = enabled;
	}

	@Override
	public boolean isDisplayHiddenCommandsEnabled() {
		return displayHiddenCommands;
	}

	@Override
	public void setConsoleStackTrace(boolean enabled) {
		this.consoleTrace = enabled;
	}

	@Override
	public void setCommandManager(CommandManager commandManager) {
		this.commandManager = commandManager;
	}

	@Override
	public CommandManager getCommandManager() {
		return commandManager;
	}

	@Override
	public void setMaxEntries(int numEntries) {
	}

	@Override
	public void clear() {
	}

	@Override
	public void setSize(int width, int height) {
	}

	@Override
	public void setSizePercent(float wPct, float hPct) {
	}

	@Override
	public void setPosition(int x, int y) {
	}

	@Override
	public void setPositionPercent(float xPosPct, float yPosPct) {
	}

	@Override
	public void resetInputProcessing() {
	}

	@Override
	public InputProcessor getInputProcessor() {
		return null;
	}

	@Override
	public void draw() {
	}

	@Override
	public void refresh() {
	}

	@Override
	public void refresh(boolean retain) {
	}

	@Override
	public int getDisplayKeyID() {
		return 0;
	}

	@Override
	public void setDisplayKeyID(int code) {
	}

	@Override
	public boolean hitsConsole(float screenX, float screenY) {
		return false;
	}

	@Override
	public void dispose() {
	}

	@Override
	public boolean isVisible() {
		return false;
	}

	@Override
	public void setVisible(boolean visible) {
	}

	@Override
	public boolean hasFocus() {
		return false;
	}

	@Override
	public void setFocus(boolean focus) {
	}

	@Override
	public void select() {
	}

	@Override
	public void deselect() {
	}

	@Override
	public void addLogConverter(LogConverter converter) {
		if (logConverters == null)
			logConverters = new Array<LogConverter>();
		logConverters.add(converter);
	}
}
