package com.munzi.tomcat.study.socket3;

import javax.servlet.ServletException;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor {


    private HttpRequest request;
    private HttpResponse response;
    private HttpRequestLine3 requestLine = new HttpRequestLine3();


    public HttpProcessor(HttpConnector httpConnector) {

    }

    public void process(Socket socket) {
        SocketInputStream3 input = null;
        OutputStream output = null;

        try {
            input = new SocketInputStream3(socket.getInputStream(), 2048);
            output = socket.getOutputStream();

            request = new HttpRequest(input);
            response = new HttpResponse(output);
            response.setRequest(request);
            response.setHeader("Server", "Munzi Servlet Container");

            parseRequest(input, output);
            parseHeaders(input);

            if (request.getRequestURI().startsWith("/servlet/")) {
                ServletProcessor3 processor = new ServletProcessor3();
                processor.process(request, response);
            }

            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseRequest(SocketInputStream3 input, OutputStream output) throws ServletException {
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

    private void parseHeaders(SocketInputStream3 input) {

    }

}
