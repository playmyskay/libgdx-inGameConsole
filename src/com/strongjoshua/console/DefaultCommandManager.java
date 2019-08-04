package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;

public class DefaultCommandManager implements CommandManager {
	private Array<Command> commands = new Array<>();

	@Override
	public Command getCommand(String name) {
		String[] parts = name.split("\\.");

		Command searchedCommand = null;
		Array<Command> subCommands = commands;
		for (String part : parts) {
			for (Command command : subCommands) {
				if (part.compareToIgnoreCase(command.getName()) == 0) {
					searchedCommand = command;
				}
			}

			if (searchedCommand == null)
				return null;

			subCommands = searchedCommand.getSubCommands();
		}
		return searchedCommand;
	}

	@Override
	public Array<Command> getCommands() {
		return commands;
	}

	@Override
	public void add(Command command) {
		this.commands.add(command);
	}

}
