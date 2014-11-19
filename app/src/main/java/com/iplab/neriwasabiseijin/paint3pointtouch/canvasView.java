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
    final int CENTER = 0;
    final int LEFT = 1;
    final int RIGHT = 2;
    final int OUTSIDE = 3;
    private int nowPos = OUTSIDE;

    // 何点タッチで選択を起動するか
    static int HOLDFINGER = 3;

    // タッチ位置
    private PointF touchPos = new PointF(0f, 0f);

    // マルチタッチ用
    static int[] mPointerID;
    static PointF[] candidateTouchStart; // 3点タッチの開始点候補

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

    // コピペ用
    private Paint pasteRectPen = new Paint();
    private Bitmap copyBitmap;
    private Canvas copyCvs;
    private int [] copyPixels = null;
    private RectF pasteRect = new RectF(0f, 0f, 0f, 0f);

    // コピーメニュー用
    private int selectionFingerPointer = -1;
    private PointF showMenuPos = new PointF(-1f, -1f);
    private PointF totalMoveAmount = new PointF(0f,0f);
    private boolean showMenuFlag = false;

    // 範囲選択後やペースト後の範囲を移動できるかどうか
    private boolean frameMovementFlag = false;

    public canvasView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setPen();
        setSelectionPen();
        setPasteRectPen();
    }

    public void myinit(){
        if(PaintActivity.testMode == PaintActivity.TEST_2FINGER){
            canvasView.HOLDFINGER = 2;
        }else{
            canvasView.HOLDFINGER = 3;
        }

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
        if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION) {
            canvas.drawBitmap(selectionBitmap, 0, 0, null);

        }

        // ペースト範囲の枠
        if(PaintActivity.paintMode == PaintActivity.MODE_PASTE){
            setPasteRectPen();
            pasteRectPen.setColor(Color.argb(255, 255, 255, 255));
            pasteRectPen.setStyle(Paint.Style.FILL);
            canvas.drawRect(pasteRect,pasteRectPen);
            canvas.drawBitmap(copyBitmap, pasteRect.left, pasteRect.top, null);
            pasteRectPen.setColor(Color.argb(128,180,50,50));
            canvas.drawRect(pasteRect,pasteRectPen);
            pasteRectPen.setColor(Color.argb(255,180,50,50));
            pasteRectPen.setStyle(Paint.Style.STROKE);
            canvas.drawRect(pasteRect,pasteRectPen);
        }
    }

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh){
        super.onSizeChanged(w, h, oldw, oldh);

        bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        cvs = new Canvas(bitmap);
        selectionBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        selectionCvs = new Canvas(selectionBitmap);
        setCopyBitmap(w,h);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        int action = ev.getActionMasked();
        int count = ev.getPointerCount();
        int pointerId = ev.getPointerId(ev.getActionIndex());

        touchPos = new PointF(ev.getX(), ev.getY());
        setMPointerID(ev, pointerId, action);

        // 選択後の再マルチホールド時の処理
        if(PaintActivity.testMode == PaintActivity.TEST_MENU){
            if(!copyMenu(ev, pointerId, action)){return false;}
        }else if(PaintActivity.testMode == PaintActivity.TEST_GESTURE) {
            copyGesture(ev, pointerId, action);
        }

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
                            frameMovementFlag = false;
                            invalidate();
                            return false;
                        }
                    }
                }else if(PaintActivity.paintMode == PaintActivity.MODE_PASTE){
                    if(frameMovementFlag){
                        if(pasteRect.contains(touchPos.x, touchPos.y)){
                            beforeTouchMove = touchPos;
                        }else{
                            paste();
                            PaintActivity.paintMode = PaintActivity.MODE_DRAW;
                            frameMovementFlag = false;
                            invalidate();
                            return false;
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                if(path != null){path = null;}
                if (count == HOLDFINGER) {
                    if (PaintActivity.paintMode == PaintActivity.MODE_DRAW || PaintActivity.paintMode == PaintActivity.MODE_ERASE) {
                        PaintActivity.tV.setText("SELECTION!");
                        PaintActivity.paintMode = PaintActivity.MODE_SELECTION;
                        return false; // 上のビューにタッチイベントを流す
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
                    }else if(PaintActivity.paintMode == PaintActivity.MODE_PASTE){
                        pasteRect = setPasteRect();
                        invalidate();
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

    public void setPasteRectPen(){
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
    public void copyBitmap(){
        int top = (int)selectionRect.top;
        int bottom = (int)selectionRect.bottom;
        int left = (int)selectionRect.left;
        int right = (int)selectionRect.right;
        if(top < 0){top=0;}
        if(bottom > bitmap.getHeight()){bottom=bitmap.getHeight();}
        if(left < 0){left=0;}
        if(right > bitmap.getWidth()){right=bitmap.getWidth();}

        setCopyBitmap(right - left, bottom - top);
        pasteRect = new RectF(0, 0, right-left, bottom-top);


        // pixelsに現在のbitmapを全コピー，一部をcopyPixelsにコピーする．
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        copyPixels = new int[copyBitmap.getWidth() * copyBitmap.getHeight()];

        for(int y=top, tmpNum=0; y<bottom; y++){
            for(int x=left; x<right; x++){
                int pos = x + y*bitmap.getWidth();
                copyPixels[tmpNum] = pixels[pos];
                tmpNum++;
            }
        }
    }
    public void pasteBitmap(){
        if(copyPixels != null) {
            frameMovementFlag = true;

            copyBitmap.setPixels(copyPixels, 0, copyBitmap.getWidth(), 0, 0, copyBitmap.getWidth(), copyBitmap.getHeight());

            pasteRect = new RectF(0,0, pasteRect.width(), pasteRect.height());
            moveStart = new PointF(-1f, -1f);
            moveEnd = new PointF(-1f, -1f);
            selectionCvs.drawColor(0, PorterDuff.Mode.CLEAR);

            PaintActivity.paintMode = PaintActivity.MODE_PASTE;

            invalidate();
        }
    }
    public void paste(){
        bitmap.setPixels(copyPixels, 0, (int)pasteRect.width(), (int)pasteRect.left, (int)pasteRect.top, (int)pasteRect.width(), (int)pasteRect.height());
        /*
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for(int y=(int)pasteRect.top, tmpNum=0; y<(int)pasteRect.bottom; y++){
            for(int x=(int)pasteRect.left; x<(int)pasteRect.right; x++){
                int pos = x + y*bitmap.getWidth();
                if(copyPixels[tmpNum] != Color.WHITE || copyPixels[tmpNum] != Color.alpha(0)){
                    pixels[pos] = copyPixels[tmpNum];
                }
                tmpNum++;
            }
        }

        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        */
    }

    public RectF setPasteRect(){
        float left=0, right=0, top=0, bottom=0;

        if(frameMovementFlag) { // 範囲選択後
            PointF moveAmount = new PointF(touchPos.x - beforeTouchMove.x, touchPos.y - beforeTouchMove.y);
            left = pasteRect.left + moveAmount.x;
            right = pasteRect.right + moveAmount.x;
            top = pasteRect.top + moveAmount.y;
            bottom = pasteRect.bottom + moveAmount.y;

            if(left < 0){
                right = right - left;
                left = 0;
            }
            if(top < 0){
                bottom = bottom - top;
                top = 0;
            }
            if(right > bitmap.getWidth()){
                left = left - (right-bitmap.getWidth());
                right = bitmap.getWidth();
            }
            if(bottom > bitmap.getHeight()){
                top = top -(bottom-bitmap.getHeight());
                bottom = bitmap.getHeight();
            }

            beforeTouchMove = touchPos;
        }
        return new RectF(left, top, right, bottom);
    }

    public void cutBitmap(){
        int top = (int)selectionRect.top;
        int bottom = (int)selectionRect.bottom;
        int left = (int)selectionRect.left;
        int right = (int)selectionRect.right;
        if(top < 0){top=0;}
        if(bottom > bitmap.getHeight()){bottom=bitmap.getHeight();}
        if(left < 0){left=0;}
        if(right > bitmap.getWidth()){right=bitmap.getWidth();}

        setCopyBitmap(right - left, bottom - top);
        pasteRect = new RectF(0, 0, right-left, bottom-top);

        // pixelsに現在のbitmapを全コピー，一部をcopyPixelsにコピーする．
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        copyPixels = new int[copyBitmap.getWidth() * copyBitmap.getHeight()];

        for(int y=top, tmpNum=0; y<bottom; y++){
            for(int x=left; x<right; x++){
                int pos = x + y*bitmap.getWidth();
                copyPixels[tmpNum] = pixels[pos];
                pixels[pos] = Color.WHITE;
                tmpNum++;
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        PaintActivity.paintMode = PaintActivity.MODE_DRAW;

        invalidate();
    }

    public boolean copyMenu(MotionEvent ev, int pointerId, int action){
        int count = ev.getPointerCount();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION && !frameMovementFlag) {
                    if (count == HOLDFINGER && selectionFingerPointer != -1) {
                        int mPointerIndex = ev.findPointerIndex(selectionFingerPointer);
                        showMenuPos = new PointF(ev.getX(mPointerIndex), ev.getY(mPointerIndex));
                        showMenu((int) showMenuPos.x, (int) showMenuPos.y, CENTER);
                        totalMoveAmount = new PointF(0f,0f);
                    }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION && !frameMovementFlag) {
                    if(count == HOLDFINGER && selectionFingerPointer != -1) {
                        int mPointerIndex = ev.findPointerIndex(selectionFingerPointer);
                        totalMoveAmount.x = (ev.getX(mPointerIndex) - showMenuPos.x);
                        totalMoveAmount.y = (ev.getY(mPointerIndex) - showMenuPos.y);
                        PaintActivity.tV.setText(totalMoveAmount.x+","+totalMoveAmount.y);

                        if(totalMoveAmount.length()<=50f){
                            showMenu((int) showMenuPos.x, (int) showMenuPos.y, CENTER);
                        }else if (50f<totalMoveAmount.length() && totalMoveAmount.length()<180f){
                            if(totalMoveAmount.x <0){
                                showMenu((int) showMenuPos.x, (int) showMenuPos.y, LEFT);
                            }else{
                                showMenu((int) showMenuPos.x, (int) showMenuPos.y, RIGHT);
                            }
                        }else{
                            showMenu((int) showMenuPos.x, (int) showMenuPos.y, OUTSIDE);
                        }
                    }


                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION && !frameMovementFlag) {
                    if(selectionFingerPointer == -1) {
                        int countFingers = 0;
                        for (int i = 0; i < HOLDFINGER; i++) {
                            if (mPointerID[i] != -1) {
                                selectionFingerPointer = mPointerID[i];
                                countFingers++;
                            }
                        }
                        if (countFingers != 1) {
                            selectionFingerPointer = -1;
                        }
                    }else{
                        int notCount = 0;
                        for (int i = 0; i < HOLDFINGER; i++) {
                            if (mPointerID[i] != selectionFingerPointer) {
                               notCount++;
                            }
                        }
                        if(notCount == HOLDFINGER && showMenuFlag){
                            selectionFingerPointer = -1;
                            selectMenu();
                            showMenuFlag = false;
                            return false;
                        }
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if(selectionFingerPointer != -1 && showMenuFlag) {
                    selectMenu();
                    selectionFingerPointer = -1;
                    showMenuFlag = false;
                    PaintActivity.tV.setText("CCC");
                    return false;
                }
                selectionFingerPointer = -1;
                break;

        }

        return true;
    }

    public void showMenu(int x,int y, int position){
        int r1 = 50;
        int r2 = 180;

        nowPos = position;
        showMenuFlag = true;

        selectionCvs.drawColor(0, PorterDuff.Mode.CLEAR);
        drawSelectionRect();

        // 枠線
        Paint menuPen = new Paint();
        menuPen.setColor(Color.argb(255, 200, 30, 30));
        menuPen.setStrokeWidth((4));
        menuPen.setAntiAlias(true);

        menuPen.setStyle(Paint.Style.FILL);
        switch (position){
            case CENTER:
                menuPen.setColor(Color.argb(220, 200, 30, 30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), -90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,180,180,30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), 90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,130,130,180));
                selectionCvs.drawCircle(x,y,r1,menuPen);
                break;
            case LEFT:
                menuPen.setColor(Color.argb(220, 200, 30, 30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), -90, 180, true, menuPen);
                menuPen.setColor(Color.argb(250,250,250,80));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), 90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,30,30,80));
                selectionCvs.drawCircle(x,y,r1,menuPen);
                break;
            case RIGHT:
                menuPen.setColor(Color.argb(250, 250, 80, 80));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), -90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,180,180,30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), 90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,30,30,80));
                selectionCvs.drawCircle(x,y,r1,menuPen);
                break;
            case OUTSIDE:
                menuPen.setColor(Color.argb(220, 200, 30, 30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), -90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,180,180,30));
                selectionCvs.drawArc(new RectF(x-r2, y-r2, x+r2, y+r2), 90, 180, true, menuPen);
                menuPen.setColor(Color.argb(220,30,30,80));
                selectionCvs.drawCircle(x,y,r1,menuPen);
                break;
        }


        menuPen.setStyle(Paint.Style.STROKE);
        menuPen.setColor(Color.argb(255,200,30,30));
        selectionCvs.drawCircle(x,y,r1,menuPen);
        selectionCvs.drawCircle(x,y,r2,menuPen);

        invalidate();
    }
    public void selectMenu(){
        switch (nowPos){
            case LEFT:
                copyBitmap();
                break;
            case RIGHT:
                cutBitmap();
                break;
        }

        moveStart = new PointF(-1f, -1f);
        moveEnd = new PointF(-1f, -1f);
        selectionCvs.drawColor(0, PorterDuff.Mode.CLEAR);
        PaintActivity.paintMode = PaintActivity.MODE_DRAW;
        frameMovementFlag = false;
        invalidate();
    }


    public void copyGesture(MotionEvent ev, int pointerId, int action){
        int count = ev.getPointerCount();

        if(PaintActivity.paintMode == PaintActivity.MODE_SELECTION) {
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    if (count == HOLDFINGER) {
                        PaintActivity.tV.setText("CUT!");
                        cutBitmap();
                    }
                    if (count == HOLDFINGER - 1) {
                        PaintActivity.tV.setText("COPY!!");
                        copyBitmap();
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_POINTER_UP:
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    break;
            }
        }
    }



}
