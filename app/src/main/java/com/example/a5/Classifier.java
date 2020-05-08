package com.example.a5;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.location.Location;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

import org.tensorflow.lite.Interpreter;

public class Classifier {

    private Interpreter tflite;

    public Classifier(AssetManager assetManager){
        try {
            tflite =  new Interpreter(loadModelFile(assetManager));
            Log.d("woog","started roll");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int doInference(){
        float[][][][] input = new float[1][40][44][1];
        float[][] out = new float[1][20];
        Log.d("woof",tflite.getOutputTensorCount()+"otputtenrsors");
        tflite.run(input,out);
        return out[0][0] > out[0][1] ? 0 : 1;
    }
    public int doInference(float[][][][] input){
        input = new float[1][40][44][1];
        float[][] out = new float[1][20];
        Log.d("woof",tflite.getOutputTensorCount()+"otputtenrsors");
        tflite.run(input,out);
        return out[0][0] > out[0][1] ? 0 : 1;
    }
    public int doInferenceWof(float[][][][] input){
        input = new float[1][128][44][1];
        float[][] out = new float[1][20];
        Log.d("woof",tflite.getOutputTensorCount()+"otputtenrsors");
        tflite.run(input,out);
        return out[0][0] > out[0][1] ? 0 : 1;
    }


    private static MappedByteBuffer loadModelFile(AssetManager assetManager)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assetManager.openFd("wof2.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
