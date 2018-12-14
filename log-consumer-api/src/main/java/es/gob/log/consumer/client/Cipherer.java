package es.gob.log.consumer.client;

import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Clase para el cifrado del token de inicio de sesi&oacute;n.
 */
class Cipherer {

	private static final String CIPHER_CONFIG = "AES/CBC/PKCS5PADDING"; //$NON-NLS-1$

	private static final String CIPHER_ALGORITHM = "AES"; //$NON-NLS-1$

	static byte[] cipher(final byte[] data, final byte[] key, final byte[] iv)
			throws	GeneralSecurityException {

		final SecretKeySpec secretKey = new SecretKeySpec(key, CIPHER_ALGORITHM);
		final Cipher cipher = Cipher.getInstance(CIPHER_CONFIG);
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

		return cipher.doFinal(data);
	}

//	public static void main(final String[] args) throws NoSuchAlgorithmException {
//		final KeyGenerator keyGen = KeyGenerator.getInstance(CIPHER_ALGORITHM);
//        keyGen.init(128);
//        final SecretKey secretKey = keyGen.generateKey();
//
//        System.out.println(Base64.encode(secretKey.getEncoded()));
//	}
}
