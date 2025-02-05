package com.hms.test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpHeaders;
import java.util.Map;

public class CurlToJavaHttpClient {
    public static void main(String[] args) {
        try {
            // 创建 HttpClient
            HttpClient client = HttpClient.newHttpClient();

            // 创建 HttpRequest
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://community.yonyou.com/iuap-ptc-ybdc/system/clock/clock"))
                    .header("Accept", "*/*")
                    .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6")
                    .header("Authorization", "Bearer eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImxvZ2luX3Rva2Vuczo1OTA0NWZiMC0wYTkxLTRlYmEtYWI4MC1lZDU2ZGExODU0ZDgifQ.7Srtd7A6cX1t1y3mUYMgTtGqw6mzv6wBMZYlJl_8_ADb_2Vsfhluv7eWiMyq4Ej985smtk7nLvflh4HYfhMA3w")
//                    .header("Content-Length", "0")
                    .header("Cookie", "acw_tc=1a0c640817325855537104061e00699f606706643ecadac8b79633dee0fbb6; Hm_lvt_79cba97204629d9fcf1ed81661fa43d3=1730611832,1731407581; HMACCOUNT=7F7416303E2B4DE3; community-web-Token=eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6ImxvZ2luX3Rva2Vuczo1OTA0NWZiMC0wYTkxLTRlYmEtYWI4MC1lZDU2ZGExODU0ZDgifQ.7Srtd7A6cX1t1y3mUYMgTtGqw6mzv6wBMZYlJl_8_ADb_2Vsfhluv7eWiMyq4Ej985smtk7nLvflh4HYfhMA3w; Hm_lpvt_79cba97204629d9fcf1ed81661fa43d3=1731407873")
                    .header("Origin", "https://community.yonyou.com")
                    .header("Priority", "u=1, i")
                    .header("Referer", "https://community.yonyou.com/profile/index")
                    .header("Sec-Ch-Ua", "\"Chromium\";v=\"130\", \"Microsoft Edge\";v=\"130\", \"Not?A_Brand\";v=\"99\"")
                    .header("Sec-Ch-Ua-Mobile", "?0")
                    .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36 Edg/130.0.0.0")
                    .POST(HttpRequest.BodyPublishers.noBody())  // 发送一个没有body的POST请求
                    .build();

            // 发送请求并获取响应
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // 打印响应状态码和响应体
            System.out.println("Response Code: " + response.statusCode());
            System.out.println("Response Body: " + response.body());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
