package com.example;

import lombok.SneakyThrows;

import java.lang.reflect.Field;

public class InjectByTypeAnnotationObjectConfigurator implements ObjectConfigurator {
    @Override
    @SneakyThrows
    public void configure(Object t, ApplicationContext context) {
//        берём все филды
        for (Field field : t.getClass().getDeclaredFields()) {
//            если у нас филд аннотирован аннотацией @InjectByType,
            if (field.isAnnotationPresent(InjectByType.class)) {
//                то тогда хотим просетить этот филд объектом который подходит по типу этого филда
                field.setAccessible(true);
                Object object = context.getObject(field.getType());
                field.set(t, object);
            }
        }
    }
}
