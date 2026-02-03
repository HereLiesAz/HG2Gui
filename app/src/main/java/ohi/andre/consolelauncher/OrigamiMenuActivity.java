package ohi.andre.consolelauncher;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

public class OrigamiMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_origami_menu);

        View card = findViewById(R.id.card_container);
        if (card != null) {
            // Unfold animation: Scale Y from 0 to 1
            card.setPivotY(0); // Unfold from top
            ObjectAnimator animator = ObjectAnimator.ofFloat(card, "scaleY", 0f, 1f);
            animator.setDuration(1000);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }
}
