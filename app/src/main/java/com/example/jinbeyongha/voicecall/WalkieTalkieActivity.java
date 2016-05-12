package com.example.jinbeyongha.voicecall;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.content.Context;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.*;
import android.net.sip.*;

import java.text.ParseException;

public class WalkieTalkieActivity extends Activity implements View.OnTouchListener {
    public String sipAddress = null;
    public SipManager manager = null;
    public SipProfile me = null;
    public SipAudioCall call = null;
    public IncomingCallReceiver callReceiver;

    private static final int CALL_ADDRESS = 1;
    private static final int SET_AUTH_INFO = 2;
    private static final int UPDATE_SETTING_DIALOG = 3;
    private static final int HANG_UP = 4;

    //여기요 경찰아져!!
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.walkietalkie);

        ToggleButton pushToTalkButton = (ToggleButton) findViewById(R.id.pushToTalk);
        pushToTalkButton.setOnTouchListener(this);

        // 인텐트 필터를 설정함 incomingCallReceiver를 작동할 때 사용됨
        IntentFilter filter = new IntentFilter();
        //  여기요 아저씨!!!!!
        filter.addAction("android.VoiceCall.INCOMING_CALL");
        callReceiver = new IncomingCallReceiver();
        this.registerReceiver(callReceiver, filter);

        // "push to talk"는 스크린이 꺼지면 안된다 그것을 방지함
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        initializeManager();
    }
    @Override
    public void onStart() {
        super.onStart();
        initializeManager();
    }
    @Override
    public void onResume() {
        super.onResume();
        //initializeLocalProfile();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if( call != null ) {
            call.close();
        }

        closeLocalProfile();

        if( callReceiver != null ) {
            this.unregisterReceiver(callReceiver);
        }
    }
    public void initializeManager() {
        if(manager == null) {
            manager = SipManager.newInstance(this);
        }

        initializeLocalProfile();
    }
    public void initializeLocalProfile() {
        if(manager == null) {
            return;
        }

        if(me != null) {
            closeLocalProfile();
        }
        //sharedPreferences: 문자열 저장은 DB인데 간단한 저장은 SharedPreferences가 해줄 수 있음
        // 추후 DB와 연동시키면 됨
        // 관련자료 http://arabiannight.tistory.com/entry/%EC%95%88%EB%93%9C%EB%A1%9C%EC%9D%B4%EB%93%9CAndroid-SharedPreferences-%EC%82%AC%EC%9A%A9-%EC%98%88%EC%A0%9C
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String username = prefs.getString("namePref", "");
        String domain = prefs.getString("domainPref", "");
        String password = prefs.getString("passPref", "");

        if( username.length() == 0 || domain.length() == 0 || password.length() == 0) {
            showDialog(UPDATE_SETTING_DIALOG);
            return;
        }

        try {
            SipProfile.Builder builder = new SipProfile.Builder(username, domain);
            builder.setPassword(password);
            me = builder.build();

            /*Toast toast = Toast.makeText(getApplicationContext(), me.getUserName(), Toast.LENGTH_LONG);
            toast.show();
*/
            SipRegistrationListener Siplistener = new SipRegistrationListener() {
                public void onRegistering(String localProfileUri) {
                    updateStatus("Registering with SIP Server...");
                }

                public void onRegistrationDone(String localProfileUri, long expiryTime) {
                    updateStatus("Ready");
                }

                public void onRegistrationFailed(String localProfileUri, int errorCode, String errorMessage) {
                    updateStatus("Registration failed. Please Check setting");
                }
            };



            Intent i = new Intent();
            i.setAction("android.VoiceCall.INCOMING_CALL");
            PendingIntent pi = PendingIntent.getBroadcast(this, 0, i, Intent.FILL_IN_DATA);

            Toast toast = Toast.makeText(getApplicationContext(), pi.toString(), Toast.LENGTH_LONG);
            toast.show();


            manager.open(me, pi, Siplistener);



            //manager.open(me);


            if(manager.isOpened(me.getUriString())) {
                Toast toast1 = Toast.makeText(getApplicationContext(), "not opened", Toast.LENGTH_LONG);
                toast1.show();
            }

            // listener는 manager가 open된 이후에 call되야됨. 아니면 오작동할 우려가 있음





            manager.register(me, 30, Siplistener);

            manager.setRegistrationListener(me.getUriString(), Siplistener);


            Toast toast2 = Toast.makeText(getApplicationContext(), "very well", Toast.LENGTH_LONG);
            toast2.show();


        } catch (ParseException pe) {
            updateStatus("Connecting Error.(ParseException)");
        } catch (SipException se) {
            Toast toast = Toast.makeText(getApplicationContext(), se.getMessage(), Toast.LENGTH_LONG);
            toast.show();

            updateStatus("connecting error.(SipException)");
        }
    }
    public void closeLocalProfile() {
        if (manager == null) { return; }

        try {
            if( me != null) {
                manager.close(me.getUriString());
            }
        } catch (Exception ee) {
            Log.d("WalkieTalkieActivity/onDestroy", "Failed to close local profile.", ee);
        }
    }
    public void initiateCall() {
        updateStatus(sipAddress);

        try {
            SipAudioCall.Listener listener = new SipAudioCall.Listener() {
                @Override
                public void onCallEstablished(SipAudioCall call) {
                    call.startAudio();
                    call.setSpeakerMode(true);
                    call.toggleMute();
                    updateStatus(call);
                }

                @Override
                public void onCallEnded(SipAudioCall call) {
                    updateStatus("Ready");
                }
            };

            call = manager.makeAudioCall(me.getUriString(), sipAddress, listener, 30);
        } catch (Exception e) {
            Log.i("WalkieTalkieActivity/InitiateCall", "Error when trying to cloase manager.", e);

            if( me != null) {
                try {
                    manager.close(me.getUriString());
                } catch (Exception ee) {
                    Log.i("WalkieTalkieActivity/InitiateCall", "Error when trying to close manager", ee);

                    ee.printStackTrace();
                }
            }

            if( call != null) {
                call.close();
            }
        }

    }
    public void updateStatus(final String status) {

        // 스레드 변화
        TextView labelView = (TextView) findViewById(R.id.sipLabel);
        labelView.setText(status);

    }
    public void updateStatus(SipAudioCall call) {
        String useName = call.getPeerProfile().getDisplayName();
        if( useName == null ) {
            useName = call.getPeerProfile().getUserName();
        }

        updateStatus(useName + "@" + call.getPeerProfile().getSipDomain());
    }
    public boolean onTouch(View v, MotionEvent event) {
        if( call == null) {
            return false;
        } else if (event.getAction() == MotionEvent.ACTION_DOWN && call != null && call.isMuted()) {
            call.toggleMute();
        } else if (event.getAction() == MotionEvent.ACTION_UP && !call.isMuted()) {
            call.toggleMute();
        }

        return false;
    }
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, CALL_ADDRESS, 0, "Call someone");
        menu.add(0, SET_AUTH_INFO, 0, "Edit your SIP Info");
        menu.add(0, HANG_UP, 0, "End Current CAll.");

        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case CALL_ADDRESS:
                showDialog(CALL_ADDRESS);
                break;
            case SET_AUTH_INFO:
                updatePreferences();
                break;
            case HANG_UP:
                if( call != null) {
                    try {
                        call.endCall();
                    } catch (SipException se) {
                        Log.d("WalkieTalkieActivity/onOptionsItemSelected", "Error ending call.", se);
                    }

                    call.close();
                }

                break;
        }

        return true;
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case CALL_ADDRESS:
                LayoutInflater factory = LayoutInflater.from(this);
                final View textBoxView = factory.inflate(R.layout.call_address_dialog, null);
                return new AlertDialog.Builder(this)
                        .setTitle("Call Someone")
                        .setView(textBoxView)
                        .setPositiveButton(
                                android.R.string.ok, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichbutton) {
                                        EditText textField = (EditText) (textBoxView.findViewById(R.id.calladdress_edit));
                                        sipAddress = textField.getText().toString();
                                        initiateCall();
                                    }
                                }
                        )
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop
                                    }
                                }
                        )
                        .create();

            case UPDATE_SETTING_DIALOG:
                return new AlertDialog.Builder(this)
                        .setMessage("Please update your SIP Account Settings.")
                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButon) {
                                updatePreferences();
                            }
                        })
                        .setNegativeButton(
                                android.R.string.cancel, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // Noop.
                                    }
                                }
                        )
                        .create();


        }


        return null;

    }
    public void updatePreferences() {
        Intent settingActivity = new Intent(getBaseContext(), SipSettings.class);
        startActivity(settingActivity);
    }
}
