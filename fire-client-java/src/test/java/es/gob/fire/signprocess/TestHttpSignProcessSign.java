/* Copyright (C) 2017 [Gobierno de Espana]
 * This file is part of FIRe.
 * FIRe is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * Date: 08/09/2017
 * You may contact the copyright holder at: soporte.afirma@correo.gob.es
 */
package es.gob.fire.signprocess;

import java.security.cert.X509Certificate;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import es.gob.clavefirma.client.certificatelist.HttpCertificateList;
import es.gob.clavefirma.client.signprocess.HttpLoadProcess;
import es.gob.clavefirma.client.signprocess.HttpSignProcess;
import es.gob.clavefirma.client.signprocess.HttpSignProcessConstants;
import es.gob.clavefirma.client.signprocess.HttpSignProcessConstants.SignatureAlgorithm;
import es.gob.clavefirma.client.signprocess.HttpSignProcessConstants.SignatureFormat;
import es.gob.clavefirma.client.signprocess.HttpSignProcessConstants.SignatureOperation;
import es.gob.clavefirma.client.signprocess.LoadResult;
import es.gob.clavefirma.client.signprocess.TriphaseData;
import es.gob.fire.client.Base64;
import es.gob.fire.client.Utils;

/** Pruebas de firma en la nube. */
public final class TestHttpSignProcessSign {

	private static final String APP_ID = "spt"; //$NON-NLS-1$
	private static final String SUBJECT = "00001"; //$NON-NLS-1$

	private static final String ALGRTH = "SHA512withRSA"; //$NON-NLS-1$
	private static final String PADES = "PAdES"; //$NON-NLS-1$

