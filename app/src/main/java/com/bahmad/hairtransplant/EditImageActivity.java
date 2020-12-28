package com.bahmad.hairtransplant;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.SeekBar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import ja.burhanrashid52.photoeditor.OnSaveBitmap;
import ja.burhanrashid52.photoeditor.PhotoEditor;
import ja.burhanrashid52.photoeditor.PhotoEditorView;

public class EditImageActivity extends AppCompatActivity {
    private Bitmap image;
    private PhotoEditorView editorView;
    private PhotoEditor photoEditor;
    private int eraserSize = 6;
    private final String fileName = "myImage";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);

        editorView = findViewById(R.id.photo_editor_view);
        try{
            image = BitmapFactory.decodeStream(openFileInput(fileName));
            editorView.getSource().setImageBitmap(image);
        } catch (FileNotFoundException ex){
            ex.printStackTrace();
        }

        photoEditor = new PhotoEditor.Builder(this, editorView).build();
        photoEditor.setBrushDrawingMode(true);
        photoEditor.setBrushEraserSize((float)eraserSize);
        photoEditor.setBrushColor(0xFFFFFFFF);

        SeekBar seekBar = findViewById(R.id.seekBar_eraser);
        seekBar.setProgress(eraserSize - 1);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                eraserSize = seekBar.getProgress() + 1;
                photoEditor.setBrushSize((float)eraserSize);

                Log.d("Eraser", String.valueOf(eraserSize));
            }
        });
    }

    public void onClickDone(View view){
        photoEditor.saveAsBitmap(new OnSaveBitmap() {
            @Override
            public void onBitmapReady(Bitmap saveBitmap) {
                image = saveBitmap;
                try{
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    image.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
                    FileOutputStream fileOutputStream = openFileOutput(fileName, Context.MODE_PRIVATE);
                    fileOutputStream.write(bytes.toByteArray());
                    fileOutputStream.close();
                    bytes.close();
                } catch (Exception ex){
                    ex.printStackTrace();
                }

                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }

            @Override
            public void onFailure(Exception e) {

            }
        });
    }
}