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
import android.widget.TextView;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.KeyStoreFileManager;
import kellinwood.security.zipsigner.optional.PasswordObfuscator;
import kellinwood.zipsigner2.R;


public class DeleteKeyDialog
{

    public static final String MSG_DATA_ALIAS_ID = "aliasId";
    public static final String MSG_DATA_KEYSTORE_PASSWORD = "storePass";


    // Show the dialog with remember password option
    public static void show( final Context context, final Handler handler,
                             final int msgCode, final Alias alias)
    {
        final AndroidLogger logger = AndroidLogManager.getAndroidLogger(DeleteKeyDialog.class);
        logger.setToastContext(context);

        final Dialog dialog = new Dialog(context);

        dialog.setTitle(String.format( context.getResources().getString(R.string.ConfirmDeleteKeyTitle), alias.getName()));

        dialog.setContentView(R.layout.delete_key_dialog);
        LayoutParams params = dialog.getWindow().getAttributes();
        params.width = LayoutParams.FILL_PARENT;
        dialog.getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        TextView message = (TextView)dialog.findViewById(R.id.ConfirmDeleteKeyMessage);

        message.setText( String.format( context.getResources().getString(R.string.ConfirmDeleteKeyMessage), alias.getName()));

        final EditText keystorePasswordView = (EditText)dialog.findViewById(R.id.KeystorePassword);

        if (alias.getKeystore().rememberPassword()) {
            keystorePasswordView.setVisibility(View.GONE);
            TextView passwordLabel = (TextView)dialog.findViewById(R.id.KeystorePasswordLabel);
            passwordLabel.setVisibility(View.GONE);
        }

        Button okButton = (Button)dialog.findViewById(R.id.OkButton);
        okButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (!alias.getKeystore().rememberPassword() && keystorePasswordView.getText().length() == 0) {
                    logger.error( context.getResources().getString(R.string.KeystorePasswordRequired));
                    return;
                }

                String storePass = alias.getKeystore().rememberPassword() ? alias.getKeystore().getPassword() :
                    PasswordObfuscator.getInstance().encodeKeystorePassword(alias.getKeystore().getPath(),
                        keystorePasswordView.getText().toString());

                try {
                    KeyStoreFileManager.validateKeystorePassword( alias.getKeystore().getPath(), storePass);
                } catch (Exception x) {
                    logger.error( context.getResources().getString(R.string.WrongKeystorePassword));
                    return;
                }

                Message msg = new Message();
                msg.what = msgCode;
                Bundle data = new Bundle();
                data.putLong(MSG_DATA_ALIAS_ID, alias.getId());
                data.putString(MSG_DATA_KEYSTORE_PASSWORD, storePass);

                msg.setData(data);
                handler.sendMessage(msg);
                dialog.dismiss();
            }
        });
        
        Button cancelButton = (Button)dialog.findViewById(R.id.CancelButton);
        cancelButton.setOnClickListener( new OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
            }
        });
        
        dialog.show();    	
    }

}
