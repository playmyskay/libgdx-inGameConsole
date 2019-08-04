
package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.ObjectSet.ObjectSetIterator;

public class CommandCompleter {
	private ObjectSet<Command> possibleCommands;
	private ObjectSetIterator<Command> iterator;
	private String commandPrefix;
	private String originalStr = "";

	public CommandCompleter() {
		possibleCommands = new ObjectSet<>();
		commandPrefix = "";
	}

	public int count(String str, char c) {
		int count = 0;
		for (int i = 0; i < str.length(); i++) {
			if (str.charAt(i) == c) {
				count++;
			}
		}
		return count;
	}

	public void set(CommandManager ce, String s) {
		reset();

		originalStr = s;
		s = s.toLowerCase();
		if (s.contains(".")) {
			int dotCount = count(s, '.');
			String[] parts = s.split("\\.");

			Array<Command> commands = ce.getCommands();

			int dot = 0;
			for (String part : parts) {
				if (dot == dotCount)
					continue;

				Command searchCommand = null;
				for (Command command : commands) {
					if (command.isHidden())
						continue;

					if (part.compareToIgnoreCase(command.getName()) == 0) {
						searchCommand = command;
					}
				}

				if (searchCommand == null) {
					return;
				}

				commands = searchCommand.getSubCommands();
				if (commands == null)
					return;

				if (!commandPrefix.isEmpty())
					commandPrefix += ".";

				commandPrefix += searchCommand.getName();
				++dot;
			}

			String lastPart = parts[parts.length - 1];
			for (Command command : commands) {
				if (command.isHidden())
					continue;

				if (command.getName().startsWith(lastPart)) {
					possibleCommands.add(command);
				}
			}
		} else {
			for (Command command : ce.getCommands()) {
				if (command.isHidden())
					continue;

				if (command.getName().startsWith(s)) {
					possibleCommands.add(command);
				}
			}
		}

		iterator = new ObjectSetIterator<>(possibleCommands);
	}

	public void reset() {
		possibleCommands.clear();
		commandPrefix = "";
		iterator = null;
	}

	public boolean isNew() {
		return possibleCommands.size == 0;
	}

	public boolean wasSetWith(String s) {
		return commandPrefix.equalsIgnoreCase(s);
	}

	public String next() {
		if (iterator == null)
			return null;
		if (!iterator.hasNext) {
			iterator.reset();
			return originalStr;
		}

		if (!commandPrefix.isEmpty())
			return commandPrefix + "." + iterator.next().getName();

		return iterator.next().getName();
	}

}
