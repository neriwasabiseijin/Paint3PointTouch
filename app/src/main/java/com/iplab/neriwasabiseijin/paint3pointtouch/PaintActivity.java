package com.iplab.neriwasabiseijin.paint3pointtouch;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;


public class PaintActivity extends ActionBarActivity {
    static final int MODE_DRAW = 0;
    static final int MODE_ERASE = 1;
    static final int MODE_SELECTION = 2;
    static final int MODE_PASTE = 3;

    static final int TEST_NORMAL = 0;
    static final int TEST_MENU = 1;
    static final int TEST_GESTURE = 2;
    static final int TEST_2FINGER = 3;

    static int paintMode = MODE_DRAW;
    static int testMode = 0;

    ActionBar actionBar;
    Menu myMenu;

    canvasView cvsView;
    static TextView tV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paint);

        actionBar = getSupportActionBar();
        actionBar.setTitle("Paint");

        cvsView = (canvasView)findViewById(R.id.canvasView);
        tV = (TextView)findViewById(R.id.textView1);

        setTestMode();
        tV.setText(testMode+"");
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev){
        int count = ev.getPointerCount();
        changeActionBar();
        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_paint, menu);
        myMenu = menu;
        changeIcon(myMenu.findItem(R.id.menu_draw));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id){
            case R.id.menu_draw:
                paintMode = MODE_DRAW;
                changeIcon(item);
                cvsView.setPen();
                break;
            case R.id.menu_erase:
                paintMode = MODE_ERASE;
                changeIcon(item);
                cvsView.setPen();
                break;
            case R.id.menu_copy:
                Toast.makeText(this, "copy", Toast.LENGTH_SHORT).show();
                cvsView.copyBitmap();
                break;
            case R.id.menu_cut:
                Toast.makeText(this, "cut", Toast.LENGTH_SHORT).show();
                cvsView.cutBitmap();
                break;
            case R.id.menu_paste:
                Toast.makeText(this, "paste", Toast.LENGTH_SHORT).show();
                cvsView.pasteBitmap();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void changeIcon(MenuItem clickItem){
        MenuItem otherItem;
        if(clickItem.getItemId() == R.id.menu_draw){
            otherItem = (MenuItem)myMenu.findItem(R.id.menu_erase);
            otherItem.setIcon(R.drawable.ic_menu_erase_holo_dark);
            clickItem.setIcon(R.drawable.ic_menu_draw_selected_holo_dark);
        }else{
            otherItem = (MenuItem)myMenu.findItem(R.id.menu_draw);
            otherItem.setIcon(R.drawable.ic_menu_draw_holo_dark);
            clickItem.setIcon(R.drawable.ic_menu_erase_selected_holo_dark);
        }
    }

    public void changeActionBar(){
        int color= R.color.action_bar_bg_black;

        myMenu.clear();
        if(paintMode == MODE_SELECTION){
            color = R.color.action_bar_bg_blue;
            getMenuInflater().inflate(R.menu.my_paint_selection, myMenu);
        }else{
            getMenuInflater().inflate(R.menu.my_paint, myMenu);
            changeIcon((MenuItem)myMenu.findItem(R.id.menu_draw));
            cvsView.setPen();
        }
        Drawable bgDrawable = getApplicationContext().getResources().getDrawable(color);
        actionBar.setBackgroundDrawable(bgDrawable);
    }

    public void setTestMode(){
        Intent intent = getIntent();
        testMode = Integer.parseInt(intent.getStringExtra("MODE"));

        cvsView.myinit();
    }
}
