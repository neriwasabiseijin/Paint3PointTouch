package com.iplab.neriwasabiseijin.paint3pointtouch;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.RectF;
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

    // 範囲選択用
    private PointF beforeTouchMove = new PointF(0f, 0f);
    private PointF moveStart = new PointF(-1f, -1f);
    private PointF moveEnd = new PointF(-1f, -1f);
    private RectF selectionRect = new RectF(0f, 0f, 0f, 0f);
    private Bitmap selectionBitmap;
    private Canvas selectionCvs;
    private Paint selectionPen = new Paint();

    // 範囲選択後やペースト後の範囲を移動できるかどうか
    private boolean frameMovementFlag = false;

    public canvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPen();
        setSelectionPen();

        mPointerID = new int[HOLDFINGER];
        candidateTouchStart = new PointF[HOLDFINGER];
        for(int i=0; i<HOLDFINGER; i++){
            mPointerID[i] = -1;
            candidateTouchStart[i] = new PointF(-1, -1);
        }
    }

    @Override
    public void onDraw(Canvas canvas){
        // 今まで描いた線
        canvas.drawBitmap(bitmap, 0, 0, null);
        // 現在描いている線
        if(path != null){
            canvas.drawPath(path, pen);
        }

        // 範囲選択の枠
        canvas.drawBitmap(selectionBitmap, 0, 0, null);
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cvs = new Canvas(bitmap);
        selectionBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        selectionCvs = new Canvas(selectionBitmap);
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
                if(PaintActivity.paintMode == PaintActivity.MODE_DRAW || PaintActivity.paintMode == PaintActivity.MODE_ERASE){
                    path = new Path();
                    path.moveTo(touchPos.x, touchPos.y);
                    invalidate();
                }else if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION){
                    if(frameMovementFlag){
                        if(selectionRect.contains(touchPos.x, touchPos.y)){
                            beforeTouchMove = touchPos;
                        }else{
                            moveStart = new PointF(-1f, -1f);
                            moveEnd = new PointF(-1f, -1f);
                            selectionCvs.drawColor(0, PorterDuff.Mode.CLEAR);
                            PaintActivity.paintMode = PaintActivity.MODE_DRAW;
                            invalidate();
                            frameMovementFlag = false;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if(path != null){path = null;}
                if (count == HOLDFINGER) {
                    if(PaintActivity.paintMode == PaintActivity.MODE_DRAW || PaintActivity.paintMode == PaintActivity.MODE_ERASE) {
                        PaintActivity.paintMode = PaintActivity.MODE_SELECTION;
                    }
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if(count == 1) {
                    if (PaintActivity.paintMode == PaintActivity.MODE_DRAW || PaintActivity.paintMode == PaintActivity.MODE_ERASE) {
                        if(path != null) {
                            path.lineTo(touchPos.x, touchPos.y);
                            invalidate();
                        }
                    }else if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION){
                        selectionRect = setSelectionRect();
                        drawSelectionRect();
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(PaintActivity.paintMode == PaintActivity.MODE_DRAW || PaintActivity.paintMode == PaintActivity.MODE_ERASE){
                    if(path != null) {
                        path.lineTo(touchPos.x, touchPos.y);
                        cvs.drawPath(path, pen);
                        path = null;
                        invalidate();
                    }
                }else if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION){
                    if(!frameMovementFlag){
                        frameMovementFlag = true;
                    }
                }
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

    public void setSelectionPen(){
        // 範囲選択のペンのセット
        selectionPen = new Paint();
        selectionPen.setColor(Color.argb(255, 50, 50, 180));
        selectionPen.setStrokeWidth(4);
        selectionPen.setAntiAlias(true);
        selectionPen.setStyle(Paint.Style.STROKE);
        selectionPen.setStrokeCap(Paint.Cap.ROUND);
        selectionPen.setStrokeJoin(Paint.Join.ROUND);
    }
    public RectF setSelectionRect(){
        float left=0, right=0, top=0, bottom=0;

        if(frameMovementFlag){ // 範囲選択後
            PointF moveAmount = new PointF(touchPos.x-beforeTouchMove.x,touchPos.y-beforeTouchMove.y);
            left = selectionRect.left + moveAmount.x;
            right = selectionRect.right + moveAmount.x;
            top = selectionRect.top + moveAmount.y;
            bottom = selectionRect.bottom + moveAmount.y;

            beforeTouchMove = touchPos;
        }else{ // 範囲選択中
            // 初回は範囲選択の開始点を候補点から取得
            if(moveStart.x == -1f && moveStart.y == -1f){
                moveStart = new PointF(0f, 0f);
                for(int i=0; i<HOLDFINGER; i++){
                    if(candidateTouchStart[i].x>=0 && candidateTouchStart[i].y>=0){
                        moveStart = candidateTouchStart[i];
                        break;
                    }
                }
            }

            moveEnd = touchPos;
            if(moveEnd.x < 0f){moveEnd.x = 0f;}
            if(moveEnd.y < 0f){moveEnd.y = 0f;}

            left = moveStart.x; top = moveStart.y;
            right = moveEnd.x; bottom = moveEnd.y;
            if(left > right){
                float tmp=left; left=right; right=tmp;
            }
            if(top > bottom){
                float tmp=top; top=bottom; bottom=tmp;
            }
        }
        return new RectF(left, top, right, bottom);
    }
    public void drawSelectionRect(){
        selectionCvs.drawColor(0, PorterDuff.Mode.CLEAR);
        // 枠線
        selectionPen.setColor(Color.argb(255, 50, 50, 180));
        selectionPen.setStyle(Paint.Style.STROKE);
        selectionCvs.drawRect(selectionRect, selectionPen);
        // 内部．半透明に塗りつぶす
        selectionPen.setColor(Color.argb(128, 50, 50, 180));
        selectionPen.setStyle(Paint.Style.FILL);
        selectionCvs.drawRect(selectionRect, selectionPen);
        invalidate();
    }

    public void copyBitmap(){}
    public void pasteBitmap(){}
}
