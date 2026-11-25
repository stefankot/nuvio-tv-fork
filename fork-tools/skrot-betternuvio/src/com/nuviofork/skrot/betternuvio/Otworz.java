package com.nuviofork.skrot.betternuvio;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

/** Otwiera Better Nuvio (com.betternuvio.tv) i od razu się zamyka. */
public class Otworz extends Activity {
    private static final String TAG = "ForkSkrot";
    private static final String CEL = "com.betternuvio.tv";

    @Override
    protected void onCreate(Bundle stan) {
        super.onCreate(stan);
        Intent i = getPackageManager().getLeanbackLaunchIntentForPackage(CEL);
        if (i == null) {
            Log.i(TAG, "Brak wejścia leanback, próbuję zwykłego");
            i = getPackageManager().getLaunchIntentForPackage(CEL);
        }
        if (i == null) {
            Log.w(TAG, "Better Nuvio nie jest zainstalowane");
            Toast.makeText(this, "Better Nuvio nie jest zainstalowane", Toast.LENGTH_LONG).show();
        } else {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(i);
                Log.i(TAG, "Otwieram Better Nuvio: " + i.getComponent());
            } catch (Exception e) {
                Log.w(TAG, e.getClass().getSimpleName() + ": " + e.getMessage());
                Toast.makeText(this, "Nie udało się otworzyć Better Nuvio", Toast.LENGTH_LONG).show();
            }
        }
        finish();
    }
}
