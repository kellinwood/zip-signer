package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.zipsigner2.R;

public class ManageKeysHelpActivity extends Activity {

    AndroidLogger logger = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        logger = AndroidLogManager.getAndroidLogger(ManageKeysHelpActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.manage_keys_help);

        ((TextView)findViewById(R.id.P4)).setVisibility(View.GONE);

        Button button = (Button)findViewById(R.id.OkButton);
        button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}