package com.hms.quartz.task;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

public class Test {
    public static void main(String[] args) throws Exception {
        // URL of the request
        String url = "https://community.yonyou.com/iuap-ptc-ybdc/system/clock/clock";

        // Create a new URL object and open a connection
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // Set request method (POST)
        con.setRequestMethod("POST");

        // Add headers
        con.setRequestProperty("accept", "*/*");
        con.setRequestProperty("accept-language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
        con.setRequestProperty("authorization", "Bearer " + Base64.getEncoder().encodeToString("eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjA4ODI3NGM3LTQ5ZmMtNGZkZC1iNWUwLTU4M2VmM2U5NDFhNSJ9.kbBv5KayFSCTurHVoYEm2f8sl_YtrBksAa073zVUH9bLNFaBz1_1f3WG6ZtTOjZVPhXjN9uPo6iuVInqCQQa5Q".getBytes()));
        con.setRequestProperty("content-length", "0");
        con.setRequestProperty("cookie", "Hm_lvt_79cba97204629d9fcf1ed81661fa43d3=1726108219,1726192939; HMACCOUNT=36D24796E35A22DD; acw_tc=1a0c65d317261969634783077e0037ea4bc1b72e68d2dfac3633c37552c09b; community-web-Token=eyJhbGciOiJIUzUxMiJ9.eyJsb2dpbl91c2VyX2tleSI6IjA4ODI3NGM3LTQ5ZmMtNGZkZC1iNWUwLTU4M2VmM2U5NDFhNSJ9.kbBv5KayFSCTurHVoYEm2f8sl_YtrBksAa073zVUH9bLNFaBz1_1f3WG6ZtTOjZVPhXjN9uPo6iuVInqCQQa5Q; Hm_lpvt_79cba97204629d9fcf1ed81661fa43d3=1726197569");
        con.setRequestProperty("origin", "https://community.yonyou.com");
        con.setRequestProperty("priority", "u=1, i");
        con.setRequestProperty("referer", "https://community.yonyou.com/profile/index");
        con.setRequestProperty("sec-ch-ua", "\"Chromium\";v=\"128\", \"Not;A=Brand\";v=\"24\", \"Microsoft Edge\";v=\"128\"");
        con.setRequestProperty("sec-ch-ua-mobile", "?0");
        con.setRequestProperty("sec-ch-ua-platform", "\"Windows\"");
        con.setRequestProperty("sec-fetch-dest", "empty");
        con.setRequestProperty("sec-fetch-mode", "cors");
        con.setRequestProperty("sec-fetch-site", "same-origin");
        con.setRequestProperty("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36 Edg/128.0.0.0");

        // Enable output and set the request to use application/x-www-form-urlencoded
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        // Send POST request
        try(OutputStream os = con.getOutputStream()) {
            // Since we have no body content, we don't need to write anything here.
            // If there were any parameters, they would be written here.
        }

        // Get response code
        int responseCode = con.getResponseCode();
        System.out.println("Response Code : " + responseCode);

        // Close the connection
        con.disconnect();
    }
}