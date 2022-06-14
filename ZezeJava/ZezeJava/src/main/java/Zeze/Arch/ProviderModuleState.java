package Zeze.Arch;

public class ProviderModuleState {
	public final long SessionId;
	public final int ModuleId;
	public final int ChoiceType;
	public final int ConfigType;

	public ProviderModuleState(long sessionId, int moduleId, int choiceType, int configType) {
		SessionId = sessionId;
		ModuleId = moduleId;
		ChoiceType = choiceType;
		ConfigType = configType;
	}

	@Override
	public String toString() {
		return "ProviderModuleState{" + "SessionId=" + SessionId + ", ModuleId=" + ModuleId +
				", ChoiceType=" + ChoiceType + ", ConfigType=" + ConfigType + '}';
	}
}
