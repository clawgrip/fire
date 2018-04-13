package es.gob.log.consumer.service;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Servicio de consulta de logs.
 */
public class LogService extends HttpServlet {

	/** Serial ID. */
	private static final long serialVersionUID = -8162434026674748888L;

	private static final Logger LOGGER = Logger.getLogger(LogService.class.getName());


	@Override
	protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

		final String opString = req.getParameter(ServiceParams.OPERATION);
		if (opString == null) {
			LOGGER.warning("No se ha indicado codigo de operacion"); //$NON-NLS-1$
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "No se ha indicado codigo de operacion"); //$NON-NLS-1$
			return;
		}

		// Comprobamos que se solicite una operacion valida
		ServiceOperations op;
		try {
			op = checkOperation(opString);
		}
		catch (final Exception e) {
			LOGGER.warning(String.format("Codigo de operacion no soportado (%s). Se rechaza la peticion.", opString)); //$NON-NLS-1$
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Codigo de operacion no soportado"); //$NON-NLS-1$
			return;
		}

		// Procesamos la peticion segun si requieren login o no
		final int statusCode = HttpServletResponse.SC_OK;
		byte[] result;
		try {
			if (!needLogin(op)) {
				switch (op) {
				case ECHO:
					LOGGER.info("Solicitud entrando de comprobacion del servicio");
					result = echo();
					break;
				case REQUEST_LOGIN:
					LOGGER.info("Solicitud entrando de inicio de sesion");
					result = requestLogin(req);
					break;
				case VALIDATE_LOGIN:
					LOGGER.info("Solicitud entrante de validacion de sesion");
					result = validateLogin(req);
					break;
				default:
					LOGGER.warning("Operacion no soportada sin login previo a pesar de estar marcada como tal. Este resultado refleja un problema en el codigo del servicio"); //$NON-NLS-1$
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Operacion no soportada sin login previo"); //$NON-NLS-1$
					return;
				}
			}
			// Comprobamos si el usuario esta registrado
			else if (checkLogin(req)) {

				switch (op) {
				case GET_LOG_FILES:
					LOGGER.info("Solicitud entrante de listado de ficheros");
					result = getLogFiles();
					break;
				case OPEN_FILE:
					LOGGER.info("Solicitud entrante de apertura de fichero");
					result = openFile(req);
					break;
				case CLOSE_FILE:
					LOGGER.info("Solicitud entrante de cierre de fichero");
					result = closeFile(req);
					break;
				case TAIL:
					LOGGER.info("Solicitud entrante de consulta del final del log");
					result = getLogTail(req);
					break;
				case GET_MORE:
					LOGGER.info("Solicitud entrante de mas log");
					result = getMoreLog(req);
					break;
				case SEARCH_TEXT:
					LOGGER.info("Solicitud entrante de busqueda de texto");
					result = searchText(req);
					break;
				case FILTER:
					LOGGER.info("Solicitud entrante de filtrado de log");
					result = getLogFiltered(req);
					break;
				case DOWNLOAD:
					LOGGER.info("Solicitud entrante de descarga de fichero");
					result = download(req);
					break;
				default:
					LOGGER.warning("Operacion no soportada. Este resultado refleja un problema en el codigo del servicio"); //$NON-NLS-1$
					resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Operacion no soportada sin login previo"); //$NON-NLS-1$
					return;
				}
			}
			else {
				LOGGER.warning("Operacion no soportada sin login previo"); //$NON-NLS-1$
				resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Operacion no soportada sin login previo"); //$NON-NLS-1$
				return;
			}
		}
		catch (final SessionException e) {
			LOGGER.log(
					Level.WARNING,
					"Se solicito una operacion sin haber abierto sesion u ocurrio un error al abrirla", //$NON-NLS-1$
					e);
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Sesion no iniciada"); //$NON-NLS-1$
			return;
		}
		catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Ocurrio un error al procesar la peticion", e); //$NON-NLS-1$
			resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Ocurrio un error al procesar la peticion"); //$NON-NLS-1$
			return;
		}

		resp.setStatus(statusCode);
		resp.getOutputStream().write(result);
		resp.getOutputStream().flush();
	}

	private static ServiceOperations checkOperation(final String opString)
			throws NumberFormatException, UnsupportedOperationException {

		final int op = Integer.parseInt(opString);
		return ServiceOperations.parseOperation(op);
	}

	private static boolean needLogin(final ServiceOperations op) {
		// Las operaciones echo, peticion de login y validacion de login, son
		// las unicas que pueden realizarse sin haber establecido logging.
		return 	op != ServiceOperations.ECHO &&
				op != ServiceOperations.REQUEST_LOGIN &&
				op != ServiceOperations.VALIDATE_LOGIN;
	}

	private static boolean checkLogin(final HttpServletRequest req) {

		boolean logged = false;

		final HttpSession session = req.getSession(false);
		if (session != null) {
			final Object loggedValue = session.getAttribute(SessionParams.LOGGED);
			logged = loggedValue != null ? ((Boolean) loggedValue).booleanValue() : false;
		}

		return logged;
	}

	private static byte[] echo() {
		return EchoServiceManager.process();
	}

	private static byte[] requestLogin(final HttpServletRequest req) throws SessionException {
		final HttpSession session = req.getSession();
		if (session == null) {
			throw new SessionException("No ha sido posible crear la sesion"); //$NON-NLS-1$
		}
		return RequestLoginManager.process(session);
	}

	private static byte[] validateLogin(final HttpServletRequest req) throws SessionException {
		final HttpSession session = req.getSession(false);
		if (session == null) {
			throw new SessionException("No se ha encontrado una sesion iniciada"); //$NON-NLS-1$
		}
		return ValidationLoginManager.process(req, session);
	}

	private static byte[] getLogFiles() {
		final byte[] result = LogFilesServiceManager.process();
		return result;
	}

	private static byte[] openFile(final HttpServletRequest req) throws SessionException {
		final HttpSession session = req.getSession(true);
		if (session == null) {
			throw new SessionException("No ha sido posible crear la sesion"); //$NON-NLS-1$
		}
		final byte[] result = LogOpenServiceManager.process(req);
		return result;
	}

	private static byte[] closeFile(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}

	private static byte[] getLogTail(final HttpServletRequest req) throws SessionException{

		final HttpSession session = req.getSession(true);
		if (session == null) {
			throw new SessionException("No ha sido posible crear la sesion"); //$NON-NLS-1$
		}

		final byte[] result = LogTailServiceManager.process(req);
		session.setAttribute("FilePosition", LogTailServiceManager.getPosition()); //$NON-NLS-1$
		return result;
	}

	private static byte[] getMoreLog(final HttpServletRequest req)throws SessionException {
		final HttpSession session = req.getSession(true);
		if (session == null) {
			throw new SessionException("No ha sido posible crear la sesion"); //$NON-NLS-1$
		}
		final byte[] result = LogMoreServiceManager.process(req);
		session.setAttribute("FilePosition", LogMoreServiceManager.getPosition()); //$NON-NLS-1$
		return result;
	}

	private static byte[] getLogFiltered(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}

	private static byte[] searchText(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}

	private static byte[] compress(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}

	private static byte[] compressChecking(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}

	private static byte[] download(final HttpServletRequest req) {
		throw new UnsupportedOperationException();
	}
}