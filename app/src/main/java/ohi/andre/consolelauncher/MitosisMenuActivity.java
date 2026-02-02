package ohi.andre.consolelauncher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import java.util.Random;

public class MitosisMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Randomize Theme
        boolean useClockwork = new Random().nextBoolean();

        if (useClockwork) {
            setupClockworkTheme();
        } else {
            setupMitosisTheme();
        }
    }

    private void setupClockworkTheme() {
        setContentView(R.layout.activity_clockwork_menu);

        // Gear Rotation
        View gear = findViewById(R.id.clockwork_gear);
        if (gear != null) {
            ObjectAnimator rotate = ObjectAnimator.ofFloat(gear, "rotation", 0f, 360f);
            rotate.setDuration(10000);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.setRepeatCount(ValueAnimator.INFINITE);
            rotate.start();
        }

        // Lever Movement (Cranking)
        View lever = findViewById(R.id.clockwork_lever);
        if (lever != null) {
            ObjectAnimator crank = ObjectAnimator.ofFloat(lever, "rotation", -25f, 0f, -25f);
            crank.setDuration(2000);
            crank.setInterpolator(new AccelerateDecelerateInterpolator());
            crank.setRepeatCount(ValueAnimator.INFINITE);
            crank.start();
        }

        // Card Slide In
        View card = findViewById(R.id.clockwork_card);
        if (card != null) {
            // Start off-screen
            card.setTranslationX(1000f);
            ObjectAnimator slide = ObjectAnimator.ofFloat(card, "translationX", 1000f, 0f);
            slide.setDuration(800);
            slide.setStartDelay(500);
            slide.setInterpolator(new AccelerateDecelerateInterpolator());
            slide.start();
        }
    }

    private void setupMitosisTheme() {
        setContentView(R.layout.activity_mitosis_menu);

        // Header Pulse
        View headerIcon = findViewById(R.id.header_icon);
        if (headerIcon != null) {
            ObjectAnimator pulse = ObjectAnimator.ofFloat(headerIcon, "alpha", 1f, 0.5f, 1f);
            pulse.setDuration(2000);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();
        }

        // Buttons Float
        View btnOpen = findViewById(R.id.btn_open);
        View btnSave = findViewById(R.id.btn_save);
        View btnDelete = findViewById(R.id.btn_delete);

        if (btnOpen != null) animateFloat(btnOpen, 0);
        if (btnSave != null) animateFloat(btnSave, 500);
        if (btnDelete != null) animateFloat(btnDelete, 1000);

        // Heartbeat
        View ecgDot = findViewById(R.id.ecg_dot);
        if (ecgDot != null) {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(ecgDot, "scaleX", 1f, 1.2f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(ecgDot, "scaleY", 1f, 1.2f, 1f);
            scaleX.setDuration(1000);
            scaleY.setDuration(1000);
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.start();
            scaleY.start();
        }
    }

    private void animateFloat(View view, long delay) {
        ObjectAnimator floatAnim = ObjectAnimator.ofFloat(view, "translationY", 0f, -20f, 0f);
        floatAnim.setDuration(3000);
        floatAnim.setStartDelay(delay);
        floatAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.start();
    }
}
