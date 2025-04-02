//package com.hms.ollama;
//
//import io.github.amithkoujalgi.ollama4j.core.OllamaAPI;
//import io.github.amithkoujalgi.ollama4j.core.models.OllamaResult;
//import io.github.amithkoujalgi.ollama4j.core.utils.OptionsBuilder;
//
//import java.io.IOException;
//
//public class Chat {
//    private static String host = "http://localhost:11434/";
//    private static String model = "qwen2";
//
//    public static String generate(String msg) throws Exception {
//
//        OllamaAPI ollamaAPI = new OllamaAPI(host);
//
//        OllamaResult result =ollamaAPI.generate(model, msg, new OptionsBuilder().build());
//
//        return result.getResponse();
//    }
//}
