package com.abdurazaaqmohammed.androidmanifesteditor.main;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.abdurazaaqmohammed.androidmanifesteditor.R;
import com.apk.axml.aXMLEncoder;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import mt.modder.hub.axml.AXMLPrinter;
import yuku.ambilwarna.AmbilWarnaDialog;

public class MainActivity extends Activity {

    // Declaration of request codes for ActivityResult
    private static final int REQUEST_CODE_DECODE_XML = 1;
    private static final int REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML = 2;
    private static final int REQUEST_CODE_SAVE_ENCODED_XML = 4;
    private static final int REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING = 5;

    // Declaration of variables
    private static boolean saveDecodedFile;
    private static boolean useRegex;
    private static boolean caseSensitive;
    private static boolean encodeFromTextField = false;
    private static boolean recompileAPK;
    private static boolean isAPK;
    private final static boolean doesntSupportBuiltInAndroidFilePicker = Build.VERSION.SDK_INT<19;
    private final static boolean supportsAsyncTask = Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE;
    private InputStream is;

    private void setTextColor(int textColor) {
        ((EditText) findViewById(R.id.outputField)).setTextColor(textColor);
        ((TextView) findViewById(R.id.recompileApkLabel)).setTextColor(textColor);
        ((TextView) findViewById(R.id.saveDecodedFilesLabel)).setTextColor(textColor);
        ((TextView) findViewById(R.id.useRegexLabel)).setTextColor(textColor);

        android.widget.EditText search = findViewById(R.id.editText_search);
        search.setTextColor(textColor);
        search.setHintTextColor(textColor);
        android.widget.EditText replace = findViewById(R.id.toReplace);
        replace.setTextColor(textColor);
        replace.setHintTextColor(textColor);
        android.widget.TextView errorField = findViewById(R.id.errorField);
        errorField.setHintTextColor(textColor);
        errorField.setTextColor(textColor);
        android.widget.TextView workingFile = findViewById(R.id.workingFileField);
        workingFile.setTextColor(textColor);
        workingFile.setHintTextColor(textColor);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            try {
                Objects.requireNonNull(getActionBar()).hide();
            } catch (NullPointerException ignored) {}
        }
        setContentView(R.layout.activity_main);

        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);

        // Fetch settings from SharedPreferences
        useRegex = settings.getBoolean("useRegex", false);
        saveDecodedFile = settings.getBoolean("saveDecodedFile", false);
        caseSensitive = settings.getBoolean("caseSensitive", false);
        recompileAPK = settings.getBoolean("recompileAPK", false);
        EditText output = findViewById(R.id.outputField);


        // Configure switches
        ToggleButton useRegexSwitch = findViewById(R.id.replaceWithRegexSwitch);
        useRegexSwitch.setChecked(useRegex);
        useRegexSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> useRegex = isChecked);
        ToggleButton saveDecodedSwitch = findViewById(R.id.saveDecodedSwitch);
        saveDecodedSwitch.setChecked(saveDecodedFile);
        saveDecodedSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> saveDecodedFile = isChecked);
        ToggleButton recompileAPKSwitch = findViewById(R.id.recompileAPKSwitch);
        recompileAPKSwitch.setChecked(recompileAPK);
        recompileAPKSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> recompileAPK = isChecked);

        setTextColor(settings.getInt("textColor", 0xFF691383));
        RelativeLayout background = findViewById(R.id.main);
        background.setBackgroundColor(settings.getInt("backgroundColor", 0xff000000));

        if (doesntSupportBuiltInAndroidFilePicker) {
            findViewById(R.id.oldAndroidInfo).setVisibility(View.VISIBLE);
            // Android versions below 4.4 are too old to use the file picker for ACTION_OPEN_DOCUMENT/ACTION_CREATE_DOCUMENT.
            // The location of the file must be manually input. The files will be saved to Download folder in the internal storage.
        }

        // Configure main buttons
        findViewById(R.id.decodeButton).setOnClickListener(v -> openFilePickerOrStartProcessing(REQUEST_CODE_DECODE_XML));
        findViewById(R.id.encodeButton).setOnClickListener(v -> openFilePickerOrStartProcessing(REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML));
        findViewById(R.id.encodeFromField).setOnClickListener(v -> {
            if(doesntSupportBuiltInAndroidFilePicker) {
                String fromTextField = output.getText().toString().trim();

                byte[] encodedData;
                try {
                    encodedData = fromTextField.length()==0 ? encodeFromFile() : new aXMLEncoder().encodeString(this, fromTextField);
                    TextView t = findViewById(R.id.workingFileField);
                    final String filePath = t.getText().toString();
                    if (supportsAsyncTask) {
                        new saveAsyncTask(this, encodedData).execute(zipUriForRepacking, Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/Download/" + filePath.substring(filePath.lastIndexOf("/") + 1))));
                    } else process(zipUriForRepacking, Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/Download/" + filePath.substring(filePath.lastIndexOf("/") + 1))), encodedData);
                } catch (XmlPullParserException | IOException e) {
                    showError(e.toString());
                }
            } else {
                openFileManagerToSaveEncodedXML();
                encodeFromTextField = true;
            }
        });

        // Configure stuff for edit bar
        Button buttonSearch = findViewById(R.id.button_search);
        EditText editTextSearch = findViewById(R.id.editText_search);

        buttonSearch.setOnClickListener(v -> {
            final String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            final String outputText =  caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();
            if (!findText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });

        Button buttonReplace = findViewById(R.id.button_rep);
        EditText textToReplaceWith = findViewById(R.id.toReplace);

        Button buttonPrev = findViewById(R.id.button_prev);
        buttonPrev.setOnClickListener(v -> {
            String searchQuery = caseSensitive ? editTextSearch.getText().toString() : editTextSearch.getText().toString().toLowerCase();
            String outputText =  caseSensitive ? output.getText().toString() : output.getText().toString().toLowerCase();
            if (!findPreviousText(searchQuery, outputText))
                Toast.makeText(MainActivity.this, "Text not found", Toast.LENGTH_SHORT).show();
        });
        buttonReplace.setOnClickListener(v -> {
            String searchQuery = editTextSearch.getText().toString();
            if (!useRegex) searchQuery = escapeRegex(searchQuery);
            if (!caseSensitive) searchQuery = convertToUniversalCase(searchQuery);
            String replacementText = textToReplaceWith.getText().toString();

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
            String outputText = output.getText().toString();
            output.setText(outputText.replaceAll(searchQuery, replacementText));
        });

        // Configure settings menu
        findViewById(R.id.dropdown_menu).setOnClickListener((Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) ? v -> {
            PopupMenu popup = new PopupMenu(new ContextThemeWrapper(this, R.style.PopupMenuStyle), v);
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
                    output.setText(output.getText().toString().replaceAll("<[^>]*(AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id|PlayCoreDialog)[^>]*(.*\\n.*\\n.*/(?!.*(application|manifest)).*>|.*\\n.*/(?!.*(application|manifest))>|>)", "").replace("isSplitRequired=\"true", "isSplitRequired=\"false").replaceAll("(splitTypes|requiredSplitTypes)=\".*\"", "").trim());
                    return true;
                } else if (id == R.id.caseSensitive) {
                    caseSensitive = !caseSensitive;
                    return true;
                } else if (id == R.id.goToBottom) {
                    if (!output.isFocused()) output.requestFocus();
                    output.setSelection(output.getText().length(), output.getText().length());
                    return true;
                } else if (id == R.id.goToTop) {
                    if (!output.isFocused()) output.requestFocus();
                    output.setSelection(1, 1);
                    return true;
                } else if (id == R.id.saveDecoded) {
                    callSaveFileResultLauncherForPlainTextData();
                    return true;
                }  else if (id == R.id.mergeActivities) {
                    showInputDialog();
                    return true;
                } else if (id == R.id.setBackgroundColor) {
                    AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, settings.getInt("backgroundColor", 0xff000000), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                        @Override
                        public void onOk(AmbilWarnaDialog dialog, int color) {
                            final SharedPreferences.Editor e = settings.edit();
                                    e.putInt("backgroundColor", color);
                            e.apply();
                            background.setBackgroundColor(color);
                        }

                        @Override
                        public void onCancel(AmbilWarnaDialog dialog) {
                            // cancel was selected by the user
                        }
                    });
                    dialog.show();
                    return true;
                } else if (id == R.id.setTextColor) {
                    AmbilWarnaDialog dialog = new AmbilWarnaDialog(this, settings.getInt("textColor", 0x6765239), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                        @Override
                        public void onOk(AmbilWarnaDialog dialog, int color) {
                            final SharedPreferences.Editor e = settings.edit();
                            e.putInt("textColor", color);
                            setTextColor(color);
                            e.apply();
                        }

                        @Override
                        public void onCancel(AmbilWarnaDialog dialog) {
                            // cancel was selected by the user
                        }
                    });
                    dialog.show();
                    return true;
                }
                return false;
            });
            popup.show();
        } : v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select an option");

            String[] options = {
                    "(" + (useRegex ? "on" : "off") + ") " + getString(R.string.use_regex),
                    getString(R.string.remove_useless_info),
                    "(" + (caseSensitive ? "on" : "off") + ") " + getString(R.string.case_sensitive),
                    getString(R.string.go_to_bottom),
                    getString(R.string.go_to_top),
                    getString(R.string.save_decoded_xml),
                    getString(R.string.merge_activities),
                    getString(R.string.set_bg),
                    getString(R.string.set_editor_text_color)
            };

            builder.setItems(options, (dialog, which) -> {
                switch (which) {
                    case 0:
                        useRegex = !useRegex;
                        useRegexSwitch.setChecked(useRegex);
                        break;
                    case 1:
                        output.setText(output.getText().toString().replaceAll("<[^>]*(AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id|PlayCoreDialog)[^>]*(.*\\n.*\\n.*/(?!.*(application|manifest)).*>|.*\\n.*/(?!.*(application|manifest))>|>)", "").replace("isSplitRequired=\"true", "isSplitRequired=\"false").replaceAll("(splitTypes|requiredSplitTypes)=\".*\"", "").trim());
                        break;
                    case 2:
                        caseSensitive = !caseSensitive;
                        break;
                    case 3:
                        if (!output.isFocused()) output.requestFocus();
                        output.setSelection(output.getText().length(), output.getText().length());
                        break;
                    case 4:
                        if (!output.isFocused()) output.requestFocus();
                        output.setSelection(1, 1);
                        break;
                    case 5:
                        callSaveFileResultLauncherForPlainTextData();
                        break;
                    case 6:
                        showInputDialog();
                        break;
                    case 7:
                        AmbilWarnaDialog bgDialog = new AmbilWarnaDialog(this, settings.getInt("backgroundColor", 0xff000000), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                SharedPreferences.Editor e = settings.edit();
                                e.putInt("backgroundColor", color);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                                    e.apply();
                                } else {
                                    e.commit();
                                }
                                background.setBackgroundColor(color);
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {
                                // cancel was selected by the user
                            }
                        });
                        bgDialog.show();
                        break;
                    case 8:
                        AmbilWarnaDialog textDialog = new AmbilWarnaDialog(this, settings.getInt("textColor", 0x6765239), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                SharedPreferences.Editor e = settings.edit();
                                e.putInt("textColor", color);
                                setTextColor(color);
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
                                    e.apply();
                                } else {
                                    e.commit();
                                }
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {
                                // cancel was selected by the user
                            }
                        });
                        textDialog.show();
                        break;
                }
            });

            builder.show();
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
            final String filename = getOriginalFileName(this, sharedUri);
            if (filename.endsWith("pk") || filename.endsWith("apkm")) {
                InputStream zipInputStream = getContentResolver().openInputStream(sharedUri);
                is = getAndroidManifestInputStreamFromZip(zipInputStream);
                zipUriForRepacking = sharedUri;
                isAPK = true;
                decode(null);
            } else {
                is = getContentResolver().openInputStream(sharedUri);
                decode(getContentResolver().openInputStream(sharedUri));
            }
        } catch (IOException e) {
            showError(e.toString());
        }
    }
    private static String getOriginalFileName(Context context, Uri uri) {
        String result = null;
        try {
            if (Objects.equals(uri.getScheme(), "content")) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME));
                    }
                }
            }
            if (result == null) {
                result = uri.getPath();
                int cut = Objects.requireNonNull(result).lastIndexOf('/'); // Ensure it throw the NullPointerException here to be caught
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        } catch (NullPointerException | IllegalArgumentException ignored) {
            result = "filename_not_found";
        }
        return result;
    }
    private void decode(InputStream check) {
        try {
            BufferedReader br;
            StringBuilder content = new StringBuilder();
            String line;
            // You can't read twice from the same InputStream, so to check if the xml is binary or not we need 2 InputStream, if its from an APK it should always be binary
            if (check != null && (line = (br = new BufferedReader(new InputStreamReader(check))).readLine()).startsWith("<?xml version=")) {
                content.append(line);
                while ((line = br.readLine()) != null) {
                    content.append("\n").append(line);
                }
            } else {
                content = new StringBuilder(new AXMLPrinter().convertXml(is).trim());
                if (saveDecodedFile) callSaveFileResultLauncherForPlainTextData();
            }
            boolean realRegexValue;
            EditText outputField = findViewById(R.id.outputField);

            outputField.setText(content.toString());
            realRegexValue = useRegex;
            useRegex = true;
            if(findText("plitTypes|PlayCoreDialog|AssetPack|assetpack|MissingSplit|com\\.android\\.dynamic\\.apk\\.fused\\.modules|com\\.android\\.stamp\\.source|com\\.android\\.stamp\\.type|com\\.android\\.vending\\.splits|com\\.android\\.vending\\.derived\\.apk\\.id", content.toString())) {
                showError(getString(R.string.useless_info));
            }
            useRegex = realRegexValue;

            findViewById(R.id.encodeFromField).setVisibility(View.VISIBLE);
            findViewById(R.id.editBar).setVisibility(View.VISIBLE);
        } catch (IOException e) {
            showError(e.toString());
        }
    }

    @Override
    protected void onPause() {
        final SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        final SharedPreferences.Editor e = settings.edit();
        e.putBoolean("useRegex", useRegex).putBoolean("saveDecodedFile", saveDecodedFile).putBoolean("caseSensitive", caseSensitive).putBoolean("recompileAPK", recompileAPK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            e.apply();
        } else e.commit();
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
        if(doesntSupportBuiltInAndroidFilePicker) {
            TextView t = findViewById(R.id.workingFileField);
            final String filePath = t.getText().toString();
            Uri uri = Uri.fromFile(new File(filePath));
            if (uri == null) {
                showError(getString(R.string.invalid));
            } else {
                try {
                    final String filename =  filePath.substring(filePath.lastIndexOf("/") + 1);
                    if (filename.endsWith("apk") || filename.endsWith("apkm")) {
                        InputStream zipInputStream = getContentResolver().openInputStream(uri);
                        is = getAndroidManifestInputStreamFromZip(zipInputStream);
                        zipUriForRepacking = uri;
                        isAPK = true;
                        decode(null);
                    } else {
                        is = getContentResolver().openInputStream(uri);
                        decode(getContentResolver().openInputStream(uri));
                    }
                    if(requestCode==REQUEST_CODE_SAVE_ENCODED_XML) {
                        OutputStream outputStream = new FileOutputStream(Environment.getExternalStorageDirectory()+ File.separator + "Download" + File.separator + filename);
                        EditText outputField = findViewById(R.id.outputField);
                        outputStream.write(outputField.getText().toString().trim().getBytes());
                        outputStream.flush();
                        outputStream.close();
                    }
                } catch (IOException e) {
                    showError(e.toString());
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
        if (doesntSupportBuiltInAndroidFilePicker) {
            final TextView output = findViewById(R.id.outputField);
            String fromTextField = output.getText().toString().trim();

            TextView t = findViewById(R.id.workingFileField);
            final String filePath = t.getText().toString();
            final byte[] encodedData = fromTextField.getBytes();
            if (supportsAsyncTask) {
                new saveAsyncTask(this, encodedData).execute(null, Uri.fromFile(new File(Environment.getExternalStorageDirectory()+"/Download/" + filePath.substring(filePath.lastIndexOf("/") + 1))));
            } else {
                process(null,
                        Uri.fromFile(
                                new File(Environment.getExternalStorageDirectory()+"/Download/" + filePath.substring(filePath.lastIndexOf("/") + 1))),
                        encodedData);
            }
        } else {
            String fileName = "decoded";
            Intent saveFileIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            saveFileIntent.addCategory(Intent.CATEGORY_OPENABLE);
            saveFileIntent.setType("text/xml");
            saveFileIntent.putExtra(Intent.EXTRA_TITLE, fileName);
            startActivityForResult(saveFileIntent, REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING);
        }
    }

    private Uri zipUriForRepacking;

    @TargetApi(19)
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
                                decode(null);
                            } else {
                                is = getContentResolver().openInputStream(uri);
                                decode(getContentResolver().openInputStream(uri));
                            }
                            break;
                        case REQUEST_CODE_OPEN_FILE_MANAGER_TO_SAVE_ENCODED_XML:
                            is = getContentResolver().openInputStream(uri);
                            openFileManagerToSaveEncodedXML();
                            break;
                        case REQUEST_CODE_SAVE_DECODED_XML_FROM_STRING:
                            ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri, "w");
                            OutputStream outputStream = new FileOutputStream(pfd.getFileDescriptor());
                            outputStream.write(outputField.getText().toString().trim().getBytes());
                            outputStream.flush();
                            outputStream.close();
                            pfd.close();
                            break;
                        case REQUEST_CODE_SAVE_ENCODED_XML:
                            byte[] encodedData;
                            if (encodeFromTextField) {
                                String fromTextField = outputField.getText().toString().trim();
                                encodedData = fromTextField.length()==0 ? encodeFromFile() : new aXMLEncoder().encodeString(this, fromTextField);
                            } else {
                                encodedData = encodeFromFile();
                            }
                            new saveAsyncTask(this, encodedData).execute(zipUriForRepacking, uri);
                    }
                } catch (IOException | XmlPullParserException e) {
                   showError(e.toString());
                }

            }
        }
    }

    private void process(Uri inputZipUri, Uri outputUri, byte[] encodedData) {
        try {
            if(isAPK && recompileAPK) {
                try (ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(inputZipUri));
                     ZipOutputStream zos = new ZipOutputStream(getContentResolver().openOutputStream(outputUri))) {
                    // This doesn't work on split APKs and no point making it work till it can sign as all apks in the split APK need to be signed
                    ZipEntry entry;
                    while ((entry = zis.getNextEntry()) != null) {
                        final String filename = entry.getName();
                        ZipEntry newEntry = new ZipEntry(filename);
                        if(filename.startsWith("res/") && !filename.contains(".xml")) {
                            zos.setMethod(ZipOutputStream.STORED);
                            newEntry.setSize(entry.getSize());
                            newEntry.setCrc(entry.getCrc());
                        } else {
                            zos.setMethod(ZipOutputStream.DEFLATED);
                        }
                        zos.putNextEntry(newEntry);

                        if ("AndroidManifest.xml".equals(filename)) {
                            zos.write(encodedData, 0, encodedData.length);
                        } else {
                            byte[] buffer = new byte[1024];
                            int len;
                            while ((len = zis.read(buffer)) > 0) {
                                zos.write(buffer, 0, len);
                            }
                        }
                        zos.closeEntry();
                        zis.closeEntry();
                    }
                }
                toast(getString(R.string.success) + " APK");
            } else {
                try (FileOutputStream fos = (FileOutputStream) getContentResolver().openOutputStream(outputUri)) {
                    fos.write(encodedData);
                    fos.close();
                    toast(getString(R.string.success) + " XML");
                }
            }
        } catch (IOException e) {
            showError(e.toString());
        }
    }

    private void toast(String message) {
        runOnUiThread(() ->  Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }
    @TargetApi(Build.VERSION_CODES.CUPCAKE)
    private static class saveAsyncTask extends AsyncTask<Uri, Void, Void> {
        private static WeakReference<MainActivity> activityReference;
        // only retain a weak reference to the activity
        public saveAsyncTask(MainActivity context, byte[] data) {
            activityReference = new WeakReference<>(context);
            encodedData = data;
        }
        final byte[] encodedData;

        @Override
        protected Void doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            final Uri inputZipUri = uris[0];
            final Uri outputUri = uris[1];
            activity.process(inputZipUri, outputUri, encodedData);
            return null;
        }
    }
    private void showInputDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final Spinner spinner = new Spinner(this);
        String[] options = {"Any attribute", "application", "meta-data", "activity", "receiver"};
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        final EditText inputField = new EditText(this);
        inputField.setHint("Enter search query");

        final EditText replacementField = new EditText(this);
        replacementField.setHint("Enter replacement text");

        layout.addView(spinner);
        layout.addView(inputField);
        layout.addView(replacementField);
        replacementField.setVisibility(View.INVISIBLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.merge_activities));
        builder.setView(layout);

        boolean[] checkedItems = new boolean[options.length];
        ArrayList<String> selectedItems = new ArrayList<>();
        builder.setMultiChoiceItems(options, checkedItems, (dialog, index, isChecked) -> {
            if (isChecked) {
                selectedItems.add(options[index]);
            } else {
                selectedItems.remove(options[index]);
            }
        });

        final int[] pos = new int[1];
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                pos[0] = position;

                replacementField.setVisibility(position == 2 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        builder.setPositiveButton("OK", (dialog, which) -> {
            String userInput = inputField.getText().toString();
            String textToFind;

            if (selectedItems.isEmpty()) {
                showError("No attribute type selected");
            } else {
                if (selectedItems.contains(options[0])) {
                    textToFind = "<[^>]*(" + userInput + ")[^>]*(.*\\n.*\\n.*/(?!.*(application|manifest)).*>|.*\\n.*/(?!.*(application|manifest))>|>)";
                } else {
                    StringBuilder selectedItemsRegex = new StringBuilder();
                    selectedItemsRegex.append("<(")
                            .append(selectedItems.get(0));
                    for (int i = 1; i < selectedItems.size(); i++) {
                        selectedItemsRegex.append('|').append(selectedItems.get(i));
                    }
                    selectedItemsRegex.append(").*[^>]*(")
                            .append(userInput)
                            .append(")[^>]*(.*\\n.*\\n.*/(?!.*(application|manifest)).*>|.*\\n.*/(?!.*(application|manifest))>|>)");
                    textToFind = selectedItemsRegex.toString();
                }

                EditText outputField = findViewById(R.id.outputField);
                String outputText = outputField.getText().toString().trim();

                if (pos[0] == 0) {
                    findText(textToFind, outputText);
                } else if (pos[0] == 1) {
                    outputField.setText(outputText.replaceAll(textToFind, ""));
                } else {
                    String replacementText = replacementField.getText().toString();
                    outputField.setText(outputText.replaceAll(textToFind, replacementText));
                }
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void showError(String error) {
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(error);
            Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
        });
    }
    private byte[] encodeFromFile() throws XmlPullParserException, IOException {
        // If encoding from file the input stream will be the decoded XML
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(is, "UTF-8");
        return aXMLEncoder.encode(getApplicationContext(), parser);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private void openFileManagerToSaveEncodedXML() {
        final boolean saveAsAPK = recompileAPK && isAPK;
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(saveAsAPK ? "application/vnd.android.package-archive" : "text/xml");
        intent.putExtra(Intent.EXTRA_TITLE, saveAsAPK ? "edited.apk" : "AndroidManifest.xml"); // when I support parsing all xml files from an apk need to find a way to get the chosen file name
        startActivityForResult(intent, REQUEST_CODE_SAVE_ENCODED_XML);
    }
}