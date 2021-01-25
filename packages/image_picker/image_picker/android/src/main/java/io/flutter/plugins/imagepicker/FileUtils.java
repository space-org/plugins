package io.flutter.plugins.imagepicker;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

public class FileUtils {

  private static final String TAG = "FilePickerUtils";

  String getPathFromUri(final Context context, final Uri uri) {
    String path = getPathFromLocalUri(context, uri);
    if (path == null) {
      path = getPathFromRemoteUri(context, uri);
    }
    return path;
  }

  public static String getPathFromLocalUri(Context context, final Uri uri) {
    if ("content".equalsIgnoreCase(uri.getScheme())) {
      if (isGooglePhotosUri(uri)) {
        return uri.getLastPathSegment();
      }
      return getDataColumn(context, uri, null, null);
    } else if ("file".equalsIgnoreCase(uri.getScheme())) {
      return uri.getPath();
    }
    return null;
  }

  private static String getDataColumn(Context context, Uri uri, String selection,
                                      String[] selectionArgs) {
    Cursor cursor = null;
    final String column = "_data";
    final String[] projection = {
            column
    };
    try {
      cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
              null);
      if (cursor != null && cursor.moveToFirst()) {
        final int index = cursor.getColumnIndexOrThrow(column);
        return cursor.getString(index);
      }
    } catch(Exception ex){
    } finally {
      if (cursor != null)
        cursor.close();
    }
    return null;
  }

  public static String getFileName(Uri uri, Context context) {
    String result = null;

    //if uri is content
    if (uri.getScheme() != null && uri.getScheme().equals("content")) {
      Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
      try {
        if (cursor != null && cursor.moveToFirst()) {
          //local filesystem
          int index = cursor.getColumnIndex("_data");
          if (index == -1)
            //google drive
            index = cursor.getColumnIndex("_display_name");
          result = cursor.getString(index);
          if (result != null)
            uri = Uri.parse(result);
          else
            return null;
        }
      } finally {
        cursor.close();
      }
    }

    if(uri.getPath() != null) {
      result = uri.getPath();
      int cut = result.lastIndexOf('/');
      if (cut != -1)
        result = result.substring(cut + 1);
    }

    return result;
  }

  public static String getPathFromRemoteUri(Context context, Uri uri) {
    Log.i(TAG, "Caching file from remote/external URI");
    FileOutputStream fos = null;
    final String fileName = FileUtils.getFileName(uri, context);
    String externalFile = context.getCacheDir().getAbsolutePath() + "/" + (fileName != null ? fileName : new Random().nextInt(100000));

    try {
      fos = new FileOutputStream(externalFile);
      try {
        BufferedOutputStream out = new BufferedOutputStream(fos);
        InputStream in = context.getContentResolver().openInputStream(uri);

        byte[] buffer = new byte[8192];
        int len = 0;

        while ((len = in.read(buffer)) >= 0) {
          out.write(buffer, 0, len);
        }

        out.flush();
      } finally {
        fos.getFD().sync();
      }
    } catch (Exception e) {
      try {
        fos.close();
      } catch(IOException | NullPointerException ex) {
        Log.e(TAG, "Failed to close file streams: " + e.getMessage(),null);
        return null;
      }
      Log.e(TAG, "Failed to retrieve path: " + e.getMessage(),null);
      return null;
    }

    Log.i(TAG, "File loaded and cached at:" + externalFile);
    return externalFile;
  }

  private static boolean isGooglePhotosUri(Uri uri) {
    return "com.google.android.apps.photos.content".equals(uri.getAuthority());
  }
}
