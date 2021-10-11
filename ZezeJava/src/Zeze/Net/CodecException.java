package Zeze.Net;

public class CodecException extends Exception {
	private static final long serialVersionUID = 501428574934410873L;

	public CodecException() {
	}

	public CodecException(String message) {
		super(message);
	}

	public CodecException(Throwable cause) {
		super(cause);
	}

	public CodecException(String message, Throwable cause) {
		super(message, cause);
	}
}
