package com.jeremy.courses;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/")
    public String hello() {
        return "Hello Spring Boot!";
    }

    @GetMapping("/course")
    public Course getCourse() {
        return new Course("Java Backend", "Learn Spring Boot from scratch");
    }
}
