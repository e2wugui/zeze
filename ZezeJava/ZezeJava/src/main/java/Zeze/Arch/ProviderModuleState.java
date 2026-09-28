package Zeze.Arch;

/**
 * 模块在某个 Provider 连接上的订阅状态：会话、choiceType 与是否动态模块。
 */
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
