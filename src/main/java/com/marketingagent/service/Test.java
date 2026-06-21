package com.marketingagent.service;
import org.apache.pdfbox.Loader;
import java.io.InputStream;
public class Test {
    public void test(InputStream is) throws Exception {
        Loader.loadPDF(is);
    }
}
