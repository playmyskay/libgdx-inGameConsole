package com.strongjoshua.console;

import java.util.ArrayList;
import java.util.Collections;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.reflect.Annotation;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Method;
import com.badlogic.gdx.utils.reflect.ReflectionException;
import com.strongjoshua.console.annotation.ConsoleDoc;
import com.strongjoshua.console.log.LogLevel;

public class AnnotationCommandCreator {
	private CommandExecutor exec;
	private Console console;
	private boolean consoleTrace;

	public AnnotationCommandCreator(CommandExecutor exec, Console console) {
		this.exec = exec;
		this.console = console;
	}

	public void setCommandExecutor(CommandExecutor exec) {
		this.exec = exec;
	}

	private class AnntationCommand extends CommandAdapter {
		private Method m;

		@Override
		public void execute(String paramStr) {
			String[] parts = paramStr.split(" ");
			String methodName = parts[0];
			String[] sArgs = null;
			if (parts.length > 1) {
				sArgs = new String[parts.length - 1];
				for (int i = 1; i < parts.length; i++) {
					sArgs[i - 1] = parts[i];
				}
			}

			Class<? extends CommandExecutor> clazz = exec.getClass();
			Method[] methods = ClassReflection.getMethods(clazz);
			Array<Integer> possible = new Array<Integer>();
			for (int i = 0; i < methods.length; i++) {
				Method method = methods[i];
				if (method.getName().equalsIgnoreCase(methodName)
						&& ConsoleUtils.canExecuteCommand(console, method)) {
					possible.add(i);
				}
			}

			if (possible.size <= 0) {
				console.log("No such method found.", LogLevel.ERROR);
				return;
			}

			int size = possible.size;
			int numArgs = sArgs == null ? 0 : sArgs.length;
			for (int i = 0; i < size; i++) {
				Method m = methods[possible.get(i)];
				Class<?>[] params = m.getParameterTypes();
				if (numArgs == params.length) {
					try {
						Object[] args = null;

						try {
							if (sArgs != null) {
								args = new Object[numArgs];

								for (int j = 0; j < params.length; j++) {
									Class<?> param = params[j];
									final String value = sArgs[j];

									if (param.equals(String.class)) {
										args[j] = value;
									} else if (param.equals(Boolean.class)
											|| param.equals(boolean.class)) {
										args[j] = Boolean.parseBoolean(value);
									} else if (param.equals(Byte.class)
											|| param.equals(byte.class)) {
										args[j] = Byte.parseByte(value);
									} else if (param.equals(Short.class)
											|| param.equals(short.class)) {
										args[j] = Short.parseShort(value);
									} else if (param.equals(Integer.class)
											|| param.equals(int.class)) {
										args[j] = Integer.parseInt(value);
									} else if (param.equals(Long.class)
											|| param.equals(long.class)) {
										args[j] = Long.parseLong(value);
									} else if (param.equals(Float.class)
											|| param.equals(float.class)) {
										args[j] = Float.parseFloat(value);
									} else if (param.equals(Double.class)
											|| param.equals(double.class)) {
										args[j] = Double.parseDouble(value);
									}
								}
							}
						} catch (Exception e) {
							// Error occurred trying to parse parameter, continue
							// to next function
							continue;
						}

						m.setAccessible(true);
						m.invoke(exec, args);
						return;
					} catch (ReflectionException e) {
						String msg = e.getMessage();
						if (msg == null || msg.length() <= 0 || msg.equals("")) {
							msg = "Unknown Error";
							e.printStackTrace();
						}
						console.log(msg, LogLevel.ERROR);
						if (consoleTrace) {
							console.log(e, LogLevel.ERROR);
						}
						return;
					}
				}
			}

			console.log("Bad parameters. Check your code.", LogLevel.ERROR);
		}

		public void setMethod(Method m) {
			this.m = m;
		}

		@Override
		public boolean isHidden() {
			return ConsoleUtils.canDisplayCommand(console, m);
		}

		@Override
		public boolean isExecutable() {
			return ConsoleUtils.canExecuteCommand(console, m);
		}

		@Override
		public Array<Command> getSubCommands() {
			return null;
		}

	}

	public Array<Command> createCommands() {
		Array<Command> commands = new Array<>();
		for (Method m : getAllMethods()) {
			Command command = createCommand(m, m.getName());
			commands.add(command);
		}
		return commands;
	}

	private Command createCommand(Method m, String name) {
		AnntationCommand command = new AnntationCommand();
		command.setName(name);
		command.setMethod(m);

		Annotation annotation = m.getDeclaredAnnotation(ConsoleDoc.class);
		if (annotation != null) {
			ConsoleDoc doc = annotation.getAnnotation(ConsoleDoc.class);
			command.setDescription(doc.description());
			Class<?>[] params = m.getParameterTypes();
			for (int i = 0; i < params.length; i++) {
				DefaultCommandParameter parameter = new DefaultCommandParameter();
				parameter.setName(params[i].getSimpleName());
				if (i < doc.paramDescriptions().length) {
					parameter.setDescription(doc.paramDescriptions()[i]);
					command.addParameter(parameter);
				}
			}
		} else {
			Class<?>[] params = m.getParameterTypes();
			for (int i = 0; i < params.length; i++) {
				DefaultCommandParameter parameter = new DefaultCommandParameter();
				parameter.setName(params[i].getSimpleName());
			}
		}

		return command;
	}

	private ArrayList<Method> getAllMethods() {
		ArrayList<Method> methods = new ArrayList<Method>();
		Class<?> c = exec.getClass();
		while (c != Object.class) {
			Collections.addAll(methods, ClassReflection.getDeclaredMethods(c));
			c = c.getSuperclass();
		}
		return methods;
	}
}
