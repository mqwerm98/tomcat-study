package com.munzi.tomcat.study.socket1;

import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

public class ServletProcessor1 {

    public void process(Request1 request, Response1 response) {
        String uri = request.getUri();
        String servletName = uri.substring(uri.lastIndexOf("/") + 1);
        URLClassLoader loader = null;

        try {
            URL[] urls = new URL[1];
            URLStreamHandler urlStreamHandler = null;
            File classPath = new File(HttpServer1.WEB_ROOT);

            String repository = new URL("file", null, classPath.getCanonicalPath() + File.separator).toString();
            System.out.println("repository : " + repository);
//            repository = "file:/Users/hjpark/Documents/dkargo/tomcat-study2/src/main/java/com/munzi/tomcat/study/socket1/";
            urls[0] = new URL(null, repository, urlStreamHandler);
            loader = new URLClassLoader(urls);
        } catch (IOException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        Class myClass = null;
        try {
//            myClass = loader.loadClass(servletName);
            myClass = Class.forName("com.munzi.tomcat.study.socket1." + servletName);
        } catch (ClassNotFoundException e) {
            System.out.println(e);
            e.printStackTrace();
        }

        Servlet servlet = null;

        try {
            servlet = (Servlet) myClass.newInstance();
            servlet.service((ServletRequest) request, (ServletResponse) response);
        } catch (Throwable e) {
            System.out.println(e);
        }
    }
}
