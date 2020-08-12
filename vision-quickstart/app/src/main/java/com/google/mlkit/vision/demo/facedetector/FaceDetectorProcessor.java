/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.facedetector;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.PointF;
import androidx.renderscript.Allocation;
import androidx.renderscript.RenderScript;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.LivePreviewActivity;
import com.google.mlkit.vision.demo.VisionProcessorBase;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Face Detector Demo.
 */
public class FaceDetectorProcessor extends VisionProcessorBase<List<Face>> {
    private static final String TAG = "MOBED_FaceDetector";
    public static Bitmap image;
    private final FaceDetector detector;
    Interpreter tflite;
    public float leftEyeleft, leftEyetop, leftEyeright, leftEyebottom;
    public float rightEyeleft, rightEyetop, rightEyeright, rightEyebottom;
    private RenderScript RS;
    private ScriptC_singlesource script;
    private static final float EYE_BOX_RATIO = 1.4f;

    private float[][][][] left_4d, right_4d, detface_4d, face_grid;

    private float yhatX =0, yhatY=0;

    public FaceDetectorProcessor(Context context, FaceDetectorOptions options, Interpreter interpreter) {
        super(context);
        Log.v(MANUAL_TESTING_LOG, "Face detector options: " + options);
        detector = FaceDetection.getClient(options);
        tflite = interpreter;
        RS = RenderScript.create(context);
        script = new ScriptC_singlesource(RS);
    }

    @Override
    public void stop() {
        super.stop();
        detector.close();
    }

    @Override
    protected Task<List<Face>> detectInImage(InputImage image) {
        return detector.process(image);
    }

