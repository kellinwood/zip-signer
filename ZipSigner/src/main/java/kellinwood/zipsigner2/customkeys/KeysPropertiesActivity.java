package kellinwood.zipsigner2.customkeys;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.spongycastle.asn1.x509.X509Name;
import org.spongycastle.jce.PrincipalUtil;
import org.spongycastle.jce.X509Principal;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;

import kellinwood.logging.android.AndroidLogManager;
import kellinwood.logging.android.AndroidLogger;
import kellinwood.security.zipsigner.optional.Fingerprint;
import kellinwood.security.zipsigner.optional.KeyStoreFileManager;
import kellinwood.zipsigner2.R;

public class KeysPropertiesActivity extends Activity {

    AndroidLogger logger = null;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


        logger = AndroidLogManager.getAndroidLogger(KeysPropertiesActivity.class);
        // enable toasts for info level logging.  toasts are default for error and warnings.
        logger.setToastContext(getBaseContext());
        logger.setInfoToastEnabled(true);

        setContentView(R.layout.key_properties);

        String keystorePath = getIntent().getStringExtra(KeyParameters.KEYSTORE_FILENAME);
        String keyName = getIntent().getStringExtra(KeyParameters.KEY_NAME);
        String keyPass = getIntent().getStringExtra(KeyParameters.KEY_PASSWORD);

        TextView v = (TextView)findViewById(R.id.KeyName);
        v.setText(keyName);

        v = (TextView)findViewById(R.id.KeystoreFilename);
        v.setText(keystorePath);

        try {
            KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) KeyStoreFileManager.getKeyEntry(keystorePath, null, keyName, keyPass);

            PrivateKey privateKey = entry.getPrivateKey();

            v = (TextView)findViewById(R.id.KeyType);
            v.setText(privateKey.getAlgorithm());

            v = (TextView)findViewById(R.id.KeyFormat);
            v.setText(privateKey.getFormat());

            v = (TextView)findViewById(R.id.KeySize);

            if (privateKey instanceof RSAPrivateCrtKey) {
                v.setText( Integer.toString(((RSAPrivateCrtKey)privateKey).getModulus().bitLength()));
            } else {
                v.setText("?");
            }

            Certificate certificate = entry.getCertificate();
            if (certificate instanceof X509Certificate) {


                X509Certificate x509 = (X509Certificate)certificate;

                v = (TextView)findViewById(R.id.Expires);
                v.setText(x509.getNotAfter().toGMTString());

                X509Principal x509Principal = PrincipalUtil.getSubjectX509Principal(x509);
                LinearLayout subjectDnLayout = (LinearLayout)findViewById(R.id.SubjectDnLinearLayout);

                renderDN(x509Principal, subjectDnLayout);

                v = (TextView)findViewById(R.id.SelfSigned);
                if (x509Principal.equals( PrincipalUtil.getIssuerX509Principal(x509))) {
                    v.setText(getResources().getString(R.string.CertIsSelfSigned));
                    findViewById(R.id.IssuerDnLinearLayout).setVisibility(View.GONE);
                } else {
                    v.setVisibility(View.GONE);
                    renderDN( PrincipalUtil.getIssuerX509Principal(x509), (ViewGroup)findViewById(R.id.IssuerDnLinearLayout));
                }

            }
            byte[] encodedCert = certificate.getEncoded();


            View.OnCreateContextMenuListener copyContextMenuListener =
                new View.OnCreateContextMenuListener() {
                    @Override
                    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                        menu.setHeaderTitle((String)v.getTag());
                        menu.add(0, v.getId(), 0, R.string.CopyToClipboardMenuItemLabel);
                        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(((TextView)v).getText());
                    }
                };

            TextView md5FingerprintView = (TextView)findViewById(R.id.MD5Fingerprint);
            md5FingerprintView.setText(Fingerprint.hexFingerprint("MD5", encodedCert));
            md5FingerprintView.setTag(getResources().getString(R.string.MD5FingerprintLabel));
            md5FingerprintView.setOnCreateContextMenuListener(copyContextMenuListener);

            TextView sha1FingerprintView = (TextView)findViewById(R.id.SHA1Fingerprint);
            sha1FingerprintView.setText(Fingerprint.hexFingerprint("SHA1", encodedCert));
            sha1FingerprintView.setTag(getResources().getString(R.string.SHA1FingerprintLabel));
            sha1FingerprintView.setOnCreateContextMenuListener(copyContextMenuListener);

            TextView sha1KeyHashView = (TextView)findViewById(R.id.SHA1KeyHash);
            sha1KeyHashView.setText(Fingerprint.base64Fingerprint("SHA1", encodedCert));
            sha1KeyHashView.setTag(getResources().getString(R.string.SHA1KeyHashLabel));
            sha1KeyHashView.setOnCreateContextMenuListener(copyContextMenuListener);

        } catch (Exception x) {
            logger.error(x.getMessage(), x);
        }

        Button button = (Button)findViewById(R.id.BackButton);
        button.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    void renderDN( X509Principal x509Principal, ViewGroup layout) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        for (int i = 0; i < x509Principal.getOIDs().size(); i++) {
            String symbol = (String)X509Name.DefaultSymbols.get( x509Principal.getOIDs().elementAt(i));
            String value = (String)x509Principal.getValues().elementAt(i);

            TextView v = (TextView)inflater.inflate(R.layout.text_view, null);
            v.setText(symbol + "=" + value);
            layout.addView(v);
        }
    }

}