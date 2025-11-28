package Zeze.Arch;

public class ProviderModuleState {
	public final long sessionId;
	public final int moduleId;
	public final int choiceType;
	public final boolean dynamic;

	public ProviderModuleState(long sessionId, int moduleId, int choiceType, boolean dynamic) {
		this.sessionId = sessionId;
		this.moduleId = moduleId;
		this.choiceType = choiceType;
		this.dynamic = dynamic;
	}

	@Override
	public String toString() {
		return "(" + sessionId + "," + moduleId + "," + choiceType + "," + dynamic + ")";
	}
}
