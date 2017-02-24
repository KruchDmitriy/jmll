package com.spbsu.ml.methods.cart;

import com.spbsu.commons.math.Trans;
import com.spbsu.commons.math.vectors.Mx;
import com.spbsu.commons.math.vectors.Vec;
import com.spbsu.commons.math.vectors.impl.vectors.ArrayVec;
import com.spbsu.ml.data.set.VecDataSet;
import com.spbsu.ml.loss.L2;
import com.spbsu.ml.loss.WeightedLoss;
import com.spbsu.ml.methods.VecOptimization;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Created by n_buga on 16.10.16.
 */
public class CARTTreeOptimization extends VecOptimization.Stub<WeightedLoss<? extends L2>> {


    private List<Leaf> ownerLeafOfData;
    private Vec[] orderedFeatures;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);
    private final double lambda = 0;

    public CARTTreeOptimization(VecDataSet learn) {
        orderedFeatures = new Vec[learn.xdim()];
        final Mx data = learn.data();
        for (int f = 0; f < orderedFeatures.length; f++) {
            orderedFeatures[f] = new ArrayVec(learn.length());
            final int[] order = learn.order(f);
            for (int i = 0; i < order.length; i++) {
                orderedFeatures[f].set(i, data.get(order[i], f));
            }
        }
    }

    public Trans fit(VecDataSet learn, WeightedLoss loss) {
        List<Leaf> tree = new ArrayList<>();
        Leaf firstLeaf = new Leaf(0);

        ownerLeafOfData = new ArrayList<>(learn.length());
        for (int i = 0; i < learn.length(); i++) {
            ownerLeafOfData.add(firstLeaf);
            for (int j = 0; j < loss.weight(i); j++) {
                firstLeaf.addNewItem(loss.target().get(i));
            }
        }

        firstLeaf.calcError();
        firstLeaf.calcMean();

        double sigma2 = firstLeaf.getError()/firstLeaf.getCount();
        Leaf.setSigma2(sigma2);

        tree.add(firstLeaf);

        constructTree(tree, learn, loss);

        return new CARTTree(tree);
    }

    private void constructTree(List<Leaf> tree, VecDataSet learn, WeightedLoss loss) {
        int count = 0;
        int old_size = tree.size();
        while (count < 6) { //!!!!
            makeStep(tree, learn, loss);
            count++;
            if (old_size == tree.size()) {
                break;
            }
            old_size = tree.size();
        }
    }

    private double makeStep(List<Leaf> tree, VecDataSet learn, WeightedLoss loss) { //return maxError along new leaves  ?streams?

        double bestError[] = new double[tree.size()];
        Condition bestCondition[] = new Condition[tree.size()];

        for (int i = 0; i < tree.size(); i++) {
            bestError[i] = Double.POSITIVE_INFINITY;
            bestCondition[i] = new Condition();
        }

        //предподсчитать

        final int dim = learn.xdim();
        final Vec target = loss.target();
        final int length = learn.length();

        final Future[] tasks = new Future[dim];

        for (int i = 0; i < dim; i++) { // sort out feature
            final int k = i;
            tasks[i] = executorService.submit(() -> handleFeature(learn, loss, target, orderedFeatures,
                    bestError, bestCondition,
                    k, tree.size(), length));
        }

        int countLeavesBefore = tree.size();
        Leaf pairLeaf[] = new Leaf[countLeavesBefore];

        try {
            for (int i = 0; i < dim; i++) {
                tasks[i].get();
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

/*        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } */

        for (int i = 0; i < countLeavesBefore; i++) {
            Leaf leaf = tree.get(i);
            if (leaf.getError() <= bestError[i]) { // if new error is worse then old
                pairLeaf[i] = leaf;
                continue;
            }

            Leaf newLeaf = new Leaf(leaf, tree.size());
            tree.add(newLeaf);
            pairLeaf[i] = newLeaf;

            leaf.getListFeatures().addFeature(bestCondition[i]);
            newLeaf.getListFeatures().addFeature(new Condition(bestCondition[i]).set(false));

        }

        for (int i = 0; i < learn.length(); i++) {
            Leaf curLeaf = ownerLeafOfData.get(i);
            int leafNumber = curLeaf.getLeafNumber();
            if (!bestCondition[leafNumber].checkFeature(learn.data().row(i))) {
                ownerLeafOfData.set(i, pairLeaf[leafNumber]);
            }
        }

        for (Leaf leaf: tree) {
            leaf.clearStatistic();
        }


        for (int i = 0; i < learn.length(); i++) {
            Leaf curLeaf = ownerLeafOfData.get(i);
            for (int j = 0; j < loss.weight(i); j++) {
                curLeaf.addNewItem(target.get(i));
            }
        }

        double maxErr = 0; //the return value

        for (Leaf leaf: tree) {
            if (leaf.getCount() == 0) {
                continue;
            }
            leaf.calcError();
            leaf.calcMean();
            maxErr = Math.max(maxErr, leaf.getError());
        }

        //0.075, ограничить размер глубины 7.

        //распараллетить нахождение лучше ошибки(например, по value или по feature)
        // дисперсия, исправленная дисперсия, средневыборочная
        // выкинуть фрейм(иконка) - для дебага
        // код грязный, внести апдейт внутрь листа, чтобы до приватных переменных не дошло
        // лист умеет обновляться и делится - вынести в лист.
        // secondPartsum - внутрь лифов.

        // Функция слишком большая - разделить
        // Метод для подсчёта ошибки

        return maxErr;
    }

    private void handleFeature(VecDataSet learn, WeightedLoss loss, Vec target, Vec[] orderedFeatures,
                               double[] bestScore, Condition[] bestCondition,
                               int numFeature, int treeSize, int learnLength) {
        final int[] order = learn.order(numFeature);
        final Vec orderedFeature = orderedFeatures[numFeature];

        int curCount[] = new int[treeSize];
        double partSum[] = new double[treeSize];
        double last[] = new double[treeSize];
        double partSqrtSum[] = new double[treeSize];

        for (int j = 0; j < treeSize; j++) {
            curCount[j] = 0;
            partSum[j] = 0;
            last[j] = 0;
            partSqrtSum[j] = 0;
        }

        for (int j = 0; j < learnLength; j++) { //sort out vector on barrier
            final int curIndex = order[j];                  //check error of this barrier
            final double weight = loss.weight(curIndex);
            if (weight == 0) {
                continue;
            }
            final Leaf curLeaf = ownerLeafOfData.get(curIndex);
            final int leafNumber = curLeaf.getLeafNumber();

            final double x_ji = orderedFeature.get(j);

            if (curCount[leafNumber] > 0 && last[leafNumber] < x_ji) { // catch boarder
                final double firstPartSum = partSum[leafNumber];
                final int firstPartCount = curCount[leafNumber];
                final double secondPartSum = curLeaf.getSum() - firstPartSum;
                final int secondPartCount = curLeaf.getCount() - firstPartCount;
                final double firstPartSqrSum = partSqrtSum[leafNumber];
                final double secondPartSqrSum = curLeaf.getSqrSum() - firstPartSqrSum;
                final double errorLeft = score(firstPartSum, firstPartCount, firstPartSqrSum);
                final double errorRight = score(secondPartSum, secondPartCount, secondPartSqrSum);
                final double curError = errorLeft + errorRight + lambda*entropy(curLeaf.getCount(),
                        firstPartCount, learnLength, treeSize);
                synchronized (curLeaf) {
                    if (curError < bestScore[leafNumber] && curError < curLeaf.getError()) {
                        bestScore[leafNumber] = curError;
                        bestCondition[leafNumber].set(numFeature, x_ji, true);
                    }
                }
            }

            double y = target.get(curIndex);

            partSum[leafNumber] += y*weight;
            curCount[leafNumber] += weight;
            last[leafNumber] = x_ji; //last value of data in this leaf
            partSqrtSum[leafNumber] += y*y*weight;
        }
    }

    private double score(double sum, int count, double sqrSum) {
        double score;
        if (count <= 2) {
            score = Double.POSITIVE_INFINITY;
        } else {
            score = Scores.getnDisp(sum, count, sqrSum);
        }
        return score;
    }

    private double entropy(int genCount, int leftCount, int n, int leathCount) {
        int rightCount = genCount - leftCount;
        double p1 = (leftCount + 1)*1.0/(genCount + 2)*(genCount + 1)/(n + leathCount);
        double p2 = (rightCount + 1)*1.0/(genCount + 2)*(genCount + 1)/(n + leathCount);

//        double p1 = (leftCount + 1)*1.0/(genCount + leftCount);
//        double p2 = (rightCount + 1)*1.0/(genCount + leftCount);
        return -(-p1*Math.log(p1) - p2*Math.log(p2));
    }
}