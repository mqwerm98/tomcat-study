package com.munzi.tomcat.study.socket3;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 들어오는 HTTP 요청을 기다렸다가 서버 소켓 생성
 * Runnable을 구현해 스스로 스레드로서 동작
 */
public class HttpConnector implements Runnable {

    boolean stopped;
    private String scheme = "http";

    public String getScheme() {
        return scheme;
    }

    @Override
    public void run() {
        ServerSocket serverSocket = null;
        int port = 5252;

        try {
            serverSocket = new ServerSocket(port, 1, InetAddress.getByName("127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        while (!stopped) {
            Socket socket;

            try {
                // HTTP 요청을 기다린다
                socket = serverSocket.accept();
            } catch (Exception e) {
                continue;
            }

            // HTTP 요청이 들어오면, 각 요청에 대해 HttpProcessor 인스턴스 생성 및 process 호출
            HttpProcessor processor = new HttpProcessor(this);
            processor.process(socket);
        }

    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }
}
