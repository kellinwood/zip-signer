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
package kellinwood.zipsigner2.customkeys;


import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;

import kellinwood.logging.Logger;
import kellinwood.zipsigner2.R;

public class EditDisplayNameDialog
{
    public static final String MSG_DATA_TEXT = "text";
    public static final String MSG_DATA_ALIAS_ID = "aliasId";

    private static Dialog dialog = null;

    // Show the dialog with remember password option
    public static void show( Context context, final Handler handler, String title,
                             final int msgCode, final long aliasId, String displayName) {
        dialog = new Dialog(context);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(title);

        dialog.setContentView(R.layout.edit_display_name);
        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        final EditText editTextView = (EditText)dialog.findViewById( R.id.EditDisplayNamedView);
        editTextView.setText( displayName);

        Button closeButton = (Button)dialog.findViewById(R.id.EditDisplayNameDoneButton);
        closeButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Message msg = new Message();
                msg.what = msgCode;
                Bundle data = new Bundle();
                data.putLong(MSG_DATA_ALIAS_ID,  aliasId);
                data.putString(MSG_DATA_TEXT, editTextView.getText().toString());
                msg.setData(data);
                handler.sendMessage(msg);
                dialog.dismiss();
            }
        });
        
        Button cancelButton = (Button)dialog.findViewById(R.id.EditDisplayNameCancelButton);
        cancelButton.setOnClickListener( new OnClickListener() {
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
