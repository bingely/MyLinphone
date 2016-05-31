package org.linphone;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import static android.content.Intent.ACTION_MAIN;

public class SplashActivity extends Activity {
    private Handler mHandler;
    private ServiceWaitThread mThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mHandler = new Handler();

        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            startService(new Intent(ACTION_MAIN).setClass(this, LinphoneService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    protected void onServiceReady() {
        final Class<? extends Activity> classToStart;
        classToStart = MainActivity.class;
        LinphoneService.instance().setActivityToLaunchOnIncomingReceived(classToStart);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent().setClass(SplashActivity.this, classToStart).setData(getIntent().getData()));
                finish();
            }
        }, 1000);
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }
}
