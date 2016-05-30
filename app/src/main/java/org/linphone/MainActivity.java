package org.linphone;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Toast;

import org.linphone.core.LinphoneCall;
import org.linphone.mediastream.Log;

import static android.content.Intent.ACTION_MAIN;

public class MainActivity extends AppCompatActivity {
    private static final int CALL_ACTIVITY = 19;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 得拿到对方的号码

        //initView();
        init();

        findViewById(R.id.dial).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "hello", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, InMyCallActivity.class));
            }
        });

    }

    private void initView() {
        findViewById(R.id.dial).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               /* LinphoneCore lc = LinphoneManager.getLc();
                *//*if (lc.getCurrentCall() == null) {
                    return;
                }*//*
                // 向指定的电话打电话
                lc.transferCall(lc.getCurrentCall(), "13537688026");
                LinphoneActivity.instance().resetClassicMenuLayoutAndGoBackToCallIfStillRunning();


                if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() > 0) {
                    LinphoneCall call = LinphoneManager.getLc().getCalls()[0];
                    if (call.getState() == LinphoneCall.State.IncomingReceived) {
                        startActivity(new Intent(MainActivity.this, IncomingCallActivity.class));
                    } else {
                        startIncallActivity(call);
                    }
                }*/

                startActivity(new Intent(MainActivity.this,InCallActivity.class));
            }
        });
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

    private void startIncallActivity(LinphoneCall call) {
        Intent intent = new Intent(this, InCallActivity.class);
        /*intent.putExtra("VideoEnabled", true);
        startActivityForResult(intent, CALL_ACTIVITY);*/
    }

}
