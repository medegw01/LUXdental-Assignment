package com.michaeledegware.www.luxdental;
import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.hardware.Camera;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ZoomControls;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    /* variables declaration for taking image*/

    @SuppressWarnings("deprecation")
    private android.hardware.Camera.PictureCallback mPicture;
    private Button capture_image, save_image, back;
    private FrameLayout camera_layout, crop_layout;
    private int currentZoomLevel = 0;
    private String image_name, image_path;

    private SurfaceView camcoder;
    private SurfaceHolder surfaceHolder;
    @SuppressWarnings("deprecation")
    private android.hardware.Camera camera;
    @SuppressWarnings("deprecation")


    private ImageView imageView;
    private static final String TAG = "MyActivity";
    public Bitmap initial_imageBitmap, rotated_initialBitmap, cropped_imageBitmap;
    private Matrix rotator;

    /* variables for cropping */
    Canvas canvas;
    Paint paint;
    private Path clipPath;
    Matrix matrix;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*rotates the image by 90 degrees if need be */
        rotator = new Matrix();
        rotator.postRotate(90);
        rotator.preScale(-1.0f, 1.0f);

        image_name = String.valueOf(Math.round(Math.random()*100000))+".jpg";

        /* find variable ids' */
        capture_image = (Button) findViewById(R.id.capture);
        save_image    = (Button) findViewById(R.id.save_image);
        back          = (Button) findViewById(R.id.return_);

        crop_layout   = (FrameLayout) findViewById(R.id.crop_layout) ;
        camera_layout = (FrameLayout) findViewById(R.id.camera_layout);


        imageView     = (ImageView) findViewById(R.id.view_imageid);
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        /*set cropping settings invisible */
        crop_layout.setVisibility(View.GONE);
        camera_layout.setVisibility(View.VISIBLE);

        camcoder      = (SurfaceView) findViewById(R.id.image_captured);
        surfaceHolder = camcoder.getHolder();

        /* access the surface holder*/
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);


        /* set up to save image when captured*/
        mPicture = new android.hardware.Camera.PictureCallback() {

            @Override
            public void onPictureTaken(byte[] data, android.hardware.Camera camera) {
                FileOutputStream fos = null;
                try {
                    File image_folder = getStorageLocationFolder();
                    image_path ="sdcard/luxdental_images/" + image_name;
                    File file = new File(image_folder, image_name);
                    fos = new FileOutputStream(file);
                    fos.write(data);
                    fos.flush();
                    fos.close();
                    initial_imageBitmap = BitmapFactory.decodeFile(image_path);
                    rotated_initialBitmap = Bitmap.createBitmap(initial_imageBitmap, 0, 0,
                            initial_imageBitmap.getWidth(), initial_imageBitmap.getHeight(),
                            rotator, true);
                    initial_imageBitmap.recycle();
                    camera_layout.setVisibility(View.GONE);
                    crop_layout.setVisibility(View.VISIBLE);
                    doCrop();

                } catch (FileNotFoundException e) {
                        Log.d(TAG, "File not found: " + e.getMessage());
                } catch (IOException e) {
                        Log.d(TAG, "Error accessing file: " + e.getMessage());
                }
            }
        };

       /* set buttons functionality */
        capture_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (camera != null) {
                    camera.takePicture(null, null, mPicture);
                }

            }
        });

        /* frees allocated memories/data and returns to image capture */
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                image_name = String.valueOf(Math.round(Math.random()*100000))+".jpg";
                deconstructor();
                File f = new File(image_path);
                if (f.exists()) {
                    f.delete();
                }
                camera_layout.setVisibility(View.VISIBLE);
                crop_layout.setVisibility(View.GONE);

            }
        });

        /* saves the crop image and delete the temporary image taken before edit */
        save_image.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    OutputStream fos = null;
                    File file = new File(getStorageLocationFolder(), String.valueOf(Math.round
                            (Math.random()*100000))+"_LUXcropped.jpg");
                    fos = new FileOutputStream(file);

                    cropped_imageBitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                    fos.flush();
                    fos.close();
                    cropped_imageBitmap.recycle();

                    AlertDialog.Builder builder		= new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Saved");
                    AlertDialog dialog = builder.create();
                    dialog.show();

                    /* delete the temporary image that was edited */
                    File f = new File(image_path);
                    if (f.exists()) {
                        f.delete();
                    }

                    image_name = String.valueOf(Math.round(Math.random()*100000))+".jpg";
                    deconstructor();
                    camera_layout.setVisibility(View.VISIBLE);
                    crop_layout.setVisibility(View.GONE);


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    private Camera.Size getBestPreviewSize(int width, int height,Camera.Parameters parameters){
        Camera.Size result=null;
        for (Camera.Size size : parameters.getSupportedPreviewSizes())
        {
            if (size.width<=width && size.height<=height)
            {
                if (result==null) {
                    result=size;
                }   else {
                    int resultArea=result.width*result.height;
                    int newArea=size.width*size.height;
                    if (newArea>resultArea) {
                        result=size;
                    }
                }
            }
        }
        return(result);
    }

    /* creates if need be and returns th address for the phone gallery */
    private File getStorageLocationFolder(){
        File folder = new File("sdcard/luxdental_images");
        if(!folder.exists()){
            folder.mkdir();
        }
        return folder;
    }


    /* the auto generated camera methods */
    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        /*open camera*/
        try {
            camera = android.hardware.Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);

        } catch (RuntimeException e) {
            System.err.println(e);
            return;
        }
        camera.setDisplayOrientation(90);
        android.hardware.Camera.Parameters param;
        param = camera.getParameters();
       // param.setFlashMode(android.hardware.Camera.Parameters.FLASH_MODE_ON);
       // param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        param.setJpegQuality(100);
        camera.setParameters(param);

        try {
           camera.setPreviewDisplay(surfaceHolder);
           camera.startPreview();
       } catch (Exception e) {
            System.err.println(e);
           return;
       }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,int height) {

        final android.hardware.Camera.Parameters params;
        if(camera == null) {
           return;
        }
        params = camera.getParameters();
        Camera.Size size=getBestPreviewSize(width, height,
                params);
        if (size!=null) {
            params.setPreviewSize(size.width, size.height);
            params.setJpegQuality(100);
            camera.setParameters(params);
            camera.startPreview();
            ZoomControls zoomControls = (ZoomControls) findViewById(R.id.zoomControls);

            if (params.isZoomSupported()) {
                final int maxZoomLevel = params.getMaxZoom();
                Log.i("max ZOOM ", "is " + maxZoomLevel);
                zoomControls.setIsZoomInEnabled(true);
                zoomControls.setIsZoomOutEnabled(true);

                zoomControls.setOnZoomInClickListener(new View.OnClickListener(){
                    public void onClick(View v){
                        if(currentZoomLevel < maxZoomLevel){
                            currentZoomLevel++;
                            params.setZoom(currentZoomLevel);
                            camera.setParameters(params);
                        }
                    }
                });

                zoomControls.setOnZoomOutClickListener(new View.OnClickListener(){
                    public void onClick(View v){
                        if(currentZoomLevel > 0){
                            currentZoomLevel--;
                            params.setZoom(currentZoomLevel);
                            camera.setParameters(params);
                        }
                    }
                });
            }
            else
                zoomControls.setVisibility(View.GONE);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        /*end camera when closing the application*/
        camera.stopPreview();
        camera.release();
        camera = null;
    }


    private void cropImageByPath() {

        clipPath.close();
        /*selects the unneeded part of the image and make it transparent red.*/
        clipPath.setFillType(Path.FillType.INVERSE_WINDING);
        Paint negA = new Paint(Paint.ANTI_ALIAS_FLAG);
        negA.setColor(Color.BLACK);
        canvas.drawPath(clipPath, negA);
        negA.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawBitmap(cropped_imageBitmap, 0, 0, negA);
    }

    /* does the cropping of image*/
    private void doCrop() {
        /*makes a copy of image and crop*/
        cropped_imageBitmap = Bitmap.createBitmap(rotated_initialBitmap.getWidth(),
                rotated_initialBitmap.getHeight(), rotated_initialBitmap.getConfig());

        canvas = new Canvas(cropped_imageBitmap);
        paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStrokeWidth(5);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setAntiAlias(true);
        matrix = new Matrix();
        canvas.drawBitmap(rotated_initialBitmap, matrix, paint);
        rotated_initialBitmap.recycle();
        imageView.setImageBitmap(cropped_imageBitmap);
        /*set imageView on touch listener so coordinates touched can be reported to canvas*/
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return draw(view,motionEvent);
            }
        });
    }
    /* draws green line on image*/
    private boolean draw(View v, MotionEvent event) {
        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                clipPath = new Path();
                clipPath.moveTo(touchX, touchY);
                canvas.drawPath(clipPath,paint);
                break;
            case MotionEvent.ACTION_MOVE:
                clipPath.lineTo(touchX, touchY);
                canvas.drawPath(clipPath,paint);
                break;
            case MotionEvent.ACTION_UP:
                clipPath.lineTo(touchX, touchY);
                canvas.drawPath(clipPath,paint);
                cropImageByPath();
                break;
            default:
                return false;
        }
        imageView.invalidate();
        return true;
    }

    /*free allocated spaces*/
    private void deconstructor(){
        initial_imageBitmap.recycle();
        rotated_initialBitmap.recycle();
        cropped_imageBitmap.recycle();
    }

}
