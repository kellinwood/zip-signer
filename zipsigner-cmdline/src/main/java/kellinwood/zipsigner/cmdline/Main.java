/*
 * Copyright (C) 2010 Ken Ellinwood
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
package kellinwood.zipsigner.cmdline;


import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.Parser;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;

import kellinwood.logging.Logger;
import kellinwood.logging.log4j.Log4jLoggerFactory;
import kellinwood.security.zipsigner.ZipSigner;
import kellinwood.security.zipsigner.optional.CustomKeySigner;
import kellinwood.security.zipsigner.optional.KeyStoreFileManager;



/**
 * Sign files from the command line using zipsigner-lib.
 */
public class Main 
{

    static void usage( Options options)
    {
        HelpFormatter formatter = new HelpFormatter();

        formatter.printHelp(140,
                "ZipSignerCmdline [options] <input.zip> <output.zip>",
                "Sign the input file and write the result to the given output file\n\n"+
            "Examples:\n\n"+
            "java -jar zipsigner-cmdline-<version>.jar input.zip output-signed.zip (signs in auto-testkey mode)\n\n"+
            "java -jar zipsigner-cmdline-<version>.jar -m <keyMode> input.zip output-signed.zip (signs in specified mode)\n\n"+
            "java -jar zipsigner-cmdline-<version>.jar -s <keystore file> input.zip output-signed.zip (signs with first key in the keystore)\n\n"+
            "java -jar zipsigner-cmdline-<version>.jar -s <keystore file> -a <key alias> input.zip output-signed.zip (signs with specified key in keystore)",
                options, "");

        System.exit(1);
    }

    static char[] readPassword( String prompt)  {
        System.out.print(prompt + ": ");
        System.out.flush();
        return System.console().readPassword();
    }

    private static Logger log;
    
    public static void main( String[] args) {
        try {

            Options options = new Options();
            CommandLine cmdLine = null;
            Option helpOption =  new Option("h", "help", false, "Display usage information");

            Option modeOption = new Option("m", "keymode", false, "Keymode one of: auto, auto-testkey, auto-none, media, platform, shared, testkey, none");
            modeOption.setArgs( 1);
            
            Option keyOption = new Option("k", "key", false, "PCKS#8 encoded private key file");
            keyOption.setArgs( 1);

            Option pwOption = new Option("p", "keypass", false, "Private key password");
            pwOption.setArgs( 1);

            Option certOption = new Option("c", "cert", false, "X.509 public key certificate file");
            certOption.setArgs( 1);

            Option sbtOption = new Option("t", "template", false, "Signature block template file");
            sbtOption.setArgs( 1);
            
            Option keystoreOption = new Option("s", "keystore", false, "Keystore file");
            keystoreOption.setArgs(1);
            
            Option aliasOption = new Option("a", "alias", false, "Alias for key/cert in the keystore");
            aliasOption.setArgs(1);
            
            options.addOption( helpOption);
            options.addOption( modeOption);
            options.addOption( keyOption);
            options.addOption( certOption);
            options.addOption( sbtOption); 
            options.addOption( pwOption);
            options.addOption( keystoreOption);
            options.addOption( aliasOption);

            Parser parser = new BasicParser();

            try {
                cmdLine = parser.parse(options, args);
            }
            catch (MissingOptionException x)
            {
                System.out.println("One or more required options are missing: " + x.getMessage());
                usage( options);
            }
            catch (ParseException x) {
                System.out.println( x.getClass().getName() + ": " + x.getMessage());
                usage( options);
            }

            if (cmdLine.hasOption( helpOption.getOpt())) usage(options);

            // The distribution script passes the props file name in a system property
            String log4jProps = System.getProperty("log4j.properties");

            if (log4jProps == null || !(new File( log4jProps)).exists()) {
                log4jProps = "log4j.properties";
            }

            Properties log4jProperties = new Properties();
            log4jProperties.load ( new FileReader( log4jProps));
            PropertyConfigurator.configure( log4jProperties);
            Log4jLoggerFactory.activate();
            log = Logger.getLogger(Main.class);

            List<String> argList = cmdLine.getArgList();
            if (argList.size() != 2) usage(options);

            ZipSigner signer = new ZipSigner();

            signer.addAutoKeyObserver( new Observer() {
                @Override
                public void update(Observable observable, Object o) {
                    log.info("Signing with key: "+o);
                }
            });

            Class bcProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            Provider bcProvider = (Provider)bcProviderClass.newInstance();

            KeyStoreFileManager.setProvider( bcProvider);

            signer.loadProvider( "org.spongycastle.jce.provider.BouncyCastleProvider");


            PrivateKey privateKey = null;            
            if (cmdLine.hasOption( keyOption.getOpt())) {
                if (!cmdLine.hasOption( certOption.getOpt())) {
                    log.error("Certificate file is required when specifying a private key");
                    usage( options);
                }

                String keypw = null;
                if (cmdLine.hasOption( pwOption.getOpt())) keypw = pwOption.getValue();
                else {
                    keypw = new String(readPassword("Key password"));
                    if (keypw.equals("")) keypw = null;
                }
                URL privateKeyUrl = new File( keyOption.getValue()).toURI().toURL();
                
                privateKey = signer.readPrivateKey( privateKeyUrl, keypw);
            }

            X509Certificate cert = null;
            if (cmdLine.hasOption( certOption.getOpt())) {

                if (!cmdLine.hasOption( keyOption.getOpt())) {
                    log.error("Private key file is required when specifying a certificate");
                    usage( options);
                }

                URL certUrl = new File( certOption.getValue()).toURI().toURL();
                cert = signer.readPublicKey( certUrl);
            }

            byte[] sigBlockTemplate = null;
            if (cmdLine.hasOption( sbtOption.getOpt())) {
                URL sbtUrl = new File( sbtOption.getValue()).toURI().toURL();
                sigBlockTemplate = signer.readContentAsBytes( sbtUrl);
            }

            if (cmdLine.hasOption( keyOption.getOpt())) {
                signer.setKeys( "custom", cert, privateKey, sigBlockTemplate);
                signer.signZip( argList.get(0), argList.get(1));
            }
            else if (cmdLine.hasOption( modeOption.getOpt())) {
                signer.setKeymode(modeOption.getValue());
                signer.signZip( argList.get(0), argList.get(1));
            }
            else if (cmdLine.hasOption(( keystoreOption.getOpt()))) {
                String alias = null;

                if (!cmdLine.hasOption( aliasOption.getOpt())) {

                    KeyStore keyStore =  KeyStoreFileManager.loadKeyStore( keystoreOption.getValue(), (char[])null);
                    for (Enumeration<String> e = keyStore.aliases(); e.hasMoreElements(); ) {
                        alias = e.nextElement();
                        log.info("Signing with key: " + alias);
                        break;
                    }
                }
                else alias = aliasOption.getValue();

                
                String keypw = null;
                if (cmdLine.hasOption( pwOption.getOpt())) keypw = pwOption.getValue();
                else {
                    keypw = new String(readPassword("Key password"));
                    if (keypw.equals("")) keypw = null;
                }

                CustomKeySigner.signZip( signer,  keystoreOption.getValue(), null, alias, keypw.toCharArray(), "SHA1withRSA", argList.get(0), argList.get(1));
            }
            else {
                signer.setKeymode("auto-testkey");
                signer.signZip( argList.get(0), argList.get(1));
            }
            
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

}
