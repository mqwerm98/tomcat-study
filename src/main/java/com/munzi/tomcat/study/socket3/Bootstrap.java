package com.munzi.tomcat.study.socket3;

public final class Bootstrap {

    public static void main(String[] args) {
        HttpConnector connector = new HttpConnector();
        connector.start();
    }
}
