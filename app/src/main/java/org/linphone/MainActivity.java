package org.linphone;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.ui.AddressText;

public class MainActivity extends AppCompatActivity {
    private static final int CALL_ACTIVITY = 19;
    private Button mDial;
    private Button mDial_telphone;
    private boolean accountCreated = false;
    private LinphoneAddress address;

    private LinphonePreferences mPrefs;
    private LinphoneCoreListenerBase mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // 得拿到对方的号码

        //initView();
        init();

        mDial_telphone = (Button)findViewById(R.id.dial_telphone);
        mDial_telphone.setOnClickListener(dialListener);
        mDial = (Button) findViewById(R.id.dial);
        mDial.setOnClickListener(dialListener);

    }

    @Override
    protected void onResume() {
        super.onResume();
        /*LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }*/
    }

    private void init() {
        if (!LinphoneManager.isInstanciated()) {
            Log.e("No service running: avoid crash by starting the launcher", this.getClass().getName());
            // super.onCreate called earlier
            finish();
            startActivity(getIntent().setClass(this, SplashActivity.class));
            return;
        }
        mPrefs = LinphonePreferences.instance();
        initTelephoneLorgin();

        mListener = new LinphoneCoreListenerBase(){
            @Override
            public void registrationState(LinphoneCore lc, LinphoneProxyConfig cfg, LinphoneCore.RegistrationState state, String smessage) {
                if(accountCreated){
                    if(address != null && address.asString().equals(cfg.getIdentity()) ) {
                        if (state == LinphoneCore.RegistrationState.RegistrationOk) {
                            if (LinphoneManager.getLc().getDefaultProxyConfig() != null) {
                                launchEchoCancellerCalibration(true);
                            }
                        } else if (state == LinphoneCore.RegistrationState.RegistrationFailed) {
                            Toast.makeText(MainActivity.this, getString(R.string.first_launch_bad_login_password), Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        };
    }


    private void initTelephoneLorgin() {
        saveCreatedAccount("803", "803", "10.1.20.210:5363");

        if (LinphoneManager.getLc().getDefaultProxyConfig() != null) {
            launchEchoCancellerCalibration(false);
        }
    }



    private View.OnClickListener dialListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
            if (lc != null) {
                LinphoneProxyConfig lpc = lc.getDefaultProxyConfig();
                String to;
                if (lpc != null) {
                    String address = "13537688026";
                    if (address.contains("@")) {
                        to = lpc.normalizePhoneNumber(address.split("@")[0]);
                    } else {
                        to = lpc.normalizePhoneNumber(address);
                    }
                } else {
                    to = "bingley666@sip.linphone.org";
                }

                LinphoneManager.AddressType address = new AddressText(MainActivity.this, null);
                address.setText(to);
                LinphoneManager.getInstance().newOutgoingCall(address);
            }
        }
    };

    public void saveCreatedAccount(String username, String password, String domain) {
      /*  if (accountCreated)
            return;*/

        String identity = "sip:" + username + "@" + domain;
        try {
            address = LinphoneCoreFactory.instance().createLinphoneAddress(identity);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
        boolean isMainAccountLinphoneDotOrg = domain.equals(getString(R.string.default_domain));
        boolean useLinphoneDotOrgCustomPorts = getResources().getBoolean(R.bool.use_linphone_server_ports);
        LinphonePreferences.AccountBuilder builder = new LinphonePreferences.AccountBuilder(LinphoneManager.getLc())
                .setUsername(username)
                .setDomain(domain)
                .setPassword(password);

        if (isMainAccountLinphoneDotOrg && useLinphoneDotOrgCustomPorts) {
            if (getResources().getBoolean(R.bool.disable_all_security_features_for_markets)) {
                builder.setProxy(domain + ":5228")
                        .setTransport(LinphoneAddress.TransportType.LinphoneTransportTcp);
            }
            else {
                builder.setProxy(domain + ":5223")
                        .setTransport(LinphoneAddress.TransportType.LinphoneTransportTls);
            }

            builder.setExpires("604800")
                    .setOutboundProxyEnabled(true)
                    .setAvpfEnabled(true)
                    .setAvpfRRInterval(3)
                    .setQualityReportingCollector("sip:voip-metrics@sip.linphone.org")
                    .setQualityReportingEnabled(true)
                    .setQualityReportingInterval(180)
                    .setRealm("sip.linphone.org");


            mPrefs.setStunServer(getString(R.string.default_stun));
            mPrefs.setIceEnabled(true);
        } else {
            String forcedProxy = getResources().getString(R.string.setup_forced_proxy);
            if (!TextUtils.isEmpty(forcedProxy)) {
                builder.setProxy(forcedProxy)
                        .setOutboundProxyEnabled(true)
                        .setAvpfRRInterval(5);
            }
        }

        if (getResources().getBoolean(R.bool.enable_push_id)) {
            String regId = mPrefs.getPushNotificationRegistrationID();
            String appId = getString(R.string.push_sender_id);
            if (regId != null && mPrefs.isPushNotificationEnabled()) {
                String contactInfos = "app-id=" + appId + ";pn-type=google;pn-tok=" + regId;
                builder.setContactParameters(contactInfos);
            }
        }

        try {
            builder.saveNewAccount();
            accountCreated = true;
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    private void launchEchoCancellerCalibration(boolean sendEcCalibrationResult) {
        /*boolean needsEchoCalibration = LinphoneManager.getLc().needsEchoCalibration();
        if (needsEchoCalibration && mPrefs.isFirstLaunch()) {
            mPrefs.setAccountEnabled(mPrefs.getAccountCount() - 1, false); //We'll enable it after the echo calibration
            EchoCancellerCalibrationFragment fragment = new EchoCancellerCalibrationFragment();
            fragment.enableEcCalibrationResultSending(sendEcCalibrationResult);
            changeFragment(fragment);
            currentFragment = SetupFragmentsEnum.ECHO_CANCELLER_CALIBRATION;
            back.setVisibility(View.VISIBLE);
            next.setVisibility(View.GONE);
            next.setEnabled(false);
            cancel.setEnabled(false);
        } else {
            if (mPrefs.isFirstLaunch()) {
                mPrefs.setEchoCancellation(LinphoneManager.getLc().needsEchoCanceler());
            }
            success();
        }*/

        if (mPrefs.isFirstLaunch()) {
            mPrefs.setEchoCancellation(LinphoneManager.getLc().needsEchoCanceler());
        }
        success();
    }

    public void success() {
        mPrefs.firstLaunchSuccessful();
        /*setResult(Activity.RESULT_OK);
        finish();*/
    }
}
