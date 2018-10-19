package com.tiger.controller;

import com.tiger.annotation.MyController;
import com.tiger.annotation.MyRequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Administrator on 2018/8/2.
 */
@MyController
@MyRequestMapping("/hello")
public class HelloController {
    @MyRequestMapping("/h1")
    public void h1(HttpServletResponse response, HttpServletRequest request) throws Exception {
        response.getWriter().write("h1");
    }
    @MyRequestMapping("/h2")
    public  void h2(HttpServletResponse response, HttpServletRequest request)throws Exception{
        response.getWriter().write("h2");
    }
    @MyRequestMapping("/h3")
    public void h3(HttpServletResponse response, HttpServletRequest request)throws Exception{
        response.getWriter().write("h3");
    }
    @MyRequestMapping("/h4")
    public void h4(HttpServletResponse response, HttpServletRequest request)throws Exception{
        response.getWriter().write("h4");
    }
}