	/** Prueba completa de firma.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	public void testCompleteSign() throws Exception {

		// Obtenemos el certificado de firma
		final X509Certificate cert = HttpCertificateList.getList(APP_ID, SUBJECT).get(0);

		// Indicamos las redirecciones
		final Properties config = new Properties();
		config.put("redirectOkUrl", "http://www.google.com"); //$NON-NLS-1$ //$NON-NLS-2$
		config.put("redirectErrorUrl", "http://www.ibm.com"); //$NON-NLS-1$ //$NON-NLS-2$

		final byte[] dataToSign = "Hola".getBytes(); //$NON-NLS-1$

		// Cargamos los datos a firmar
		final LoadResult res = HttpLoadProcess.loadData(
			APP_ID,
			SUBJECT,
			SignatureOperation.SIGN,
			SignatureFormat.XADES,
			SignatureAlgorithm.SHA384WITHRSA,
			null, // ExtraParams
			cert,
			dataToSign, // Datos a firmar
			config // Configuracion del servicio servidor
		);

		System.out.println("Hay que autorizar:\n" + res.getRedirectUrl()); //$NON-NLS-1$

		final TriphaseData td = res.getTriphaseData();

		final byte[] signResult = HttpSignProcess.sign(
			"spt",  // ID App//$NON-NLS-1$
			res.getTransactionId(), // ID Transaccion
			HttpSignProcessConstants.SignatureOperation.SIGN, // Operacion
			HttpSignProcessConstants.SignatureFormat.XADES,  // Formato
			HttpSignProcessConstants.SignatureAlgorithm.SHA512WITHRSA, // Algoritmo
			null,   // Parametros adicionales
			cert, // Certificado de firma en Base64
			dataToSign, // Datos a firmar en Base 64
			td, // Sesion trifasica
			null // HttpSignProcessConstants.SignatureUpgrade.T_FORMAT  // Formato de mejora
		);

		System.out.println("Resultado de la firma:\n" + new String(signResult)); //$NON-NLS-1$
	}

	/** Prueba de firma PAdES.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
//	@Ignore
	public void testPAdES() throws Exception {

		final byte[] data = Utils.getDataFromInputStream(
			TestHttpSignProcessSign.class.getResourceAsStream("/TEST_PDF.pdf") //$NON-NLS-1$
		);

		final String CERTIFICATE_TEST = "MIIKhTCCCG2gAwIBAgIIAivtj8K_w0AwDQYJKoZIhvcNAQELBQAwggEhMQswCQYDVQQGEwJFUzESMBAGA1UECAwJQmFyY2Vsb25hMVgwVgYDVQQHDE9CYXJjZWxvbmEgKHNlZSBjdXJyZW50IGFkZHJlc3MgYXQgaHR0cDovL3d3dy5hbmYuZXMvZXMvYWRkcmVzcy1kaXJlY2Npb24uaHRtbCApMScwJQYDVQQKDB5BTkYgQXV0b3JpZGFkIGRlIENlcnRpZmljYWNpb24xLjAsBgNVBAsMJUFORiBBdXRvcmlkYWQgSW50ZXJtZWRpYSBkZSBJZGVudGlkYWQxGjAYBgkqhkiG9w0BCQEWC2luZm9AYW5mLmVzMRIwEAYDVQQFEwlHNjMyODc1MTAxGzAZBgNVBAMMEkFORiBBc3N1cmVkIElEIENBMTAeFw0xNDAzMTEwOTE3NDZaFw0xNjAzMTAwOTE3NDZaMIIBDjE5MDcGA1UECwwwQ2VydGlmaWNhZG8gZGUgQ2xhc2UgMiBkZSBQZXJzb25hIEZpc2ljYSAoRklSTUEpMR0wGwYDVQQDDBRGSVNJQ08gQUNUSVZPIFBSVUVCQTEWMBQGA1UEBAwNQUNUSVZPIFBSVUVCQTEPMA0GA1UEKgwGRklTSUNPMRIwEAYDVQQFEwkzODg2NDE1OVgxJTAjBgkqhkiG9w0BCQEWFnNlcnZpY2lvdGVjbmljb0BhbmYuZXMxEjAQBgNVBAcMCVBPQkxBQ0lPTjESMBAGA1UECAwJUFJPVklOQ0lBMQswCQYDVQQGEwJlczEZMBcGCisGAQQBgZMWAQEMCTM4ODY0MTU5WDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKlS0ytfsK6eZQMfpHQcAQhEc2jU4u3CkFPQAibIeIqZq3Xev5Y5edpFJlb1D93huT9yXzOe8bKpFtgKpncr6iwPbnuTQIl32fb271psJSPXpnQwgI98xZBhZLc6ApOJU-26xvnJOdSsljLsq0dbUw8q6mb4KnKSg4oqnbhoSantWrAfzXWcgzDmxeahdMkwTZn95ssYGT0HmT8wR_q55DDNDV_ENmGGLFJmLpEpxaS7rWTaL3m3BASjRWz_Lky_G0qdXTUMNu2JXzkdThlHbDR7JEg2Xcj0d6RBD_ri_QclY_7V6glYnQALnXsgt_Kd9mA3EFcTq2yPVy56lqIG7UMCAwEAAaOCBM4wggTKMBkGCisGAQQBgZMWAQEECwwJMzg4NjQxNTlYMB0GA1UdDgQWBBQqrVvOUzevn5VMiJzRz3fQby1aZzAJBgNVHRMEAjAAMDcGCCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL29jc3AuYW5mLmVzL3NwYWluL0FWMIIBhAYDVR0fBIIBezCCAXcwOqA4oDaGNGh0dHBzOi8vY3JsLmFuZi5lcy9jcmwvQU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwwOqA4oDaGNGh0dHBzOi8vd3d3LmFuZi5lcy9jcmwvQU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwwgfyggfmggfaGcWxkYXA6Ly9sZGFwLmFuZi5lczozODkvY249QU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwsb3U9QU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1NixvdT1BTkZfR2xvYmFsX1Jvb3RfQ0EsZGM9YW5mpIGAMH4xEzARBgoJkiaJk_IsZAEZFgNhbmYxJjAkBgNVBAMMHUFORl9Bc3N1cmVkX0lEX0NBMV9TSEEyNTYuY3JsMSIwIAYDVQQLDBlBTkZfQXNzdXJlZF9JRF9DQTFfU0hBMjU2MRswGQYDVQQLDBJBTkZfR2xvYmFsX1Jvb3RfQ0EwHwYDVR0jBBgwFoAUNS4t5ikcv4vZSnyeK-CVR9HGTz0wEwYKKwYBBAGBjxwKCAQFDANOSUUwHAYJKwYBBAGBjxwTBA8MDTIwMTg2NS01MDMwMzMwKAYKKwYBBAGBjxwTAQQaDBgxMzIxNC0xNTY0Nzk4MTM3Nzc4Njc1ODQwEwYKKwYBBAGBjxwUCAQFDANOSUUwSAYIKwYBBQUHAQMEPDA6MAoGCCsGAQUFBwsCMAgGBgQAjkYBATALBgYEAI5GAQMCAQ8wFQYGBACORgECMAsTA0VVUgIBAwIBATALBgNVHQ8EBAMCBkAwIQYDVR0RBBowGIEWc2VydmljaW90ZWNuaWNvQGFuZi5lczCB6QYDVR0gBIHhMIHeMIHbBg0rBgEEAYGPHAMEAQIWMIHJMCkGCCsGAQUFBwIBFh1odHRwczovL3d3dy5hbmYuZXMvZG9jdW1lbnRvczCBmwYIKwYBBQUHAgIwgY4MgYtBbnRlcyBkZSBhY2VwdGFyIGVzdGUgY2VydGlmaWNhZG8sIGNvbXBydWViZTotQ29uZGljaW9uZXMsIGxpbWl0YWNpb25lcyB5IHVzb3MgYXV0b3JpemFkb3Mgc2Vnw7puIENQIGEgbGEgcXVlIHNlIHNvbWV0ZS4tRXN0YWRvIGRlIHZpZ2VuY2lhMIHIBgNVHQkEgcAwgb0wDgYDVQQRMQcTBTAwMDAwMBIGA1UEFDELEwk5OTk5OTk5OTkwHQYDVQQDMRYTFEZJU0lDTyBBQ1RJVk8gUFJVRUJBMA8GA1UEKjEIEwZGSVNJQ08wFgYDVQQEMQ8TDUFDVElWTyBQUlVFQkEwEgYDVQQFMQsTCTM4ODY0MTU5WDATBgNVBAIxDBMKMTAvMDMvMjAxNDASBgNVBAkxCxMJRElSRUNDSU9OMBIGA1UEEDELEwlESVJFQ0NJT04wDQYJKoZIhvcNAQELBQADggIBAFA5kvEZ6fQZBN_XgB6Hmt7-EW38DjduGqYlS2QynTf4QtxkvoAXJ1UJjxGAmMunlbmOVRSs8eNFTka0N8wtt2oYpWT1t9vzT4QQNp7qZc_RdoHpB7uD0_jBv4eGOYmb_LLOpMh9ihTSNfckMaKidIUfX_hBJu8EQZYEmAVFUcgsNteFHWF6guKTXK8x6utRp39_c6Hpum9wpKYjYTmqPKsM3TQQCkS5y4VIo0vOv2qfAZzFlRnfcoIjbA8bQWWhKAFW8uCSDg5TCeq8tFv6e_DZUyrJUDSHXqc2-laLm6m3DJMOqtWEappxHXjkAaj07J1f8Yjxt0Srz9FfGkPPkZIuU_KmS_caM-eJ8Nj5sEnWCRVnzS4yiPgbF92M8xkLqisE0mWU3OlTNF_GxaMntNDhVp-sDBUv800ttzha5zZeT1lVvJlHpd1s66e10559P8xkuJM7fS8zNlGhZ596u9OUo9GzGaLr3FyzhzN7VpwlGjQcA694FRy6htQrdCXQm6w_RY7GKm_xiuu042jnlMXY7aP55_NziZenvObD3Zj3Xld2ze7eI6-1fAfVSp5-NQnMYmkvKZ9ZinVaxGahDuw5QhtcwOGPLamGr0Qr5E7VBhEEEluFqvbtQ_B6o-jZfxM1b7IF6IqwM_MW-Pg_wdqRVbOfgqCtkg0FCHb0I7EF"; //$NON-NLS-1$

		final byte[] signResult = HttpSignProcess.sign(
				"spt",  // ID App//$NON-NLS-1$
				"1234", // ID Transaccion //$NON-NLS-1$
				"sign", // Operacion //$NON-NLS-1$
				PADES,  // Formato
				ALGRTH, // Algoritmo
				null,   // Parametros adicionales
				CERTIFICATE_TEST, // Certificado de firma en Base64
				Base64.encode(data, true), // Datos a firmar en Base 64
				Base64.encode(data, true), // Sesion trifasica
				null  // Formato de mejora
		);
		System.out.println("Resultado, fichero PDF firmado PAdES: " + Base64.encode(signResult, true)); //$NON-NLS-1$
	}

	/** Prueba de firma XAdES.
	 * @throws Exception En cualquier error. */
	@SuppressWarnings("static-method")
	@Test
	@Ignore
	public void testXAdES() throws Exception {

		final byte[] data = Utils.getDataFromInputStream(
			TestHttpSignProcessSign.class.getResourceAsStream("/xml_with_ids.xml") //$NON-NLS-1$
		);

		final String cert = "MIIKhTCCCG2gAwIBAgIIAivtj8K_w0AwDQYJKoZIhvcNAQELBQAwggEhMQswCQYDVQQGEwJFUzESMBAGA1UECAwJQmFyY2Vsb25hMVgwVgYDVQQHDE9CYXJjZWxvbmEgKHNlZSBjdXJyZW50IGFkZHJlc3MgYXQgaHR0cDovL3d3dy5hbmYuZXMvZXMvYWRkcmVzcy1kaXJlY2Npb24uaHRtbCApMScwJQYDVQQKDB5BTkYgQXV0b3JpZGFkIGRlIENlcnRpZmljYWNpb24xLjAsBgNVBAsMJUFORiBBdXRvcmlkYWQgSW50ZXJtZWRpYSBkZSBJZGVudGlkYWQxGjAYBgkqhkiG9w0BCQEWC2luZm9AYW5mLmVzMRIwEAYDVQQFEwlHNjMyODc1MTAxGzAZBgNVBAMMEkFORiBBc3N1cmVkIElEIENBMTAeFw0xNDAzMTEwOTE3NDZaFw0xNjAzMTAwOTE3NDZaMIIBDjE5MDcGA1UECwwwQ2VydGlmaWNhZG8gZGUgQ2xhc2UgMiBkZSBQZXJzb25hIEZpc2ljYSAoRklSTUEpMR0wGwYDVQQDDBRGSVNJQ08gQUNUSVZPIFBSVUVCQTEWMBQGA1UEBAwNQUNUSVZPIFBSVUVCQTEPMA0GA1UEKgwGRklTSUNPMRIwEAYDVQQFEwkzODg2NDE1OVgxJTAjBgkqhkiG9w0BCQEWFnNlcnZpY2lvdGVjbmljb0BhbmYuZXMxEjAQBgNVBAcMCVBPQkxBQ0lPTjESMBAGA1UECAwJUFJPVklOQ0lBMQswCQYDVQQGEwJlczEZMBcGCisGAQQBgZMWAQEMCTM4ODY0MTU5WDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAKlS0ytfsK6eZQMfpHQcAQhEc2jU4u3CkFPQAibIeIqZq3Xev5Y5edpFJlb1D93huT9yXzOe8bKpFtgKpncr6iwPbnuTQIl32fb271psJSPXpnQwgI98xZBhZLc6ApOJU-26xvnJOdSsljLsq0dbUw8q6mb4KnKSg4oqnbhoSantWrAfzXWcgzDmxeahdMkwTZn95ssYGT0HmT8wR_q55DDNDV_ENmGGLFJmLpEpxaS7rWTaL3m3BASjRWz_Lky_G0qdXTUMNu2JXzkdThlHbDR7JEg2Xcj0d6RBD_ri_QclY_7V6glYnQALnXsgt_Kd9mA3EFcTq2yPVy56lqIG7UMCAwEAAaOCBM4wggTKMBkGCisGAQQBgZMWAQEECwwJMzg4NjQxNTlYMB0GA1UdDgQWBBQqrVvOUzevn5VMiJzRz3fQby1aZzAJBgNVHRMEAjAAMDcGCCsGAQUFBwEBBCswKTAnBggrBgEFBQcwAYYbaHR0cDovL29jc3AuYW5mLmVzL3NwYWluL0FWMIIBhAYDVR0fBIIBezCCAXcwOqA4oDaGNGh0dHBzOi8vY3JsLmFuZi5lcy9jcmwvQU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwwOqA4oDaGNGh0dHBzOi8vd3d3LmFuZi5lcy9jcmwvQU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwwgfyggfmggfaGcWxkYXA6Ly9sZGFwLmFuZi5lczozODkvY249QU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1Ni5jcmwsb3U9QU5GX0Fzc3VyZWRfSURfQ0ExX1NIQTI1NixvdT1BTkZfR2xvYmFsX1Jvb3RfQ0EsZGM9YW5mpIGAMH4xEzARBgoJkiaJk_IsZAEZFgNhbmYxJjAkBgNVBAMMHUFORl9Bc3N1cmVkX0lEX0NBMV9TSEEyNTYuY3JsMSIwIAYDVQQLDBlBTkZfQXNzdXJlZF9JRF9DQTFfU0hBMjU2MRswGQYDVQQLDBJBTkZfR2xvYmFsX1Jvb3RfQ0EwHwYDVR0jBBgwFoAUNS4t5ikcv4vZSnyeK-CVR9HGTz0wEwYKKwYBBAGBjxwKCAQFDANOSUUwHAYJKwYBBAGBjxwTBA8MDTIwMTg2NS01MDMwMzMwKAYKKwYBBAGBjxwTAQQaDBgxMzIxNC0xNTY0Nzk4MTM3Nzc4Njc1ODQwEwYKKwYBBAGBjxwUCAQFDANOSUUwSAYIKwYBBQUHAQMEPDA6MAoGCCsGAQUFBwsCMAgGBgQAjkYBATALBgYEAI5GAQMCAQ8wFQYGBACORgECMAsTA0VVUgIBAwIBATALBgNVHQ8EBAMCBkAwIQYDVR0RBBowGIEWc2VydmljaW90ZWNuaWNvQGFuZi5lczCB6QYDVR0gBIHhMIHeMIHbBg0rBgEEAYGPHAMEAQIWMIHJMCkGCCsGAQUFBwIBFh1odHRwczovL3d3dy5hbmYuZXMvZG9jdW1lbnRvczCBmwYIKwYBBQUHAgIwgY4MgYtBbnRlcyBkZSBhY2VwdGFyIGVzdGUgY2VydGlmaWNhZG8sIGNvbXBydWViZTotQ29uZGljaW9uZXMsIGxpbWl0YWNpb25lcyB5IHVzb3MgYXV0b3JpemFkb3Mgc2Vnw7puIENQIGEgbGEgcXVlIHNlIHNvbWV0ZS4tRXN0YWRvIGRlIHZpZ2VuY2lhMIHIBgNVHQkEgcAwgb0wDgYDVQQRMQcTBTAwMDAwMBIGA1UEFDELEwk5OTk5OTk5OTkwHQYDVQQDMRYTFEZJU0lDTyBBQ1RJVk8gUFJVRUJBMA8GA1UEKjEIEwZGSVNJQ08wFgYDVQQEMQ8TDUFDVElWTyBQUlVFQkEwEgYDVQQFMQsTCTM4ODY0MTU5WDATBgNVBAIxDBMKMTAvMDMvMjAxNDASBgNVBAkxCxMJRElSRUNDSU9OMBIGA1UEEDELEwlESVJFQ0NJT04wDQYJKoZIhvcNAQELBQADggIBAFA5kvEZ6fQZBN_XgB6Hmt7-EW38DjduGqYlS2QynTf4QtxkvoAXJ1UJjxGAmMunlbmOVRSs8eNFTka0N8wtt2oYpWT1t9vzT4QQNp7qZc_RdoHpB7uD0_jBv4eGOYmb_LLOpMh9ihTSNfckMaKidIUfX_hBJu8EQZYEmAVFUcgsNteFHWF6guKTXK8x6utRp39_c6Hpum9wpKYjYTmqPKsM3TQQCkS5y4VIo0vOv2qfAZzFlRnfcoIjbA8bQWWhKAFW8uCSDg5TCeq8tFv6e_DZUyrJUDSHXqc2-laLm6m3DJMOqtWEappxHXjkAaj07J1f8Yjxt0Srz9FfGkPPkZIuU_KmS_caM-eJ8Nj5sEnWCRVnzS4yiPgbF92M8xkLqisE0mWU3OlTNF_GxaMntNDhVp-sDBUv800ttzha5zZeT1lVvJlHpd1s66e10559P8xkuJM7fS8zNlGhZ596u9OUo9GzGaLr3FyzhzN7VpwlGjQcA694FRy6htQrdCXQm6w_RY7GKm_xiuu042jnlMXY7aP55_NziZenvObD3Zj3Xld2ze7eI6-1fAfVSp5-NQnMYmkvKZ9ZinVaxGahDuw5QhtcwOGPLamGr0Qr5E7VBhEEEluFqvbtQ_B6o-jZfxM1b7IF6IqwM_MW-Pg_wdqRVbOfgqCtkg0FCHb0I7EF"; //$NON-NLS-1$

		final String algo = "SHA512withRSA"; //$NON-NLS-1$
		final String xades = "XAdES"; //$NON-NLS-1$

		final byte[] signResult = HttpSignProcess.sign(
			"spt",  // AppID          //$NON-NLS-1$
			"1234", // Id Transaccion //$NON-NLS-1$
			"sign", //$NON-NLS-1$
			xades,
			algo,
			null,   // Parametros adicionales
			cert,
			Base64.encode(data, true),
			null,   // Sesion trifasica
			"ES-A"  // Formato de mejora //$NON-NLS-1$
		);
		System.out.println("Resultado, fichero XML firmado XAdES: " + new String(signResult)); //$NON-NLS-1$
	}

}
