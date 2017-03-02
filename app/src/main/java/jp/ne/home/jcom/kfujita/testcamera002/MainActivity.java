package jp.ne.home.jcom.kfujita.testcamera002;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity {
    private SurfaceView cameraPreview;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cameraPreview = (SurfaceView) findViewById(R.id.cameraPreview);
        cameraPreview.setOnClickListener(onSurfaceClickListener);

        SurfaceHolder holder = cameraPreview.getHolder();
        holder.addCallback(callback);
    }

    private SurfaceHolder.Callback callback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder surfaceHolder) {
            camera = Camera.open();

            try {
                camera.setPreviewDisplay(surfaceHolder);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
            camera.startPreview();
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
            camera.release();
            camera = null;
        }
    };

    private View.OnClickListener onSurfaceClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(camera != null){
                camera.autoFocus(autoFocusCallback);
            }
        }
    };

    private Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean b, Camera camera) {
            camera.setOneShotPreviewCallback(previewCallback);
        }
    };

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera) {
//            Toast.makeText(getApplicationContext(), "Auto Focus", Toast.LENGTH_SHORT).show();

            if(bytes == null){
                return;
            }
            int width = camera.getParameters().getPreviewSize().width;
            int height = camera.getParameters().getPreviewSize().height;

            YuvImage yuvImage = new YuvImage(bytes, ImageFormat.NV21, width, height, null);
            saveJpegFromYuv(yuvImage, width, height);
        }
    };

    private void saveJpegFromYuv(YuvImage yuvImage, int width, int height) {
        String saveDir = Environment.getExternalStorageDirectory().getPath() + "/test";
        File file = new File(saveDir);

        if(!file.exists()){
            file.mkdir();
        }

        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String imgPath = saveDir + "/" + simpleDateFormat.format(calendar.getTime()) + ".jpg";

        OutputStream outputStream;
        try {
            outputStream = new FileOutputStream(new File(imgPath));
            yuvImage.compressToJpeg(new Rect(0, 0, width, height), 100, outputStream);
            outputStream.close();
            registAndroidDB(imgPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream = null;

    }

    private void registAndroidDB(String path) {
        ContentValues contentValues = new ContentValues();
        ContentResolver contentResolver = MainActivity.this.getContentResolver();
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        contentValues.put("_data", path);
        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }
}
