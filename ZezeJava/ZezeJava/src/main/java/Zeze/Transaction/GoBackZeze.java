package Zeze.Transaction;

import org.jetbrains.annotations.Contract;

public class GoBackZeze extends Error {
	public GoBackZeze(Throwable cause) {
		super(cause);
	}

	public GoBackZeze(String msg) {
		super(msg);
	}

	public GoBackZeze(String msg, Throwable cause) {
		super(msg, cause);
	}

	public GoBackZeze() {
	}

	@Contract("_, _ -> fail")
	public static void Throw(String msg, Throwable cause) {
		throw cause != null ? new GoBackZeze(msg, cause) : new GoBackZeze(msg);
	}
}