    @Override
    protected void onSuccess(@NonNull List<Face> faces, @NonNull GraphicOverlay graphicOverlay) {
        /**
         * TODO
         * MOBED
         * Notice!
         * Real "Left eye" would be "Right eye" in the face detection. Because camera is left and right reversed.
         * And all terms in face detection would follow direction of camera preview image
         * */
        for (Face face : faces) {
            //MOBED
            List<PointF> leftEyeContour = face.getContour(FaceContour.LEFT_EYE).getPoints();
            List<PointF> rightEyeContour = face.getContour(FaceContour.RIGHT_EYE).getPoints();
            float righteye_leftx = rightEyeContour.get(0).x;
            float righteye_lefty = rightEyeContour.get(0).y;
            float righteye_rightx = rightEyeContour.get(8).x;
            float righteye_righty = rightEyeContour.get(8).y;
            float lefteye_leftx = leftEyeContour.get(0).x;
            float lefteye_lefty = leftEyeContour.get(0).y;
            float lefteye_rightx = leftEyeContour.get(8).x;
            float lefteye_righty = leftEyeContour.get(8).y;
            float righteye_centerx = (righteye_leftx + righteye_rightx)/2.0f;
            float righteye_centery = (righteye_lefty + righteye_righty)/2.0f;
            float lefteye_centerx = (lefteye_leftx + lefteye_rightx)/2.0f;
            float lefteye_centery = (lefteye_lefty + lefteye_righty)/2.0f;
            float lefteyeboxsize = (lefteye_centerx-lefteye_leftx)*EYE_BOX_RATIO;
            float righteyeboxsize = (righteye_centerx-righteye_leftx)*EYE_BOX_RATIO;
            leftEyeleft = lefteye_centerx - lefteyeboxsize;
            leftEyetop = lefteye_centery + lefteyeboxsize;
            leftEyeright = lefteye_centerx + lefteyeboxsize;
            leftEyebottom = lefteye_centery - lefteyeboxsize;
            rightEyeleft = righteye_centerx - righteyeboxsize;
            rightEyetop = righteye_centery + righteyeboxsize;
            rightEyeright = righteye_centerx + righteyeboxsize;
            rightEyebottom = righteye_centery - righteyeboxsize;
//            Log.d(TAG, "Right Eye: "+rightEyeleft+", "+rightEyetop+", "+rightEyeright+", "+rightEyebottom);
//            Log.d(TAG, "Left Eye: "+leftEyeleft+", "+leftEyetop+", "+leftEyeright+", "+leftEyebottom);
            try {
                Bitmap leftBitmap=Bitmap.createBitmap(image, (int)leftEyeleft,(int)leftEyebottom,(int)(lefteyeboxsize*2), (int)(lefteyeboxsize*2));
                Bitmap rightBitmap=Bitmap.createBitmap(image, (int)rightEyeleft,(int)rightEyebottom,(int)(righteyeboxsize*2), (int)(righteyeboxsize*2));
//                Allocation inputAllocation = Allocation.createFromBitmap( RS, leftBitmap);
//                Allocation outputAllocation = Allocation.createTyped( RS, inputAllocation.getType());
//                script.invoke_process(inputAllocation, outputAllocation);
//                outputAllocation.copyTo(leftBitmap);
//                inputAllocation = Allocation.createFromBitmap( RS, rightBitmap);
//                outputAllocation = Allocation.createTyped( RS, inputAllocation.getType());
//                script.invoke_process(inputAllocation, outputAllocation);
//                outputAllocation.copyTo(rightBitmap);

                /**
                 * MOBED SaveBitmapToFileCache
                 * Made For Debug Purpose
                 * */
//                SharedPreferences sf = LivePreviewActivity.getSf();
//                int count = sf.getInt("count",0);
//                String file0 = "lefteye"+count+".jpg";
//                String file1 = "righteye"+count+".jpg";
//                Log.d(TAG, "file0: "+file0);
//                SaveBitmapToFileCache(leftBitmap,"/sdcard/CaptureApp/lefteye/",file0);
//                SaveBitmapToFileCache(rightBitmap,"/sdcard/CaptureApp/righteye/",file1);
//                Log.d(TAG, "Bitmap saved");
//                LivePreviewActivity.addCount();
            }
            catch (java.lang.IllegalArgumentException e) {
                Log.e(TAG, "java.lang.IllegalArgumentException x + width must be <= bitmap.width()");
            }
            Log.d(TAG, "Bitmap created");

            graphicOverlay.add(new FaceGraphic(graphicOverlay, face));
            //logExtrasForTesting(face);
        }
        if (image!=null) {
            Bitmap originalCameraImage = image;
            Log.d(TAG, "Image "+originalCameraImage.getWidth()+","+originalCameraImage.getHeight());
            Log.d(TAG, "Overlay "+graphicOverlay.getWidth()+","+graphicOverlay.getHeight());
        }
        else {
            Log.d(TAG, "originalCameraImage is null");
        }
    }

/**
 * MOBED
 * Made For Debug Purpose

 * */
public static void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath, String filename) {
    File file = new File(strFilePath);
    if (!file.exists())
        file.mkdirs();
    File fileCacheItem = new File(strFilePath + filename);
    Log.d(TAG, "filename: "+strFilePath + filename);
    FileOutputStream out = null;
    try {
        out = new FileOutputStream(fileCacheItem);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
    } catch (Exception e) {
        e.printStackTrace();
    }
}




    private static void logExtrasForTesting(Face face) {
        if (face != null) {
            Log.v(MANUAL_TESTING_LOG, "face bounding box: " + face.getBoundingBox().flattenToString());
            Log.v(MANUAL_TESTING_LOG, "face Euler Angle X: " + face.getHeadEulerAngleX());
            Log.v(MANUAL_TESTING_LOG, "face Euler Angle Y: " + face.getHeadEulerAngleY());
            Log.v(MANUAL_TESTING_LOG, "face Euler Angle Z: " + face.getHeadEulerAngleZ());

            // All landmarks
            int[] landMarkTypes =
                    new int[]{
                            FaceLandmark.MOUTH_BOTTOM,
                            FaceLandmark.MOUTH_RIGHT,
                            FaceLandmark.MOUTH_LEFT,
                            FaceLandmark.RIGHT_EYE,
                            FaceLandmark.LEFT_EYE,
                            FaceLandmark.RIGHT_EAR,
                            FaceLandmark.LEFT_EAR,
                            FaceLandmark.RIGHT_CHEEK,
                            FaceLandmark.LEFT_CHEEK,
                            FaceLandmark.NOSE_BASE
                    };
            String[] landMarkTypesStrings =
                    new String[]{
                            "MOUTH_BOTTOM",
                            "MOUTH_RIGHT",
                            "MOUTH_LEFT",
                            "RIGHT_EYE",
                            "LEFT_EYE",
                            "RIGHT_EAR",
                            "LEFT_EAR",
                            "RIGHT_CHEEK",
                            "LEFT_CHEEK",
                            "NOSE_BASE"
                    };
            for (int i = 0; i < landMarkTypes.length; i++) {
                FaceLandmark landmark = face.getLandmark(landMarkTypes[i]);
                if (landmark == null) {
                    Log.v(
                            MANUAL_TESTING_LOG,
                            "No landmark of type: " + landMarkTypesStrings[i] + " has been detected");
                } else {
                    PointF landmarkPosition = landmark.getPosition();
                    String landmarkPositionStr =
                            String.format(Locale.US, "x: %f , y: %f", landmarkPosition.x, landmarkPosition.y);
                    Log.v(
                            MANUAL_TESTING_LOG,
                            "Position for face landmark: "
                                    + landMarkTypesStrings[i]
                                    + " is :"
                                    + landmarkPositionStr);
                }
            }
            Log.v(
                    MANUAL_TESTING_LOG,
                    "face left eye open probability: " + face.getLeftEyeOpenProbability());
            Log.v(
                    MANUAL_TESTING_LOG,
                    "face right eye open probability: " + face.getRightEyeOpenProbability());
            Log.v(MANUAL_TESTING_LOG, "face smiling probability: " + face.getSmilingProbability());
            Log.v(MANUAL_TESTING_LOG, "face tracking id: " + face.getTrackingId());
        }
    }

    @Override
    protected void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Face detection failed " + e);
    }
}
