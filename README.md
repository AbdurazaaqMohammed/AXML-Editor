Android app to edit android binary XML files (AndroidManifest.xml and layout XML files) without having to decompile all resources

This project is a simple GUI implementation of axml2xml by [apk-editor](https://github.com/apk-editor/aXML), [codyi96](https://github.com/codyi96/xml2axml), [hzw1199](https://github.com/hzw1199/xml2axml) and [l741589](https://github.com/l741589/xml2axml).
It has an inbuilt text editor with search and replace (supports regex and case sensitivity).

There are already several apps that perform this task, but they all have some problem;
* [APK Explorer & Editor](https://github.com/apk-editor/APK-Explorer-Editor) - no search and replace
* NP Manager - closed source Chinese app with tracking
* MT Manager - requires VIP (paid)
* Modder Hub - closed source
AEE and Modder Hub also inherit an issue from axml2xml where XML files that contain large integers will fail to be encoded. The fix was implemented in AEE but Integer.parseInt is called before checking if the value is too big so it still crashes (this should be fixed soon)

## Todo
* make it actually work on SDK<19
* support parsing xml files from an apk (List all xml files in the apk to the user for selection)
* add button to go to top/bottom of text
