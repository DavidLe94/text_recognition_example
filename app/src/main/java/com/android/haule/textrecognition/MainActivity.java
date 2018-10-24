package com.android.haule.textrecognition;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.IOException;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CODE = 999;
    private static final int MY_PERMISSIONS_REQUEST = 100;

    private Button btnOpenFile;
    private TextView tvResult;
    private ImageView imgView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnOpenFile = findViewById(R.id.btn_open_file);
        tvResult = findViewById(R.id.tv_result);
        imgView = findViewById(R.id.img_view);

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                askForPermission();
            }
        });
    }

    private void runTextRecognition(Bitmap bitmap) {
        FirebaseVisionImage image = FirebaseVisionImage
                .fromBitmap(bitmap);
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getOnDeviceTextRecognizer();
        detector.processImage(image)
                .addOnSuccessListener(
                        new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText texts) {
                                tvResult.setText(texts.getText().trim());
                                Log.d("TAG", "onSuccess: " + texts.getText().trim());
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Task failed with an exception
                                e.printStackTrace();
                            }
                        });

    }

    private Bitmap drawableToBitmap(Drawable drawable){
        return ((BitmapDrawable) drawable).getBitmap();
    }

    //get permission from client
    private void askForPermission() {
        boolean hasPermission = ContextCompat.checkSelfPermission(Objects.requireNonNull(this),
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(Objects.requireNonNull(this),
                        Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(Objects.requireNonNull(this),
                        Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if(hasPermission){
            selectAction();
        }else{
            // No explanation needed; request the permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST);
        }
    }

    //select action take a photo or pic image in storage
    private void selectAction(){
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick .putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        pick.setType("image/*");
        Intent photo = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Intent chooser = Intent.createChooser(pick, "Choose: ");
        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS,new Intent[]{photo});
        startActivityForResult(chooser, REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == MY_PERMISSIONS_REQUEST){
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // permission was granted, yay! Do the
                selectAction();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //take picture from camera
            if(data.getExtras()!=null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imgView.setImageBitmap(imageBitmap);
            }

            if(data.getData() != null){
                Uri uri = data.getData();
                try {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    // Log.d(TAG, String.valueOf(bitmap));
                    imgView.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //start recognition
            runTextRecognition(((BitmapDrawable)imgView.getDrawable()).getBitmap());
        }
    }
}
