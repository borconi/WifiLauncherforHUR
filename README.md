# Wifi Launcher for Headunit Realoaded or AAGateWay

This app is a companion app which should be installed on the phone and it will facilitate the start of Android Auto in wireless mode for those users who use Headunit Reloaded or AAGateWay.

Works in Android 9 and above.

## Usage

* Install the app
* Grant Draw Overlay permissions
* Grant Location Services "Allow all the time" permissions
* Grant WifiSuggestions (only for Android 10 and up) permissions
* Select your car's bluetooth device

## Contributing

### Pre requisites

* Java 8
* Android Studio

### Getting Started

* Clone project
* Open it in Android Studio
* Have targeted API and Build Tools downloaded in your SDK Manager
* You are ready to code!

### Permissions

#### Draw Overlay

This is mandatory to launch Android Auto

#### Location Services

For Android 9 with `ACCESS_COARSE_LOCATION` or `ACCESS_FINE_LOCATION` (aka `Allow only while using the app`) is fine to get Wi-Fi info
For Android 10 and above `ACCESS_BACKGROUND_LOCATION` (aka `Allow all the time`) is mandatory in order to get Wi-Fi info, and it must be requested in incremental requests

#### Turn on/off Wi-Fi

Starting Android 10 we can't turn on Wi-Fi programmatically, so an alert will popup to let the user know.

In Android 9 this app will turn on Wi-Fi automatically.

#### Wi-Fi Suggestions (Android 10 and up)

We need to grant permissions to add suggested Wi-Fis to autoconnect to HUR Wi-Fi automatically, otherwise you have to connect manually in order to save that network in your phone.

## Collaborators