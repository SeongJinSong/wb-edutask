package com.wb.edutask.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 화면 라우팅을 담당하는 컨트롤러
 * 
 * @author WB Development Team
 * @version 1.0.0
 * @since 2024-01-01
 */
@Controller
public class ViewController {

    /**
     * 메인 페이지 (강의 목록)
     * 
     * @return 강의 목록 화면
     */
    @GetMapping("/")
    public String index() {
        return "redirect:/index.html";
    }
    
    /**
     * 강의 목록 페이지
     * 
     * @return 강의 목록 화면
     */
    @GetMapping("/courses")
    public String courses() {
        return "index";
    }
    
    /**
     * 수강 신청 페이지
     * 
     * @return 수강 신청 화면
     */
    @GetMapping("/enrollment")
    public String enrollment() {
        return "redirect:/enrollment.html";
    }
    
    /**
     * 회원 관리 페이지
     * 
     * @return 회원 관리 화면
     */
    @GetMapping("/members")
    public String members() {
        return "redirect:/members.html";
    }
    
    /**
     * 회원가입 페이지
     * 
     * @return 회원가입 화면
     */
    @GetMapping("/signup")
    public String signup() {
        return "redirect:/signup.html";
    }
    
    /**
     * 강의등록 페이지
     * 
     * @return 강의등록 화면
     */
    @GetMapping("/course-register")
    public String courseRegister() {
        return "redirect:/course-register.html";
    }
}
