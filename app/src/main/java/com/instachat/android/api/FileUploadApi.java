package com.instachat.android.api;

import android.support.v4.util.Pair;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.instachat.android.Constants;
import com.instachat.android.util.HttpMessage;
import com.instachat.android.util.ThreadWrapper;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class FileUploadApi {

    private static final String TAG = FileUploadApi.class.getSimpleName();
    private static final int CHUNK_SIZE = 8024;

    /**
     * Posts a potentially large file to an intermediary server, which then
     * posts it to S3.
     * <p/>
     * BLOCKING! Must call within thread.
     *
     * @param file
     * @param uploadListener
     * @throws Exception
     */
    public String postFileToS3(final File file, final String filename, final String targetBucket, UploadListener uploadListener)
            throws Exception {

        final UploadListener listener = uploadListener != null ? uploadListener : new UploadListener() {
            @Override
            public void onErrorReducingPhotoSize() {

            }

            @Override
            public void onPhotoUploadStarted() {

            }

            @Override
            public void onPhotoUploadProgress(int max, int current) {

            }

            @Override
            public void onPhotoUploadSuccess(String photoId, String photoUrl) {

            }

            @Override
            public void onPhotoUploadError(Exception exception) {

            }
        };

        ThreadWrapper.executeInUiThread(new Runnable() {
            @Override
            public void run() {
                listener.onPhotoUploadStarted();
            }
        });

        try {
            final Pair<String, String> pair = NetworkApi.pair();
            //MLog.i(TAG, "amz s: "+ InstagramApp.sAmzSecKey + " a: " + InstagramApp.sAmzAccKey);
            final AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(pair.first, pair.second));
            final PutObjectRequest request = new PutObjectRequest(targetBucket, filename, file);
            final AtomicInteger counter = new AtomicInteger(0);
            request.setGeneralProgressListener(new ProgressListener() {

                @Override
                public void progressChanged(final ProgressEvent progressEvent) {
                    ThreadWrapper.executeInUiThread(new Runnable() {
                        @Override
                        public void run() {
                            counter.set(counter.intValue() + (int) progressEvent.getBytesTransferred());
                            listener.onPhotoUploadProgress((int) file.length() / 1024, counter.intValue() / 1024);
                        }
                    });
                }
            });
            s3Client.putObject(request);
            ThreadWrapper.executeInUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onPhotoUploadSuccess(filename, null);
                }
            });
            return filename;

        } catch (final Throwable t) {
            return postFileToS3Fallback(file, filename, targetBucket, listener);
        }

    }

    private String postFileToS3Fallback(final File file, final String filename, final String targetBucket, final UploadListener listener)
            throws Exception {

        RandomAccessFile fis = null;
        String key = null;

        try {

            final Map<String, String> request = new HashMap<String, String>(20);

            final byte[] bytes = new byte[CHUNK_SIZE];

            fis = new RandomAccessFile(file, "r");
            int total = 0;
            while (total < fis.length()) {

                request.clear();

                final boolean first = total == 0;

                fis.seek(total);
                final int read = fis.read(bytes, 0, CHUNK_SIZE);
                total = total + read;
                boolean isReadAllBytes = false;

                byte[] filepart = null;

                if (total == fis.length()) {
                    isReadAllBytes = true;
                }

                if (read < CHUNK_SIZE) {

                    filepart = copyByteArray(bytes, read);

                } else {

                    filepart = bytes;
                }

                if (isReadAllBytes) {
                    request.put("done", "1");
                }
                if (first) {
                    request.put("first", "1");
                }
                request.put("filename", filename);

                request.put("bucket", targetBucket);

                if (isReadAllBytes) {
                    fis.close();
                }

                final JSONObject response = new JSONObject(post(Constants.API_BASE_URL + "/sfile", request, "filepart", filepart));

                final int finalTotal = total;
                ThreadWrapper.executeInUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onPhotoUploadProgress((int) file.length() / 1024, finalTotal / 1024);
                    }
                });

                response.getString(NetworkApi.KEY_RESPONSE_STATUS).equals(NetworkApi.RESPONSE_OK);

                if (isReadAllBytes) {
                    key = response.getJSONObject("data").getString("key");
                    ThreadWrapper.executeInUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onPhotoUploadSuccess(filename, null);
                        }
                    });
                    break;
                }
            } // while

        } finally {
            if (fis != null) {
                fis.close();
            }
        }
        return key;
    }

    private String post(final String url, final Map<String, String> formParams, final String bytesName, final byte[] bytes) throws Exception {
        return new HttpMessage(url).postBytes(formParams, bytesName, bytes);
    }

    private byte[] copyByteArray(final byte[] bytes, final int size) {

        final byte[] newbytes = new byte[size];
        for (int i = 0; i < size; i++) {
            newbytes[i] = bytes[i];
        }
        return newbytes;
    }
}
