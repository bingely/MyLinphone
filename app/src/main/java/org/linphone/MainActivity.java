package org.linphone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.mediastream.Log;
import org.linphone.ui.AddressText;

import static android.content.Intent.ACTION_MAIN;

public class MainActivity extends AppCompatActivity {
    private static final int CALL_ACTIVITY = 19;
    private Button mDial;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 得拿到对方的号码

        //initView();
        init();

        mDial = (Button) findViewById(R.id.dial);
        mDial.setOnClickListener(dialListener);

    }

    private void init() {
        if (LinphoneService.isReady()) {
            // 可以做拨打电话的操作
        } else {
            // start linphone as background
            startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
        }

        if (!LinphoneManager.isInstanciated()) {
            Log.e("No service running: avoid crash by starting the launcher", this.getClass().getName());
            // LinphoneManager需要做初始化

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
}
