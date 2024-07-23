package com.abdurazaaqmohammed.AntiSplit.main;

import static android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.abdurazaaqmohammed.AntiSplit.R;
import com.reandroid.apkeditor.merge.LogUtil;
import com.reandroid.apkeditor.merge.Merger;
import com.starry.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import yuku.ambilwarna.AmbilWarnaDialog;

/** @noinspection deprecation*/
public class MainActivity extends Activity implements Merger.LogListener {
    private final static int REQUEST_CODE_OPEN_SPLIT_APK_TO_ANTISPLIT = 1;
    private final static int REQUEST_CODE_SAVE_APK = 2;
    private final static boolean supportsFilePicker = Build.VERSION.SDK_INT>19;
    private static boolean logEnabled;
    private static boolean ask;
    private static boolean showDialog;
    private Uri splitAPKUri;
    private ArrayList<Uri> uris;
    private boolean urisAreSplitApks = true;
    public static int textColor;
    public static int bgColor;

    private void setTextColor(int color) {
        textColor = color;
        ((TextView) findViewById(R.id.oldAndroidInfo)).setTextColor(color);
        final TextView wf = findViewById(R.id.workingFileField);
        wf.setTextColor(color);
        wf.setHintTextColor(color);
        ((TextView) findViewById(R.id.errorField)).setTextColor(color);
        ((TextView) findViewById(R.id.logField)).setTextColor(color);
        ((TextView) findViewById(R.id.logSwitch)).setTextColor(color);
        ((TextView) findViewById(R.id.ask)).setTextColor(color);
        ((TextView) findViewById(R.id.showDialog)).setTextColor(color);
    }

