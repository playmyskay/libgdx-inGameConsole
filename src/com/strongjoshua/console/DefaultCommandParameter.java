package com.strongjoshua.console;

public class DefaultCommandParameter implements CommandParameter {
	private String name;
	private String description;

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

}
