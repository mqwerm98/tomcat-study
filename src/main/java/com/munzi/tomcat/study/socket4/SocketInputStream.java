package com.munzi.tomcat.study.socket4;

import java.io.IOException;
import java.io.InputStream;

public class SocketInputStream extends InputStream {

    public SocketInputStream(InputStream inputStream, int bufferSize) {
//        super(inputStream, bufferSize);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    public void readRequestLine(HttpRequestLine4 requestLine) {

    }
}
