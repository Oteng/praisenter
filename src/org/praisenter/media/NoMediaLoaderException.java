package org.praisenter.media;

/**
 * Custom exception thrown when a {@link MediaLoader} of the desired
 * {@link MediaType} does not exist.
 * @author William Bittle
 * @version 1.0.0
 * @since 1.0.0
 */
public class NoMediaLoaderException extends MediaLibraryException {
	/** The version id */
	private static final long serialVersionUID = 4881059013625339909L;

	/**
	 * Default constructor.
	 */
	public NoMediaLoaderException() {
		super();
	}

	/**
	 * Full constructor.
	 * @param message the message
	 * @param cause the root exception
	 */
	public NoMediaLoaderException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Optional constructor.
	 * @param message the message
	 */
	public NoMediaLoaderException(String message) {
		super(message);
	}

	/**
	 * Optional constructor.
	 * @param cause the root exception
	 */
	public NoMediaLoaderException(Throwable cause) {
		super(cause);
	}
}
