package com.example.gesturerecognition;

import android.annotation.SuppressLint;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TensorFlowGestureClassifier implements Classifier {

    private static final int MAX_RESULTS = 3;
    private static final float THRESHOLD = 0.1f;



    private Interpreter interpreter;
    private int inputSize;
    private List<String> labelList;
    private boolean quant;

    private TensorFlowGestureClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath,
                             int inputSize,
                             boolean quant) throws IOException {

        TensorFlowGestureClassifier classifier = new TensorFlowGestureClassifier();
        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, modelPath), new Interpreter.Options());
        classifier.labelList = classifier.loadLabelList(assetManager, labelPath);
        classifier.inputSize = inputSize;
        classifier.quant = quant;

        return classifier;
    }

    @Override
    public List<Recognition> recognizeGesture(float[][][][] data) {

//        ByteBuffer byteBuffer = convertBitmapToByteBuffer(bitmap);
        int[] ans = new int[6];
        if(quant){
            byte[][] result = new byte[1][labelList.size()];
            interpreter.run(data, result);
            return getSortedResultByte(result);
        } else {
            float [][] Ans = new float[1][labelList.size()];
            float [][] result = new float[data.length][6];
            interpreter.run(data, result);
            for(int i=0;i<result.length;i++)
            {
                ans[getMinimumIndex(result[i])]++;
            }

            for(int i=0;i<6;i++)
            Log.d("hey++",String.valueOf(ans[i]));

            for(int i=0;i<labelList.size();i++)
            {
                double d = ans[i]*1.0/result.length;
                float f = (float)d;
                Log.d("hey--",labelList.get(i)+" "+String.valueOf(f));
                Ans[0][i]=f;
            }
            return getSortedResultFloat(Ans);
        }

    }

    private int getMinimumIndex(float[] d)
    {
        float score=100;
        int ans=-1;
        for(int i=0;i<d.length;i++)
        {
            Log.d("hey_score",String.valueOf(d[i]));
            if(score>d[i])
            {
                score=d[i];
                ans=i;
            }
        }
        Log.d("hey_score","break");
        return ans;
    }
    @Override
    public void close() {
        interpreter.close();
        interpreter = null;
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResultByte(byte[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < labelList.size(); ++i) {
            float confidence = (labelProbArray[0][i] & 0xff) / 255.0f;
            if (confidence > THRESHOLD) {
                pq.add(new Recognition("" + i,
                        labelList.size() > i ? labelList.get(i) : "unknown",
                        confidence, quant));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

    @SuppressLint("DefaultLocale")
    private List<Recognition> getSortedResultFloat(float[][] labelProbArray) {

        PriorityQueue<Recognition> pq =
                new PriorityQueue<>(
                        MAX_RESULTS,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });

        for (int i = 0; i < labelList.size(); ++i) {
            float confidence = labelProbArray[0][i];
            if (confidence > THRESHOLD) {
                pq.add(new Recognition("" + i,
                        labelList.size() > i ? labelList.get(i) : "unknown",
                        confidence, quant));
            }
        }

        final ArrayList<Recognition> recognitions = new ArrayList<>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

}
