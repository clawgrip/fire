/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services.triphase.document;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/** Interfaz para la recuperaci&oacute;n de documentos desde un servidor o repositorio documental.
 * <u>ES OBLIGATORIO</u>, que las clases que implementen esta interfaz dispongan de un constructor que reciba
 * &uacute;nicamente un objeto Properties (<code>java.util.Properties</code>).
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s */
public interface DocumentManager {

	/** Obtiene un documento en base a su identificador.
	 * Si no es posible recuperar el fichero se debe lanzar una excepci&oacute;n. El mensaje se recibir&aacute;
	 * como parte del mensaje de error en el cliente de firma.
	 * @param id Identificador del documento
	 * @param certChain Cadena de certificados que se usar&aacute; para realizar la firma
	 * @param config Par&aacute;metros para la configuraci&oacute;n de la recuperaci&oacute;n del documento.
	 * @return Documento (en binario)
	 * @throws IOException Cuando ocurre alg&uacute;n problema con la recuperaci&oacute;n */
	byte[] getDocument(String id, X509Certificate[] certChain, Properties config) throws IOException;

	/** Almacena un documento firmado.
	 * Si no es posible almacenar el fichero se debe lanzar una excepci&oacute;n. El mensaje se recibir&aacute;
	 * como parte del mensaje de error en el cliente de firma.
	 * @param id Identificador del documento original no firmado.
	 * @param certChain Cadena de certificados de firma.
	 * @param data Datos firmados.
	 * @param config Par&aacute;metros para la configuraci&oacute;n del guardado del documento.
	 * @return Identificador del nuevo documento codificado en base 64.
	 * @throws IOException Cuando ocurre alg&uacute;n problema con la recuperaci&oacute;n */
	String storeDocument(String id, final X509Certificate[] certChain, byte[] data, Properties config) throws IOException;
}
