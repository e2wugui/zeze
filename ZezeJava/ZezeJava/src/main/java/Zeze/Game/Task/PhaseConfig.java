package Zeze.Game.Task;

import java.util.ArrayList;

public class PhaseConfig {
	private String description;
	private ArrayList<Condition> conditions = new ArrayList<>();

	public PhaseConfig() {

	}

	public PhaseConfig(String description) {
		this.description = description;
	}

	public ArrayList<Condition> getConditions() {
		return conditions;
	}
}
