/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.server.services.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import es.gob.fire.server.services.HttpCustomErrors;
import es.gob.fire.server.services.RequestParameters;
import es.gob.fire.server.services.statistics.SignatureRecorder;
import es.gob.fire.server.services.statistics.TransactionRecorder;
import es.gob.fire.signature.ConfigManager;

/** Manejador que gestiona las peticiones para iniciar el proceso de firma de un lote. */
public final class SignBatchManager {

	private static final Logger LOGGER = Logger.getLogger(SignBatchManager.class.getName());
	private static final SignatureRecorder SIGNLOGGER = SignatureRecorder.getInstance();
	private static final TransactionRecorder TRANSLOGGER = TransactionRecorder.getInstance();

	/**
     * Inicia el proceso de firma de un lote.
	 * @param request Petici&oacute;n de firma del lote.
	 * @param params Par&aacute;metros extra&iacute;dos de la petici&oacute;n.
	 * @param response Respuesta con el resultado del inicio de firma del lote.
	 * @throws IOException Cuando se produce un error de lectura o env&iacute;o de datos.
     */
	public static void signBatch(final HttpServletRequest request, final RequestParameters params, final HttpServletResponse response)
    		throws IOException {

		// Recogemos los parametros proporcionados en la peticion
		final String appId = params.getParameter(ServiceParams.HTTP_PARAM_APPLICATION_ID);
    	final String transactionId = params.getParameter(ServiceParams.HTTP_PARAM_TRANSACTION_ID);
    	final String subjectId = params.getParameter(ServiceParams.HTTP_PARAM_SUBJECT_ID);
		final String stopOnError = params.getParameter(ServiceParams.HTTP_PARAM_BATCH_STOP_ON_ERROR);

		final LogTransactionFormatter logF = new LogTransactionFormatter(appId, transactionId);

		// Comprobamos que se hayan prorcionado los parametros indispensables
    	if (transactionId == null || transactionId.isEmpty()) {
    		LOGGER.warning(logF.format("No se ha proporcionado el ID de transaccion")); //$NON-NLS-1$
        	response.sendError(HttpServletResponse.SC_BAD_REQUEST,
    				"No se ha proporcionado el identificador de la transaccion"); //$NON-NLS-1$
    		return;
    	}

		LOGGER.fine(logF.format("Peticion bien formada")); //$NON-NLS-1$

    	final FireSession session = SessionCollector.getFireSession(transactionId, subjectId, request.getSession(false), false, true);
    	if (session == null) {
    		LOGGER.warning(logF.format("La transaccion no se ha inicializado o ha caducado")); //$NON-NLS-1$
    		response.sendError(HttpCustomErrors.INVALID_TRANSACTION.getErrorCode(), "La transaccion no se ha inicializado o ha caducado"); //$NON-NLS-1$
    		return;
    	}

    	final BatchResult batchResult = (BatchResult) session.getObject(ServiceParams.SESSION_PARAM_BATCH_RESULT);
    	if (batchResult == null || batchResult.documentsCount() == 0) {
    		LOGGER.warning(logF.format("Se ha pedido firmar un lote sin documentos. Se aborta la operacion.")); //$NON-NLS-1$
    		SIGNLOGGER.register(session, false, null);
    		TRANSLOGGER.register(session, false);
        	SessionCollector.removeSession(session);
    		response.sendError(HttpCustomErrors.BATCH_NO_DOCUMENT.getErrorCode(), HttpCustomErrors.BATCH_NO_DOCUMENT.getErrorDescription());
    		return;
    	}

		// Recuperamos los proveedores cargados para la aplicacion
		final String[] provs = (String[]) session.getObject(ServiceParams.SESSION_PARAM_PROVIDERS);

    	// TODO: Borrar esto cuando se terminen los cambios en el componente distribuido PHP
    	// en el que, por ahora, no se envia el subjectId por parametro, asi que hemos de usar
    	// el de sesion. Esto impide realizar la comprobacion de seguridad adicional de que sea
    	// el mismo usuario el que crease la transaccion y el que ahora dice ser
    	final String currentUserId = session.getString(ServiceParams.SESSION_PARAM_SUBJECT_ID);

        final TransactionConfig connConfig =
        		(TransactionConfig) session.getObject(ServiceParams.SESSION_PARAM_CONNECTION_CONFIG);

		// Listamos los certificados del usuario
		if (connConfig == null || !connConfig.isDefinedRedirectErrorUrl()) {
			LOGGER.warning(logF.format("No se proporcionaron las URL de redireccion para la operacion")); //$NON-NLS-1$
			SIGNLOGGER.register(session, false, null);
			TRANSLOGGER.register(session, false);
			response.sendError(HttpServletResponse.SC_BAD_REQUEST,
					"No se proporcionaron las URL de redireccion para la operacion"); //$NON-NLS-1$
			return;
		}

        // Configuramos en la sesion si se debe detener el proceso de error cuando se encuentre uno
        // para tenerlo en cuenta en este paso y los siguientes
        session.setAttribute(ServiceParams.SESSION_PARAM_BATCH_STOP_ON_ERROR, stopOnError);
        session.setAttribute(ServiceParams.SESSION_PARAM_PREVIOUS_OPERATION, SessionFlags.OP_SIGN);
        SessionCollector.commit(session);


        LOGGER.info(logF.format("Generamos la URL de redireccion")); //$NON-NLS-1$

		final String redirectErrorUrl = connConfig.getRedirectErrorUrl();

		// Obtenemos la URL de las paginas web de FIRe (parte publica). Si no se define,
		// se calcula en base a la URL actual
		final String redirectUrlBase = getPublicContext(request.getRequestURL().toString());

        // Si ya se definio el origen del certificado, se envia al servicio que se encarga de
        // redirigirlo. Si no, se envia la pagina de seleccion
        String redirectUrl;
        if (provs != null && provs.length == 1) {
        	redirectUrl = "chooseCertificateOriginService?" + //$NON-NLS-1$
        			ServiceParams.HTTP_PARAM_CERT_ORIGIN + "=" + provs[0] + "&" + //$NON-NLS-1$ //$NON-NLS-2$
        			ServiceParams.HTTP_PARAM_CERT_ORIGIN_FORCED + "=true"; //$NON-NLS-1$
        } else {
        	redirectUrl = "ChooseCertificateOrigin.jsp?" + ServiceParams.HTTP_PARAM_OPERATION + "=" + ServiceParams.OPERATION_BATCH; //$NON-NLS-1$ //$NON-NLS-2$
        }

        // Devolvemos la pagina a la que debe dirigir al usuario
        final SignOperationResult result = new SignOperationResult(
        		transactionId,
        		redirectUrlBase + redirectUrl +
        			"&" + ServiceParams.HTTP_PARAM_TRANSACTION_ID + "=" + transactionId + //$NON-NLS-1$ //$NON-NLS-2$
        			"&" + ServiceParams.HTTP_PARAM_SUBJECT_ID + "=" + currentUserId + //$NON-NLS-1$ //$NON-NLS-2$
        			"&" + ServiceParams.HTTP_PARAM_ERROR_URL + "=" + redirectErrorUrl); //$NON-NLS-1$ //$NON-NLS-2$

        LOGGER.info(logF.format("Devolvemos la URL de redireccion con el ID de transaccion")); //$NON-NLS-1$

        sendResult(response, result);
	}

	private static String getPublicContext(final String requestUrl) {
		String redirectUrlBase = ConfigManager.getPublicContextUrl();
		if ((redirectUrlBase == null || redirectUrlBase.isEmpty()) && requestUrl != null) {
			redirectUrlBase = requestUrl.substring(0, requestUrl.lastIndexOf('/'));
		}

		if (redirectUrlBase != null && !redirectUrlBase.endsWith("/public/")) { //$NON-NLS-1$
			if (redirectUrlBase.endsWith("/public")) { //$NON-NLS-1$
				redirectUrlBase += "/"; //$NON-NLS-1$
			}
			else {
				redirectUrlBase += "/public/"; //$NON-NLS-1$
			}
		}
		return redirectUrlBase;
	}

	private static void sendResult(final HttpServletResponse response, final SignOperationResult result) throws IOException {
        response.setContentType("application/json"); //$NON-NLS-1$
        try (
    		final PrintWriter out = response.getWriter();
		) {
	        out.print(result.toString());
	        out.flush();
	        out.close();
        }
	}
}
