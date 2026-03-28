//package com.hms.util;
//
//import java.io.IOException;
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//
//public class HttpConnection {
//    public static String sendGET(String url) throws IOException, InterruptedException {
//        // 创建HttpClient实例
//        HttpClient client = HttpClient.newHttpClient();
//        // 创建HttpRequest实例
//        HttpRequest request = HttpRequest.newBuilder()
//                .uri(URI.create(url))
//                .GET() // 默认情况下就是GET请求
//                .build();
//
//        // 发送请求并获取响应
//
//        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//        // 输出响应状态码
//        System.out.println("Response Code : " + response.statusCode());
//
//        // 输出响应体
//        System.out.println("Response Body : " + response.body());
//        return response.body();
//    }
//}
