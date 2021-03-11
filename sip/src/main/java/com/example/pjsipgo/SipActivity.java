package com.example.pjsipgo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import net.gotev.sipservice.BroadcastEventReceiver;
import net.gotev.sipservice.Logger;
import net.gotev.sipservice.SipAccountData;
import net.gotev.sipservice.SipServiceCommand;

import org.pjsip.pjsua2.pjsip_status_code;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

public class SipActivity extends AppCompatActivity {
    private static final String TAG = "SipActivity";

    @BindView(R.id.etCallNumer)
    EditText mEtCallNumer;
    @BindView(R.id.layoutCallOut)
    LinearLayout mLayoutCallOut;

    private SipAccountData mAccount;
    private String mAccountId;

    private static final int REQUEST_PERMISSIONS_STORAGE = 0x100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sip);
        ButterKnife.bind(this);
        mReceiver.register(this);
        Logger.setLogLevel(Logger.LogLevel.DEBUG);
        requestPermissions();
    }


    public void login() {
        mAccount = new SipAccountData();
        mAccount.setHost("x.x.x.x");
        mAccount.setRealm("*"); //realm：sip:1004@192.168.2.243
        mAccount.setPort(5060);
        mAccount.setUsername("123456");
        mAccount.setPassword("yourpassword");
        mAccountId = SipServiceCommand.setAccount(this, mAccount);
        Log.i(TAG, "login: " + mAccountId);
    }


    public void audioCall(View view) {



        requestPermissions();
        String callNumber = mEtCallNumer.getText().toString().trim();
        if (TextUtils.isEmpty(callNumber)) {
            Toast.makeText(this, "Please enter the number！", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            SipServiceCommand.makeCall(this, mAccountId, callNumber, false, false);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Account error", Toast.LENGTH_SHORT).show();
        }
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
        };
        if (!checkPermissionAllGranted(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS_STORAGE);
        } else {
            if (mAccount == null) login();
        }
    }

    /**
     *
     * @param permissions
     * @return
     */
    protected boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {

                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS_STORAGE) {
            boolean ok = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    ok = false;
                }
            }
            if (ok) {
                Toast.makeText(SipActivity.this, "Permission application is successful！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mReceiver.unregister(this);
        if (mAccount != null) {
            SipServiceCommand.removeAccount(this, mAccountId);
        }
    }

    public BroadcastEventReceiver mReceiver = new BroadcastEventReceiver() {

        @Override
        public void onRegistration(String accountID, pjsip_status_code registrationStateCode) {
            super.onRegistration(accountID, registrationStateCode);
            Log.i(TAG, "onRegistration: ");
            if (registrationStateCode == pjsip_status_code.PJSIP_SC_OK) {
                Toast.makeText(receiverContext, "Login successfully, account：" + accountID, Toast.LENGTH_SHORT).show();
                mLayoutCallOut.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(receiverContext, "Login failed，code：" + registrationStateCode, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onIncomingCall(String accountID, int callID, String displayName, String remoteUri, boolean isVideo) {
            super.onIncomingCall(accountID, callID, displayName, remoteUri, isVideo);
            SipCallActivity.startActivityIn(getReceiverContext(), accountID, callID, displayName, remoteUri, isVideo);
        }

        @Override
        public void onOutgoingCall(String accountID, int callID, String number, boolean isVideo, boolean isVideoConference) {
            super.onOutgoingCall(accountID, callID, number, isVideo, isVideoConference);
            SipCallActivity.startActivityOut(getReceiverContext(), accountID, callID, number, isVideo, isVideoConference);
        }
    };
}