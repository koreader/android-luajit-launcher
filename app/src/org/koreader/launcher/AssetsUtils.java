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
   Heavily inspired by https://stackoverflow.com/a/27375602

   This file needs a refactor to work with kotlin */

class AssetsUtils {
    private static final int BASE_BUFFER_SIZE = 1024;
    private static final String TAG = "AssetsUtils";

    /* do not extract assets if the same version is already installed */
    static boolean isSameVersion(Context context, String zipFile) {
        final String new_version = getPackageRevision(zipFile);
        try {
            String output = context.getFilesDir().getAbsolutePath();
            FileReader fileReader = new FileReader(output + "/git-rev");
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String installed_version = bufferedReader.readLine();
            bufferedReader.close();
            if (new_version.equals(installed_version)) {
                Logger.INSTANCE.i("Skip installation for revision " + new_version);
                return true;
            } else {
                Logger.INSTANCE.i("Found new package revision " + new_version);
                return false;
            }
        } catch (Exception e) {
            Logger.INSTANCE.i("Found new package revision " + new_version);
            return false;
        }
    }

    /* get the first zip file inside the assets module */
    static String getZipFile(Context context) {
        AssetManager assetManager = context.getAssets();
        try {
            String[] assets = assetManager.list("module");
            if (assets != null) {
                for (String asset : assets) {
                    if (asset.endsWith(".zip")) {
                        return asset;
                    }
                }
            }
            return null;
        } catch (IOException e) {
            Logger.INSTANCE.e(TAG, "error listing assets:\n" + e.toString());
            return null;
        }
    }

    /* copy raw assets under assets/module */
    static boolean copyUncompressedAssets(Context context) {
        AssetManager assetManager = context.getAssets();
        String assets_dir = context.getFilesDir().getAbsolutePath();
        // llapp_main.lua is the entry point for frontend code.
        boolean entry_point = false;
        try {
            String[] assets = assetManager.list("module");
            if (assets != null) {
                for (String asset : assets) {
                    File file = new File(assets_dir, asset);
                    InputStream input = assetManager.open("module/" + asset);
                    OutputStream output = new FileOutputStream(file);
                    Logger.INSTANCE.d(TAG, "copying " + asset +
                        " to " + file.getAbsolutePath());
                    copyFile(input, output);
                    input.close();
                    output.flush();
                    output.close();
                    if ("llapp_main.lua".equals(asset)) {
                        entry_point = true;
                    }
                }
            }
        } catch (IOException e) {
            Logger.INSTANCE.e(TAG, "error with raw assets:\n" + e.toString());
            entry_point = false;
        }
        return entry_point;
    }

    /* unzip from stream */
    static void unzip(InputStream stream, String output) {
        byte[] buffer = new byte[BASE_BUFFER_SIZE * 512];
        try {
            ZipInputStream inputStream = new ZipInputStream(stream);
            ZipEntry zipEntry = null;

            while ((zipEntry = inputStream.getNextEntry()) != null) {
                Logger.INSTANCE.d(TAG, "unzipping " + zipEntry.getName());

                if (zipEntry.isDirectory()) {
                    dirChecker(output, zipEntry.getName());
                } else {
                    File f = new File(output, zipEntry.getName());
                    if (!f.exists()) {
                        boolean success = f.createNewFile();
                        if (!success) {
                            Logger.INSTANCE.w(TAG,
                                "Failed to create file " + f.getName());
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
            Logger.INSTANCE.e(TAG, "error unzipping assets:\n" + e.toString());
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
                Logger.INSTANCE.w(TAG, "failed to create folder " + f.getName());
            }
        }
    }

    /* get package revision from zipFile with scheme: name-revision.zip */
    private static String getPackageRevision(String zipFile) {
        String zipName = zipFile.replace(".zip","");
        String[] parts = zipName.split("-");
        return zipName.replace(parts[0] + "-", "");
    }
}
