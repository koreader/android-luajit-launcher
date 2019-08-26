package org.koreader.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import android.content.Context;
import android.content.res.AssetManager;


/* Utils to extract compressed assets from the asset loader.
   Heavily inspired by https://stackoverflow.com/a/27375602 */

class AssetsUtils {
    private static final int BASE_BUFFER_SIZE = 1024;
    private static final String TAG = "AssetsUtils";

    /* extract APK assets on the internal app storage */
    static int extract(Context context) {
        String output = context.getFilesDir().getAbsolutePath();
        try {
            // is there any zip file inside the asset module?
            String zipFile = getZipFile(context);
            if (zipFile != null) {
                // zipfile found! it will be extracted or not based on its version name
                if (!isSameVersion(context, zipFile)) {
                    InputStream stream = context.getAssets().open(zipFile);
                    unzip(stream, output);
                }
                // extracted without errors.
                return 1;
            } else {
                // check if the app has other, non-zipped, raw assets
                Logger.i(TAG, "zip file not found, trying raw assets...");
                return copyUncompressedAssets(context) ? 1 : 0;
            }
        } catch (IOException e) {
            Logger.e(TAG, "error extracting assets:\n" + e.toString());
            return 0;
        }
    }

    /* do not extract assets if the same version is already installed */
    private static boolean isSameVersion(Context context, String zipFile) {
        try {
            String output = context.getFilesDir().getAbsolutePath();
            FileReader fileReader = new FileReader(output + "/git-rev");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String installed_version = bufferedReader.readLine();
            bufferedReader.close();
            return zipFile.contains(installed_version) ? true : false;
        } catch (Exception e) {
            Logger.d(TAG, "git revision not found, we should update");
            return false;
        }
    }

    /* get the first zip file inside the assets module */
    private static String getZipFile(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] assets = assetManager.list("module");
            for (String asset: assets) {
                if (asset.endsWith(".zip")) {
                    return "module/" + asset;
                }
            }
            return null;
        } catch (IOException e) {
            Logger.e(TAG, "error listing assets:\n" + e.toString());
            return null;
        }
    }

    /* little programs don't need zipped assets.
       ie: create a file under assets/module/ called llapp_main.lua,
       and put there your start code. You can place other files under
       assets/module and they will be copied too. */

    private static boolean copyUncompressedAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        String assets_dir = context.getFilesDir().getAbsolutePath();
        // llapp_main.lua is the entry point for frontend code.
        boolean entry_point = false;
        try {
            String[] assets = assetManager.list("module");
            for (String asset: assets) {
                File file = new File(assets_dir, asset);
                InputStream input = assetManager.open("module/" + asset);
                OutputStream output = new FileOutputStream(file);
                Logger.d(TAG, "copying " + asset + " to " + file.getAbsolutePath());
                copyFile(input, output);
                input.close();
                output.flush();
                output.close();
                if ("llapp_main.lua".equals(asset)) {
                    entry_point = true;
                }
            }
        } catch (IOException e) {
            Logger.e(TAG, "error with raw assets:\n" + e.toString());
            entry_point = false;
        }
        return entry_point;
    }

    /* unzip from stream */
    private static void unzip(InputStream stream, String output) {
        byte[] buffer = new byte[BASE_BUFFER_SIZE * 512];
        try {
            ZipInputStream inputStream = new ZipInputStream(stream);
            ZipEntry zipEntry = null;

            while ((zipEntry = inputStream.getNextEntry()) != null) {
                Logger.d(TAG, "unzipping " + zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    dirChecker(output, zipEntry.getName());
                } else {
                    File f = new File(output, zipEntry.getName());
                    if (!f.exists()) {
                        boolean success = f.createNewFile();
                        if (!success) {
                            Logger.w(TAG, "Failed to create file " + f.getName());
                            continue;
                        }
                        FileOutputStream outputStream = new FileOutputStream(f);
                        int count;
                        while ((count = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                        inputStream.closeEntry();
                        outputStream.close();
                    }
                }
            }
            inputStream.close();
        } catch (Exception e) {
            Logger.e(TAG, "error unzipping assets:\n" + e.toString());
        }
    }

    /* copy files from stream */
    private static void copyFile(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[BASE_BUFFER_SIZE];
        int read;
        while((read = input.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
    }

    /* create new folders on demand */
    private static void dirChecker(String path, String file) {
        File f = new File(path, file);
        if (!f.isDirectory()) {
            boolean success = f.mkdirs();
            if (!success) {
                Logger.w(TAG, "failed to create folder " + f.getName());
            }
        }
    }
}
