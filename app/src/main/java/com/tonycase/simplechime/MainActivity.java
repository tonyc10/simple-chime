package com.tonycase.simplechime;

import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;

/**
 * The main activity of the Chime App.   When the app opens, there is no fragment, just Bell animation.
 * After that, the fragment, {@link ChimePreferenceFragment}, comes in.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "Chime Main";
    private static final String FRAG_TAG = "PREF_FRAGMENT";

    private ChimePreferenceFragment prefFrag;

    // used to run the delayed start.
    private Handler handler = new Handler();

    // A runnable that runs after short delay from onResume.
    // Plays end animation, and launches prefs fragment
    private Runnable delayedStart = new Runnable() {

        public void run() {

            // are there any fragments out there?
            Fragment frag0 = MainActivity.this.getFragmentManager().findFragmentByTag(FRAG_TAG);
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

            // Set background color.  I think one of these was for Gingerbread.
            if (d instanceof ColorDrawable) {
                int color = ((ColorDrawable) d).getColor();
                int newColor = Color.argb(176, Color.red(color), Color.green(color), Color.blue(color));
                ll.setBackgroundColor(newColor);
            } else {
                // make it white
                ll.setBackgroundColor(Color.argb(224, 255, 255, 255));
            }
            // animation
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

    // Play the user's sound and shake the bell image.
    // package-protected to allow fragment in this package to access this method
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
        // kick off delayed start (actually, completion of start, here.)
        handler.postDelayed(delayedStart, 1300);
    }

}