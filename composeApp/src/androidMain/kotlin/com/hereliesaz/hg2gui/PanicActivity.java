package com.hereliesaz.hg2gui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PanicActivity extends AppCompatActivity {

    public static final String EXTRA_ERROR_MESSAGE = "extra_error_message";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic);

        String errorMessage = getIntent().getStringExtra(EXTRA_ERROR_MESSAGE);
        TextView messageText = findViewById(R.id.panic_message_text);

        if (errorMessage != null && !errorMessage.isEmpty()) {
            messageText.setText("> ERR: " + errorMessage);
        } else {
             messageText.setText("> ERR: Unknown system failure.");
        }

        Button calmButton = findViewById(R.id.panic_button);
        calmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
