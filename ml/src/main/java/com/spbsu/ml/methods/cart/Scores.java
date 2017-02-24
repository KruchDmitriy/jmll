package com.spbsu.ml.methods.cart;

/**
 * Created by n_buga on 23.02.17.
 */
public class Scores {
    public static double scoreSat(double sum, int count, double sqrSum) {
        return (getnDisp(sum, count, sqrSum))*count*(count - 2)/(count*count - 3*count + 1);
    }

    public static double getnDisp(double sum, int count, double sqrSum) {
        return sqrSum - (sum * sum) / count;
    }
}