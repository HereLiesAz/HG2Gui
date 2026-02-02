package ohi.andre.consolelauncher;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class MitosisMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
