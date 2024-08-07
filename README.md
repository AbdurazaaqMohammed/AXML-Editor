Android app to edit AXML - Android binary XML files (AndroidManifest.xml and layout XML files) without having to decompile all resources

# Usage
## Decoding
There are 3 ways to open the split APK to be merged:
* Share the file and select Edit AXML in the share menu
* Press (open) the file and select Edit AXML in available options
* Open the app from launcher and press the "Decode" button then select an APK or XML file.
   * This option does not work on Android < 4.4, use one of the 2 other options or type the path to the APK (on your device storage) into the box in the app.

If you open an already decoded file you can edit it in the app. 

Note: Editing layout files is supported, if you extract from /res/ or share them with the app. If you share an APK file in the app, it will open AndroidManifest.xml, though I want to display a list of all the XML files from /res/ but I didn't implement it yet.
## Encoding
If there is any text opened in the edit field, a button ("Encode from Field") will appear to encode from that field. You can encode from a saved text file by pressing the "Encode" button directly after opening the app.

# About

This project is a simple GUI implementation of axml2xml by [apk-editor](https://github.com/apk-editor/aXML), [codyi96](https://github.com/codyi96/xml2axml), [hzw1199](https://github.com/hzw1199/xml2axml) and [l741589](https://github.com/l741589/xml2axml) + [AXMLPrinter](https://github.com/developer-krushna/AXMLPrinter) by developer-krushna.
It has an inbuilt text editor with search and replace (supports regex and case sensitivity).

There are already several apps that perform this task, but they all have some problem;
* [APK Explorer & Editor](https://github.com/apk-editor/APK-Explorer-Editor) - no search and replace
* NP Manager - closed source Chinese app with tracking
* MT Manager - requires VIP (paid)
* Modder Hub - closed source

AEE and Modder Hub also inherit an issue from axml2xml where XML files that contain large integers will fail to be encoded. The fix was implemented in AEE but Integer.parseInt is called before checking if the value is too big so it still crashes ([this should be fixed soon](https://github.com/apk-editor/aXML/pull/1/commits/dec819e45c17405baefa48946ad5dba64ad0d1f5))

## Used projects
* axml2xml by [apk-editor](https://github.com/apk-editor/aXML), [codyi96](https://github.com/codyi96/xml2axml), [hzw1199](https://github.com/hzw1199/xml2axml) and [l741589](https://github.com/l741589/xml2axml)
* [AXMLPrinter](https://github.com/developer-krushna/AXMLPrinter) by [developer-krushna](https://github.com/developer-krushna)
* [AmbilWarna Color Picker](https://github.com/yukuku/ambilwarna)
* https://github.com/MuntashirAkon/apksig-android for signing APKs

# Todo
* Support parsing all xml files from an apk (List all xml files in the apk to the user for selection)
  * (Add setting to always just open AndroidManifest)
* Add line numbers/Use an actual code editor with syntax
* try to figure out how to fix obfuscated XML files like MT Manager does
* Add quick refactoring options
  * Quick edit version name, SDK versions etc. like how APK Editor and Apktool M do it
  * Merge or switch 2 activities
    * (Option to maintain attributes from both)
  * Add/remove/disable permissions/elements (Like how the menu in Lucky Patcher is)

## Done
* Added buttons (in settings menu) to jump to the top/bottom of text
* Added support for selecting an APK (including split APKs; XAPK, APKS, APKM) and editing AndroidManifest.xml from it
* Added option to save APK with the updated manifest if an APK file was selected.
  * Added option to sign the modified APK
* Added option to remove all elements or specific types (Activities/metadata/etc) containing input string/regex
