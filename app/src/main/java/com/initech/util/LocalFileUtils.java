package com.initech.util;

import android.content.Context;

import com.initech.Constants;
import com.initech.MyApp;
import com.initech.view.IhProgressiveProgressDialog;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public final class LocalFileUtils {

	private static final String TAG = LocalFileUtils.class.getSimpleName();
	static {
		MLog.enable(TAG);
	}

	private LocalFileUtils() {
	}

	/**
	 * Warning: this has problems on some phones
	 * 
	 * @param f
	 * @throws Exception
	 */
	public static void touch(final File f) throws Exception {

		if (f.exists()) {

			try {
				if (!f.setLastModified(System.currentTimeMillis())) {
					throw new IOException("Could not touch file");
				}
			} catch (final Exception e) {
				/*
				 * the pre and post suffixes must be >= 3 chars or it will throw
				 * exception
				 */
				final File tmp = File.createTempFile("touch", "tmp", f.getParentFile());
				copyFile(f, tmp);
				f.delete();
				tmp.renameTo(f);
			}
		} else {
			final FileOutputStream os = new FileOutputStream(f);
			os.write(-1);
			os.close();
		}
	}

	public static void copyFile(final File src, final File dst) throws IOException {
		final InputStream in = new FileInputStream(src);
		final OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		final byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0) {
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	/**
	 * attempts to delete entire directory and it's contents, however it won't
	 * be able to delete the directory if there are sub dirs
	 * 
	 * @param dir
	 */
	public static void deleteDirectoryAndContents(final File dir, final boolean keepDirectory) {
		try {
			final File[] files = dir.listFiles();
			if (files != null && files.length > 0) {
				for (final File file : files) {
					if (file.isFile()) {
						try {
							file.delete();
						} catch (final Exception e) {
							MLog.e(TAG, "", e);
						}
					}
				}
			}
			if (!keepDirectory) {
				dir.delete(); // finally, delete the given directory itself
			}
		} catch (final Exception e) {
			MLog.e(TAG, "deleteDirectoryAndContents", e);
		}
	}

	/**
	 * Designed for high performance. Reads the first string from a file. E.g.
	 * use when reading a single token from the file, such as an ID or an
	 * INTEGER or something.
	 * 
	 * Reads the first 128 characters only!!!!!!!!!!
	 * 
	 * @param path
	 * @return
	 * @throws IOException
	 */
	public static String readFirstLineFromFile(final String path) throws IOException {
		final FileReader fr = new FileReader(path);
		final char buf[] = new char[128];
		final int read = fr.read(buf);
		fr.close();
		return new String(buf, 0, read);
	}

	public static byte[] getBytesFromFile(final File inFile) throws IOException {

		final InputStream is = new FileInputStream(inFile);

		// Get the size of the file
		final long length = inFile.length();

		// Create the byte array to hold the data
		final byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file " + inFile.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	private static File downloadFile(final Context context, final String urlString, final File targetFile, final ProgressListener progressListener, final IhProgressiveProgressDialog progressDialog) {

		/*
		 * Sanity check; make sure the file is not zero length
		 */
		if (targetFile.exists() && targetFile.length() > 8) {
			return targetFile;
		}

		HttpURLConnection urlConnection = null;
		BufferedOutputStream out = null;

		try {
			final URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			int count=0, contentLength=0;
			try {
				//content length may not be available @ server?!
				if (progressListener != null) {
					contentLength = urlConnection.getContentLength();
					progressDialog.setMax(contentLength); 
				}
			}catch (final Exception e){}
			final InputStream in = new BufferedInputStream(urlConnection.getInputStream(), 8092);
			out = new BufferedOutputStream(new FileOutputStream(targetFile), 8092);

			int b;
			while ((b = in.read()) != -1) {
				out.write(b);
				count++;
				if (progressListener != null && progressDialog != null) {
					
					if (count % 8092 == 0) {
						progressListener.onProgress(count);
					} else if (count == contentLength) {
						progressListener.onProgress(count);
					}
				}				
			}
			return targetFile;

		} catch (final Exception e) {
			MLog.e(TAG, "Error in download file - ", e);
			targetFile.delete();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
			if (out != null) {
				try {
					out.close();
				} catch (final IOException e) {
					MLog.e(TAG, "Error in downloadBitmap - " + e);
				}
			}
		}
		return null;
	}
	
	public static File getPermanentFile(final String key, final ProgressListener progressListener, final IhProgressiveProgressDialog progressDialog) {
		
		final boolean isPhoto = key.startsWith("t_") || key.startsWith("p_");
		File permPath = null;
		if (isPhoto) {
			permPath = new File(CacheUtils.getPermanentPhotoDir(MyApp.getInstance()).getPath());
		} else {
			permPath = new File(CacheUtils.getPermanentExternalDir(MyApp.getInstance()).getPath() + "/v");
		}

		permPath.mkdirs();
		final File targetPath = new File(permPath.getPath() + "/" + key);
		return LocalFileUtils.downloadFile(MyApp.getInstance(), "http://ih.usr.s3.amazonaws.com/" + key, targetPath, progressListener, progressDialog);
	}	

	/**
	 * Returns the local file on disk by key. If does not exist on local disk,
	 * download it from S3. See
	 * <code>LocalFileUtils.setPermanentFile(final File temp, final String key)</code>
	 * 
	 * @param key
	 * @return - File is successfully found by key, or null otherwise
	 * 
	 */
	public static File getPermanentFile(final String key) {
		
		final boolean isPhoto = key.startsWith("t_") || key.startsWith("p_");
		File permPath = null;
		if (isPhoto) {
			permPath = new File(CacheUtils.getPermanentPhotoDir(MyApp.getInstance()).getPath());
		} else {
			permPath = new File(CacheUtils.getPermanentExternalDir(MyApp.getInstance()).getPath() + "/v");
		}
		
		permPath.mkdirs();
		final File targetPath = new File(permPath.getPath() + "/" + key);
		return LocalFileUtils.downloadFile(MyApp.getInstance(), "http://ih.usr.s3.amazonaws.com/" + key, targetPath, null, null);
	}

	/**
	 * Saves the given 'temp' file to local disk by key. See
	 * <code>LocalFileUtils.getPermanentFile(String key)</code>
	 * 
	 * @param temp
	 * @param key
	 */
	public static void setPermanentFile(final File temp, final String key) throws Exception {
		
		final boolean isPhoto = key.startsWith("t_") || key.startsWith("p_");
		File permPath = null;
		if (isPhoto) {
			permPath = new File(CacheUtils.getPermanentPhotoDir(MyApp.getInstance()).getPath());
		} else {
			permPath = new File(CacheUtils.getPermanentExternalDir(MyApp.getInstance()).getPath() + "/v");
		}
		permPath.mkdirs();
		final File targetPath = new File(permPath.getPath() + "/" + key);
		copyFile(temp, targetPath);
	}
	
	/**
	 * Deletes from local storage.  If you want, you can choose to also delete from Amazon S3.
	 * @param key
	 * @param deleteFromS3
	 */
	public static void deletePermanentFile(final String key, final boolean deleteFromS3) {
		
		final boolean isPhoto = key.startsWith("t_") || key.startsWith("p_");
		
		File permPath = null;
		if (isPhoto) {
			permPath = new File(CacheUtils.getPermanentPhotoDir(MyApp.getInstance()).getPath());
		} else {
			permPath = new File(CacheUtils.getPermanentExternalDir(MyApp.getInstance()).getPath() + "/v");
		}
		permPath.mkdirs();
		final File targetPath = new File(permPath.getPath() + "/" + key);
		targetPath.delete();
		try {
			if (deleteFromS3) {
				new HttpMessage(Constants.API_BASE_URL + "/ds3").post("k", key);
			}
		} catch(final Exception e) {
			MLog.e(TAG, "deletePermanentFile failed..", e);
		}
	}
	
	public static void migrateOldPhotosToNewPhotos() {
		final File oldPhotoDir = new File(CacheUtils.getPermanentExternalDir(MyApp.getInstance()).getPath() + "/v");
		final File newPhotoDir = new File(CacheUtils.getPermanentPhotoDir(MyApp.getInstance()).getPath());
		
		final String list[] = oldPhotoDir.list(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				return filename.startsWith("p_") || filename.startsWith("t_");
			}
			
		});
		
		if (list == null || list.length == 0) {
			return;
		}
		
		for (final String name : list) {
			final File src = new File(oldPhotoDir.getPath() + "/" + name);
			final File dest = new File(newPhotoDir.getPath() + "/" + name);
			try {
				copyFile(src, dest);
				src.delete();
			} catch (final IOException e) {
			}
		}
	}
	
}
