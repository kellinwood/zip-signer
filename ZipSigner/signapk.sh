#! /bin/bash

# Script to sign ZipSigner for the Market.  This script ensures that the app is
# signed such that the signature created with my private key replaces the signature
# created with the Android SDK debug key.

cd $(dirname $0)

if [ ! -f build/outputs/apk/release/ZipSigner-release-unsigned.apk ]; then
    echo "ERROR: build/outputs/apk/release/ZipSigner-release-unsigned.apk does not exist!"
    exit 1
fi

rm -rf build/outputs/apk/release-rezip
mkdir build/outputs/apk/release-rezip

cd build/outputs/apk/release-rezip
unzip -q ../release/ZipSigner-release-unsigned.apk

find . -type f | sort | zip -q -n png ../release/ZipSigner-rezipped-unsigned.apk -@
cd ../release
rm -f *-signed.apk 
rm -f *-aligned.apk 
jarsigner -keystore ~/.keystore -sigfile CERT -signedjar ZipSigner-rezipped-signed.apk ZipSigner-rezipped-unsigned.apk kellinwood
$(find ~/Library/Android/sdk/build-tools -name "zipalign" | sort | tail -n 1) 4 ZipSigner-rezipped-signed.apk ZipSigner-rezipped-signed-aligned.apk
