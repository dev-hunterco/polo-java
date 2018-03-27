package com.hunterco.polo;

public class PoloMessagingException extends Exception {

	public PoloMessagingException() {
		super();
	}

	public PoloMessagingException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public PoloMessagingException(String message, Throwable cause) {
		super(message, cause);
	}

	public PoloMessagingException(String message) {
		super(message);
	}

	public PoloMessagingException(Throwable cause) {
		super(cause);
	}
}
