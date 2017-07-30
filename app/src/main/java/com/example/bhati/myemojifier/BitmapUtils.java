package com.example.bhati.myemojifier;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Bhati on 7/29/2017.
 */

public class BitmapUtils {
    public static final String TAG=BitmapUtils.class.getSimpleName();
    public static File createTempFile(Context context) throws IOException {
        String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName="JPEG_"+timeStamp;
        File storageDir=context.getExternalCacheDir();
        return File.createTempFile(fileName,".jpeg",storageDir);
    }

    public static Bitmap rescaleImage(Context context, String tempPhotoPath) {

        DisplayMetrics metrics=new DisplayMetrics();
        WindowManager windowManager= (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getMetrics(metrics);

        int displayHeight=metrics.heightPixels;
        int displayWidth=metrics.widthPixels;

        BitmapFactory.Options options=new BitmapFactory.Options();
        options.inJustDecodeBounds=true;
        BitmapFactory.decodeFile(tempPhotoPath,options);

        int imageHeight=options.outHeight;
        int imageWidth=options.outWidth;

        int scaleFactor=Math.min(imageHeight/displayHeight,imageWidth/displayWidth);
        options.inJustDecodeBounds=false;
        options.inSampleSize=scaleFactor;
        return BitmapFactory.decodeFile(tempPhotoPath);
    }

    public static void deleteTempFile(String tempPhotoPath) {
        File file=new File(tempPhotoPath);
        if(file.exists()){
            file.delete();
        }
    }

    public static String saveFile(Context context, Bitmap resultantBitmap) {
        File imageFile;
        String timeStamp=new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName="JPEG_"+timeStamp+".jpg";
        File storageDirectory=
                new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+"/MyEmojifier");
        boolean success=true;
        String savedImagePath = null;
        if(!storageDirectory.exists()){
           success= storageDirectory.mkdir();
        }
        if(success){
            imageFile=new File(storageDirectory,fileName);

            FileOutputStream fileOutputStream;
            try {
                fileOutputStream=new FileOutputStream(imageFile);
                resultantBitmap.compress(Bitmap.CompressFormat.JPEG,100,fileOutputStream);
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                Log.v(TAG,"File not found exception");
            } catch (IOException e) {
                e.printStackTrace();
                Log.v(TAG,"IOException");
            }
            savedImagePath=imageFile.getAbsolutePath();
        }

        addPictureToGallery(context,savedImagePath);

        Toast.makeText(context, "Image Saved Successfully", Toast.LENGTH_SHORT).show();
        return savedImagePath;

    }

    private static void addPictureToGallery(Context context, String savedImagePath) {
        Intent mediaScanner=new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file=new File(savedImagePath);
       mediaScanner.setData(Uri.fromFile(file));
        context.sendBroadcast(mediaScanner);
    }

    public static void shareFile(Context context, String privateImagePath) {
        Intent shareIntent=new Intent(Intent.ACTION_SEND);
        File file=new File(privateImagePath);
        shareIntent.setType("image/*");
        Uri uri=FileProvider.getUriForFile(context,MainActivity.AUTHORITY,file);
        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
        context.startActivity(shareIntent);
    }
}
