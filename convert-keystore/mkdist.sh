#! /bin/bash

version=`lstags -n 1 | grep -o '[0-9.]\+[0-9]'`
cd target
rm -rf convert-keystore-$version
mkdir convert-keystore-$version
cp ~/.m2/repository/kellinwood/keystore/convert-keystore/$version/convert-keystore-$version.jar convert-keystore-$version
cp ~/.m2/repository/kellinwood/keystore/convert-keystore/$version/convert-keystore-$version-sources.jar convert-keystore-$version
cp ~/.m2/repository/org/bouncycastle/bcprov-jdk16/1.46/bcprov-jdk16-1.46.jar convert-keystore-$version
find convert-keystore-$version -name \*~ | xargs rm -f
cat >convert-keystore-$version/README.txt <<EOF
This is a utility to convert a JKS formatted keystore to BKS format
and visa-versa.  The JKS format is widely used, and if you don't know
what format your keystore is, then chances are that its a JKS
keystore. The BKS format is supported on Android, JKS is not, hence
the need to convert formats.

In order to convert the keystore as shown below you must have the Java
Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy
Files installed in your JRE.  Google "java unlimited strength policy
files" and download the jce_policy zip file.  Unpack the zip and copy
local_policy.jar and US_export_policy.jar to your jre/lib/security
directory.  If you don't install these files and try to convert the
keystore, you will get this error "java.security.KeyStoreException:
java.io.IOException: Error initialising store of key store:
java.security.InvalidKeyException: Illegal key size."

Run this command:

java -jar convert-keystore-$version.jar <input JKS file> <output BKS file>

You will be prompted to enter the keystore and key passwords.

Copy the BKS file to the sdcard of your device.

To use a BKS keystore in ZipSigner, follow this procedure:

    * Select "My Keys" from the menu.

    * Touch "Register keystore..." and select the BKS file from your sdcard.

    * You will be prompted for the keystore password.  Entering the
      keystore password is optional, so you may leave it blank and
      click OK.  If you enter the password and choose to have it
      remembered, and the password is the same for your key(s), then
      this password will automatically be remembered for each key
      where the key password is the same as the keystore password.

    * Your keystore will appear in an expandable list view below the
      register button.  Expand your keystore to reveal this list of
      keys (aka, aliases) within the keystore.  

    * The check-marked keys will be added to ZipSigner's key/mode
      list, so de-select any keys that you don't want to use in
      ZipSigner.

    * You can opt to have passwords remembered or not.  Each entry in
      the expandable list has a context menu that is activated on
      long-press.  If remembered, passwords are stored obfuscated in
      the local database (they are encrypted but not entirely
      secure since the algorithm is open-source).  Long-press on a key
      entry, select "Remember Password...", and you will be prompted to
      enter the password for the key. Once remembered, a password may
      be forgotten via the "Forget Password" context menu option.

    * When signing with a key and the password is not remembered for
      the key, you will be prompted to enter the password each time
      you sign with the key.

    * Repeat the process above to register more keystore files. You
      can register as many keystore files as you like.

    * Press the back button to return to ZipSigner's main view.  Your
      selected keys will appear in the key/mode list so you can
      sign files with them.

A BKS keystore can be converted to a JKS keystore using the -r option
as shown here:

java -jar convert-keystore-$version.jar -r <input BKS file> <output JKS file>


EOF
zip -r convert-keystore-$version.zip convert-keystore-$version
mv convert-keystore-$version.zip ../.
