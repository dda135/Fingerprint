package fanjh.mine.fingerprintdemo;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.RequiresApi;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;

import java.security.KeyStore;
import java.security.KeyStoreException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

/**
* @author fanjh
* @date 2018/2/28 9:12
* @description 指纹识别辅助类
* @note
**/
@RequiresApi(Build.VERSION_CODES.M)
public class FingerprintHelper {
    private Context context;
    private FingerprintCipher fingerprintCipher;
    private CancellationSignal cancellationSignal;
    private boolean cancel;

    public FingerprintHelper(Context context) {
        this.context = context.getApplicationContext();
        fingerprintCipher = new FingerprintCipher();
    }

    /**
     * 校验当前是否能够使用系统的指纹识别
     * @return true可以
     */
    public boolean canUseFingerprint(){
        KeyguardManager keyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if(null == keyguardManager){
            return false;
        }
        if(!keyguardManager.isKeyguardSecure()){
            return false;
        }
        FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
        return fingerprintManagerCompat.isHardwareDetected() && fingerprintManagerCompat.hasEnrolledFingerprints();
    }

    public void auth(FingerprintManagerCompat.AuthenticationCallback callback){
        FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
        Cipher cipher = fingerprintCipher.createFingerprintCipher();
        FingerprintManagerCompat.CryptoObject cryptoObject = new FingerprintManagerCompat.CryptoObject(cipher);
        if(null == cancellationSignal){
            cancellationSignal = new CancellationSignal();
        }
        cancel = false;
        fingerprintManagerCompat.authenticate(cryptoObject,0,cancellationSignal,callback,null);
    }

    public boolean isAuthing(){
        return null != cancellationSignal;
    }

    public void cancelAuth(){
        if(null != cancellationSignal) {
            cancel = true;
            cancellationSignal.cancel();
            cancellationSignal = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private class FingerprintCipher{
        private static final String ALIAS = "fanjh.fingerprint.cipher";
        private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
        private static final String AES = KeyProperties.KEY_ALGORITHM_AES;
        private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
        private static final String ENCRYPTION_PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
        private KeyStore keyStore;

        public FingerprintCipher() {
            try {
                keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }

        /**
         * 创建指纹识别所需要的加解密套件
         * @return 加解密套件
         */
        Cipher createFingerprintCipher() throws IllegalArgumentException{
            try {
                generateKeyStore(ALIAS);
                Cipher cipher = Cipher.getInstance(AES + "/" + BLOCK_MODE + "/" + ENCRYPTION_PADDING);
                keyStore.load(null);
                SecretKey secretKey = (SecretKey) keyStore.getKey(ALIAS,null);
                if(null == secretKey){
                    throw new IllegalArgumentException("加解密套件生成失败！");
                }
                cipher.init(Cipher.ENCRYPT_MODE,secretKey);
                return cipher;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private void generateKeyStore(String alias) throws Exception{
            keyStore.load(null);
            KeyGenerator keyGenerator = KeyGenerator.getInstance(AES,ANDROID_KEYSTORE);
            int purpose = KeyProperties.PURPOSE_DECRYPT | KeyProperties.PURPOSE_ENCRYPT;
            KeyGenParameterSpec.Builder builder = new KeyGenParameterSpec.Builder(alias,purpose).
                    setUserAuthenticationRequired(true).
                    setBlockModes(BLOCK_MODE).
                    setEncryptionPaddings(ENCRYPTION_PADDING).
                    setKeySize(256);
            keyGenerator.init(builder.build());
            keyGenerator.generateKey();
        }

    }

}
