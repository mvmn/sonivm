package x.mvmn.sonivm.util;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.experimental.UtilityClass;

@UtilityClass
public class EncryptionUtil {

	private static final String CIPHER_NAME_AES = "AES";
	private static final String CIPHER_SPEC_AES_GCM_NO_PADDING = CIPHER_NAME_AES + "/GCM/NoPadding";
	public static final int AES_KEY_SIZE = 128;
	public static final int GCM_INITVECTOR_LENGTH = 12;
	public static final int GCM_TAG_LENGTH = 16;

	public static class KeyAndNonce {
		private final SecretKey key;
		private final byte[] nonce;

		public KeyAndNonce(SecretKey key, byte[] nonce) {
			this.key = key;
			this.nonce = nonce;
		}

		public SecretKey getKey() {
			return key;
		}

		public byte[] getNonce() {
			return nonce;
		}

		public String serialize() {
			Encoder base64enc = Base64.getEncoder();
			return base64enc.encodeToString(nonce) + ":" + base64enc.encodeToString(key.getEncoded());
		}

		public static KeyAndNonce deserialize(String value) {
			String[] parts = value.split(":");
			if (parts.length != 2) {
				throw new IllegalArgumentException("Invalid value");
			}
			Decoder base64dec = Base64.getDecoder();
			return new KeyAndNonce(new SecretKeySpec(base64dec.decode(parts[1]), CIPHER_NAME_AES), base64dec.decode(parts[0]));
		}
	}

	public static KeyAndNonce generateKeyAndNonce() throws NoSuchAlgorithmException {
		KeyGenerator keyGenerator = KeyGenerator.getInstance(CIPHER_NAME_AES);
		keyGenerator.init(AES_KEY_SIZE);
		SecretKey key = keyGenerator.generateKey();

		byte[] nonce = new byte[GCM_INITVECTOR_LENGTH];
		new SecureRandom().nextBytes(nonce);

		return new KeyAndNonce(key, nonce);
	}

	public static String encrypt(String value, KeyAndNonce keyAndNonce) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance(CIPHER_SPEC_AES_GCM_NO_PADDING);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, keyAndNonce.getNonce());
		cipher.init(Cipher.ENCRYPT_MODE, keyAndNonce.getKey(), gcmParameterSpec);
		byte[] cipherText = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

		return Base64.getEncoder().encodeToString(cipherText);
	}

	public static String decrypt(String encryptedValue, KeyAndNonce keyAndNonce) throws GeneralSecurityException {
		Cipher cipher = Cipher.getInstance(CIPHER_SPEC_AES_GCM_NO_PADDING);
		GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, keyAndNonce.getNonce());
		cipher.init(Cipher.DECRYPT_MODE, keyAndNonce.getKey(), gcmParameterSpec);
		byte[] decryptedText = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
		return new String(decryptedText, StandardCharsets.UTF_8);
	}
}
