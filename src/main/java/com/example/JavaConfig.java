package com.example;

import org.reflections.Reflections;

import java.util.Map;
import java.util.Set;

public class JavaConfig implements Config {
    private Reflections scanner;
    private Map<Class, Class> ifc2ImplClass;

    public JavaConfig(String packageToScan, Map<Class, Class> ifc2ImplClass) {
        this.scanner = new Reflections(packageToScan);
        this.ifc2ImplClass = ifc2ImplClass;
    }

    @Override
    public <T> Class<? extends T> getImplClass(Class<T> ifc) {

//        если наш JavaConfig не знает про имплементацию для интерфейса ничего
        return ifc2ImplClass.computeIfAbsent(ifc, aClass -> {

//        просим у сканнера все подвиды данного интерфейса.
//        другими словами получаем все классы которые реализуют данный интерфейс
            Set<Class<? extends T>> classes = scanner.getSubTypesOf(ifc);
//        если у нас размер не является 1 (либо есть импл или их больше 1, то продолжать не можем)
            if (classes.size() != 1) {
                throw new RuntimeException(ifc + " has 0 or more than one implementation");
            }

//        Берём ту единственную имплементацию которую нашли
            return classes.iterator().next();
        });
    }

    @Override
    public Reflections getScanner() {
        return this.scanner;
    }
}
