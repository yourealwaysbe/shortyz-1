# Forkyz Crosswords

Forkyz is an unofficial fork of [Shortyz](https://github.com/kebernet/shortyz/)
implementing my own customisations.

## Main Changes

Additions:

* Updated file handling compatible with Android 11.
* Download of daily Guardian and Independent Cryptics.
* Weeding/updating of downloadable puzzles.
* Less crazy night mode colors and night mode by system theme.
* Clues view on board page for smaller screens.
* Update to androidx libraries.
* History of viewed clues in clues lists.
* Progress measures number of filled boxes instead of correctly filled
  boxes (no cheating).
* Memory of last highlighted clue (implementation completed).
* Introduction of Notes screen for storing ideas and solving anagrams.
* Notes can be displayed on the game board.
* Automatically scale input box size on Clues List and Notes to fit the entire
  word on screen.
* Less intrusive keyboard pop-ups.
* Highlight selected clue and grey-out completed clues in Clues List.
* Reorganisation of game menu.
* Scale clue size inside clue line.
* Option to not delete characters of crossing words when deleting.

Removals:

* No Google email or games integration.
* No crashlytics.
* No New York Times or layouts for large screens.
* No native keyboard.

The penultimate removal is for no reason other than i didn't want to
maintain them. They could be put back.

The native keyboard was removed because soft input methods are
discouraged from sending key events.

## Compilation

Gradle should compile fine.

    $ ./gradlew assembleRelease

You will then need to handle signing/installing the apk. Hopefully this is standard.

## Project Structure

  * ./app The Android App.
  * ./puzzlib A platform independent Java library for dealing with Across Lite and other puzzle formats
  * ./gfx Misc art assets

License
-------

Copyright (C) 2010-2016 Robert Cooper (and 2018- Yourealwaysbe)

Licensed under the GNU General Public License, Version 3
