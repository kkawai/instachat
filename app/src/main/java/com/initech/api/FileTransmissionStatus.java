package com.initech.api;

import java.io.Serializable;


/**
 * This class contains the state of a data transfer.
 * @author kevin
 *
 */
public final class FileTransmissionStatus implements Serializable {
	
	private static final String TAG = FileTransmissionStatus.class.getSimpleName();
	
	private static final long serialVersionUID = -5559502064631529330L;
	
	private FileTransmissionListener _fileTransmissionListener;
	private boolean _cancelled;
	private boolean _hadError;
	private int _progress;
	private int _totalSize;		
	
	public static interface FileTransmissionListener {
		public void updateCurrentProgress(final int currentSize);
	}	
	
	public String toString() {
		return " currentSize=" + this._progress + " totalSize=" + this._totalSize 
		+ " isCancelled=" +this._cancelled;
	}
	
	public void cancel() {
		_cancelled = true;
	}
	
	public boolean isCancelled() {
		return _cancelled;
	}
	
	public void initialize() {
		_progress = 0;
		_cancelled = false;
		_hadError = false;
		_totalSize = 0;
	}
	
	public int getCurrentProgress() {
		return _progress;
	}

	public void setCurrentProgress(final int progress) {
		this._progress = progress;
	}
	
	public void increment(final int chunk) {
		_progress += chunk;
	}
	
	public void updateProgress() {
		if (_fileTransmissionListener != null)
			_fileTransmissionListener.updateCurrentProgress(_progress);
	}

	public int getTotalSize() {
		return _totalSize;
	}

	public void setTotalSize(final int totalSize) {
		this._totalSize = totalSize;
	}	
	
	public boolean isComplete() {
		return _progress == _totalSize;
	}
	
	public void setProgressListener(final FileTransmissionListener fileTransmissionListener) {
		_fileTransmissionListener = fileTransmissionListener;
	}
	
	public void setHadError(final boolean error) {
		_hadError = error;
	}
	
	public boolean hadError() {
		return _hadError;
	}
	
	public boolean isInProgress() {
		if (!isCancelled() && !isComplete() && !hadError() && _totalSize > 0) {
			return true;
		}
		return false;
	}
}