    private void setBgColor(int color) {
        bgColor = color;
        findViewById(R.id.main).setBackgroundColor(color);
    }
    public Handler getHandler() {
        return handler;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler(Looper.getMainLooper());

        try {
            Objects.requireNonNull(getActionBar()).hide();
            deleteDir(Objects.requireNonNull(getExternalCacheDir()));
        } catch (NullPointerException ignored) {}
        setContentView(R.layout.activity_main);

        if (!supportsFilePicker) {
            findViewById(R.id.oldAndroidInfo).setVisibility(View.VISIBLE);
            // Android versions below 4.4 are too old to use the file picker for ACTION_OPEN_DOCUMENT/ACTION_CREATE_DOCUMENT. The location of the file must be manually input. The files will be saved to "AntiSplit-M" folder in the internal storage.
        }

        SharedPreferences settings = getSharedPreferences("set", Context.MODE_PRIVATE);
        setTextColor(settings.getInt("textColor", 0xffffffff));
        setBgColor(settings.getInt("backgroundColor", 0xff000000));

        // Fetch settings from SharedPreferences
        logEnabled = settings.getBoolean("logEnabled", true);
        LogUtil.setLogListener(this);
        LogUtil.setLogEnabled(logEnabled);

        Switch logSwitch = findViewById(R.id.logSwitch);
        logSwitch.setChecked(logEnabled);
        logSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            logEnabled = isChecked;
            LogUtil.setLogEnabled(logEnabled);
        });

        Switch askSwitch = findViewById(R.id.ask);
        if (Build.VERSION.SDK_INT > 22) {
            ask = settings.getBoolean("ask", true);
            askSwitch.setChecked(ask);
            askSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                ask = isChecked;
                if(!isChecked) checkStoragePerm();
            });
        } else askSwitch.setVisibility(View.INVISIBLE);

        showDialog = settings.getBoolean("showDialog", false);
        Switch showDialogSwitch = findViewById(R.id.showDialog);
        showDialogSwitch.setChecked(showDialog);
        showDialogSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> showDialog = isChecked);

        findViewById(R.id.decodeButton).setOnClickListener(v -> openFilePickerOrStartProcessing());
        findViewById(R.id.changeBgColor).setOnClickListener(v -> new AmbilWarnaDialog(this, settings.getInt("backgroundColor", 0xff000000), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog1, int color) {
                        settings.edit().putInt("backgroundColor", color).apply();
                        setBgColor(color);
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog1) {
                        // cancel was selected by the user
                    }
                }).show());
        findViewById(R.id.changeTextColor).setOnClickListener(v -> new AmbilWarnaDialog(this, settings.getInt("textColor", 0xffffffff), new AmbilWarnaDialog.OnAmbilWarnaListener() {
                    @Override
                    public void onOk(AmbilWarnaDialog dialog1, int color) {
                        settings.edit().putInt("textColor", color).apply();
                        setTextColor(color);
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog1) {
                        // cancel was selected by the user
                    }
                }).show());

        // Check if user shared or opened file with the app.
        final Intent fromShareOrView = getIntent();
        final String fromShareOrViewAction = fromShareOrView.getAction();
        if (Intent.ACTION_SEND.equals(fromShareOrViewAction)) {
            splitAPKUri = fromShareOrView.getParcelableExtra(Intent.EXTRA_STREAM);
        } else if (Intent.ACTION_VIEW.equals(fromShareOrViewAction)) {
            splitAPKUri = fromShareOrView.getData();
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(fromShareOrViewAction)) {
            if (fromShareOrView.hasExtra(Intent.EXTRA_STREAM)) {
                uris = fromShareOrView.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
                final Uri uri = uris.get(0);
                urisAreSplitApks = !uri.getPath().endsWith(".apk");
                if(urisAreSplitApks) splitAPKUri = uri;
                else selectDirToSaveAPKOrSaveNow();
            }
        }
        if (splitAPKUri != null) {
            if(showDialog) showApkSelectionDialog();
            else selectDirToSaveAPKOrSaveNow();
        }
    }

    final File getAntisplitMFolder() {
        final File antisplitMFolder = new File(Environment.getExternalStorageDirectory() + File.separator + "AntiSplit-M");
        return antisplitMFolder.exists() || antisplitMFolder.mkdir() ? antisplitMFolder : new File(Environment.getExternalStorageDirectory() + File.separator + "Download");
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkStoragePerm() {
        final boolean write = Build.VERSION.SDK_INT < 30;
        final boolean noPermission = write ? checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED : !Environment.isExternalStorageManager();
        if (noPermission) {
            Toast.makeText(this, R.string.grant_storage, Toast.LENGTH_LONG).show();
            if(write) requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            else startActivityForResult(new Intent(ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + getPackageName())), 0);
        }
    }

    @Override
    protected void onPause() {
        getSharedPreferences("set", Context.MODE_PRIVATE).edit().putBoolean("logEnabled", logEnabled).putBoolean("ask", ask).putBoolean("showDialog", showDialog).apply();
        super.onPause();
    }

    @Override
    public void onLog(String log) {
        runOnUiThread(() -> {
            TextView logTextView = findViewById(R.id.logField);
            logTextView.append(log + "\n");
            ScrollView scrollView = findViewById(R.id.scrollView);
            scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
        });
    }
    private void openFilePickerOrStartProcessing() {
        if (supportsFilePicker) startActivityForResult(new Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("*/*")
                .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                .putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/zip", "application/vnd.android.package-archive", "application/octet-stream"})
                , REQUEST_CODE_OPEN_SPLIT_APK_TO_ANTISPLIT); // XAPK is octet-stream
        else {
            final String workingFilePath = ((TextView) findViewById(R.id.workingFileField)).getText().toString();
            splitAPKUri = Uri.fromFile(new File(workingFilePath));
            if(showDialog) showApkSelectionDialog();
            else new ProcessTask(this).execute(Uri.fromFile(new File(getAntisplitMFolder() + File.separator + workingFilePath.substring(workingFilePath.lastIndexOf("/") + 1).replaceFirst("\\.(?:xapk|aspk|apk[sm])", "_antisplit.apk"))));
        }
    }
    private Handler handler;

    private void process(Uri outputUri) {
        runOnUiThread(() -> ((TextView)findViewById(R.id.logField)).setText(""));
        final File cacheDir = getExternalCacheDir();
        if (cacheDir != null && urisAreSplitApks) {
            deleteDir(cacheDir);
        }

        Uri xapkUri;
        try {
            xapkUri = !urisAreSplitApks || !Objects.requireNonNull(splitAPKUri.getPath()).endsWith("xapk") ? null : splitAPKUri;
        } catch (NullPointerException ignored) {
            xapkUri = null;
        }

        try {
            Merger.run(urisAreSplitApks ? getContentResolver().openInputStream(splitAPKUri) : null, cacheDir, getContentResolver().openOutputStream(outputUri), xapkUri, this, splitsToUse);
        } catch (IOException e) {
            showError(e);
        }
    }

    /** @noinspection ResultOfMethodCallIgnored*/
    public static void deleteDir(File dir){
        String[] children = dir.list();
        // There should never be folders in here.
        if (children != null) for (String child : children) new File(dir, child).delete();
    }

    @Override
    protected void onDestroy() {
        deleteDir(getExternalCacheDir());
        super.onDestroy();
    }

    private List<String> splitsToUse = null;

    private static class ProcessTask extends AsyncTask<Uri, Void, String> {
        private final WeakReference<MainActivity> activityReference;

        // only retain a weak reference to the activity
        ProcessTask(MainActivity context) {
            activityReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Uri... uris) {
            MainActivity activity = activityReference.get();
            if(!activity.urisAreSplitApks) {
                for(Uri u : activity.uris) {
                    try(InputStream is = activity.getContentResolver().openInputStream(u);
                        FileOutputStream fos = new FileOutputStream(activity.getExternalCacheDir() + File.separator + getOriginalFileName(activity, u))) {
                        byte[] buffer = new byte[4096];
                        int length;
                        while ((length = is.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    } catch (IOException e) {
                        activity.showError(e);
                    }
                }
            }
            activity.process(uris[0]);
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            MainActivity activity = activityReference.get();
            if(activity.urisAreSplitApks) activity.getHandler().post(() -> {
                try {
                    activity.uris.remove(0);
                    activity.splitAPKUri = activity.uris.get(0);
                    if(showDialog) activity.showApkSelectionDialog();
                    else activity.selectDirToSaveAPKOrSaveNow();
                } catch (IndexOutOfBoundsException | NullPointerException ignored) {
                    // End of list, I don't know why but isEmpty is not working
                    activity.showSuccess();
                }
            });
            else activity.showSuccess();
        }
    }

    private void processOneSplitApkUri(Uri uri) {
        splitAPKUri = uri;
        if (showDialog) showApkSelectionDialog();
        else selectDirToSaveAPKOrSaveNow();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null) {
            switch(requestCode) {
                case 0:
                    checkStoragePerm();
                break;
                case REQUEST_CODE_OPEN_SPLIT_APK_TO_ANTISPLIT:
                    ClipData clipData = data.getClipData();
                    if (clipData == null) processOneSplitApkUri(data.getData());
                    else {
                        //multiple files selected
                        Uri first = clipData.getItemAt(0).getUri();
                        try {
                            if(Objects.requireNonNull(first.getPath()).endsWith(".apk")) {
                                urisAreSplitApks = false;
                                uris = new ArrayList<>();
                                uris.add(first);
                            } else processOneSplitApkUri(first);
                        } catch (NullPointerException ignored) {}
                        for (int i = 1; i < clipData.getItemCount(); i++) {
                            Uri uri = clipData.getItemAt(i).getUri();
                            if(urisAreSplitApks) processOneSplitApkUri(uri);
                            else uris.add(uri);
                        }
                        if(!urisAreSplitApks) selectDirToSaveAPKOrSaveNow();
                    }
                break;
                case REQUEST_CODE_SAVE_APK:
                    new ProcessTask(this).execute(data.getData());
                break;
            }
        }
    }

    public void showApkSelectionDialog() {
        List<String> splits = new ArrayList<>();

        if (splitAPKUri.getPath().endsWith("xapk")) {
            File bruh = new File(new FileUtils(this).getPath(splitAPKUri));
            final boolean couldntRead = !bruh.canRead();
            if (couldntRead) {
                // copy to cache dir if no permission
                bruh = new File(getExternalCacheDir() + File.separator + getOriginalFileName(this, splitAPKUri));
                try (FileOutputStream fos = new FileOutputStream(bruh);
                     InputStream ins = getContentResolver().openInputStream(splitAPKUri)) {
                    byte[] buffer = new byte[4096];
                    int length;
                    while ((length = ins.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                } catch (IOException e) {
                    showError(e);
                }
            }
            try (ZipFile zipFile = new ZipFile(bruh)) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    String fileName = entries.nextElement().getName();
                    if (fileName.endsWith(".apk")) splits.add(fileName);
                }
                if (couldntRead) bruh.delete();
            } catch (IOException e) {
                showError(e);
            }
        } else {
            try (ZipInputStream zis = new ZipInputStream(getContentResolver().openInputStream(splitAPKUri))) {
                ZipEntry zipEntry = zis.getNextEntry();
                while (zipEntry != null) {
                    final String name = zipEntry.getName();
                    if (name.endsWith(".apk")) splits.add(name);
                    zipEntry = zis.getNextEntry();
                }
                zis.closeEntry();
            } catch (IOException e) {
                showError(e);
            }
        }
        final int initialSize = splits.size();
        String[] apkNames = new String[initialSize + 2];
        boolean[] checkedItems = new boolean[initialSize + 2];

        apkNames[0] = getString(R.string.all);
        apkNames[1] = getString(R.string.for_device);
        for (int i = 2; i < initialSize + 2; i++) {
            apkNames[i] = splits.get(i - 2);
            checkedItems[i] = false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        TextView title = new TextView(this);
        title.setTextColor(textColor);
        title.setTextSize(20);
        title.setText(R.string.select_splits);
        builder.setCustomTitle(title);

        builder.setMultiChoiceItems(apkNames, checkedItems, (dialog, which, isChecked) -> {
            switch (which) {
                case 0:
                    // "Select All" option
                    for (int i = 2; i < checkedItems.length; i++) {
                        checkedItems[i] = isChecked;
                        ((AlertDialog) dialog).getListView().setItemChecked(i, isChecked);
                    }
                    break;
                case 1:
                    // device specs option
                    String densityType;
                    switch (getResources().getDisplayMetrics().densityDpi) {
                        case DisplayMetrics.DENSITY_LOW:
                            densityType = "ldpi";
                            break;
                        case DisplayMetrics.DENSITY_MEDIUM:
                        case DisplayMetrics.DENSITY_140:
                            densityType = "mdpi";
                            break;
                        case DisplayMetrics.DENSITY_HIGH:
                        case DisplayMetrics.DENSITY_220:
                        case DisplayMetrics.DENSITY_200:
                        case DisplayMetrics.DENSITY_180:
                            densityType = "hdpi";
                            break;
                        case DisplayMetrics.DENSITY_XHIGH:
                        case DisplayMetrics.DENSITY_280:
                        case DisplayMetrics.DENSITY_260:
                        case DisplayMetrics.DENSITY_300:
                            densityType = "xhdpi";
                            break;
                        case DisplayMetrics.DENSITY_340:
                        case DisplayMetrics.DENSITY_360:
                        case DisplayMetrics.DENSITY_390:
                        case DisplayMetrics.DENSITY_400:
                        case DisplayMetrics.DENSITY_420:
                        case DisplayMetrics.DENSITY_440:
                        case DisplayMetrics.DENSITY_450:
                        case DisplayMetrics.DENSITY_XXHIGH:
                            densityType = "xxhdpi";
                            break;
                        case DisplayMetrics.DENSITY_520:
                        case DisplayMetrics.DENSITY_560:
                        case DisplayMetrics.DENSITY_600:
                        case DisplayMetrics.DENSITY_XXXHIGH:
                            densityType = "xxxhdpi";
                            break;
                        case DisplayMetrics.DENSITY_TV:
                            densityType = "tvdpi";
                            break;
                        default:
                            densityType = "unknown";
                            break;
                    }
                    densityType += ".apk";
                    for (int i = 2; i < checkedItems.length; i++) {
                        final String name = apkNames[i];
                        if (name.equals("base.apk")
                                || !name.startsWith("config") && !name.startsWith("split") // this is base.apk probably
                                || name.contains(Locale.getDefault().getLanguage())
                                || name.contains((
                                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) ? Build.SUPPORTED_ABIS[0] : Build.CPU_ABI)
                                    .replace('-', '_')) // fix the format
                                || (name.endsWith(densityType) && !name.replace(densityType, "").endsWith("x"))) // ensure that it dont select xxhdpi for xhdpi etc
                        {
                            checkedItems[i] = isChecked;
                            ((AlertDialog) dialog).getListView().setItemChecked(i, isChecked);
                        }
                    }
                    boolean didNotFindAppropriateDpi = true;
                    for (int i = 2; i < checkedItems.length; i++) {
                        if (checkedItems[i] && apkNames[i].contains("dpi")) {
                            didNotFindAppropriateDpi = false;
                            break;
                        }
                    }
                    if (didNotFindAppropriateDpi) {
                        for (int i = 2; i < checkedItems.length; i++) {
                            if (apkNames[i].contains("hdpi")) {
                                checkedItems[i] = isChecked;
                                ((AlertDialog) dialog).getListView().setItemChecked(i, isChecked);
                                break;
                            }
                        }
                    }
                    break;
                default:
                    // Uncheck "Select All" if any individual item is unchecked
                    if (!isChecked) {
                        checkedItems[0] = false;
                        ((AlertDialog) dialog).getListView().setItemChecked(0, false);
                    }
                    break;
            }
        });
        builder.setPositiveButton("OK", (dialog, which) -> {
            for (int i = 1; i < checkedItems.length; i++) {
                if (checkedItems[i]) splits.remove(apkNames[i]); // ?????
            }

            if (splits.size() == initialSize) {
                urisAreSplitApks = true; // reset
                showError(getString(R.string.nothing));
            } else {
                splitsToUse = splits;
                selectDirToSaveAPKOrSaveNow();
            }
        });
        builder.setNegativeButton("Cancel", null);
        runOnUiThread(() -> {
            final AlertDialog ad = builder.create();
            ad.show();
            ad.getListView().setAdapter(new CustomArrayAdapter(this, apkNames, textColor));
            try {
                Objects.requireNonNull(ad.getWindow()).getDecorView().getBackground().setColorFilter(new LightingColorFilter(0xFF000000, bgColor));
            } catch (NullPointerException ignored) {}
        });
    }

    private void showSuccess() {
        final String successMessage = getString(R.string.success_saved);
        LogUtil.logMessage(successMessage);
        runOnUiThread(() -> Toast.makeText(this, successMessage, Toast.LENGTH_SHORT).show());
    }

    private void showError(Exception e) {
        final String mainErr = e.toString();
        StringBuilder stackTrace = new StringBuilder().append(mainErr);
        for(StackTraceElement line : e.getStackTrace()) {
            stackTrace.append(line).append('\n');
        }
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(stackTrace);
            Toast.makeText(this, mainErr, Toast.LENGTH_SHORT).show();
        });
    }

    private void showError(String err) {
        runOnUiThread(() -> {
            TextView errorBox = findViewById(R.id.errorField);
            errorBox.setVisibility(View.VISIBLE);
            errorBox.setText(err);
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    public static String getOriginalFileName(Context context, Uri uri) {
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
            return result.replaceFirst("\\.(?:xapk|aspk|apk[sm])", "_antisplit");
        } catch (NullPointerException | IllegalArgumentException ignored) {
            return "filename_not_found";
        }
    }

    private void selectDirToSaveAPKOrSaveNow() {
        if (android.os.Build.VERSION.SDK_INT < 19 || !ask) {
            checkStoragePerm();
            final String originalFilePath = urisAreSplitApks ? new FileUtils(this).getPath(splitAPKUri) : getNameFromNonSplitApks();
            runOnUiThread(() -> ((TextView)findViewById(R.id.workingFileField)).setText(originalFilePath));

            String newFilePath = originalFilePath.replaceFirst("\\.(?:xapk|aspk|apk[sm])", "_antisplit.apk");
            if(newFilePath.isEmpty() || newFilePath.startsWith("/data/")) { // when shared it in /data/ bruh
                newFilePath = getAntisplitMFolder() + File.separator + newFilePath.substring(newFilePath.lastIndexOf(File.separator) + 1);
                showError(getString(R.string.no_filepath));
            }
            LogUtil.logMessage(getString(R.string.output) + " " + newFilePath);

            new ProcessTask(this).execute(Uri.fromFile(new File(newFilePath)));
        }
        else startActivityForResult(new Intent(Intent.ACTION_CREATE_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("application/vnd.android.package-archive")
                .putExtra(Intent.EXTRA_TITLE, urisAreSplitApks ? getOriginalFileName(this, splitAPKUri) : getNameFromNonSplitApks()), REQUEST_CODE_SAVE_APK);
    }

    private String getNameFromNonSplitApks() {
        String fileName = null;
        for(Uri uri : uris) {
            final String name = getOriginalFileName(this, uri);
            if (name.equals("base.apk")) {
                try {
                    fileName = Objects.requireNonNull(getPackageManager().getPackageArchiveInfo(new FileUtils(this).getPath(uri), 0)).packageName;
                    break;
                } catch (NullPointerException ignored) {}
            } else if (!name.startsWith("config") && !name.startsWith("split")) {
                fileName = name;
                break;
            }
        }
        return (fileName == null ? "unknown" : fileName.replace(".apk", "")) + "_antisplit";
    }
}
