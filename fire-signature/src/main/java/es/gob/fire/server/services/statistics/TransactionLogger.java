package es.gob.fire.server.services.statistics;

import java.sql.SQLException;

import es.gob.fire.server.services.FIReServiceOperation;
import es.gob.fire.server.services.internal.FireSession;
import es.gob.fire.server.services.internal.ServiceParams;
import es.gob.fire.services.FireLogger;
import es.gob.fire.services.statistics.config.ConfigManager;
import es.gob.fire.services.statistics.config.DBConnectionException;
import es.gob.fire.services.statistics.dao.AplicationsDAO;
import es.gob.fire.services.statistics.entity.Application;
import es.gob.fire.services.statistics.entity.TransactionCube;

public class TransactionLogger {


	private FireLogger fireLogger ;

	private static String LOGGER_NAME = "TRANSACTION"; //$NON-NLS-1$

	private static String ROLLDATE = "DIARIA"; //$NON-NLS-1$

	private static String OTRO = "Otro"; //$NON-NLS-1$

	private static TransactionLogger transactlogger;

	private  TransactionCube transactCube;


	private TransactionLogger() {
		this.setFireLogger(new FireLogger(LOGGER_NAME, ConfigManager.getStatisticsDir(),ROLLDATE));
	}


	/**
	 *
	 * @param fireSesion
	 * @param result
	 */
	public final void log(final FireSession fireSesion, final boolean result) {

		 String[] provsSession = null;
		 String prov =  null;
		 String provForced = null;

		if(getTransactCube() == null) {
			this.setTransactCube(new TransactionCube());
		}

		this.getTransactCube().setResultTransaction(result);

		if(fireSesion.getObject(ServiceParams.SESSION_PARAM_TRANSACTION_ID) != null
				&& !"".equals(fireSesion.getObject(ServiceParams.SESSION_PARAM_TRANSACTION_ID))) { //$NON-NLS-1$
			final String id_tr = fireSesion.getString(ServiceParams.SESSION_PARAM_TRANSACTION_ID);
			this.getTransactCube().setId_transaccion(id_tr != null ? id_tr : "0"); //$NON-NLS-1$
		}

		if(fireSesion.getString(ServiceParams.SESSION_PARAM_APPLICATION_ID) != null && !"".equals(fireSesion.getString(ServiceParams.SESSION_PARAM_APPLICATION_ID))) { //$NON-NLS-1$
			final String idaplication = fireSesion.getString(ServiceParams.SESSION_PARAM_APPLICATION_ID);
			Application app;
			try {
				app = AplicationsDAO.selectApplication(idaplication);
				this.getTransactCube().setAplicacion(app.getNombre());
			} catch (final SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (final DBConnectionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		if(fireSesion.getString(ServiceParams.SESSION_PARAM_OPERATION) != null && !"".equals(fireSesion.getString(ServiceParams.SESSION_PARAM_OPERATION))) { //$NON-NLS-1$
			final FIReServiceOperation fsop = FIReServiceOperation.parse(fireSesion.getString(ServiceParams.SESSION_PARAM_OPERATION)) ;
			final Operations op = Operations.parse(fsop);
			this.getTransactCube().setOperacion(op.name());

		}

		if(fireSesion.getObject(ServiceParams.SESSION_PARAM_PROVIDERS) != null ) {
			 provsSession = (String []) fireSesion.getObject(ServiceParams.SESSION_PARAM_PROVIDERS);
		}

		if(fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN) != null &&
				!"".equals(fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN))) { //$NON-NLS-1$
			 prov = fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN);
		}

		if(fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN_FORCED) != null &&
				!"".equals(fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN_FORCED))) { //$NON-NLS-1$
			provForced = fireSesion.getString(ServiceParams.SESSION_PARAM_CERT_ORIGIN_FORCED);
		}

		if(provForced != null && !"".equals(provForced)) { //$NON-NLS-1$
			this.getTransactCube().setProveedor(provForced);
			this.getTransactCube().setProveedorForzado(true);
		}
		else if(prov != null && !"".equals(prov)) { //$NON-NLS-1$
			this.getTransactCube().setProveedor(prov);
		}
		else if(provsSession != null && provsSession.length == 1) {
			this.getTransactCube().setProveedor(provsSession[0]);
			this.getTransactCube().setProveedorForzado(true);
		}
		else {
			this.getTransactCube().setProveedor(OTRO);
		}

		this.getFireLogger().getLogger().info(this.getTransactCube().toString());
	}


	public static final TransactionLogger getTransactLogger(final String confStatistics) {
		int conf = 0;
		if(confStatistics != null && !"".equals(confStatistics)) {//$NON-NLS-1$
			conf = Integer.parseInt(confStatistics);
		}
		if (conf != 0) {
			if(transactlogger == null) {
				transactlogger = new TransactionLogger();
			}
			return transactlogger;
		}
		return null;
	}



	private final FireLogger getFireLogger() {
		return this.fireLogger;
	}

	private final void setFireLogger(final FireLogger fireLogger) {
		this.fireLogger = fireLogger;
	}


	private final TransactionCube getTransactCube() {
		return this.transactCube;
	}


	private final void setTransactCube(final TransactionCube transactCube) {
		this.transactCube = transactCube;
	}



}
