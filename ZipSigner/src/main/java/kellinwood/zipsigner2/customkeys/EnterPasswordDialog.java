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
import android.widget.CheckBox;
import android.widget.EditText;

import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.R;

public class EnterPasswordDialog 
{
    public static final String MSG_DATA_ID = "id";
    public static final String MSG_DATA_MESSAGE = "msg";
    public static final String MSG_DATA_PASSWORD = "text";
    public static final String MSG_DATA_PATH = "path";
    public static final String MSG_DATA_REMEMBER_PASSWORD = "rememberPassword";

    public static final int MESSAGE_CODE_ENTER_PASSWORD_CANCELLED = -42;

    private static Dialog dialog = null;


    // Show the dialog with remember password option
    public static void show( Context context, final Handler handler, String title,
                             final int msgCode, final String cdataPath, final long cdataId, boolean rememberPassword, final String aliasName) {
        dialog = new Dialog(context);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(title);

        dialog.setContentView(R.layout.enter_password_with_remember_dialog);
        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);
        
        final EditText editTextView = (EditText)dialog.findViewById( R.id.EnterPasswordView);
        final CheckBox rememberPwCheckbox = (CheckBox)dialog.findViewById(R.id.RememberPwCheckbox);
        rememberPwCheckbox.setChecked( rememberPassword);

        Button closeButton = (Button)dialog.findViewById(R.id.EnterPasswordDoneButton);
        closeButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Message msg = new Message();
                msg.what = msgCode;
                Bundle data = new Bundle();
                data.putLong(MSG_DATA_ID,  cdataId);
                String password = editTextView.getText().toString();
                if (password.length() == 0) {
                    password = null;
                }
                else {
                    if (aliasName == null) {
                        password = PasswordObfuscator.getInstance().encodeKeystorePassword(cdataPath, editTextView.getText().toString());
                    } else {
                        password = PasswordObfuscator.getInstance().encodeAliasPassword(cdataPath, aliasName, editTextView.getText().toString());
                    }
                }
                data.putBoolean(MSG_DATA_REMEMBER_PASSWORD, rememberPwCheckbox.isChecked());
                data.putString(MSG_DATA_PASSWORD, password);
                data.putString(MSG_DATA_PATH,  cdataPath);

                msg.setData(data);
                handler.sendMessage(msg);
                dialog.dismiss();
            }
        });
        
        Button cancelButton = (Button)dialog.findViewById(R.id.EnterPasswordCancelButton);
        cancelButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Message msg = new Message();
                msg.what = MESSAGE_CODE_ENTER_PASSWORD_CANCELLED;
                handler.sendMessage(msg);
                dialog.dismiss();
            }
        });
        
        dialog.show();    	
    }

    // Show the dialog without remember option
    public static void show( Context context, final Handler handler, String title,
                             final int msgCode, final String cdataPath, final long cdataId, final String aliasName) {
        dialog = new Dialog(context);
        // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setTitle(title);

        dialog.setContentView(R.layout.enter_password_dialog);
        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        final EditText editTextView = (EditText)dialog.findViewById( R.id.EnterPasswordView);

        Button closeButton = (Button)dialog.findViewById(R.id.EnterPasswordDoneButton);
        closeButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Message msg = new Message();
                msg.what = msgCode;
                Bundle data = new Bundle();
                data.putLong(MSG_DATA_ID,  cdataId);
                String password = editTextView.getText().toString();
                if (password.length() == 0) password = null;
                else {
                    if (aliasName == null)
                        password = PasswordObfuscator.getInstance().encodeKeystorePassword(cdataPath, editTextView.getText().toString());
                    else
                        password = PasswordObfuscator.getInstance().encodeAliasPassword(cdataPath, aliasName, editTextView.getText().toString());
                }
                data.putString(MSG_DATA_PASSWORD, password);
                data.putString(MSG_DATA_PATH,  cdataPath);
                msg.setData(data);
                handler.sendMessage(msg);
                dialog.dismiss();
            }
        });

        Button cancelButton = (Button)dialog.findViewById(R.id.EnterPasswordCancelButton);
        cancelButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                Message msg = new Message();
                msg.what = MESSAGE_CODE_ENTER_PASSWORD_CANCELLED;
                handler.sendMessage(msg);
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
