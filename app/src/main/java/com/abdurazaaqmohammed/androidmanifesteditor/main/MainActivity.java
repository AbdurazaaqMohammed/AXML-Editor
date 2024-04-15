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

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import com.codyi96.xml2axml.Encoder;
import com.abdurazaaqmohammed.androidmanifesteditor.R;
import com.codyi96.xml2axml.test.AXMLPrinter;
import com.apk.axml.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {

    // Declaration of request codes for ActivityResult
    private static final int REQUEST_CODE_DECODE_XML = 1;
    private static final int REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML = 2;
    private static final int REQUEST_CODE_SAVE_DECODED_XML = 3;
    private static final int REQUEST_CODE_SAVE_ENCODED_XML = 4;
    private static final int REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING = 5;

    // Declaration of variables
    private boolean saveAsTxt;
    private boolean doNotSaveDecodedFile;
    private boolean useRegex;
    private boolean caseSensitive;
    private boolean fromShare = false;
    private boolean encodeFromTextField = false;
    private final boolean isOldAndroid = Build.VERSION.SDK_INT<19;
    private InputStream is;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().hide();
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);

        // Fetch settings from SharedPreferences
        saveAsTxt = settings.getBoolean("saveAsTxt", false);
        useRegex = settings.getBoolean("useRegex", false);
        doNotSaveDecodedFile = settings.getBoolean("doNotSaveDecodedFile", false);
        caseSensitive = settings.getBoolean("caseSensitive", false);

        // Configure switches
        Switch useRegexSwitch = findViewById(R.id.replaceWithRegexSwitch);
        useRegexSwitch.setChecked(useRegex);
        useRegexSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> useRegex = isChecked);
        Switch saveAsTxtSwitch = findViewById(R.id.saveAsTxtSwitch);
        saveAsTxtSwitch.setChecked(saveAsTxt);
        saveAsTxtSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveAsTxt = isChecked);
        Switch dontSaveDecodedSwitch = findViewById(R.id.doNotSaveDecodedSwitch);
        dontSaveDecodedSwitch.setChecked(doNotSaveDecodedFile);
        dontSaveDecodedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> doNotSaveDecodedFile = isChecked);

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
            String outputText = output.getText().toString().toLowerCase();
            if (!findText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });

        Button buttonReplace = findViewById(R.id.button_rep);
        EditText textToReplaceWith = findViewById(R.id.toReplace);

        Button buttonPrev = findViewById(R.id.button_prev);
        buttonPrev.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            EditText output = findViewById(R.id.outputField);
            String outputText = output.getText().toString().toLowerCase();
            if (!findPreviousText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });

        buttonReplace.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            String replacementText = textToReplaceWith.getText().toString();

            EditText output = findViewById(R.id.outputField);
            String outputText = caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();

            int startPos = output.getSelectionEnd();
            int foundPos = outputText.indexOf(searchQuery, startPos);

            if (foundPos != -1) {
                // Replace the found text with the replacement text
                outputText = useRegex ? outputText.replaceFirst(searchQuery, replacementText) :
                        (outputText.substring(0, foundPos) + replacementText +
                                outputText.substring(foundPos + searchQuery.length()));
                output.setText(outputText);
                output.setSelection(foundPos, foundPos + replacementText.length());
            } else {
                // Wrap around and search from the beginning if not found after reaching the end
                foundPos = outputText.indexOf(searchQuery);
                if (foundPos != -1) {
                    outputText = useRegex ? outputText.replaceFirst(searchQuery, replacementText) :
                            (outputText.substring(0, foundPos) + replacementText +
                                    outputText.substring(foundPos + searchQuery.length()));
                    output.setText(outputText);
                    output.setSelection(foundPos, foundPos + replacementText.length());
                } else {
                    Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
                }
            }
        });

        Button buttonReplaceAll = findViewById(R.id.button_repAll);
        buttonReplaceAll.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            String replacementText = textToReplaceWith.getText().toString();
            EditText output = findViewById(R.id.outputField);
            String outputText = caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();
            output.setText(useRegex ? outputText.replaceAll(searchQuery, replacementText) : outputText.replace(searchQuery, replacementText));
        });

        // Configure settings menu
        findViewById(R.id.dropdown_menu).setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(this, v);
            popup.getMenuInflater().inflate(R.menu.arrow_drop_down, popup.getMenu());
            popup.getMenu().findItem(R.id.regex).setTitle(getString(R.string.use_regex) + " (" + (useRegex ? "on" : "off") + ")");
            popup.getMenu().findItem(R.id.caseSensitive).setTitle(getString(R.string.case_sensitive) + " (" + (caseSensitive ? "on" : "off") + ")");
            popup.setOnMenuItemClickListener(menuItem -> {
                final int id = menuItem.getItemId();

                if (id == R.id.regex) {
                    useRegex = !useRegex;
                    useRegexSwitch.setChecked(useRegex);
                    return true;
                } else if (id == R.id.fix) {
                    EditText output = findViewById(R.id.outputField);
                    output.setText(output.getText().toString().replaceAll("<[^>]*(assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id|PlayCoreDialog)[^>]*(.*\n.*/.*>|>)", "").replaceAll("<\\/service>\n\n", "").replace("isSplitRequired=\"true", "isSplitRequired=\"false").replaceAll("splitTypes=\".*\"", ""));
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
                }
                return false;
            });
            popup.show();
        });

        if (Intent.ACTION_SEND.equals(getIntent().getAction())) {
            fromShare = true;
            Uri sharedUri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
            if (sharedUri != null) processSharedFile(sharedUri);
        }
    }

    private void processSharedFile(Uri sharedUri) {
        try {
            final String mimeType = getApplicationContext().getContentResolver().getType(sharedUri);

            if (mimeType.startsWith("app")) {
                InputStream zipInputStream = getContentResolver().openInputStream(sharedUri);
                is = ZipUtils.getAndroidManifestInputStreamFromZip(zipInputStream);
                decode();
            } else {
                is = getContentResolver().openInputStream(sharedUri);

                BufferedReader br = new BufferedReader(new InputStreamReader(is));
                // Check if the file should be encoded or decoded
                if (mimeType.endsWith("plain") || br.readLine().startsWith("<?xml version")) {
                    EditText output = findViewById(R.id.outputField);
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.setText(output.getText().toString() + "\n" + line);
                    }
                    findViewById(R.id.encodeFromField).setVisibility(View.VISIBLE);
                    findViewById(R.id.editBar).setVisibility(View.VISIBLE);
                }
                else decode();
            }
        } catch (IOException e) {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            final String error = e.toString();
            errorBox.setText(error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        }
    }
    private void decode() {
        try {
            final String decodedXML = new aXMLDecoder().decode(is).trim();

            EditText outputField = findViewById(R.id.outputField);
            if (!doNotSaveDecodedFile) {
                callSaveFileResultLauncherForPlainTextData();
            }

            outputField.setText(decodedXML);

            if (decodedXML.contains("assetpack") || decodedXML.contains("MissingSplit") || decodedXML.contains("com.android.dynamic.apk.fused.modules") || decodedXML.contains("com.android.stamp.source") || decodedXML.contains("com.android.stamp.type") || decodedXML.contains("com.android.vending.splits") || decodedXML.contains("com.android.vending.derived.apk.id")) {
                TextView errorBox = findViewById(R.id.errorField);
                errorBox.setVisibility(View.VISIBLE);
                final String error = getString(R.string.useless_info);
                errorBox.setText(error);
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
            }

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

    @Override
    protected void onPause() {
        final SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        settings.edit().putBoolean("useRegex", useRegex).putBoolean("doNotSaveDecodedFile", doNotSaveDecodedFile).putBoolean("saveAsTxt", saveAsTxt).putBoolean("caseSensitive", caseSensitive).apply();
        super.onPause();
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
                        inputStream.close();
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
            intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[] {"application/vnd.android.package-archive", "application/zip", "text/plain", "text/xml"});
            startActivityForResult(intent, requestCode);
        }
    }

    private void callSaveFileResultLauncherForPlainTextData() {
        String fileName = "decoded" ;
        Intent saveFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        saveFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
        saveFileIntent.setType(saveAsTxt ? "text/plain" : "text/xml");
        saveFileIntent.putExtra(Intent.EXTRA_TITLE, fileName);
        startActivityForResult(saveFileIntent, REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING);
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
                            if(getApplicationContext().getContentResolver().getType(uri).startsWith("app")) {
                                InputStream zipInputStream = getContentResolver().openInputStream(uri);
                                is = ZipUtils.getAndroidManifestInputStreamFromZip(zipInputStream);
                            } else is = getContentResolver().openInputStream(uri);
                            final String decodedXML = new aXMLDecoder().decode(is).trim();
                            if(!doNotSaveDecodedFile) callSaveFileResultLauncherForPlainTextData();
                            outputField.setText(decodedXML);

                            if(decodedXML.contains("assetpack") || decodedXML.contains("MissingSplit") || decodedXML.contains("com.android.dynamic.apk.fused.modules") || decodedXML.contains("com.android.stamp.source") || decodedXML.contains("com.android.stamp.type") || decodedXML.contains("com.android.vending.splits") || decodedXML.contains("com.android.vending.derived.apk.id")) {
                                TextView errorBox = findViewById(R.id.errorField);
                                errorBox.setVisibility(View.VISIBLE);
                                final String error = getString(R.string.useless_info);
                                errorBox.setText(error);
                                Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                            }
                            findViewById(R.id.encodeFromField).setVisibility(View.VISIBLE);
                            findViewById(R.id.editBar).setVisibility(View.VISIBLE);
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
                        case REQUEST_CODE_SAVE_DECODED_XML:
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            AXMLPrinter.decode(is, os);
                            Toast.makeText(this, "XML decoded and saved successfully", Toast.LENGTH_SHORT).show();
                            break;
                        case REQUEST_CODE_SAVE_ENCODED_XML:
                            final String fromTextField = outputField.getText().toString();

                            FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
                            byte[] encodedData;
                            if(encodeFromTextField && !fromTextField.isEmpty()) {
                                encodedData = new aXMLEncoder().encodeString(this, fromTextField);
                            } else {
                                // If encoding from file the input stream will be the decoded XML
                                XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
                                factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
                                XmlPullParser parser = factory.newPullParser();
                                parser.setInput(is, "UTF-8");
                                encodedData = Encoder.encode(getApplicationContext(), parser);
                            }

                            fos.write(encodedData);
                            fos.close();
                            Toast.makeText(this, "XML encoded and saved successfully", Toast.LENGTH_SHORT).show();
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

    private void openFileManagerToSaveEncodedXML() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, "encoded.xml");
        startActivityForResult(intent, REQUEST_CODE_SAVE_ENCODED_XML);
    }
}
