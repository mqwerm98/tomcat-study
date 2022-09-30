package com.munzi.tomcat.study.socket4;

import com.munzi.tomcat.study.socket3.HttpRequestLine;
import com.munzi.tomcat.study.socket3.ServletProcessor3;
import com.munzi.tomcat.study.socket3.SocketInputStream;

import javax.servlet.ServletException;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor implements Runnable {


    boolean stopped = true;
    boolean available = false;
    private HttpConnector connector;
    private Socket socket;
    private HttpRequest request;
    private HttpResponse response;
    private HttpRequestLine requestLine = new HttpRequestLine();


    public HttpProcessor(HttpConnector connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        while (!stopped) {
            Socket socket = await(); // 여기서 정지!
            if (socket == null) continue;

            try {
                process(socket);
            } catch (Throwable e) {
//                log("process.invoke", t);
                System.out.println("invoke error");
            }

            connector.recycle(this);
        }

        synchronized (threadSync) {
            threadSync.notifyAll();
        }
    }

    private synchronized Socket await() {
        // 커넥터가 새로운 소켓을 제공할 때까지 기다림
        while (!available) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("wait error!!!");
            }
        }

        Socket socket = this.socket;
        available = false;
        notifyAll();
        if (debug >= 1 && socket != null) {
            System.out.println("The incoming request has been awaited!");
        }

        return socket;
    }

    public void process(Socket socket) {
        SocketInputStream input = null;
        OutputStream output = null;

        try {
            input = new SocketInputStream(socket.getInputStream(), 2048);
            output = socket.getOutputStream();

            request = new HttpRequest(input);
            response = new HttpResponse(output);
            response.setRequest(request);
            response.setHeader("Server", "Munzi Servlet Container");

            parseRequest(input, output);
            parseHeaders(input);

            if (request.getRequestURI().startsWith("/servlet/")) {
//                ServletProcessor3 processor = new ServletProcessor3();
//                processor.process(request, response);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseRequest(SocketInputStream input, OutputStream output) throws ServletException {
        input.readRequestLine(requestLine);
        String method = new String(requestLine.method, 0, requestLine.methodEnd);
        String uri = null;
        String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);

        if (method.length() < 1) {
            throw new ServletException("Missing HTTP request method");
        } else if (requestLine.uriEnd < 1) {
            throw new ServletException("Missing HTTP request uri");
        }

        int question = requestLine.indexOf("?");
        if (question >= 0) {
            request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));
            uri = new String(requestLine.uri, 0, question);
        } else {
            request.setQueryString(null);
            uri = new String(requestLine.uri, 0, requestLine.uriEnd);
        }

        // URI가 절대 경로인지 확인
        if (!uri.startsWith("/")) {
            int pos = uri.indexOf("://");
            if (pos == -1) {
                pos = uri.indexOf('/', pos + 3);
                if (pos == -1) {
                    uri = "";
                } else {
                    uri = uri.substring(pos);
                }
            }
        }

        // session id를 parsing
        String match = ";jsessionid=";
        int semicolon = uri.indexOf(match);
        if (semicolon >= 0) {
            String rest = uri.substring(semicolon + match.length());
            int semicolon2 = rest.indexOf(";");
            if (semicolon2 >= 0) {
                request.setRequestedSessionId(rest.substring(0, semicolon2));
                rest = rest.substring(semicolon2);
            } else {
                request.setRequestedSessionId(rest);
                rest = "";
            }
            request.setRequestedSessionURL(false);
            uri = uri.substring(0, semicolon) + rest;
        } else {
            request.setRequestedSessionId(null);
            request.setRequestedSessionURL(false);
        }

        // 비 정상적인 uri 거르기
        String normalizedUri = normalize(uri);
        ((HttpRequest) request).setMethod(method);
        request.setProtocol(protocol);
        if (normalizedUri != null) {
            ((HttpRequest) request).setURI(normalizedUri);
        } else {
            ((HttpRequest) request).setURI(uri);
        }

        if (normalizedUri == null) {
            throw new ServletException("Invalid URI : " + uri + "'");
        }
    }

    private String normalize(String uri) {
        return null;
    }

    private void parseHeaders(SocketInputStream input) {

    }

    // 호출된 후에 바로 리턴(async)함으로써 다음 요청을 곧바로 처리할 수 있다
    synchronized void assign(Socket socket) {
        // 프로세서가 이전의 소켓을 얻을 때까지 기다림
        while (available) {
            try {
                wait();
            } catch (InterruptedException e) {
                System.out.println("wait error!!");
            }
        }

        this.socket = socket;
        available = true;
        notifyAll();
    }

}
