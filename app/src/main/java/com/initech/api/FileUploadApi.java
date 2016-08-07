package com.initech.api;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.initech.Constants;
import com.initech.util.HttpMessage;

import org.json.JSONObject;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

public final class FileUploadApi {

    private static final String TAG = FileUploadApi.class.getSimpleName();
    private static final int CHUNK_SIZE = 32000;

    /**
     * Posts a potentially large file to an intermediary server, which then
     * posts it to S3.
     * <p>
     * BLOCKING! Must call within thread.
     *
     * @param file
     * @param transmissionStatus
     * @throws Exception
     */
    public String postFileToS3(final File file, final String filename, final String targetBucket, final FileTransmissionStatus transmissionStatus)
            throws Exception {

        try {
            final JSONObject remoteSettings = NetworkApi.getRemoteSettings();
            final String a = remoteSettings.getString("a");
            final String s = remoteSettings.getString("s");
            //MLog.i(TAG, "amz s: "+ InstagramApp.sAmzSecKey + " a: " + InstagramApp.sAmzAccKey);
            final AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(a, s));
            final PutObjectRequest request = new PutObjectRequest(targetBucket, filename, file);

            if (transmissionStatus != null) {

                transmissionStatus.initialize();
                transmissionStatus.setTotalSize((int) file.length());

                request.setGeneralProgressListener(new ProgressListener() {

                    @Override
                    public void progressChanged(final ProgressEvent progressEvent) {

                        transmissionStatus.increment((int) progressEvent.getBytesTransferred());
                        transmissionStatus.updateProgress();
                    }
                });
            }
            s3Client.putObject(request);
            return filename;

        } catch (final Throwable t) {
            return postFileToS3Old(file, filename, targetBucket, transmissionStatus);
        }

    }

    private String postFileToS3Old(final File file, final String filename, final String targetBucket, FileTransmissionStatus fileTransmissionStatus)
            throws Exception {

        RandomAccessFile fis = null;
        String key = null;

        try {

            final Map<String, String> request = new HashMap<String, String>(20);

            if (fileTransmissionStatus == null) {
                fileTransmissionStatus = new FileTransmissionStatus();
            }
            fileTransmissionStatus.initialize();
            fileTransmissionStatus.setTotalSize((int) file.length());

            final byte[] bytes = new byte[CHUNK_SIZE];

            fis = new RandomAccessFile(file, "r");
            int total = 0;
            while (total < fis.length()) {

                request.clear();

                final boolean first = fileTransmissionStatus.getCurrentProgress() == 0;

                fis.seek(fileTransmissionStatus.getCurrentProgress());
                final int read = fis.read(bytes, 0, CHUNK_SIZE);
                total = total + read;
                boolean done = false;

                byte[] filepart = null;

                if (total == fis.length()) {
                    done = true;
                }

                if (read < CHUNK_SIZE) {

                    filepart = copyByteArray(bytes, read);

                } else {

                    filepart = bytes;
                }

                fileTransmissionStatus.increment(read);

                if (done) {
                    request.put("done", "1");
                }
                if (first) {
                    request.put("first", "1");
                }
                request.put("filename", filename);

                request.put("bucket", targetBucket);

                if (done) {
                    fis.close();
                }

                final JSONObject response = new JSONObject(post(Constants.API_BASE_URL + "/sfile", request, "filepart", filepart));

                response.getString("status").equals("OK");

                fileTransmissionStatus.updateProgress();

                if (done) {
                    key = response.getJSONObject("data").getString("key");
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
