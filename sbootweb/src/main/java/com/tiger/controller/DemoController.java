package com.tiger.controller;

import com.tiger.annotation.MyAutowired;
import com.tiger.annotation.MyController;
import com.tiger.annotation.MyRequestMapping;
import com.tiger.annotation.MyRequestParam;
import com.tiger.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by Administrator on 2018/8/1.
 */
@MyController
@MyRequestMapping("/demo")
public class DemoController {
    @MyAutowired
    private IDemoService service;
    @MyRequestMapping("/get")
    public void get(@MyRequestParam("name") String name, HttpServletRequest request, HttpServletResponse response)throws Exception{
        String username = service.get(name);
        response.getWriter().write("get method====>"+username);
    }

    @MyRequestMapping("/save")
    public void save(
            @MyRequestParam("name") String name,
            HttpServletRequest request, HttpServletResponse response)throws Exception{
        response.getWriter().write("get method");
    }

}
