package com.tiger.servlet;

import com.tiger.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Administrator on 2018/8/1.
 */
public class MyDispatcherServlet extends HttpServlet {

    private Properties contextConfig = new Properties();
    private List<String> classNames = new ArrayList<String>();
    private Map<String,Object> ioc = new HashMap<String,Object>();
    private List<Handler> handlerMapping = new ArrayList<Handler>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception ,Details:\r\n"+ Arrays.toString(e.getStackTrace()).replaceAll("\\[|\\]","").replaceAll(",\\s","\r\n"));
        }

    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws Exception {
/*        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        url = url.replace(contextPath,"").replaceAll("/+","/");
        if (!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found");
            return;
        }

        Method method = this.handlerMapping.get(url);
        System.out.println(method);*/

        try{

            Handler handler = getHandler(req);
            if(handler==null){
                resp.getWriter().write("404 NOT FOUND");
                return;
            }
            Class<?>[] parameterTypes = handler.method.getParameterTypes();
            Object [] paramValues = new Object[parameterTypes.length];
            Map<String, String[]> params = req.getParameterMap();
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
                if (!handler.paramIndexMapping.containsKey(param.getKey())){
                    continue;
                }
                Integer index = handler.paramIndexMapping.get(param.getKey());
                paramValues[index]=convert(parameterTypes[index],value);
            }

            Integer regIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[regIndex]=req;
            Integer respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex]=resp;
            handler.method.invoke(handler.controller,paramValues);
        }catch(Exception e){
            throw e;
        }

    }




    @Override
    public void init(ServletConfig config) throws ServletException {

        //大框架
        //1  加载配置
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2  扫描所有相关类
        doScanner(contextConfig.getProperty("scanPackage"));

        //3 初始化所有相关类并将所有扫描到的类实例化放入到IOC容器中
        doInstance();
        //4  自动化依赖注入
        doAutowired();

        //5  初始化HandlerMapping
        initHandlerMapping();

        System.out.println("My Spring MVC is init");



    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()){return;}
        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(MyController.class)){continue;}
            String baseUrl = "";
            if(clazz.isAnnotationPresent(MyRequestMapping.class)){
                MyRequestMapping requestMapping = clazz.getAnnotation(MyRequestMapping.class);
                baseUrl = requestMapping.value();

            }

            Method[] methods = clazz.getMethods();
            for (Method method : methods){
                if(!method.isAnnotationPresent(MyRequestMapping.class)){continue;}
                MyRequestMapping requestMapping = method.getAnnotation(MyRequestMapping.class);
                String url = ("/"+baseUrl +"/"+ requestMapping.value()).replaceAll("/+", "/");
                //handlerMapping.put(url,method);
                Pattern pattern = Pattern.compile(url);
                handlerMapping.add(new Handler(pattern,entry.getValue(),method));
                System.out.println("Mapped "+ url +"   into  "+method);
            }
        }

    }

    private void doAutowired() {
        if (ioc.isEmpty()){return;}
        for (Map.Entry<String,Object> entry : ioc.entrySet()){
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields){
                if(!field.isAnnotationPresent(MyAutowired.class)){
                    continue;
                }
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String beanName = myAutowired.value().trim();
                if ("".equals(beanName)){
                    beanName = field.getType().getName();
                }


                //如果这个字段是private，，  他是穿了衣服的
                // 不管你穿没穿衣服 在烦着面前 你就是一丝不挂的
                // 强吻让他妥协
                field.setAccessible(true);


                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        if (classNames.isEmpty()){return;}
        for (String className : classNames){
            // 1   第一步  拿到Class对象   接下来就可以反射

            try {
                Class<?> clazz = Class.forName(className);


                // 2  接下来通过反射机制把类的实例搞出来
                if(clazz.isAnnotationPresent(MyController.class)){
                    Object instance = clazz.newInstance();
                    String key = lowerFirstCase(clazz.getSimpleName());//默认为类名首字母小写
                    ioc.put(key,instance);
                }else if(clazz.isAnnotationPresent(MyService.class)){
                    //1  类名首字母小写

                    //2  如果自己自定义了BeanId  优先使用自定义的



                    MyService service = clazz.getAnnotation(MyService.class);
                    String beanName = service.value();
                    if ("".equals(beanName.trim())){
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    }
                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);


                    // 3  根据接口类型进行实例化
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        ioc.put(i.getName(),instance);
                    }


                }else{
                    continue;
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    private String lowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();
        //大写字母的ASCII 和小写字母的ASCII相差32
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanner(String scanPackage) {

        // 拿到包名

        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));

        File classFile = new File(url.getFile());

        for (File file : classFile.listFiles())
            //如果是一个文件夹 说明是一个子包
            //继续地柜  读取到子包下所有的class
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else{
                String className = (scanPackage+"."+file.getName()).replace(".class","");
                classNames.add(className);
            }

    }

    private void doLoadConfig(String contextConfigLocation)   {

        //拿到spring配置文件路径，读取到文件所有内容
        InputStream is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (null != is){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private Handler getHandler(HttpServletRequest request) throws Exception{
        if(handlerMapping.isEmpty()){return null;}
        String url = request.getRequestURI();
        String contextPath = request.getContextPath();
        url=url.replace(contextPath,"").replaceAll("/+","/");

        for(Handler handler: handlerMapping){
            try {
                Matcher matcher = handler.pattern.matcher(url);
                if (!matcher.matches()) {
                    continue;
                }
                return handler;
            }catch(Exception e){
                throw e;
            }
        }
        return null;








    }

    private Object convert(Class<?> type,String value){
        if(Integer.class==type){
            return Integer.valueOf(value);
        }
        return value;
    }

    private class Handler{
        protected Object controller;
        protected Method method;
        protected Pattern pattern;
        protected Map<String,Integer> paramIndexMapping;
        protected  Handler(Pattern pattern,Object controller,Method method){
            this.controller=controller;
            this.method=method;
            this.pattern=pattern;
            paramIndexMapping = new HashMap<String,Integer>();
            putPramIndexMapping(method);

        }

        private void putPramIndexMapping(Method method) {
            Annotation[][] pa = method.getParameterAnnotations();
            for (int i =0;i<pa.length;i++){
                for(Annotation a : pa[i]){
                    if(a instanceof MyRequestParam){
                        String paramName = ((MyRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName,i);
                        }
                    }
                }
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> parameterType = parameterTypes[i];
                if(parameterType == HttpServletRequest.class
                        ||parameterType == HttpServletResponse.class){
                    paramIndexMapping.put(parameterType.getName(),i);
                }
            }
        }
    }
}
