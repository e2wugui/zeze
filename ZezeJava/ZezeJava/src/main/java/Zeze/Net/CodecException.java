package Zeze.Net;

import java.io.Serial;

public class CodecException extends RuntimeException {
	@Serial
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
