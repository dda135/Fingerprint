# 系统指纹识别
从Android23开始，Android提供了官方的指纹识别API，从而开发者能够在设备允许的条件之下进行对应的操作<br>
## 思路
（1）需要判断当前设备的运行API是否大于23<br>
（2）需要判断当前设备是否支持指纹识别并且当前设备是否已经录入有指纹，如果都ok，那么可以进行指纹识别<br>
（3）为了安全期间，onPause中要取消进行中的指纹识别操作
## 例子
判断当前设备是否支持指纹识别以及是否已经有录入指纹
```
    FingerprintManagerCompat fingerprintManagerCompat = FingerprintManagerCompat.from(context);
    return fingerprintManagerCompat.isHardwareDetected() && fingerprintManagerCompat.hasEnrolledFingerprints();
```
进行认证
```
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
```
可以看到，实际上就是创建一个密钥，然后通过系统API调用就可以了，后续只需要关心对应的回调即可，看一下密钥的生成
```
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
```
其实就是根据给定的ALIAS和算法生成一个加解密套件保存在Android指定的目录里面，然后在使用的时候读取出来即可，然后可以对本地数据做加解密之类的处理
