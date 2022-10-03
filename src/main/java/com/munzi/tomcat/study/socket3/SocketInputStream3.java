package com.munzi.tomcat.study.socket3;

import java.io.IOException;
import java.io.InputStream;

public class SocketInputStream3 extends InputStream {

    public SocketInputStream3(InputStream inputStream, int bufferSize) {
//        super(inputStream, bufferSize);
    }

    @Override
    public int read() throws IOException {
        return 0;
    }

    public void readRequestLine(HttpRequestLine3 requestLine) {

    }
}
