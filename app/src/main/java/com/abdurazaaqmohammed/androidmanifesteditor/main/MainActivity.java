package com.abdurazaaqmohammed.androidmanifesteditor.main;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.codyi96.xml2axml.Encoder;
import com.abdurazaaqmohammed.androidmanifesteditor.R;
import com.apk.axml.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {

    // Declaration of request codes for ActivityResult
    private static final int REQUEST_CODE_DECODE_XML = 1;
    private static final int REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML = 2;
    private static final int REQUEST_CODE_SAVE_ENCODED_XML = 4;
    private static final int REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING = 5;

    // Declaration of variables
    private boolean doNotSaveDecodedFile;
    private boolean useRegex;
    private boolean caseSensitive;
    private boolean encodeFromTextField = false;
    private boolean recompileAPK;
    private final boolean isOldAndroid = Build.VERSION.SDK_INT<19;
    private InputStream is;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);

        // Fetch settings from SharedPreferences
        useRegex = settings.getBoolean("useRegex", false);
        doNotSaveDecodedFile = settings.getBoolean("doNotSaveDecodedFile", false);
        caseSensitive = settings.getBoolean("caseSensitive", false);
        recompileAPK = settings.getBoolean("recompileAPK", false);
        // Configure switches
        Switch useRegexSwitch = findViewById(R.id.replaceWithRegexSwitch);
        useRegexSwitch.setChecked(useRegex);
        useRegexSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> useRegex = isChecked);
        Switch dontSaveDecodedSwitch = findViewById(R.id.doNotSaveDecodedSwitch);
        dontSaveDecodedSwitch.setChecked(doNotSaveDecodedFile);
        dontSaveDecodedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> doNotSaveDecodedFile = isChecked);
        Switch recompileAPKSwitch = findViewById(R.id.recompileAPKSwitch);
        recompileAPKSwitch.setChecked(recompileAPK);
        recompileAPKSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> recompileAPK = isChecked);

        if (isOldAndroid) {
            findViewById(R.id.oldAndroidInfo).setVisibility(View.VISIBLE);
            // Android versions below 4.4 are too old to use the file picker for ACTION_OPEN_DOCUMENT/ACTION_CREATE_DOCUMENT. The location of the file must be manually input. The files will be saved to Download folder in the internal storage.
        }

        // Configure main buttons
        findViewById(R.id.decodeButton).setOnClickListener(v -> openFilePickerOrStartProcessing(REQUEST_CODE_DECODE_XML));
        findViewById(R.id.encodeButton).setOnClickListener(v -> openFilePickerOrStartProcessing(REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML));
        findViewById(R.id.encodeFromField).setOnClickListener(v -> {
            openFileManagerToSaveEncodedXML();
            encodeFromTextField = true;
        });

        // Configure stuff for edit bar
        Button buttonSearch = findViewById(R.id.button_search);
        EditText editTextSearch = findViewById(R.id.editText_search);

        buttonSearch.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            EditText output = findViewById(R.id.outputField);
            String outputText =  caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();
            if (!findText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });

        Button buttonReplace = findViewById(R.id.button_rep);
        EditText textToReplaceWith = findViewById(R.id.toReplace);

        Button buttonPrev = findViewById(R.id.button_prev);
        buttonPrev.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            EditText output = findViewById(R.id.outputField);
            String outputText =  caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();
            if (!findPreviousText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });
        buttonReplace.setOnClickListener(v -> {
            String searchQuery = editTextSearch.getText().toString();
            if (!useRegex) searchQuery = escapeRegex(searchQuery);
            if (!caseSensitive) searchQuery = convertToUniversalCase(searchQuery);
            String replacementText = textToReplaceWith.getText().toString();

            EditText output = findViewById(R.id.outputField);
            String outputText = output.getText().toString();

            Pattern pattern = Pattern.compile(searchQuery);
            Matcher matcher = pattern.matcher(outputText);
            int startPos = output.getSelectionEnd();
            if (matcher.find(startPos)) {
                int start = matcher.start();
                int end = matcher.end();

                outputText = outputText.substring(0, start) + replacementText + outputText.substring(end);
                output.setText(outputText);

                output.setSelection(start, start + replacementText.length());
            } else {
                // Wrap around and search from the beginning if not found after reaching the end
                matcher.reset(); // Reset matcher to search from the beginning
                if (matcher.find()) {
                    int start = matcher.start();
                    int end = matcher.end();

                    outputText = outputText.substring(0, start) + replacementText + outputText.substring(end);
                    output.setText(outputText);

                    output.setSelection(start, start + replacementText.length());
                } else {
                    Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonReplaceAll = findViewById(R.id.button_repAll);
        buttonReplaceAll.setOnClickListener(v -> {
            String searchQuery = editTextSearch.getText().toString();
            if (!useRegex) searchQuery = escapeRegex(searchQuery);
            if (!caseSensitive) searchQuery = convertToUniversalCase(searchQuery);

            String replacementText = textToReplaceWith.getText().toString();
            EditText output = findViewById(R.id.outputField);
            String outputText = output.getText().toString();
            output.setText(outputText.replaceAll(searchQuery, replacementText));
        });

        // Configure settings menu
        findViewById(R.id.dropdown_menu).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.arrow_drop_down, popup.getMenu());
            popup.getMenu().findItem(R.id.regex).setTitle("(" + (useRegex ? "on" : "off") + ") " + getString(R.string.use_regex));
            popup.getMenu().findItem(R.id.caseSensitive).setTitle("(" + (caseSensitive ? "on" : "off") + ") " + getString(R.string.case_sensitive));
            popup.setOnMenuItemClickListener(menuItem -> {
                final int id = menuItem.getItemId();

                if (id == R.id.regex) {
                    useRegex = !useRegex;
                    useRegexSwitch.setChecked(useRegex);
                    return true;
                } else if (id == R.id.fix) {
                    EditText output = findViewById(R.id.outputField);
                    output.setText(output.getText().toString().replaceAll("<[^>]*(AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id|PlayCoreDialog)[^>]*(.*\\n.*\\n.*/(?!.*application).*>|.*\\n.*/.*>|>)", "").replace("isSplitRequired=\"true", "isSplitRequired=\"false").replaceAll("(splitTypes|requiredSplitTypes)=\".*\"", "").trim());
                    return true;
                } else if (id == R.id.caseSensitive) {
                    caseSensitive = !caseSensitive;
                    return true;
                } else if (id == R.id.goToBottom) {
                    EditText output = findViewById(R.id.outputField);
                    if (!output.isFocused()) output.requestFocus();
                    output.setSelection(output.getText().length(), output.getText().length());
                    return true;
                } else if (id == R.id.goToTop) {
                    EditText output = findViewById(R.id.outputField);
                    if (!output.isFocused()) output.requestFocus();
                    output.setSelection(1, 1);
                    return true;
                } else if (id == R.id.saveDecoded) {
                    callSaveFileResultLauncherForPlainTextData();
                    return true;
                } else if (id == R.id.mergeActivities) {

                    return true;
                }
                return false;
            });
            popup.show();
        });
        final Intent fromShareOrView = getIntent();
        final String fromShareOrViewAction = fromShareOrView.getAction();
        if (Intent.ACTION_SEND.equals(fromShareOrViewAction)) {
            Uri sharedUri = fromShareOrView.getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) processSharedFile(sharedUri);
        } else if (Intent.ACTION_VIEW.equals(fromShareOrViewAction)) {
            Uri sharedUri = fromShareOrView.getData();
            if (sharedUri != null) processSharedFile(sharedUri);
        }
    }

    private InputStream getAndroidManifestInputStreamFromZip(InputStream zipInputStream) throws IOException {
        ZipInputStream zipInput = new ZipInputStream(new BufferedInputStream(zipInputStream));
        ZipEntry entry;
        while ((entry = zipInput.getNextEntry()) != null) {
            if (entry.getName().equals("AndroidManifest.xml")) {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int length;
                while ((length = zipInput.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, length);
                }
                return new ByteArrayInputStream(outputStream.toByteArray());
            } else if (entry.getName().equals("base.apk")) return getAndroidManifestInputStreamFromZip(zipInput); // Somehow this works on xapk files even though base.apk is renamed to the app package name
            zipInput.closeEntry();
        }

        // AndroidManifest.xml not found in the zip file
        return null;
    }
    private String escapeRegex(String input) {
        // List of characters that need to be escaped in a regex
        String specialChars = "\\[]{}()^$|*+?.";

        StringBuilder escapedString = new StringBuilder();

        for (char c : input.toCharArray()) {
            // Check if the character is a special character
            if (specialChars.indexOf(c) != -1) {
                // Escape the special character by adding a backslash before it
                escapedString.append("\\").append(c);
            } else {
                // Otherwise, just append the character as is
                escapedString.append(c);
            }
        }

        return escapedString.toString();
    }
    private String convertToUniversalCase(String input) {
        StringBuilder regexBuilder = new StringBuilder();

        for (char c : input.toCharArray()) {
            if (Character.isLetter(c)) {
                regexBuilder.append("[");
                regexBuilder.append(Character.toLowerCase(c));
                regexBuilder.append(Character.toUpperCase(c));
                regexBuilder.append("]");
            } else {
                regexBuilder.append(c);
            }
        }

        return regexBuilder.toString();
    }
    private void processSharedFile(Uri sharedUri) {
        try {
            final String mimeType = getApplicationContext().getContentResolver().getType(sharedUri);

            if (mimeType.startsWith("app")) {
                InputStream zipInputStream = getContentResolver().openInputStream(sharedUri);
                is = getAndroidManifestInputStreamFromZip(zipInputStream);
                isAPK = true;
                zipUriForRepacking = sharedUri;
                decode("text/xml");
            } else {
                is = getContentResolver().openInputStream(sharedUri);
                decode("text/xml");
            }
        } catch (IOException e) {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            final String error = e.toString();
            errorBox.setText(error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }
    private void decode(String mimeType) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String content = "";
            if (mimeType.endsWith("plain")) {
                String line;
                while ((line = br.readLine()) != null) {
                    content = content + "\n" + line;
                }
            } else {
                content = new aXMLDecoder().decode(is).trim();
                if (!doNotSaveDecodedFile) callSaveFileResultLauncherForPlainTextData();
            }
            boolean realRegexValue;
            EditText outputField = findViewById(R.id.outputField);
            if (!doNotSaveDecodedFile) {
                callSaveFileResultLauncherForPlainTextData();
            }

            outputField.setText(content);

            realRegexValue = useRegex;
            useRegex = true;
            if(findText("PlayCoreDialog|AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id", content)) {
                TextView errorBox = findViewById(R.id.errorField);
                errorBox.setVisibility(View.VISIBLE);
                final String error = getString(R.string.useless_info);
                errorBox.setText(error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }
            useRegex = realRegexValue;

            findViewById(R.id.encodeFromField).setVisibility(View.VISIBLE);
            findViewById(R.id.editBar).setVisibility(View.VISIBLE);
        } catch (IOException | XmlPullParserException e) {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            final String error = e.toString();
            errorBox.setText(error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                return deleteDir(new File(dir, child));
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    @Override
    protected void onPause() {
        final SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        settings.edit().putBoolean("useRegex", useRegex).putBoolean("doNotSaveDecodedFile", doNotSaveDecodedFile).putBoolean("caseSensitive", caseSensitive).putBoolean("recompileAPK", recompileAPK).apply();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        deleteDir(getExternalCacheDir());
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.arrow_drop_down, menu);
        return true;
    }

    private boolean findPreviousText(String query, String outputText) {
        EditText output = findViewById(R.id.outputField);
        int startPos = output.getSelectionStart();

        if (!output.isFocused()) output.requestFocus();

        if (useRegex) {
            Pattern pattern = Pattern.compile(query);
            Matcher matcher = pattern.matcher(outputText);

            int lastFoundPos = -1;
            while (matcher.find()) {
                int foundPos = matcher.start();
                if (foundPos >= startPos) break; // Exit the loop if found position is after the current position
                lastFoundPos = foundPos;
            }

            if (lastFoundPos != -1) {
                int endPos = matcher.end();
                output.setSelection(lastFoundPos, endPos);
                return true;
            }
        } else {
            int foundPos = -1;

            // First, search backward from the current position
            foundPos = outputText.lastIndexOf(query, startPos - 1);
            if (foundPos != -1) {
                output.setSelection(foundPos, foundPos + query.length());
                return true;
            }

            // If not found, wrap around and search from the end
            foundPos = outputText.lastIndexOf(query);
            if (foundPos != -1) {
                output.setSelection(foundPos, foundPos + query.length());
                return true;
            }
        }
        return false;
    }

    private boolean findText(String query, String outputText) {

        EditText output = findViewById(R.id.outputField);
        int startPos = output.getSelectionEnd();

        if(!output.isFocused()) output.requestFocus();
        if(useRegex) {
            Pattern pattern = Pattern.compile(query);
            Matcher matcher = pattern.matcher(outputText);
            while (matcher.find(startPos)) {
                int foundPos = matcher.start();
                int endPos = matcher.end();

                output.setSelection(foundPos, endPos);
                return true;
            }
            matcher = pattern.matcher(outputText);
            if (matcher.find()) {
                int foundPos = matcher.start();
                int endPos = matcher.end();

                output.setSelection(foundPos, endPos);
                return true;
            }

        } else {
            int foundPos = outputText.indexOf(query, startPos);
            if (foundPos != -1) {
                output.setSelection(foundPos, foundPos + query.length());
                return true;
            } else {
                // Wrap around and search from the beginning if not found after reaching the end
                foundPos = outputText.indexOf(query);
                if (foundPos != -1) {
                    output.setSelection(foundPos, foundPos + query.length());
                    return true;
                }
            }
        }
        return false;
    }
    private void openFilePickerOrStartProcessing(int requestCode) {
        if(isOldAndroid) {
            TextView t = findViewById(R.id.workingFileField);
            Uri uri = Uri.parse(t.getText().toString());
            if (uri != null) {
                try {
                    ContentResolver contentResolver = getContentResolver();
                    InputStream inputStream = contentResolver.openInputStream(uri);
                    if (inputStream != null) {
                        switch (requestCode) {
                            case REQUEST_CODE_DECODE_XML:
                                startActivityForResult(new Intent(), REQUEST_CODE_DECODE_XML);
                                break;
                            case REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML:
                                openFileManagerToSaveEncodedXML();
                                break;
                        }
//                        inputStream.close();
                    }
                } catch (IOException e) {
                    TextView errorBox = findViewById(R.id.errorField);
                    errorBox.setVisibility(View.VISIBLE);
                    final String error = e.toString();
                    errorBox.setText(error);
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/vnd.android.package-archive", "application/zip", "application/octet-stream", "text/plain", "text/xml"}); // XAPK is octet-stream
            startActivityForResult(intent, requestCode);
        }
    }

    private void callSaveFileResultLauncherForPlainTextData() {
        String fileName = "decoded";
        Intent saveFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        saveFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        saveFileIntent.setType("text/plain");
        saveFileIntent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(saveFileIntent, REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING);
    }

    private void extractZip(InputStream zi, File outputDir, byte[] data) throws IOException {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zis = new ZipInputStream(zi)) {
            ZipEntry zipEntry = zis.getNextEntry();
            while (zipEntry != null) {
                File newFile = newFile(outputDir, zipEntry);
                if(zipEntry.getName().startsWith("AndroidManifest")) {
                    FileOutputStream fos = new FileOutputStream(newFile);
                    fos.write(data);
                    fos.close();
                }
                else if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory: " + newFile);
                    }
                } else {
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory: " + parent);
                    }

                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        }
    }
    private boolean isAPK = false;
    private Uri zipUriForRepacking;
    private static File newFile(File outputDir, ZipEntry zipEntry) throws IOException {
        File file = new File(outputDir, zipEntry.getName());
        String canonicalizedPath = file.getCanonicalPath();
        if (!canonicalizedPath.startsWith(outputDir.getCanonicalPath() + File.separator)) {
            throw new IOException("Zip entry is outside of the target dir: " + zipEntry.getName());
        }
        return file;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                EditText outputField = findViewById(R.id.outputField);
                TextView workingFileField = findViewById(R.id.workingFileField);
                workingFileField.setText(uri.getPath());
                try {
                    switch (requestCode) {
                        case REQUEST_CODE_DECODE_XML:
                            final String mimeType = getApplicationContext().getContentResolver().getType(uri);
                            if (mimeType.startsWith("app")) {
                                InputStream zipInputStream = getContentResolver().openInputStream(uri);
                                is = getAndroidManifestInputStreamFromZip(zipInputStream);
                                zipUriForRepacking = uri;
                                isAPK = true;
                            } else is = getContentResolver().openInputStream(uri);

                            decode(mimeType);
                            break;
                        case REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML:
                            is = getContentResolver().openInputStream(uri);
                            openFileManagerToSaveEncodedXML();
                            break;
                        case REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING:
                            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                            OutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());
                            outputStream.write(outputField.getText().toString().getBytes());
                            outputStream.flush();
                            outputStream.close();
                            pfd.close();
                            break;
                        case REQUEST_CODE_SAVE_ENCODED_XML:
                            byte[] encodedData;
                            if (encodeFromTextField) {
                                String fromTextField = outputField.getText().toString().trim();
                                encodedData = fromTextField.isEmpty() ? encodeFromFile() : new Encoder().encodeString(this, fromTextField);
                                // in aXML(https://github.com/apk-editor/aXML) there is some bug that breaks encoding xml files which has an empty attribute ("") because it encodes it to "null" instead of "" for some reason (U can check in np manager, its an encoding problem not a decoding one) Idk why it happens but the old axml2xml (https://github.com/codyi96/xml2axml) doesnt have that problem
                                // // Something in aXML causes blank fields (eg. taskAffinity="") to be encoded as "null" instead of "" which breaks the app. I don't know where it is but I need to find it, I'm not sure if simply removing the empty attribute will work or break anything.
                            } else {
                                encodedData = encodeFromFile();
                            }

                            if(isAPK && recompileAPK) {
                                deleteDir(getExternalCacheDir());
                                extractZip(getContentResolver().openInputStream(zipUriForRepacking), getExternalCacheDir(), encodedData);
                                zipFolder(getExternalCacheDir(), getContentResolver().openOutputStream(uri));
                            } else {
                                FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);

                                fos.write(encodedData);
                                fos.close();
                            }
                            Toast.makeText(this, "XML encoded and saved to " + (recompileAPK ? "APK" : "XML") + " file successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException | XmlPullParserException e) {
                    TextView errorBox = findViewById(R.id.errorField);
                    errorBox.setVisibility(View.VISIBLE);
                    final String error = e.toString();
                    errorBox.setText(error);
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    private byte[] encodeFromFile() throws XmlPullParserException, IOException {
        // If encoding from file the input stream will be the decoded XML
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, "UTF-8");
        return Encoder.encode(getApplicationContext(), parser);
    }

    public static void zipFolder(final File folder, final OutputStream outputStream) throws IOException {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            processFolder(folder, zipOutputStream, folder.getPath().length() + 1);
        }
    }

    private static void processFolder(final File folder, final ZipOutputStream zipOutputStream, final int prefixLength) throws IOException {
        for (final File file : folder.listFiles()) {
            if (file.isFile()) {
                final ZipEntry zipEntry = new ZipEntry(file.getPath().substring(prefixLength));
                if(file.getPath().contains("/res") && !file.getName().endsWith(".xml")) {
                    zipOutputStream.setMethod(ZipOutputStream.STORED);
                    zipEntry.setSize(file.length());
                    CRC32 crc = new CRC32();
                    crc.update(Files.readAllBytes(Paths.get(file.getPath())));
                    zipEntry.setCrc(crc.getValue());
                } else {
                    zipOutputStream.setMethod(ZipOutputStream.DEFLATED);
                }
                zipOutputStream.putNextEntry(zipEntry);
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    byte [] buffer = new byte[1024 * 4];     int read = 0;     while ((read = inputStream.read(buffer)) != -1) {         zipOutputStream.write(buffer, 0, read);     }
                }
                zipOutputStream.closeEntry();
            } else if (file.isDirectory()) {
                processFolder(file, zipOutputStream, prefixLength);
            }
        }
    }

    private void openFileManagerToSaveEncodedXML() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(recompileAPK ? "application/vnd.android.package-archive" : "text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, recompileAPK ? "edited.apk" : "AndroidManifest.xml"); // when I support parsing all xml files from an apk need to find a way to get the chosen file name
        startActivityForResult(intent, REQUEST_CODE_SAVE_ENCODED_XML);
    }
}
