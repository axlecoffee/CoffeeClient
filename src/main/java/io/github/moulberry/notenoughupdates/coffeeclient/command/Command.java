/*
 * Copyright (C) 2025 CoffeeClient contributors
 *
 * This file is part of CoffeeClient addon for NotEnoughUpdates.
 */

package io.github.moulberry.notenoughupdates.coffeeclient.command;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {

	public final List<String> names;

	public Command(List<String> names) {
		this.names = names;
	}

	public abstract void runCommand(ArrayList<String> args);
}
