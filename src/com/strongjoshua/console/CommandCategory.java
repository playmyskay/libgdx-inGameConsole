package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;

public abstract class CommandCategory implements Command {
	private Array<Command> subCommands = new Array<>();

	public Array<CommandParameter> getParameters() {
		return null;
	}

	@Override
	public String getDescription() {
		return getName();
	}

	@Override
	public Array<Command> getSubCommands() {
		return subCommands;
	}

	@Override
	public void execute(String params) {

	}

}
