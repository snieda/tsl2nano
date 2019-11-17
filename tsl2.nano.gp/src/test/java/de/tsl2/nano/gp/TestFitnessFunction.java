package de.tsl2.nano.gp;

import java.util.function.Function;

public class TestFitnessFunction implements Function<Long[], Double> {
    double exptected = 10;
    @Override
    public Double apply(Long[] t) {
        if (t.length != 8)
            return Double.MAX_VALUE;
        double sum = 0;
        for(int i=0; i<t.length; i+=2) {
            sum += t[i] * Math.pow(t[i+1], i);
        }
        return Math.abs(sum - exptected);
    }

    
}