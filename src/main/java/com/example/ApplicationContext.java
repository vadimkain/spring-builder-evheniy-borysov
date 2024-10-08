package com.example;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext {
    @Setter
    private ObjectFactory factory;
    private Map<Class, Object> cache = new ConcurrentHashMap<>();
    @Getter
    private Config config;

    public ApplicationContext(Config config) {
        this.config = config;
    }

    //    Class<T> type - желаемый тип объекта
    public <T> T getObject(Class<T> type) {
//        сначала будет равен type потому что а вдруг это гласс конкретный?
        Class<? extends T> implClass = type;

//        если наш кеш уже содержит в себе этот тип, то просто вернем из нашего кеша то что там лежит (уже созданный готовый объект)
        if (cache.containsKey(implClass)) {
            return (T) cache.get(implClass);
        }

//        если у нас нет в кеше этого объекта, то сейчас надо его создавать и настраивать
        if (implClass.isInterface()) {
            implClass = config.getImplClass(type);
        }

//        получаем настроенный тип
        T t = factory.createObject(implClass);

//        проверяем не надо ли этот объект положить в наш кэш
        if (implClass.isAnnotationPresent(Singleton.class)) {
            cache.put(type, t);
        }

        return t;
    }
}
