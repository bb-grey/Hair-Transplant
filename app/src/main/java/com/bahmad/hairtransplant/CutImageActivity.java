package com.bahmad.hairtransplant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CutImageActivity extends AppCompatActivity {
    public static String EXTRA_IMAGE_URI = "imageUri";
    private final String fileName = "myImage";
    private Uri imageUri;
    private Bitmap bitmap;
    private SomeView someView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cut_image);
        //imageUri = getIntent().getParcelableExtra(EXTRA_IMAGE_URI);

        LinearLayout layout = findViewById(R.id.layout);
        LinearLayout.LayoutParams lp =
                new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);

//        try {
//            bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        try{
            bitmap = BitmapFactory.decodeStream(openFileInput(fileName));
        } catch (FileNotFoundException ex){
            ex.printStackTrace();
        }
        if(bitmap != null){
            someView = new SomeView(this, bitmap);
            layout.addView(someView, lp);
        }
    }

    public void cropImage() {
        setContentView(R.layout.activity_picture_preview);

        ImageView imageView = findViewById(R.id.image);

        int widthOfscreen = 0;
        int heightOfScreen = 0;

        DisplayMetrics dm = new DisplayMetrics();
        try {
            getWindowManager().getDefaultDisplay().getMetrics(dm);
        } catch (Exception ex) {
        }
        widthOfscreen = dm.widthPixels;
        heightOfScreen = dm.heightPixels;

        Bitmap bitmap2 = bitmap;

        Bitmap resultingImage = Bitmap.createBitmap(widthOfscreen,
                heightOfScreen, bitmap2.getConfig());

        Canvas canvas = new Canvas(resultingImage);

        Paint paint = new Paint();

        Path path = new Path();

        List<Point> points = someView.getPoints();
        for (int i = 0; i < points.size(); i++) {
            path.lineTo(points.get(i).x, points.get(i).y);
        }

        // Cut out the selected portion of the image...
        canvas.drawPath(path, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap2, 0, 0, paint);

        // Frame the cut out portion...
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(20f);
        canvas.drawPath(path, paint);

        imageView.setImageBitmap(resultingImage);

        returnResult(resultingImage);

    }

    private void returnResult(Bitmap image){
        try{
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.PNG, 100, bytes);
            FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
            fileOutputStream.write(bytes.toByteArray());
            fileOutputStream.close();
            bytes.close();

            Intent intent = new Intent();
            setResult(RESULT_OK, intent);
            finish();
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
}