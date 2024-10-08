package com.example;

//@Deprecated
@Singleton
public class RecommendatorImpl implements Recommendator {
    @InjectProperty("wisky")
    private String alcohol;

    public RecommendatorImpl() {
        System.out.println("RecommendatorImpl was created");
    }

    @Override
    public void recommend() {
        System.out.println("to protect from covid-2019 drink " + alcohol);
    }
}
