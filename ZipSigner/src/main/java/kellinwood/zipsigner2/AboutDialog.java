/*
 * Copyright (C) 2010 Ken Ellinwood.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kellinwood.zipsigner2;

import android.app.Dialog;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class AboutDialog 
{
    private static final String TAG = "AboutDialog";

    private static Dialog dialog = null;

    // Show the about dialog
    public static void show(Context context) {
        String versionName;
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            versionName = pInfo.versionName;
        }
        catch (NameNotFoundException x) {
            Log.e(TAG, x.getClass().getName() + ": " + x.getMessage());
            versionName = "ERROR: " + x.getMessage();
        }

        dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        dialog.setContentView(R.layout.about_dialog);
        // dialog.setTitle(R.string.start_timer_about_title);
        String versionTemplate = context.getString(R.string.AboutZipSignerVersionTemplate);
        String versionText = String.format( versionTemplate, versionName);
        TextView versionView = (TextView)dialog.findViewById( R.id.about_version_view);
        versionView.setText(versionText);

        Button closeButton = (Button)dialog.findViewById(R.id.AboutCloseButton);
        closeButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        dialog.show();    	
    }

    public static void hide() {
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }
}
