package fanjh.mine.fingerprintdemo;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    public static final int REQUEST_CODE_FINGERPRINT = 1;
    private Context context;
    private Button btnAuth;
    private Dialog authDialog;
    private FingerprintHelper fingerprintHelper;
    private AuthCallback authCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        btnAuth = findViewById(R.id.btn_auth);
        btnAuth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT)) {
                        startFingerAuth();
                    } else {
                        ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.USE_FINGERPRINT}, REQUEST_CODE_FINGERPRINT);
                    }
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_FINGERPRINT:
                if (PackageManager.PERMISSION_GRANTED == grantResults[0]) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        startFingerAuth();
                    }
                } else {
                    Toast.makeText(context, "请打开指纹校验权限！", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void startFingerAuth() {
        if (null == fingerprintHelper) {
            fingerprintHelper = new FingerprintHelper(context);
        }
        if (!fingerprintHelper.canUseFingerprint()) {
            Toast.makeText(context, "当前机型不允许使用指纹识别！", Toast.LENGTH_SHORT).show();
            return;
        }
        if (null == authCallback) {
            authCallback = new AuthCallback();
        }
        fingerprintHelper.auth(authCallback);
        showAuthDialog();
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private void showAuthDialog() {
        authDialog = new AlertDialog.Builder(context).
                setTitle("提示").
                setMessage("请根据手机情况进行指纹认证......").
                setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        fingerprintHelper.cancelAuth();
                    }
                }).setCancelable(false).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                dialog.dismiss();
                fingerprintHelper.cancelAuth();
            }
        }).create();
        authDialog.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (null != fingerprintHelper) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (fingerprintHelper.isAuthing()) {
                    fingerprintHelper.auth(authCallback);
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (null != fingerprintHelper) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                fingerprintHelper.cancelAuth();
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private class AuthCallback extends FingerprintManagerCompat.AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            super.onAuthenticationError(errMsgId, errString);
            fingerprintHelper.cancelAuth();
            authDialog.dismiss();
            Toast.makeText(context, "认证失败\n" + errString, Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
            super.onAuthenticationSucceeded(result);
            fingerprintHelper.cancelAuth();
            authDialog.dismiss();
            Toast.makeText(context, "认证成功！", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(context,SuccessActivity.class);
            context.startActivity(intent);
        }

        @Override
        public void onAuthenticationFailed() {
            super.onAuthenticationFailed();
            fingerprintHelper.cancelAuth();
            authDialog.dismiss();
            Toast.makeText(context, "认证失败，请重试！", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            super.onAuthenticationHelp(helpMsgId, helpString);
            Toast.makeText(context, helpString, Toast.LENGTH_SHORT).show();
        }
    }

}
