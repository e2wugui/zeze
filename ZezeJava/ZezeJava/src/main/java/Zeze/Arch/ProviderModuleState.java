package Zeze.Arch;

public class ProviderModuleState {
	public final long sessionId;
	public final int moduleId;
	public final int choiceType;
	public final int configType;

	public ProviderModuleState(long sessionId, int moduleId, int choiceType, int configType) {
		this.sessionId = sessionId;
		this.moduleId = moduleId;
		this.choiceType = choiceType;
		this.configType = configType;
	}

	@Override
	public String toString() {
		return "(" + sessionId + "," + moduleId + "," + choiceType + "," + configType + ")";
	}
}
