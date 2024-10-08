package com.example;

import java.util.Map;

public class ApplicationRunner {
    public static ApplicationContext run(String packageToScan, Map<Class, Class> if2cImplClass) {
        JavaConfig config = new JavaConfig(packageToScan, if2cImplClass);
        ApplicationContext context = new ApplicationContext(config);
        ObjectFactory objectFactory = new ObjectFactory(context);
        context.setFactory(objectFactory);
        return context;
    }
}
