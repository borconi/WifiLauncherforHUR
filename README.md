# Wifi Launcher for Headunit Realoaded or AAGateWay

This app is a companion app which should be installed on the phone and it will facilitate the start of Android Auto in wireless mode for those users who use Headunit Reloaded or AAGateWay.

Works in Android 9 and above.

## Usage

* Install the app
* Grant Draw Overlay permissions
* Grant Location Services "Allow all the time"
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

For Android 9 with ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION (aka Allow only while using the app) is fine to get Wi-Fi info
For Android 10 and above ACCESS_BACKGROUND_LOCATION is mandatory in order to get Wi-Fi info, and it must be requested in incremental requests

#### Change Wi-Fi State

Starting Android 10 we can't turn on Wi-Fi programmatically, so an alert will popup to let the user know.

In Android 9 this app will turn on Wi-Fi automatically.

## Collaborators