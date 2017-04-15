import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.io.*;
import java.net.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.event.*;

class Download extends Observable implements Runnable {
    
    private static final int MAX_BUFFER_SIZE = 1024;
    public static final String STATUSES[] = { "Downloading", "Paused", "Complete", "Cancelled",
					      "Error" };

    public static final int DOWNLOADING = 0;
    public static final int PAUSED = 1;
    public static final int COMPLETE = 2;
    public static final int CANCELLED = 3;
    public static final int ERROR = 4;

    private URL url; // download URL
    private String saveDir; // dir to save
    private int size; // size of download in bytes
    private int downloaded; // number of bytes downloaded
    private int status; // current status of download

    // Proxy information
    public static final boolean proxyRequired = false; // Change for your settings
    public static final String proxyIP = "127.0.0.1";
    public static final String proxyPort = "8080";
    public static final String proxyUsername = "proxyUser";
    public static final String proxyPassword = "proxyPassword";

    // Constructor for Download.
    public Download(URL url, String saveDir) {
	this.url = url;
	this.saveDir = saveDir;
	size = -1;
	downloaded = 0;
	status = DOWNLOADING;

	// Begin the download.
	download();
    }

    // Get this download's URL.
    public String getUrl() {
	return url.toString();
    }

    // Get this download's size.
    public int getSize() {
	return size;
    }

    // Get this download's progress.
    public float getProgress() {
	return ((float) downloaded / size) * 100;
    }

    public int getStatus() {
	return status;
    }

    public void pause() {
	status = PAUSED;
	stateChanged();
    }

    public void resume() {
	status = DOWNLOADING;
	stateChanged();
	download();
    }

    public void cancel() {
	status = CANCELLED;
	stateChanged();
    }

    private void error() {
	status = ERROR;
	stateChanged();
    }

    private void download() {
	Thread thread = new Thread(this);
	thread.start();
    }

    // Get file name portion of URL.
    public String getFileName(URL url) {
	String fileName = url.getFile();
	return fileName.substring(fileName.lastIndexOf('/') + 1);
    }

    // Download file.
    public void run() {
	RandomAccessFile file = null;
	InputStream stream = null;
	FileOutputStream out = null;

	try {

	    if (proxyRequired){
                // This can be put in a menu, updated via interface
		System.out.println("Setting proxy");
		Properties systemSettings = System.getProperties();
		systemSettings.put("http.proxyHost", proxyIP);
		systemSettings.put("http.proxyPort", proxyPort);
		System.setProperties(systemSettings);
	    }

            // Open connection to URL.
	    HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // Specify what portion of file to download.
	    connection.setRequestProperty("Range", "bytes=" + downloaded + "-");

	    if (proxyRequired){
		String encoded = new String(Base64.getEncoder().encodeToString(new String( proxyUsername + ":" + proxyPassword).getBytes()));
		connection.setRequestProperty("Proxy-Authorization", "Basic " + encoded);
	    }

	    System.out.println("Going to make connection");
            // Connect to server.
	    connection.connect();
	    System.out.println("Connected!");

	    int responseCode = connection.getResponseCode();
	    System.out.println("Response code from server=" + responseCode);

            // Make sure response code is in the 200 range.
            // 200 - no partial download
            // 206 - supports resume
            //if (responseCode / 100 != 2) {
	    if (responseCode == 200 || responseCode == 206) {
		error();
	    }

            // Check for valid content length.
	    System.out.println("Content length=" + connection.getContentLength());
	    int contentLength = connection.getContentLength();
	    if (contentLength < 1) {
		error();
	    }

	    /*
	     * Set the size for this download if it hasn't been already set.
	     */
	    if (size == -1) {
		size = contentLength;
		stateChanged();
	    }

            // Open file and seek to the end of it.
	    file = new RandomAccessFile(getFileName(url), "rw");
	    file.seek(downloaded);

	    System.out.println("Get InputStream");
	    stream = connection.getInputStream();
	    status = DOWNLOADING;
	    out = new FileOutputStream(saveDir + File.separator
				       + this.getFileName(url));
	    while (status == DOWNLOADING) {
		/*
		 * Size buffer according to how much of the file is left to download.
		 */
		byte buffer[];
		if (size - downloaded > MAX_BUFFER_SIZE) {
		    buffer = new byte[MAX_BUFFER_SIZE];
		} else {
		    buffer = new byte[size - downloaded];
		}

                // Read from server into buffer.
		int read = stream.read(buffer);
		if (read == -1)
		    break;

                // Write buffer to file.
                //file.write(buffer, 0, read);
		out.write(buffer, 0, read);
		downloaded += read;
		stateChanged();
	    }

            /*
	     * Change status to complete if this point was reached because downloading
	     * has finished.
	     */
	    if (status == DOWNLOADING) {
		status = COMPLETE;

		stateChanged();
	    }
	} catch (Exception e) {
	    System.out.println("Error=" + e);
	    e.printStackTrace();
	    error();
	} finally {

            // Close file.
	    if (file != null) {
		try {
                    // Complete the file
		    out.close();
		    file.close();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }

            // Close connection to server.
	    if (stream != null) {
		try {
		    stream.close();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    private void stateChanged() {
	setChanged();
	notifyObservers();
    }
}
