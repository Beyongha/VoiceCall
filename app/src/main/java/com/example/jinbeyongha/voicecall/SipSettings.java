package com.example.jinbeyongha.voicecall;

/**
 * Created by jinbeyongha on 2016. 5. 7..
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.view.View;
import android.widget.Button;

/**
 * Handles SIP authentification settings for the Walkie Talkie app.
 */

public class SipSettings extends PreferenceActivity {
    //Button okButton;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // Note that none of the preferneces are actually defiend here.
        // they're all in the XML file res/xml/preference.xml.

        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preference);

      /*  okButton = (Button) findViewById(R.id.okButton);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
*/    }

}
