package com.codyi96.xml2axml.android.content.res;

import java.lang.reflect.Field;
import java.util.HashMap;

/**
 * Created by Roy on 15-10-6.
 */
public class Resources {

    public HashMap<String, Integer> attrMap = new HashMap<>();

    private void init() {
        try {
            Field[] fs = android.R.attr.class.getFields();
            for (Field f : fs) attrMap.put(f.getName(), (Integer) f.get(null));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Resources() {
        init();
    }

    public int getIdentifier(String name, String type, String pkg, boolean log) {
        if ("android".equals(pkg) && "attr".equals(type)) {
            Integer x = attrMap.get(name);
            if (x == null) {
                if (log) System.out.println("attr not found: " + name);
            } else {
                if (log) System.out.format("@%s:%s/%s=0x%x\n", pkg, type, name, x);
                return x;
            }
        } else {
            if (log) System.out.format("@%s:%s/%s=0x%x\n", pkg, type, name, 0);
        }
        return 0;
    }
}
