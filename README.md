https://play.google.com/store/apps/details?id=uk.co.oliverdelange.flicify&hl=en_GB

A simple app that links a Flic button to your Spotify app. 
Click the Flic button to save the currently playing song to your library.
If its already in there, i'll take it out. You'll hear a sound that'll indicate which one happened.

Simples. 

When IFTTT or the official Flic app start supporting this functionality, i'll get rid of this app. 

Ps. You need to have the official Spotify app installed for this to work.

<img src="playstore/screenshot1.jpg" width="250" />
<img src="playstore/screenshot2.jpg" width="250" />


# Google Assistant
To test deeplink - `adb shell am start -a android.intent.action.VIEW -d "oliverdelange://flicify/start"`

`adb -s 08311JEC203007 shell am start -a android.intent.action.VIEW -d "https://assistant.google.com/services/invoke/uid/000015e33c694a9b?intent=actions.intent.START_EXERCISE\&param.exercise=%7B%0A++++%22%40type%22%3A+%22Exercise%22%2C%0A++++%22name%22%3A+%22Running%22%2C%0A++++%22%40context%22%3A+%22http%3A%2F%2Fschema.googleapis.com%22%0A%7D"`