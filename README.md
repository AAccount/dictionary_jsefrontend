# Java Swing Dictionary Frontend

## History
Front end for the dictionary core repo in Java Swing. Swing was chosen to generate portable executables that will run on desktop Linux and occasionally Mac OS. This program was originally written for python gtk at the end of 2020. I was gifted a free Intel Mac in 2022-07, and found Mac Ports an absolute pain to install gtk. It boiled the Mac alive trying compile multiple tool chains and other massive libraries. Further infuriating, is that every minor Mac Ports update would seemingly trigger an entire gcc AND llvm recompile reboiling the Mac. The entire program was ported to Java Swing at the end of 2022 to avoid prematurely trashing a very nice free gift.

*This program has never been tested in Windows, but will probably work.*

## Overview
This front end does not do much thinking. It simply passes your search to the dictionary core's `DbService.lookupChinese` or `DbService.lookupEnglish`. It then creates tabs for each search result. For Chinese, this means a tab for the dictionary definition of the whole string, and a tab for each alternate search that has >= 1 result. If the whole string has no dictionary entry, that tab renders blank. For English, every possible word combination with >= 1 Chinese entry is shown. If a tab has > 10 entries, it is paginated in groups of 10. See the dictionary core repo for the search strategy.

The front end keeps a forwards, backwards, navigation history of 10 entries. 

The front end has several feature flags:
- show rank (default false): in the alternate search strategy tabs, show the actual ranking of each entry. Used for debugging non sense prioritizing.
- always show single substrings (default false): for substring search where the original entry was 3+ chars, show the individual single char definitions.
- auto swap (default true): fcitx5 is fairly conservative for what it considers traditional Chinese. Unfortunately, this can be "too traditional" sometimes. Convert some of these ultra conservative forms to more normal traditional forms to avoid false negatives: "爲" -> "為", for example.
- show tracer: (default false) show a blue border around UI elements. Useful for annoying UI debugging.
- save hits (default true): save to the sqlite db when you last looked up a Chinese word and it had a dictionary entry

## Build instructions
Checkout this repo, dictionary core, and cedict in the same folder.

Create the file `.vscode/settings.json` in the folder.
```
{
  "java.project.sourcePaths": [
    "CedictParser/src",
    "DictionaryCore/src",
    "JSEFrontEnd/src"
  ],
  "java.project.referencedLibraries": [
    "sqlite-jdbc-3.53.2.0.jar"
  ]
}
```
Run from `App.java`. Use VSCodium's/VSCode's export feature to generate a jar and include the jdbc library.
