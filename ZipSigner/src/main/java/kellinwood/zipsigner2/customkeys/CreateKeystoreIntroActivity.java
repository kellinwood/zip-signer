package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.zipsigner2.R;

public class CreateKeystoreIntroActivity extends Activity {

    AndroidLogger logger = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        logger = AndroidLogManager.getAndroidLogger(CreateKeystoreIntroActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.create_keystore_intro);

        Button cancelButton = (Button)findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        Button continueButton = (Button)findViewById(R.id.ContinueButton);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent( CreateKeystoreIntroActivity.this, CreateKeystoreFormActivity.class);
                i.putExtras( getIntent().getExtras());         // also forward data from previous activity
                startActivity(i);
            }
        });

    }
}