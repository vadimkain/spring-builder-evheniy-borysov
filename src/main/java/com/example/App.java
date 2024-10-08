package com.example;

import java.util.HashMap;
import java.util.Map;

public class App {
    public static void main(String[] args) {
        ApplicationContext context = ApplicationRunner.run(
                "com.example",
                new HashMap<>(Map.of(Policeman.class, PolicemanImpl.class))
        );
        CoronaDesinfector desinfector = context.getObject(CoronaDesinfector.class);
        desinfector.start(new Room());
    }
}
