package com.example.gesturerecognition;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.util.Log;

import com.example.gesturerecognition.ml.ModelAcc;
import com.example.gesturerecognition.ml.ModelAcc4;
import com.example.gesturerecognition.ml.ModelGyro3;
import com.example.gesturerecognition.ml.ModelGyro4;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
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
    private List<String> labelList;


    private TensorFlowGestureClassifier() {

    }

    static Classifier create(AssetManager assetManager,
                             String modelPath,
                             String labelPath) throws IOException {

        TensorFlowGestureClassifier classifier = new TensorFlowGestureClassifier();
//        classifier.interpreter = new Interpreter(classifier.loadModelFile(assetManager, modelPath), new Interpreter.Options());
        classifier.labelList = classifier.loadLabelList(assetManager, labelPath);


        return classifier;
    }

    private float[] makeSingleDimension(float[][][][] Data)
    {
        float[] res = new float[240];
        for(int i=0;i<80;i++)
        {
            for(int j=0;j<3;j++)
            {
                res[j*80+i]=Data[0][i][j][0];
            }
        }
        return res;
    }


    @Override
    public int gestureRecognitionModel(float[][][][] data, Context ctx,Boolean isGyro) {


        if(isGyro)
        {
            try {
                ModelGyro3 model = ModelGyro3.newInstance(ctx);

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 80, 3, 1}, DataType.FLOAT32);
//            inputFeature0.loadBuffer(data);
                inputFeature0.loadArray(makeSingleDimension(data));
                // Runs model inference and gets result.
                ModelGyro3.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                float res[] = outputFeature0.getFloatArray();
                // Releases model resources if no longer used.
                model.close();
                return getMaximumIndex(res);
            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
        else {
            try {
                ModelAcc4 model = ModelAcc4.newInstance(ctx);

                // Creates inputs for reference.
                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 80, 3, 1}, DataType.FLOAT32);
//            inputFeature0.loadBuffer(data);
                inputFeature0.loadArray(makeSingleDimension(data));
                // Runs model inference and gets result.
                ModelAcc4.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                float res[] = outputFeature0.getFloatArray();
                // Releases model resources if no longer used.
                model.close();
                return getMaximumIndex(res);
            } catch (IOException e) {
                // TODO Handle the exception
            }
        }
            return labelList.size();

    }

    @Override
    public List<Recognition> recognizeGesture(float[][][][] data) {



            for (int i = 0; i < data.length; i++) {
                for (int j = 0; j < data[i].length; j++) {
                    Log.d("hey_value", String.valueOf(data[i][j][0][0]) + " " + String.valueOf(data[i][j][1][0]) + " " + String.valueOf(data[i][j][2][0]));
                }
            }

            float[] ans = new float[labelList.size()];

            float[][] Ans = new float[1][labelList.size()];

            if (data == null)
                return getSortedResultFloat(Ans);

            float[][] result = new float[data.length][labelList.size()];

            interpreter.run(data, result);
            for (int i = 0; i < result.length; i++) {
//                for(int j=0;j<labelList.size();j++)
//                {
//                    ans[j]=ans[j]+result[i][j];
//                }
                ans[getMaximumIndex(result[i])]++;
            }

            for (int i = 0; i < labelList.size(); i++)
                Log.d("hey++", String.valueOf(ans[i]));

            for (int i = 0; i < labelList.size(); i++) {
                double d = ans[i] / result.length;
                float f = (float) d;
                Log.d("hey--", labelList.get(i) + " " + String.valueOf(f));
                Ans[0][i] = f;
            }
            return getSortedResultFloat(Ans);


    }

    private int getMaximumIndex(float[] d)
    {
        float score=-1;
        int ans=-1;
        for(int i=0;i<d.length;i++)
        {
            Log.d("hey_score",String.valueOf(d[i]));
            if(score<d[i])
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
                        confidence));
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
                        confidence));
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
