package com.example;

public interface ProxyConfigurator {
    //    необязательно возвращает проксю. например может просто не стоять аннотация @Deprecated
    Object replaceWithProxyIfNeeded(Object t, Class implClass);
}
