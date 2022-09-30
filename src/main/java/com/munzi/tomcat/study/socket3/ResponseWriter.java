package com.munzi.tomcat.study.socket3;

import java.io.*;

public class ResponseWriter extends PrintWriter {
    public ResponseWriter(Writer out) {
        super(out);
    }

    public ResponseWriter(Writer out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWriter(OutputStream out) {
        super(out);
    }

    public ResponseWriter(OutputStream out, boolean autoFlush) {
        super(out, autoFlush);
    }

    public ResponseWriter(String fileName) throws FileNotFoundException {
        super(fileName);
    }

    public ResponseWriter(String fileName, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(fileName, csn);
    }

    public ResponseWriter(File file) throws FileNotFoundException {
        super(file);
    }

    public ResponseWriter(File file, String csn) throws FileNotFoundException, UnsupportedEncodingException {
        super(file, csn);
    }
}
