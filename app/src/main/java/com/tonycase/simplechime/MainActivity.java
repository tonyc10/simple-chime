package com.tonycase.simplechime;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class MainActivity extends Activity {

    private static final String TAG = "Chime Main";
    private static final String FRAG_TAG = "PREF_FRAGMENT";

    private ChimePreferenceFragment prefFrag;
    private final int PREF_ACTIVITY = 1;

    // relies on Activity being instantiated in main thread
    private Handler handler = new Handler();

    // A runnable that runs after short delay from onResume.
    // end animation, and launch prefs fragment
    private Runnable delayedStart = new Runnable() {

        public void run() {

            // are there any fragments out there?
            Fragment frag0 = MainActivity.this.getFragmentManager().findFragmentByTag(FRAG_TAG);
            //System.out.println("Existing frag = " + frag0);
            // Display the fragment as the main content.
            if (frag0 == null && !isFinishing()) {
                Log.i(TAG, "Replacing screen with new fragment");
                MainActivity.this.getFragmentManager().beginTransaction()
                        .replace(android.R.id.content, prefFrag, FRAG_TAG)
                        .commit();
                Log.i(TAG, "Fragment instance is " + prefFrag);
            }

            ViewGroup container = (ViewGroup) MainActivity.this.findViewById(R.id.frame1);
            LinearLayout ll = new LinearLayout(MainActivity.this);
            Drawable d = container.getBackground();
            //System.out.println("d = " + d);
            if (d instanceof ColorDrawable) {
                int color = ((ColorDrawable) d).getColor();
                int newColor = Color.argb(176, Color.red(color), Color.green(color), Color.blue(color));
                ll.setBackgroundColor(newColor);
            } else {
                // make it white
                ll.setBackgroundColor(Color.argb(176, 255, 255, 255));
            }
            ll.startAnimation(AnimationUtils.loadAnimation(MainActivity.this, R.anim.transparent_to_opaque));
            container.addView(ll, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate() instance = " + this);
        setContentView(R.layout.activity_main);

        prefFrag = new ChimePreferenceFragment();

        // play sound and start animation
        playSoundShakeBell();
    }

    // allow fragment in this package to access this method
    void playSoundShakeBell() {
        ChimeUtilities.playSound(this);
        // animate the splash view

        ImageView iView = (ImageView) findViewById(R.id.imageView1);

        Animation rotateAnim = AnimationUtils.loadAnimation(this, R.anim.rotate_bell);
        iView.startAnimation(rotateAnim);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() ");
        handler.postDelayed(delayedStart, 1300);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PREF_ACTIVITY) {
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}