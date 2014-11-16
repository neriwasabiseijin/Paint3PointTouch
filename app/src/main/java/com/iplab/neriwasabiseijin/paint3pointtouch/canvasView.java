package com.iplab.neriwasabiseijin.paint3pointtouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by neriwasabiseijin on 2014/10/26.
 */
public class canvasView extends View {
    // 何点タッチで選択を起動するか
    static int HOLDFINGER = 3;

    // タッチ位置
    private PointF touchPos = new PointF(0f, 0f);

    // マルチタッチ用
    int[] mPointerID;
    PointF[] candidateTouchStart; // 3点タッチの開始点候補

    // 描画用
    private Path path = null;
    private Paint pen = new Paint();
    private Bitmap bitmap;
    private Canvas cvs;


    public canvasView(Context context, AttributeSet attrs) {
        super(context, attrs);


        mPointerID = new int[HOLDFINGER];
        candidateTouchStart = new PointF[HOLDFINGER];
        for(int i=0; i<HOLDFINGER; i++){
            mPointerID[i] = -1;
            candidateTouchStart[i] = new PointF(-1, -1);
        }

    }

    @Override
    public void onDraw(Canvas canvas){

        // 現在描いている線
        if(path != null){
            canvas.drawPath(path, pen);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cvs = new Canvas(bitmap);

    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        int action = ev.getActionMasked();
        int count = ev.getPointerCount();
        int pointerId = ev.getPointerId(ev.getActionIndex());

        touchPos = new PointF(ev.getX(), ev.getY());

        setMPointerID(ev, pointerId, action);
        switch(action){
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                break;
        }


        return true;
    }

    // マルチタッチ用ポインタと，3点タッチ開始点候補の処理
    public void setMPointerID(MotionEvent ev, int pointerId, int action){
        int count = ev.getPointerCount();

        switch(action){
            case MotionEvent.ACTION_DOWN:
                for(int i=0; i<mPointerID.length; i++){mPointerID[i] = -1;}
                mPointerID[0] = pointerId;
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                for(int i=0; i<mPointerID.length; i++){
                    if(mPointerID[i] == -1){
                        mPointerID[i] = pointerId;
                        break;
                    }
                }

                if(count == HOLDFINGER){
                    for(int i=0; i<mPointerID.length; i++) {
                        int mPointerIndex = ev.findPointerIndex(mPointerID[i]);
                        candidateTouchStart[i] = new PointF(ev.getX(mPointerIndex), ev.getY(mPointerIndex));
                        if(candidateTouchStart[i].x < 0f){candidateTouchStart[i].x = 0f;}
                        if(candidateTouchStart[i].y < 0f){candidateTouchStart[i].y = 0f;}
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                break;
            case MotionEvent.ACTION_POINTER_UP:
                for(int i=0; i<HOLDFINGER; i++){
                    if(mPointerID[i] == pointerId){
                        mPointerID[i] = -1;
                        candidateTouchStart[i] = new PointF(-1f, -1f);
                        break;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                for(int i=0; i<HOLDFINGER; i++){
                    mPointerID[i] = -1;
                    candidateTouchStart[i] = new PointF(-1f, -1f);
                }
                break;
        }
    }

    public void setPen(){
        pen = new Paint();
        pen.setColor(Color.BLACK);
        pen.setStrokeWidth(6);
        if(PaintActivity.paintMode == PaintActivity.MODE_ERASE){
            pen.setColor(Color.WHITE);
            pen.setStrokeWidth(50);
        }
        pen.setAntiAlias(true);
        pen.setStyle(Paint.Style.STROKE);
        pen.setStrokeCap(Paint.Cap.ROUND);
        pen.setStrokeJoin(Paint.Join.ROUND);
    }
/*
    public void setSelectionPen(){
        selectionPen = new Paint();
        selectionPen.setColor(Color.argb(255, 50, 50, 180));
        selectionPen.setStrokeWidth(4);
        selectionPen.setAntiAlias(true);
        selectionPen.setStyle(Paint.Style.STROKE);
        selectionPen.setStrokeCap(Paint.Cap.ROUND);
        selectionPen.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setPasteRectPen() {
        pasteRectPen = new Paint();
        pasteRectPen.setColor(Color.argb(255,180,50,50));
        pasteRectPen.setStrokeWidth((4));
        pasteRectPen.setAntiAlias(true);
        pasteRectPen.setStyle(Paint.Style.STROKE);
        pasteRectPen.setStrokeCap(Paint.Cap.ROUND);
        pasteRectPen.setStrokeJoin(Paint.Join.ROUND);
    }

    public void setCopyBitmap(int w, int h){
        copyBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        copyCvs = new Canvas(copyBitmap);
    }

    public void copyBitmap() {

        bitmap.setPixels(copyPixels, 0, copyWidth, (int)copyCvsPos.x, (int)copyCvsPos.y, copyWidth, copyHeight);

        setCopyBitmap((int) selectionRect.width(), (int) selectionRect.height());
        int [] pixels = new int[bitmap.getWidth()*bitmap.getHeight()];
        copyWidth = copyBitmap.getWidth();
        copyHeight = copyBitmap.getHeight();
        copyPixels = new int[copyWidth*copyHeight];

        PaintActivity.tV.setText(copyWidth+","+selectionRect.width());

        int top = (int)selectionRect.top;
        int bottom = (int)selectionRect.bottom;
        int left = (int)selectionRect.left;
        int right = (int)selectionRect.right;

        if(top < 0){top=0;}
        if(bottom > bitmap.getHeight()){bottom=bitmap.getHeight();}
        if(left < 0){left=0;}
        if(right > bitmap.getWidth()){right=bitmap.getWidth();}

        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        int tmpNum = 0;
        for(int y=top; y<bottom; y++){
            for(int x=left; x<right; x++) {
                int pos = x + y*bitmap.getWidth();
                copyPixels[tmpNum] = pixels[pos];
                tmpNum++;
            }
        }

        invalidate();
    }

    public void pasteBitmap() {
        pasteFlag = true;
        copyBitmap.setPixels(copyPixels, 0, copyWidth, 0, 0, copyWidth, copyHeight);

        touchStart = new PointF(-1f,-1f);
        touchEnd = new PointF(-1f,-1f);
        selectionCvs.drawColor(0,Mode.CLEAR);
        selectedFlag = false;

        PaintActivity.paintMode = PaintActivity.MODE_PASTE;

        invalidate();
    }
*/

    public void copyBitmap(){}
    public void pasteBitmap(){}
}
