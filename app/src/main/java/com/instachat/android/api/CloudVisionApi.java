package com.instachat.android.api;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.api.services.vision.v1.model.SafeSearchAnnotation;
import com.instachat.android.Constants;
import com.instachat.android.util.MLog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by kevin on 10/27/2016.
 * <p>
 * see https://cloud.google.com/vision/docs/best-practices
 */

public class CloudVisionApi {

    private static final String TAG = "CloudVisionApi";

    public CloudVisionApi(@NonNull CloudVisionApiListener listener) {
        mListener = listener;
    }

    private CloudVisionApiListener mListener;

    public interface CloudVisionApiListener {
        void onImageInspectionCompleted(boolean isCallFailed, boolean isPossiblyAdult, boolean isPossiblyViolent);
    }

    public void checkForAdultOrViolence(final Bitmap bitmap) throws IOException {
        // Switch text to loading

        // Do the real work in an async task, because we need to use the network anyway
        new AsyncTask<Object, Void, String>() {
            @Override
            protected String doInBackground(Object... params) {
                try {
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

                    Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(new
                            VisionRequestInitializer(Constants.GOOGLE_API_KEY));
                    Vision vision = builder.build();

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
                        AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

                        // Add the image
                        Image base64EncodedImage = new Image();
                        // Convert the bitmap to a JPEG
                        // Just in case it's a format that Android understands but Cloud Vision
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
                        byte[] imageBytes = byteArrayOutputStream.toByteArray();

                        // Base64 encode the JPEG
                        base64EncodedImage.encodeContent(imageBytes);
                        annotateImageRequest.setImage(base64EncodedImage);

                        /*
                         * features: "SAFE_SEARCH_DETECTION"  "LABEL_DETECTION"  LANDMARK_DETECTION
                         * and more
                         */
                        // add the features we want
                        ArrayList<Feature> list = new ArrayList<>();
                        Feature feature = new Feature();
                        feature.setType("SAFE_SEARCH_DETECTION");
                        feature.setMaxResults(10);

//                        Feature textDetection = new Feature();
//                        textDetection.setType("TEXT_DETECTION");
//                        textDetection.setMaxResults(10);

                        list.add(feature);
//                        list.add(textDetection);
                        annotateImageRequest.setFeatures(list);

                        // Add the list of one thing to the request
                        add(annotateImageRequest);
                    }});

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    // Due to a bug: requests to Vision API containing large images fail when GZipped.
                    annotateRequest.setDisableGZipContent(true);
                    MLog.d(TAG, "created Cloud Vision request object, sending request");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    SafeSearchAnnotation safeSearchAnnotation = response.getResponses().get(0).getSafeSearchAnnotation();
                    return safeSearchAnnotation.getAdult() + ' ' + safeSearchAnnotation.getViolence();

                } catch (GoogleJsonResponseException e) {
                    MLog.d(TAG, "failed to make API request because " + e.getContent());
                } catch (IOException e) {
                    MLog.d(TAG, "failed to make API request because of other IOException " +
                            e.getMessage());
                } catch (Exception e) {
                    MLog.e(TAG, "cloud api failed ", e);
                }
                return null;
            }

            protected void onPostExecute(String result) {
                if (result == null) {
                    mListener.onImageInspectionCompleted(true, true, true); //error on side of caution
                    return;
                }
                boolean possibleAdult = result.startsWith("POSSIBLE ") || result.startsWith("VERY_LIKELY ");
                boolean possibleViolence = result.endsWith(" POSSIBLE") || result.endsWith(" VERY_LIKELY");
                mListener.onImageInspectionCompleted(false, possibleAdult, possibleViolence);
            }
        }.execute();
    }
}
