package Zeze.Hot;

import org.jspecify.annotations.Nullable;

/** 热安装与不支持类换代的能力在进程内互斥，选择后仅在 JVM 重启时重置。 */
public final class HotInstallGate {
	private static boolean installAdmitted;
	private static @Nullable String blocker;

	private HotInstallGate() {
	}

	public static synchronized void blockForProcess(String feature) {
		if (installAdmitted)
			throw new IllegalStateException(feature
					+ " cannot be enabled after a hot install was admitted in this JVM; restart the JVM");
		if (blocker == null)
			blocker = feature;
	}

	/** 返回阻止热安装的能力名称，或在准入成功时返回 null。 */
	static synchronized @Nullable String tryAdmit() {
		if (blocker == null)
			installAdmitted = true;
		return blocker;
	}
}
