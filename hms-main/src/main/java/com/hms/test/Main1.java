//package com.hms.test;
//
//import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
//import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
//import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
//
//public class Main1 {
//
//    public static void main(String[] args) throws Exception{
//        String host = "http://localhost:11434/";
//        OllamaAPI ollamaAPI = new OllamaAPI(host);
//
//        OllamaResult result =
//                ollamaAPI.generate("qwen2", "你好?", new OptionsBuilder().build());
//
//        System.out.println(result.getResponse());
//
//    }
//}
