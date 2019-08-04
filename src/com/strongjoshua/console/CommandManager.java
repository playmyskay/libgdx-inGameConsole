package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;

public interface CommandManager {

	Command getCommand(String name);

	Array<Command> getCommands();

	void add(Command command);

}
