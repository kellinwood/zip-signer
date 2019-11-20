package kellinwood.keystore;

import java.io.*;
import java.security.cert.Certificate;
import java.security.Key;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Enumeration;

public class Convert {

    static char[] readPassword( String prompt) throws java.io.IOException {
        System.out.print(prompt + ": ");
        System.out.flush();
        return System.console().readPassword();
    }
    
    @SuppressWarnings("unchecked")
    public static Provider loadProvider( String providerClassName)
    throws ClassNotFoundException, IllegalAccessException, InstantiationException
    {
        Class providerClass = Class.forName(providerClassName);
        Provider provider = (Provider)providerClass.newInstance();
        Security.insertProviderAt(provider, 1);
        return provider;
    }

    public static void usage() {
        System.out.println("USAGE: Convert [-r] <keystore.jks> <keystore.bks");
        System.out.println("Converts JKS formatted keystore to BKS format.");
        System.out.println("The -r option reverses the operation to convert BKS format to JKS");
        System.exit(1);
    }
    
    public static void main(String[] args) {

        boolean reverse = false;

        String inFile = null;
        String outFile = null;
        try {

            // System.out.println( "Default keystore type is " + KeyStore.getDefaultType());
            if (args.length == 3) {
                if ("-r".equalsIgnoreCase(args[0])) {
                    reverse = true;
                    inFile = args[1];
                    outFile = args[2];
                } else {
                    System.out.print("ERROR - unrecognized option: "+args[0]);
                }
            } else if (args.length != 2) {
                usage();
            } else {
                inFile = args[0];
                outFile = args[1];
            }

            Provider bcProvider = loadProvider("org.bouncycastle.jce.provider.BouncyCastleProvider");

            FileInputStream fis = new FileInputStream( inFile);
            KeyStore fromKeystore = !reverse ? KeyStore.getInstance("jks") : KeyStore.getInstance("bks", bcProvider);
            
            char[] keystorePassword = readPassword("Keystore password");
            fromKeystore.load(fis, keystorePassword);
            fis.close();


            KeyStore toKeystore = reverse ? KeyStore.getInstance("jks") : KeyStore.getInstance("bks", bcProvider);
            toKeystore.load(null, keystorePassword);
            
            for( Enumeration<String> e = fromKeystore.aliases(); e.hasMoreElements(); ) {
                String alias = e.nextElement();
                
                System.out.println("Alias: " + alias);
                char[] keyPassword = null;
                Key key;
                try {
                    key = fromKeystore.getKey(alias, keystorePassword);
                    keyPassword = keystorePassword;
                } catch ( java.security.UnrecoverableKeyException x) {
                    keyPassword = readPassword("Password for entry " + alias);
                    key = fromKeystore.getKey(alias, keyPassword);
                }

                Certificate cert = fromKeystore.getCertificate(alias);

                toKeystore.setKeyEntry(alias, key, keyPassword, new Certificate[]{cert});
            }
            
            FileOutputStream fos = new FileOutputStream( outFile);
            toKeystore.store(fos, keystorePassword);
            fos.close();

        } catch (Exception x) {
            x.printStackTrace();
        }

    }
}
