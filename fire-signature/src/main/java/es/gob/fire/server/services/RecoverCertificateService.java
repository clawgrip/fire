/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.gob.fire.server.services.internal.RecoverCertificateManager;
import es.gob.fire.signature.AplicationsDAO;
import es.gob.fire.signature.ApplicationChecking;
import es.gob.fire.signature.ConfigManager;

/** Servlet que recupera un certificado recien creado. */
public final class RecoverCertificateService extends HttpServlet {

    /** Serial ID. */
	private static final long serialVersionUID = -2818829387993970068L;

	private static final Logger LOGGER = Logger.getLogger(RecoverCertificateService.class.getName());

    // Parametros que necesitamos de la URL. Se mantiene por compatibilidad ya que en las nuevas
	// versiones se utiliza "appid"
    private static final String PARAMETER_NAME_APPLICATION_ID = "appid"; //$NON-NLS-1$
    private static final String OLD_PARAMETER_NAME_APPLICATION_ID = "appId"; //$NON-NLS-1$

    private static final String PARAMETER_NAME_TRANSACTION_ID = "transactionid"; //$NON-NLS-1$
    private static final String OLD_PARAMETER_NAME_TRANSACTION_ID = "transactionId"; //$NON-NLS-1$

    /** Recepci&oacute;n de la petici&oacute;n GET y realizaci&oacute;n de la
     * firma. */
    @Override
    protected void service(final HttpServletRequest request,
    					   final HttpServletResponse response) throws IOException {

		LOGGER.fine("Peticion recibida"); //$NON-NLS-1$

    	final RequestParameters params = RequestParameters.extractParameters(request);

    	updateParams(params);

    	final String appId = params.getParameter(PARAMETER_NAME_APPLICATION_ID);

    	// Comprobacion de la aplicacion solicitante
        if (ConfigManager.isCheckApplicationNeeded()) {
        	LOGGER.fine("Se realizara la validacion del Id de aplicacion"); //$NON-NLS-1$
        	if (appId == null || appId.isEmpty()) {
        		LOGGER.warning("No se ha proporcionado el identificador de la aplicacion"); //$NON-NLS-1$
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }

    		try {
    			final ApplicationChecking appCheck = AplicationsDAO.checkApplicationId(appId);
	        	if (!appCheck.isValid()) {
    				LOGGER.warning("Se proporciono un identificador de aplicacion no valido. Se rechaza la peticion"); //$NON-NLS-1$
    				response.sendError(HttpServletResponse.SC_FORBIDDEN);
    				return;
    			}
    		}
    		catch (final Exception e) {
    			LOGGER.log(Level.SEVERE, "Ocurrio un error grave al validar el identificador de la aplicacion", e); //$NON-NLS-1$
    			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    			return;
    		}
    	}
    	else {
    		LOGGER.fine("No se realiza la validacion de aplicacion en la base de datos"); //$NON-NLS-1$
    	}

    	if (ConfigManager.isCheckCertificateNeeded()) {
    		LOGGER.fine("Se realizara la validacion del certificado"); //$NON-NLS-1$
    		final X509Certificate[] certificates = ServiceUtil.getCertificatesFromRequest(request);
	    	try {
				ServiceUtil.checkValidCertificate(appId, certificates);
			} catch (final CertificateValidationException e) {
				LOGGER.severe("Error en la validacion del certificado: " + e); //$NON-NLS-1$
				response.sendError(e.getHttpError(), e.getMessage());
				return;
			}
    	}
    	else {
    		LOGGER.fine("No se validara el certificado");//$NON-NLS-1$
    	}

    	// Una vez realizadas las comprobaciones de seguridad y envio de estadisticas,
    	// delegamos el procesado de la operacion
    	RecoverCertificateManager.recoverCertificate(params, response);
    }

    private static void updateParams(final RequestParameters params) {
    	params.replaceParamKey(OLD_PARAMETER_NAME_APPLICATION_ID, PARAMETER_NAME_APPLICATION_ID);
    	params.replaceParamKey(OLD_PARAMETER_NAME_TRANSACTION_ID, PARAMETER_NAME_TRANSACTION_ID);
    }
}
