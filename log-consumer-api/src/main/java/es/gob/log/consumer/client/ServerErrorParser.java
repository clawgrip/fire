package es.gob.log.consumer.client;

/**
 * Analizador para la gesti&oacute;n de las respuestas controladas del servidor de logs.
 */
public class ServerErrorParser {

	private static final int SEPARATOR = '|';

	private int status;

	private final String message;

	/**
	 * Contruye el analizador para facilitar el tratamiento de una respuesta de error
	 * controlada del servidor de logs.
	 * @param content Contenido de la respuesta del servidor.
	 */
	public ServerErrorParser(final byte[] content) {
		this(new String(content));
	}

	/**
	 * Contruye el analizador para facilitar el tratamiento de una respuesta de error
	 * controlada del servidor de logs.
	 * @param content Contenido de la respuesta del servidor.
	 */
	public ServerErrorParser(final String content) {

		if (content == null) {
			throw new NullPointerException("No se admiten mensajes vacios"); //$NON-NLS-1$
		}

		final int sepPos = content.indexOf(SEPARATOR);
		if (sepPos > -1) {
			try {
				this.status = Integer.parseInt(content.substring(0, sepPos));
			}
			catch (final Exception e) {
				this.status = 0;
			}
			this.message = content.substring(sepPos + 1);
		}
		else {
			this.status = 0;
			this.message = content;
		}
	}

	public int getStatus() {
		return this.status;
	}

	public String getMessage() {
		return this.message;
	}
}
