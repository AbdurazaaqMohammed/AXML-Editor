package com.codyi96.xml2axml.test;

import android.content.Context;
import com.codyi96.xml2axml.Encoder;
import org.apache.commons.io.FileUtils;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Created by Roy on 15-10-6.
 */
public class Main {
    public static void encode(Context c, String in,String out) throws IOException, XmlPullParserException {
        Encoder e = new Encoder();
        byte[] bs = e.encodeFile( c, in);
        FileUtils.writeByteArrayToFile(new File(out), bs);
    }

    public static void decode(String in, String out) throws FileNotFoundException {
        AXMLPrinter.out=new PrintStream(new File(out));
        AXMLPrinter.main(new String[]{in});
        AXMLPrinter.out.close();
    }
}
