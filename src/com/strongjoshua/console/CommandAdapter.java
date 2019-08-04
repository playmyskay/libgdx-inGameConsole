package com.strongjoshua.console;

import com.badlogic.gdx.utils.Array;

public abstract class CommandAdapter implements Command {
	private String name;
	private String description;
	private Array<CommandParameter> parameters;

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public void addParameter(CommandParameter parameter) {
		if (parameters == null) {
			parameters = new Array<CommandParameter>();
		}
		parameters.add(parameter);
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public Array<CommandParameter> getParameters() {
		return parameters;
	}

}
