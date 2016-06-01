package org.linphone;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.Toast;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreListenerBase;
import org.linphone.mediastream.Log;

public class IncallAct extends Activity {
    private boolean isMicMuted = false, isSpeakerEnabled = false;
    private LinphoneCoreListenerBase mListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incall);

        mListener = new LinphoneCoreListenerBase(){
            @Override
            public void callState(LinphoneCore lc, LinphoneCall call, LinphoneCall.State state, String message) {
                if (LinphoneManager.getLc().getCallsNb() == 0) {
                    finish();
                    return;
                }
                refreshCallList();
            }
        };
    }

    public void refreshCallList() {
        for (LinphoneCall call : LinphoneManager.getLc().getCalls()) {
            registerCallDurationTimer(call);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        LinphoneCore lc = LinphoneManager.getLcIfManagerNotDestroyedOrNull();
        if (lc != null) {
            lc.addListener(mListener);
        }
    }


    private void registerCallDurationTimer(LinphoneCall call) {
        int callDuration = call.getDuration();
        if (callDuration == 0 && call.getState() != LinphoneCall.State.StreamsRunning) {
            return;
        }

        Chronometer timer = (Chronometer) findViewById(R.id.callTimer);
        if (timer == null) {
            throw new IllegalArgumentException("no callee_duration view found");
        }

        timer.setBase(SystemClock.elapsedRealtime() - 1000 * callDuration);
        timer.start();
    }

    public void toggleMicro(View view) {
        LinphoneCore lc = LinphoneManager.getLc();
        isMicMuted = !isMicMuted;
        lc.muteMic(isMicMuted);
        if (isMicMuted) {
            //micro.setBackgroundResource(R.drawable.micro_off);
        } else {
            //micro.setBackgroundResource(R.drawable.micro_on);
        }
    }

    public void hangUp(View view) {
        Toast.makeText(this, "handup", Toast.LENGTH_SHORT).show();
        LinphoneCore lc = LinphoneManager.getLc();
        LinphoneCall currentCall = lc.getCurrentCall();

        if (currentCall != null) {
            lc.terminateCall(currentCall);
        } else if (lc.isInConference()) {
            lc.terminateConference();
        } else {
            lc.terminateAllCalls();
        }
    }

    public void toggleSpeaker(View view) {
        // 从能到不能的转换
        isSpeakerEnabled = !isSpeakerEnabled;
        if (isSpeakerEnabled) {
            LinphoneManager.getInstance().routeAudioToSpeaker();
            // 切换按钮图片背景 TODO
            //speaker.setBackgroundResource(R.drawable.speaker_on);
            LinphoneManager.getLc().enableSpeaker(isSpeakerEnabled);
        } else {
            Log.d("Toggle speaker off, routing back to earpiece");
            LinphoneManager.getInstance().routeAudioToReceiver();
            //speaker.setBackgroundResource(R.drawable.speaker_off);
        }
    }
}
