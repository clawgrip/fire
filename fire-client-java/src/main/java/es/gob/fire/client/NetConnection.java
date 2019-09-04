package es.gob.fire.client;

import java.io.IOException;

import es.gob.fire.client.HttpsConnection.Method;

/** Conexi&oacute;n de red.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public interface NetConnection {

	/** Realiza una peticion HTTP a una URL.
	 * @param url URL a la que se realiza la petici&oacute;n.
	 * @param urlParameters Par&aacute;metros transmitidos en la llamada.
	 * @param method M&eacute;todo HTTP utilizado.
	 * @return Datos recuperados como resultado de la llamada.
	 * @throws IOException Cuando ocurre un error durante la conexi&oacute;n/lectura o el
	 *                     servidor devuelve un error en la operaci&oacute;n. */
	byte[] readUrl(final String url, final String urlParameters, final Method method) throws IOException;

}
