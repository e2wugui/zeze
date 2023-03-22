package Zeze.Arch;

public class ServerNotFoundException extends IllegalStateException {
	public ServerNotFoundException() {
	}

	public ServerNotFoundException(String s) {
		super(s);
	}

	public ServerNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ServerNotFoundException(Throwable cause) {
		super(cause);
	}
}
