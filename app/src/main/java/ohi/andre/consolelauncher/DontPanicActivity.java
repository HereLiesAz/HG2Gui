package ohi.andre.consolelauncher;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class DontPanicActivity extends Activity {

    public static final String EXTRA_ERROR = "error_message";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dont_panic);

        String error = getIntent().getStringExtra(EXTRA_ERROR);
        if(error != null) {
            TextView errorView = (TextView) findViewById(R.id.tv_console_error);
            errorView.setText("> ERROR: " + error);
        }

        // Close on tap anywhere
        findViewById(android.R.id.content).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Basic interactivity for buttons
        final LinearLayout btnTwl = (LinearLayout) findViewById(R.id.btn_obj_twl);
        final LinearLayout btnTea = (LinearLayout) findViewById(R.id.btn_obj_tea);
        final LinearLayout btnThmb = (LinearLayout) findViewById(R.id.btn_obj_thmb);
        final LinearLayout btnTime = (LinearLayout) findViewById(R.id.btn_obj_time);

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Reset all
                updateButtonState(btnTwl, false);
                updateButtonState(btnTea, false);
                updateButtonState(btnThmb, false);
                updateButtonState(btnTime, false);

                // Set selected
                updateButtonState((LinearLayout) v, true);
            }
        };

        btnTwl.setOnClickListener(listener);
        btnTea.setOnClickListener(listener);
        btnThmb.setOnClickListener(listener);
        btnTime.setOnClickListener(listener);
    }

    private void updateButtonState(LinearLayout btn, boolean selected) {
        if(selected) {
            btn.setBackgroundResource(R.drawable.bg_panic_button_selected);
        } else {
            btn.setBackgroundResource(R.drawable.bg_panic_button);
        }
    }
}
