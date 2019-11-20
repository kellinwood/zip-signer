INTRO

ZipSigner is an Android app that is capable of signing Zip, Apk, and
Jar files on the device.  Its purpose is to demonstrate the use of
zipsigner-lib, a separate API that does the real work of signing the
files.  

ZipSigner contains an activity that can be launched from other apps to
sign files.  This capability is demonstrated by the ZipPickerActivity.
Take a look at the code for more details.

The source is 100% Java and there are no dependencies on other
installable components such as busybox.  Root privileges are not
required (but you probably need root in order to do something
meaningful with the results).

Files are signed in a way compatible with the OTA/Update.zip
installation method offered by the various recovery programs.


LOGCAT OUTPUT

To enable debug output from the app, execute the following adb commands:

adb shell setprop log.tag.ZipSigner VERBOSE
adb logcat ZipPickerActivity:* ZipSignerActivity:* ZipSigner:*

