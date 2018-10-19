package com.tiger.service.impl;

import com.tiger.annotation.MyService;
import com.tiger.service.IDemoService;

/**
 * Created by Administrator on 2018/8/1.
 */
@MyService
public class DemoService implements IDemoService {
    @Override
    public String get(String name) {
        return "get in DemoService===>" + name;
    }
}
