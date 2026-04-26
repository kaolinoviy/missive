package missive.common.crypto;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.security.spec.*;
import java.util.Base64;

// manages local RSA key pair persistence and E2E operations
public class CryptoManager {

    private final CryptoService crypto = new CryptoImpl();
    private final Path keyDir;
    private KeyPair keyPair;

    public CryptoManager() {
        keyDir = Paths.get(System.getProperty("user.home"), ".missive", "keys");
    }

    public void init() throws Exception {
        Files.createDirectories(keyDir);
        Path privPath = keyDir.resolve("private.key");
        Path pubPath = keyDir.resolve("public.key");

        if (Files.exists(privPath) && Files.exists(pubPath)) {
            loadKeyPair(privPath, pubPath);
        } else {
            keyPair = crypto.generateRSAKeyPair();
            saveKeyPair(privPath, pubPath);
        }
    }

    private void saveKeyPair(Path privPath, Path pubPath) throws IOException {
        Files.writeString(privPath, Base64.getEncoder().encodeToString(
                keyPair.getPrivate().getEncoded()));
        Files.writeString(pubPath, Base64.getEncoder().encodeToString(
                keyPair.getPublic().getEncoded()));
    }

    private void loadKeyPair(Path privPath, Path pubPath) throws Exception {
        byte[] privBytes = Base64.getDecoder().decode(Files.readString(privPath).trim());
        byte[] pubBytes  = Base64.getDecoder().decode(Files.readString(pubPath).trim());

        KeyFactory kf = KeyFactory.getInstance("RSA");
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        PublicKey  pub  = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        keyPair = new KeyPair(pub, priv);
    }

    // encrypt a message for a given recipient public key (hybrid RSA+AES)
    public EncryptedMessage encrypt(String plaintext, PublicKey recipientPublicKey) throws Exception {
        byte[] iv = crypto.generateIV();
        SecretKey aesKey = crypto.generateAESKey();

        byte[] ciphertext = crypto.encryptAES(plaintext.getBytes("UTF-8"), aesKey, iv);
        byte[] encryptedKey = crypto.encryptRSA(aesKey.getEncoded(), recipientPublicKey);

        return new EncryptedMessage(
                Base64.getEncoder().encodeToString(ciphertext),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(encryptedKey)
        );
    }

    public String decrypt(EncryptedMessage msg) throws Exception {
        byte[] encryptedKey = Base64.getDecoder().decode(msg.encryptedKey);
        byte[] rawKey = crypto.decryptRSA(encryptedKey, keyPair.getPrivate());
        SecretKey aesKey = new SecretKeySpec(rawKey, "AES");

        byte[] iv = Base64.getDecoder().decode(msg.iv);
        byte[] ciphertext = Base64.getDecoder().decode(msg.ciphertext);
        byte[] plain = crypto.decryptAES(ciphertext, aesKey, iv);
        return new String(plain, "UTF-8");
    }

    public String getPublicKeyBase64() {
        return Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
    }

    public PublicKey publicKeyFromBase64(String b64) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(b64);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public CryptoService getCrypto() { return crypto; }

    public static class EncryptedMessage {
        public final String ciphertext;
        public final String iv;
        public final String encryptedKey;

        public EncryptedMessage(String ciphertext, String iv, String encryptedKey) {
            this.ciphertext = ciphertext;
            this.iv = iv;
            this.encryptedKey = encryptedKey;
        }
    }
}
