package missive.common.crypto;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface CryptoService {
    // AES
    byte[] encryptAES(byte[] data, SecretKey key, byte[] iv) throws Exception;
    byte[] decryptAES(byte[] data, SecretKey key, byte[] iv) throws Exception;
    SecretKey generateAESKey() throws Exception;
    byte[] generateIV();

    // RSA
    KeyPair generateRSAKeyPair() throws Exception;
    byte[] encryptRSA(byte[] data, PublicKey key) throws Exception;
    byte[] decryptRSA(byte[] data, PrivateKey key) throws Exception;

    // encoding helpers
    String toBase64(byte[] data);
    byte[] fromBase64(String data);
}
