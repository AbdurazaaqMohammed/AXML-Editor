package com.codyi96.xml2axml.android.content;

import android.content.res.Resources;

/**
 * Created by Roy on 15-10-6.
 */
public class Context {
    private Resources resources=new Resources(null, null, null);
    public Resources getResources() {
        return resources;
    }

    public String getPackageName() {
        return "com.example.reforceapp";
    }
}
