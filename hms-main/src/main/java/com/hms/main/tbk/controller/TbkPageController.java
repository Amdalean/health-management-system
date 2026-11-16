package com.hms.main.tbk.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import com.hms.common.annotation.Anonymous;

/**
 * 淘宝客 H5 页面入口
 *
 * 让用户用 /tbk/ 这样的短地址访问，自动跳到静态页 index.html。
 *
 * add by CYQ 2026-11-02 淘宝客转链模块
 */
@Anonymous
@Controller
@RequestMapping("/tbk")
public class TbkPageController {

    @GetMapping({ "", "/" })
    public String index() {
        return "redirect:/tbk/index.html";
    }
}
