package com.abdurazaaqmohammed.androidmanifesteditor.main;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.InputStream;

public class ZipUtils {

    public static InputStream getAndroidManifestInputStreamFromZip(InputStream zipInputStream) throws IOException {
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
                zipInput.close();
                return new ByteArrayInputStream(outputStream.toByteArray());
            }
            zipInput.closeEntry();
        }
        zipInput.close();
        
        // AndroidManifest.xml not found in the zip file
        return null;
    }
}
