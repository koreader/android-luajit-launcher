package org.koreader.launcher.helper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.koreader.launcher.Logger;


public class FileHelper {
    private final Context context;
    private final String tag;

    public FileHelper(Context context) {
        this.context = context.getApplicationContext();
        this.tag = this.getClass().getSimpleName();
        Logger.d(tag, "Starting");
    }

    /**
     * gets the absolute path of a document from an uri
     * @param uri with file/content schemes. Others return null.
     * @return a string containing the full path of the document.
     */
    public String getAbsolutePath(Uri uri) {
        File file = getFileFromUri(uri);
        if (file != null) {
            String path = file.getAbsolutePath();
            Logger.v(tag, String.format(Locale.US, "open file %s", path));
            return path;
        } else {
            return null;
        }
    }

    /**
     * gets a file from an uri.
     * @param uri with scheme file or content. Invalid schemes will return null
     * @return a file containing the document we want to open.
     */
    private File getFileFromUri(Uri uri) {
        File file = null;
        if (uri == null) return null;
        final String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            Logger.d(tag, "obtaining path from file uri scheme");
            file = new File(uri.getPath());
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            Logger.d(tag, "obtaining path from content uri scheme");
            file = getFileFromContentUri(uri);
        }
        return file;
    }

    /**
     * gets a file from content:// uris
     * @param uri with scheme content
     * @return a file
     */
    private File getFileFromContentUri(Uri uri) {
        // there's no way to retrieve a file, so open a inputStream using the content resolver
        // and store the content inside the cache folder.
        File file = null;
        String[] nameColumn = {MediaStore.MediaColumns.DISPLAY_NAME};
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, nameColumn, null, null, null);
        if (cursor != null) {
            cursor.moveToFirst();
            String name = cursor.getString(cursor.getColumnIndex(nameColumn[0]));
            cursor.close();
            Logger.d(tag, String.format(Locale.US, "Importing content: %s", name));
            String path = getPathFromCache(uri, name);
            file = new File(path);
        }
        return file;
    }

    /**
     * gets the absolute path of a cache file
     * @param uri with scheme content
     * @param name of the file
     * @return a string containing the full path of the document.
     */
    private String getPathFromCache(Uri uri, String name) {
        Logger.d(tag, "Getting the absolute path to the cached document");
        String path = null;
        InputStream stream = null;
        if (uri.getAuthority() != null) {
            Logger.d(tag, String.format(Locale.US, "Using authority: %s", uri.getAuthority()));
            try {
                stream = context.getContentResolver().openInputStream(uri);
                File file = getCacheFile(stream, name);
                path = file.getPath();
            } catch (IOException e) {
                Logger.e(tag, "I/O error: " + e.toString());
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (IOException e) {
                    Logger.e(tag, "I/O error: " + e.toString());
                }
            }
        }
        return path;
    }

    /**
     * gets a cache file from an inputstream buffer
     * @param stream from contentResolver.openInputStream(uri)
     * @param name of the file
     * @return cache file
     */
    private File getCacheFile(InputStream stream, String name) throws IOException {
        Logger.d(tag, "Getting a copy of content from inputstream");
        File file = null;
	    if (stream != null) {
	        int read;
	        byte[] buffer = new byte[8 * 1024];
	        file = new File(context.getCacheDir(), name);
	        Logger.d(tag, String.format(Locale.US,
                "storing new content on %s", file.getAbsolutePath()));

	        OutputStream output = new FileOutputStream(file);
	        while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            try {
                output.flush();
                output.close();
            } catch (IOException e) {
                Logger.e(tag, "I/O error: " + e.toString());
            }
        }
        return file;
    }
}
