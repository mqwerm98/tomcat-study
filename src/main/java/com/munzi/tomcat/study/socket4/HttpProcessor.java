package com.munzi.tomcat.study.socket4;

import com.munzi.tomcat.study.socket3.HttpRequestLine3;
import com.munzi.tomcat.study.socket3.SocketInputStream3;
import org.apache.catalina.connector.Constants;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.Socket;

public class HttpProcessor implements Runnable {


    boolean stopped = true;
    boolean available = false;
    private HttpConnector connector;
    private Socket socket;
    private HttpRequest request;
    private HttpResponse response;
    private HttpRequestLine3 requestLine = new HttpRequestLine3();


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

    public void process(Socket socket) {
        // 처리 중 에러가 발생했는지 여부
        boolean ok = true;
        // Response 인터페이스의 finishResponse가 호출돼야 함 여부
        boolean finishResponse = true;
        // 지속 연결 여부
        boolean keepAlive = false;
        // connector가 processor를 정지해서 process 메소드 역시 정지돼야 하는지 여부
        boolean stopped = false;
        // HTTP 요청이 HTTP 1.1을 지원하는 웹 클라이언트에서 들어오고 있는지 여부
        boolean http11 = false;

        SocketInputStream input = null;
        OutputStream output = null;

        try {
            input = new SocketInputStream(socket.getInputStream(), connector.getBufferSize());
        } catch (Exception e) {
            ok = false;
        }

        keepAlive = true;
        while(!stopped && ok && keepAlive) {
            finishResponse = true;
            try {
                request.setStream(input);
                request.setResponse(response);
                output = socket.getOutputStream();
                response.setStream(output);
                response.setRequest(request);
                ((HttpServletResponse)response.getResponse()).setHeader("Server", SERVER_INFO);
            } catch (Exception e) {
//                log("Process.create", e);
                System.out.println("process create error");
                ok = false;
            }

            try {
                if (ok) {
                    parseConnection(socket);
                    // protocol을 구해서, HTTP 1.1 이상이 아니면 지속 연결을 원하지 않는 것이므로, keepAlive = false로 설정한다.
                    parseRequest(input, output);
                    if (!request.getRequest().getProtocol().startsWith("HTTP/0")) {
                        parseHeaders(input);
                    }

                    if (http11) {
                        // client에게 승인 응답을 보냄
                        ackRequest(output);

                        // 청크 인코딩이 가능한지의 여부
                        if (connector.isChunkingAllowed()) {
                            response.setAllowChunking(true);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("?? error");
                ok = false;
            }

            try {
                ((HttpServletResponse) response).setHeader("Date", FastHttpDateFormat.getCurrentDate());
                if (ok) {
                    connector.getContainer().invoke(request, response);
                }
            } catch (Exception e) {
                ok = false;
            }


            if (finishResponse) {
                response.finishResponse();
                request.finishRequest();
                output.flush();
            }

            if ("close".equals(response.getHeader("Connection"))) {
                keepAlive = false;
            }

            status = Constants.PROCESSOR_IDLE;
            request.recycle();
            response.recycle();
        }



//        try {
//            output = socket.getOutputStream();
//
//            request = new HttpRequest(input);
//            response = new HttpResponse(output);
//            response.setRequest(request);
//            response.setHeader("Server", "Munzi Servlet Container");
//
//            parseRequest(input, output);
//            parseHeaders(input);
//
//            if (request.getRequestURI().startsWith("/servlet/")) {
////                ServletProcessor3 processor = new ServletProcessor3();
////                processor.process(request, response);
//            }
//
//            socket.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    private void ackRequest(OutputStream output) {
        // sendAck값을 확인하고, true라면 다음 문자열을 전송
        // HTTP/1.1 100 Continue\r\n\r\n
    }

    private void parseConnection(Socket socket) {

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
        // HTTP 요청에 Excpect: 100-continue 헤더가 존재할 경우 sendAck = true로 설정한다.
    }

}
