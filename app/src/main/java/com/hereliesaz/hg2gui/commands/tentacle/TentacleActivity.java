package com.hereliesaz.hg2gui.commands.tentacle;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.hereliesaz.hg2gui.R;

public class TentacleActivity extends AppCompatActivity {

    private TentacleView tentacleView;
    private View helpNode;
    private View guideNode;
    private View manualNode;
    private View tipsNode;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_tentacle_menu);

        tentacleView = findViewById(R.id.tentacle_view);
        helpNode = findViewById(R.id.node_help);
        guideNode = findViewById(R.id.node_guide);
        manualNode = findViewById(R.id.node_manual);
        tipsNode = findViewById(R.id.node_tips);

        setupClickListeners();

        // Wait for layout to determine positions
        final View rootView = findViewById(android.R.id.content);
        rootView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Remove listener to avoid multiple calls
                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                calculateAndStartAnimation();
            }
        });
    }

    private void calculateAndStartAnimation() {
        float helpX = helpNode.getX() + helpNode.getWidth() / 2f;
        float helpY = helpNode.getY() + helpNode.getHeight() / 2f;

        float guideX = guideNode.getX() + guideNode.getWidth() / 2f;
        float guideY = guideNode.getY() + guideNode.getHeight() / 2f;

        float manualX = manualNode.getX() + manualNode.getWidth() / 2f;
        float manualY = manualNode.getY() + manualNode.getHeight() / 2f;

        float tipsX = tipsNode.getX() + tipsNode.getWidth() / 2f;
        float tipsY = tipsNode.getY() + tipsNode.getHeight() / 2f;

        tentacleView.setTargetPoints(helpX, helpY, guideX, guideY, manualX, manualY, tipsX, tipsY);
        tentacleView.startAnimation();
    }

    private void setupClickListeners() {
        guideNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TentacleActivity.this, "Opening GUIDE...", Toast.LENGTH_SHORT).show();
            }
        });

        manualNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TentacleActivity.this, "Opening MANUAL...", Toast.LENGTH_SHORT).show();
            }
        });

        tipsNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(TentacleActivity.this, "Opening TIPS...", Toast.LENGTH_SHORT).show();
            }
        });

        helpNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Replay animation
                tentacleView.startAnimation();
            }
        });
    }
}
