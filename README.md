# Forkyz Crosswords 

Forkyz is an unofficial fork of [Shortyz](https://github.com/kebernet/shortyz/)
implementing my own customisations.

## Main Changes

Additions:

* Introduction of Notes screen for storing ideas and solving anagrams.
* Notes can be displayed on the game board.
* Automatically scale input box size on Clues List and Notes to fit the entire
  word on screen.
* Less intrusive keyboard pop-ups.
* Highlight selected clue and grey-out completed clues in Clues List.
* Reorganisation of game menu.

Removals:

* No Google email or games integration.
* No crashlytics.
* No New York Times.

The latter is for no reason other than i didn't want to maintain it. It could
be put back.

## Compilation

Gradle should compile fine.

    $ ./gradlew assembleRelease

You will then need to handle signing/installing the apk. Hopefully this is standard.

## Project Structure

  * ./app The Android App.
  * ./puzzlib A platform independent Java library for dealing with Across Lite and other puzzle formats
  * ./web A GWT/AppEngine web app for Shortyz that has fallen into disrepair.
  * ./gfx Art assets related to the Play Store publishing

License
-------

Copyright (C) 2010-2016 Robert Cooper (and 2018- Yourealwaysbe)

Licensed under the GNU General Public License, Version 3
