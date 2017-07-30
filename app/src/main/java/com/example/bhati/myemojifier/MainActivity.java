package com.example.bhati.myemojifier;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG=MainActivity.class.getSimpleName();
     static final String AUTHORITY="com.example.bhati.myemojifier";

    private static final int WRITE_STORAGE_PERMISSION = 54;
    private static final int CAPTURE_PICTURE_REQUEST_ID = 65;

    @BindView(R.id.emojifyMeButton) Button emojifyButton;
    @BindView(R.id.imageView) ImageView imageView;
    @BindView(R.id.clearButton) FloatingActionButton clearButton;
    @BindView(R.id.saveButton) FloatingActionButton saveButton;
    @BindView(R.id.shareButton) FloatingActionButton shareButton;

    private String tempPhotoPath;
    private String privateImagePath;
    private Bitmap resultantBitmap;
    private boolean isSaved;
    private ExifInterface exifInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.emojifyMeButton)
    public void emojifyMe(View view){
        isSaved=false;
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED){
            launchCamera();
        }else{
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},WRITE_STORAGE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case WRITE_STORAGE_PERMISSION:
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    launchCamera();
                }else{
                    Log.v(TAG,"Permission denied");
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
        }
    }

    private void launchCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager())!=null) {
            File imageFile = null;
            try {
                imageFile = BitmapUtils.createTempFile(this);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (imageFile != null) {
                tempPhotoPath = imageFile.getAbsolutePath();
                Uri photoUri = FileProvider.getUriForFile(this, AUTHORITY, imageFile);

                List<ResolveInfo> resolveInfos = getPackageManager().queryIntentActivities(takePictureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resolveInfos) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    grantUriPermission(packageName, photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                }

                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                startActivityForResult(takePictureIntent, CAPTURE_PICTURE_REQUEST_ID);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==CAPTURE_PICTURE_REQUEST_ID && resultCode==RESULT_OK){
            try {
                exifInterface=new ExifInterface(tempPhotoPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
            processAndEmojify();
        }else{
            BitmapUtils.deleteTempFile(tempPhotoPath);
            Log.v(TAG,"Image not captured");
            Toast.makeText(this, "Image could not be captured", Toast.LENGTH_SHORT).show();
        }
    }

    private void processAndEmojify() {
        shareButton.setVisibility(View.VISIBLE);
        clearButton.setVisibility(View.VISIBLE);
        saveButton.setVisibility(View.VISIBLE);
        emojifyButton.setVisibility(View.GONE);


        resultantBitmap=BitmapUtils.rescaleImage(this,tempPhotoPath);

        int orientation=exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        resultantBitmap=rotateBitmap(resultantBitmap,orientation);

        resultantBitmap=Emojify.emojifyme(this,resultantBitmap);

        imageView.setImageBitmap(resultantBitmap);
    }

    private Bitmap rotateBitmap(Bitmap bitmap, int orientation) {
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    @OnClick(R.id.saveButton)
    public void save(View view){
        if(!isSaved) {
            BitmapUtils.deleteTempFile(tempPhotoPath);

            privateImagePath=BitmapUtils.saveFile(this, resultantBitmap);
            isSaved=true;
        }

    }
    @OnClick(R.id.shareButton)
    public void share(View view){
        if(!isSaved) {
            BitmapUtils.deleteTempFile(tempPhotoPath);
           privateImagePath= BitmapUtils.saveFile(this, resultantBitmap);
            isSaved=true;
        }
        BitmapUtils.shareFile(this,privateImagePath);
    }
    @OnClick(R.id.clearButton)
    public void clear(View view){
        shareButton.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);
        saveButton.setVisibility(View.GONE);
        emojifyButton.setVisibility(View.VISIBLE);
        imageView.setImageResource(0);

        BitmapUtils.deleteTempFile(tempPhotoPath);
    }
}
