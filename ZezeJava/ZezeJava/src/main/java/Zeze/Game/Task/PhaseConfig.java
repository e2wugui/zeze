package Zeze.Game.Task;

import java.util.ArrayList;

public class PhaseConfig {
	private final String description;
	private final ArrayList<Condition> conditions = new ArrayList<>();

	public PhaseConfig() {
		description = null;
	}

	public PhaseConfig(String description) {
		this.description = description;
	}

	public String getDescription() {
		return description;
	}

	public ArrayList<Condition> getConditions() {
		return conditions;
	}
}
