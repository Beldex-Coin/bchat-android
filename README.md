BChat
======
BChat is a decentralized p2p messaging app that runs on Beldex network. Unlike other centralized messaging apps BChat never collect any user data like phone number, location, email, IP address.. etc. The message you sent using BChat will be highly secure and anonymous. No one (even us) can have control of your data that has been shared in BChat. Wtih BChat you own your data.

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="80"/>](https://play.google.com/store/apps/details?id=io.beldex.bchat)
[<img src="https://github.com/status-im/status-mobile/raw/develop/doc/github_badge.png" alt="Get it on Github" height="80"/>](https://github.com/Beldex-Coin/bchat-android/releases)



Building From Source
==================

Basics
------

BChat uses [Gradle](http://gradle.org) to build the project and to maintain
dependencies.  However, you needn't install it yourself; the
"gradle wrapper" `gradlew`, mentioned below, will do that for you.

Dependencies
---------------
You will need Java 8 set up on your machine.

Ensure that the following packages are installed from the Android SDK manager:

* Android SDK Build Tools (see buildToolsVersion in build.gradle)
* SDK Platform (all API levels)
* Android Support Repository
* Google Repository

In Android studio, this can be done from the Quickstart panel. Just choose "Configure", then "SDK Manager". In the SDK Tools tab of the SDK Manager, make sure that "Android Support Repository" is installed, and that the latest "Android SDK build-tools" are installed. Click "OK" to return to the Quickstart panel. You may also need to install API version 28 in the SDK platforms tab.

Setting up a development environment and building from Android Studio
------------------------------------

[Android Studio](https://developer.android.com/sdk/installing/studio.html) is the recommended development environment.

1. Open Android Studio. On a new installation, the Quickstart panel will appear. If you have open projects, close them using "File > Close Project" to see the Quickstart panel.
2. From the Quickstart panel, choose "Checkout from Version Control" then "git".
3. Paste the URL for the bchat-android project when prompted (https://github.com/beldex-coin/bchat-android.git).
4. Android Studio should detect the presence of a project file and ask you whether to open it. Click "yes".
5. Default config options should be good enough.
6. Project initialization and building should proceed.

Contributing code
-----------------

Code contributions should be sent via Github as pull requests, from feature branches [as explained here](https://help.github.com/articles/using-pull-requests)

Credits
-------
- Portions Copyright (c) 2014-2018 The Monero Project
- Portions Copyright (c) 2011 Whisper Systems
- Portions Copyright (c) 2013-2017 Open Whisper Systems
- Portions Copyright (c) 2018-2021 Session 

