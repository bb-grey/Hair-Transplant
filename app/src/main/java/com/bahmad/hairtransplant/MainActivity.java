package com.bahmad.hairtransplant;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ImageView imageView;
    private TextView dotsLengthView;
    private SeekBar blockSizeSeekBar;
    private SeekBar constantSeekBar;
    private TextView textView_unwantedArea;
    private TextView textView_blockSize;
    private Bitmap bitmapImage;
    private int blockSize = 11;
    private int constant = 2;

    private Mat originalImage;
    private Mat greyImage;
    private Mat blurredImage;

    private final BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
//                    Log.i("OpenCV", "OpenCV loaded successfully");
                    //imageMat=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OpenCVLoader.initDebug();

        imageView = findViewById(R.id.imageView);
        dotsLengthView = findViewById(R.id.textView_dots);

        textView_unwantedArea = findViewById(R.id.textView_unwantedArea);
        textView_blockSize = findViewById(R.id.textView_dotSize);

        textView_unwantedArea.setVisibility(View.INVISIBLE);
        textView_blockSize.setVisibility(View.INVISIBLE);

        Button uploadButton = findViewById(R.id.button_upload);
        Button findDotsButton = findViewById(R.id.button_findDots);
        findDotsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(bitmapImage != null){
                    try {
                        countDots();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else{
                    Toast.makeText(MainActivity.this, getResources().
                            getString(R.string.upldate_image_msg), Toast.LENGTH_SHORT).show();
                }
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                CropImage.startPickImageActivity(MainActivity.this);
            }
        });

        blockSizeSeekBar = findViewById(R.id.seekbar_blockSize);
        constantSeekBar = findViewById(R.id.seekBar_constant);

        blockSizeSeekBar.setProgress(blockSize);
        constantSeekBar.setProgress(constant);

        blockSizeSeekBar.setVisibility(View.INVISIBLE);
        constantSeekBar.setVisibility(View.INVISIBLE);

        blockSizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i < 3){
                    seekBar.setProgress(3);
                    blockSize = 3;
                }
                else{
                    if(i % 2 == 0){
                        blockSize = i + 1;
                    } else{
                        blockSize = i;
                    }
                }
                detectDots();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        constantSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                constant = seekBar.getProgress();
                detectDots();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }
    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri uri;
        if(resultCode == RESULT_OK){
            if(requestCode == CropImage.PICK_IMAGE_CHOOSER_REQUEST_CODE){
                Uri imageUri = CropImage.getPickImageResultUri(this, data);
                if(CropImage.isReadExternalStoragePermissionsRequired(this, imageUri)){
                    uri = imageUri;
                    requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0 );
                } else{
                    startCrop(imageUri);
                }
            }
            if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                if(resultCode == RESULT_OK){
                    uri = result.getUri();
//                    imageView.setImageURI(uri);
//                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), uri);
                    try {
//                        bitmapImage = ImageDecoder.decodeBitmap(source);
                        bitmapImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    imageView.setImageBitmap(bitmapImage);
                }
            }
        }
    }

    private void startCrop(Uri imageUri) {
        CropImage.activity(imageUri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMultiTouchEnabled(true)
                .start(this);
    }

    private void countDots() throws IOException {
        originalImage = new Mat();
        greyImage = new Mat();

        if(bitmapImage != null){
            bitmapImage = bitmapImage.copy(Bitmap.Config.ARGB_8888, true);
            Utils.bitmapToMat(bitmapImage, originalImage);

            textView_unwantedArea.setVisibility(View.VISIBLE);
            textView_blockSize.setVisibility(View.VISIBLE);
            blockSizeSeekBar.setVisibility(View.VISIBLE);
            constantSeekBar.setVisibility(View.VISIBLE);
        }

        Utils.bitmapToMat(bitmapImage, originalImage);
        blurredImage = new Mat();
//        Imgproc.medianBlur(originalImage, blurredImage, 5);
        blurredImage = originalImage.clone();
        detectDots();

//        Mat imgResult = originalImage.clone();
//
//        Imgproc.cvtColor(originalImage, greyImage, Imgproc.COLOR_BGR2GRAY);
//
//        Imgproc.adaptiveThreshold(greyImage, greyImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);
//        List<MatOfPoint> contours = new ArrayList<>();
//        Imgproc.findContours(greyImage, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
//
//        dotsLengthView.setText("Detected hair dots: " + String.valueOf(contours.size()));
//        Imgproc.drawContours(imgResult, contours, -1, new Scalar(0, 0, 0), 1);
//
//        Bitmap imgBitmap = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
//        Utils.matToBitmap(imgResult, imgBitmap);
//        imageView.setImageBitmap(imgBitmap);
    }

    private void detectDots(){

        Mat imgResult = originalImage.clone();

        Imgproc.cvtColor(blurredImage, greyImage, Imgproc.COLOR_BGR2GRAY);

        Imgproc.adaptiveThreshold(greyImage, greyImage, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, blockSize, constant);
        Imgproc.morphologyEx(greyImage, greyImage, Imgproc.MORPH_OPEN, Mat.ones(3, 3, 0), new Point(-1, 1), 1);

//        Imgproc.morphologyEx(greyImage, greyImage, Imgproc.MORPH_OPEN, Mat.ones(3, 3, 0), new Point(-1, 1));
        List<MatOfPoint> contours = new ArrayList<>();
        Imgproc.findContours(greyImage, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

        dotsLengthView.setText("Detected hair dots: " + String.valueOf(contours.size()));
        Imgproc.drawContours(imgResult, contours, -1, new Scalar(255, 0, 0), 1);

        Bitmap imgBitmap = Bitmap.createBitmap(imgResult.cols(), imgResult.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(imgResult, imgBitmap);
        imageView.setImageBitmap(imgBitmap);

    }
}