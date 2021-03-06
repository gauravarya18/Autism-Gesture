package com.example.gesturerecognition;

import android.util.Log;

import static java.lang.Integer.min;
import static java.lang.Math.max;

public final class DTW {


    public static class Result {

        private final int[][] mWarpingPath;
        private final double  mDistance;

        public Result(final int[][] pWarpingPath, final double pDistance) {

            this.mWarpingPath = pWarpingPath;
            this.mDistance    = pDistance;
        }

        public final int[][] getWarpingPath() { return this.mWarpingPath; }
        public final double     getDistance() { return this.mDistance;    }
    }


    public DTW() { }

    public DTW.Result compute(final float[][] pSample, final float[][] pTemplate) {

        final int lN = pSample[0].length;
        final int lM = pTemplate[0].length;

        if(lN == 0 || lM == 0) {

            return new DTW.Result(new int[][]{ /* No path data. */ }, Double.NaN);
        }

        int lK = 1;

        final int[][]    lWarpingPath  = new int[lN + lM][2];
        // Declare the Local Distances.
        final double[][] lL            = new double[lN][lM];
        // Declare the Global Distances.
        final double[][] lG            = new double[lN][lM];

        final double[]   lMinimaBuffer = new double[3];

        int i, j;

        for(i = 0; i < lN; i++) {
            for(j = 0; j < lM; j++) {
                // Calculate the Distance between the Sample and the Template for this Index.
                // x1 y1 z1 --- x2 y2 z2
                lL[i][j] = this.getDistanceBetween(pSample[0][i],pSample[1][i],pSample[2][i],pTemplate[0][j],pTemplate[1][j],pTemplate[2][j]);
            }
        }

        // Initialize the Global.
        lG[0][0] = lL[0][0];

        for(i = 1; i < lN; i++) {
            lG[i][0] = lL[i][0] + lG[i - 1][0];
        }

        for(j = 1; j < lM; j++) {
            lG[0][j] = lL[0][j] + lG[0][j - 1];
        }


            for (i = 1; i < lN; i++) {
                for (j = 1; j < lM; j++) {
                    lG[i][j] = Double.POSITIVE_INFINITY;
//                Log.d("hey",String.valueOf(lG[i][j]));
                }
            }

        int wrappingWindow = lN/6;
        Log.d("hey_Dtw_window size",String.valueOf(wrappingWindow));
        for (i = 1; i < lN ; i++) {
            for (j = Math.max(1,i-wrappingWindow); j < Math.min(lM,i+wrappingWindow) ; j++) {
                // Accumulate the path.
//                Log.d("hey_",String.valueOf(i)+ " "+ String.valueOf(j));
                lG[i][j] = (Math.min(Math.min(lG[i-1][j], lG[i-1][j-1]), lG[i][j-1])) + lL[i][j];
            }
        }

//        for (i = 0; i < lN ; i++) {
//            String ans ="";
//            for (j = 0; j < lM; j++) {
//                // Accumulate the path.
//                ans = ans + "("+ String.valueOf(i)+String.valueOf(j)+")"+String.valueOf(lG[i][j]) + "  ";
//            }
//            Log.d("hey__",ans);
//        }
        Log.d("hey_Dtw",String.valueOf(lG[lN-1][lM-1]));

        // Update iteration varaibles.
        i = lWarpingPath[lK - 1][0] = (lN - 1);
        j = lWarpingPath[lK - 1][1] = (lM - 1);

        // Whilst there are samples to process...
        while ((i + j) != 0) {
            // Handle the offset.
            if(i == 0) {
                // Decrement the iteration variable.
                j -= 1;
            }
            else if(j == 0) {
                // Decrement the iteration variable.
                i -= 1;
            }
            else {
                // Update the contents of the MinimaBuffer.
                lMinimaBuffer[0] = lG[i - 1][j];
                lMinimaBuffer[1] = lG[i][j - 1];
                lMinimaBuffer[2] = lG[i - 1][j - 1];
                // Calculate the Index of the Minimum.
                final int lMinimumIndex = this.getMinimumIndex(lMinimaBuffer);
                // Declare booleans.
                final boolean lMinIs0 = (lMinimumIndex == 0);
                final boolean lMinIs1 = (lMinimumIndex == 1);
                final boolean lMinIs2 = (lMinimumIndex == 2);
                // Update the iteration components.
                i -= (lMinIs0 || lMinIs2) ? 1 : 0;
                j -= (lMinIs1 || lMinIs2) ? 1 : 0;
            }
            // Increment the qualifier.
            lK++;
//            Log.d("hey_",String.valueOf(i)+ " "+ String.valueOf(j));
            // Update the Warping Path.
            lWarpingPath[lK - 1][0] = i;
            lWarpingPath[lK - 1][1] = j;
        }

        // Return the Result. (Calculate the Warping Path and the Distance.)
        return new DTW.Result(this.reverse(lWarpingPath, lK), ((lG[lN - 1][lM - 1]) / lK));
    }


    private int[][] reverse(final int[][] pPath, final int pK) {

        final int[][] lPath = new int[pK][2];

        for(int i = 0; i < pK; i++) {

            for (int j = 0; j < 2; j++) {

                lPath[i][j] = pPath[pK - i - 1][j];
            }
        }

        return lPath;
    }


    protected double getDistanceBetween(double x1, double y1, double z1, double x2, double y2, double z2) {
        double result = Math.pow((x1-x2),2) + Math.pow((y1-y2),2) + Math.pow((z1-z2),2);
        return Math.sqrt(result);
    }


    protected final int getMinimumIndex(final double[] pArray) {

        int    lIndex = 0;
        double lValue = pArray[0];

        for(int i = 1; i < pArray.length; i++) {

            final boolean lIsSmaller = pArray[i] < lValue;

            lValue = lIsSmaller ? pArray[i] : lValue;
            lIndex = lIsSmaller ?         i : lIndex;
        }

        return lIndex;
    }

}
