package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;

public interface Command {
	String getName();

	String getDescription();

	Array<CommandParameter> getParameters();

	void execute(String params);

	default Array<Command> getSubCommands() {
		return null;
	}

	default boolean isHidden() {
		return false;
	}

	default boolean isExecutable() {
		return true;
	}
}
