package com.smartparking.api_gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

    @GetMapping("/test")
    public String test() {
        System.out.println("Działa autoryzacja JWT!");
        return "Działa autoryzacja JWT!";
    }
}
