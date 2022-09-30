package com.munzi.tomcat.study.socket1;

import java.io.IOException;

public class StaticResourceProcessor1 {

    public void process(Request1 request, Response1 response) {
        try {
            response.sendStaticResource();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
