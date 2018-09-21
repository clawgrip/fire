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
import java.util.Properties;
import java.util.logging.Logger;

import es.gob.fire.server.services.internal.SignConstants;
import es.gob.fire.server.services.statistics.ImprovedSignatureFormats;
import es.gob.fire.signature.ConfigManager;
import es.gob.fire.upgrade.ConfigFileNotFoundException;
import es.gob.fire.upgrade.PlatformWsException;
import es.gob.fire.upgrade.PlatformWsHelper;
import es.gob.fire.upgrade.Upgrade;
import es.gob.fire.upgrade.UpgradeResponseException;
import es.gob.fire.upgrade.UpgradeTarget;

/**
 * Clase para la actualizaci&oacute;n de firmas mediante una conexi&oacute;n
 * con la Plataforma @firma.
 */
public class AfirmaUpgrader {

	private static final Logger LOGGER = Logger.getLogger(AfirmaUpgrader.class.getName());

	private static PlatformWsHelper conn = null;

	private static String upgradedFormat = null;


	/**
	 * Actualiza una firma utilizando la Plataforma @firma. Si no se indica formato de
	 * actualizaci&oacute;n, se devuelve la propia firma.
	 * @param signature Firma que se desea actualizar.
	 * @param upgradeFormat Formato avanzado al que actualizar.
	 * @return Firma actualizada o, si no se indica un formato de actualizacion, la propia firma.
	 * @throws UpgradeException Cuando ocurre cualquier problema que impida la
	 * actualizaci&oacute;n de la firma.
	 */
	public static byte[] upgradeSignature(final byte[] signature, final String upgradeFormat) throws UpgradeException {

		if (upgradeFormat == null || upgradeFormat.isEmpty()) {
			return signature;
		}
		setUpgradedFormat(null);
		LOGGER.info("Actualizando firma al formato: " + upgradeFormat); //$NON-NLS-1$

		if (conn == null) {
			Properties config;
			try {
				config = PlatformWsHelper.loadConfig();
			}
			catch (final Exception e) {
				throw new UpgradeException("No se encontro la configuracion para la conexion con @firma", e); //$NON-NLS-1$
			}
			if (ConfigManager.hasDecipher()) {
				for (final String key : config.keySet().toArray(new String[config.size()])) {
					config.setProperty(key, ConfigManager.getDecipheredProperty(config, key, null));
				}
			}

			conn = new PlatformWsHelper();
			conn.init(config);
		}

		byte[] upgradedSignature;
		final String afirmaId = ConfigManager.getAfirmaAplicationId();
        try {
        	upgradedSignature = Upgrade.signUpgradeCreate(
        			conn,
        			signature,
        			UpgradeTarget.getUpgradeTarget(upgradeFormat),
        			afirmaId);
        	final String[] result = Upgrade.getUpgradeResult().split(":"); //$NON-NLS-1$
        	if(result != null && result.length > 0) {
        		for(int i = 0; i <= result.length - 1; i++) {
            		if(!ImprovedSignatureFormats.getId(result[i]).equals(SignConstants.SIGN_LONGFORMATS_IDOTROS)) {
            			setUpgradedFormat(result[i].toUpperCase());
            		}
            	}
        		if(getUpgradedFormat() == null ) {
        			setUpgradedFormat(SignConstants.SIGN_LONGFORMATS_OTROS);
            	}
        	}


        } catch (final PlatformWsException e) {
        	throw new UpgradeException("Error de conexion con la Plataforma @firma para la actualizacion de la firma", e); //$NON-NLS-1$
        } catch (final UpgradeResponseException e) {
        	throw new UpgradeException("Error durante la actualizacion de la firma. MajorCode: " + e.getMajorCode() + //$NON-NLS-1$
                    ". MinorCode: " + e.getMinorCode(), e); //$NON-NLS-1$
        } catch (final IOException e) {
        	throw new UpgradeException("Error en la comunicacion con la Plataforma @firma", e); //$NON-NLS-1$
		} catch (final ConfigFileNotFoundException e) {
        	throw new UpgradeException("No se encuentra el fichero de configuracion de acceso a la Plataforma @firma", e); //$NON-NLS-1$
		} catch (final Exception e) {
        	throw new UpgradeException("Error no identificado durante el proceso de actualizacion de la firma", e); //$NON-NLS-1$
		}

        return upgradedSignature;
	}


	public static final String getUpgradedFormat() {
		return upgradedFormat;
	}


	private static final void setUpgradedFormat(final String upgradedFormat) {
		AfirmaUpgrader.upgradedFormat = upgradedFormat;
	}




}
