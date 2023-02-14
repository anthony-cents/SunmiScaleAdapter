# Sunmi Scale Adapter Demo

## Required Hardware
- Android Tablet with USB interface (Sunmi T2s used in testing)
- [Mettler Toledo BC Series Scale](https://www.mt.com/us/en/home/library/operating-instructions/industrial-scales/GSP_BC_Scale_SM.html) with Power Cable
- FTDI Chipset Male DB9-to-USB Cable
- Female DB9-to-RJ11 Cable

## Setup
Follow these steps to deploy and test the demo application.
1. Download or clone the project
2. Open the project in Android Studio
3. Build the APK
4. Install the APK on the target device. To do this on the Sunmi follow [these steps](https://sunmi-1.atlassian.net/wiki/spaces/NARHOWTO/pages/1795981317/Turn+Desktop+Device+to+Debug+Mode) to enable USB debugging. For other tablets, enable developer options and USB debugging to allow for USB deployments through Android Studio.

## Usage
The app implements a single activity and single button. Depending on current USB permission state, the button may show "QUERY WEIGHT" or "REQUEST USB PERMISSION". "QUERY WEIGHT" allows a user to write to the USB port and in turn, request the weight packet from the scale. The weight is then displayed with a Toast and in Logcat. If no USB permission has been granted by the user, the button shows "REQUEST USB PERMISSION". Pressing this will launch a prompt asking for USB permission.

## Approach
The app uses the [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) driver library for USB interfacing. We use the direct read/write functionality for writing to the scale then reading it's response payload. We then parse the payload to see if there is a valid weight present or if there is an underweight error on the scale. This is displayed with a Toast and in Logcat. The primary method for communicating app operation is through Logcat.

## Author
Anthony Rizzo\
anthony@trycents.com