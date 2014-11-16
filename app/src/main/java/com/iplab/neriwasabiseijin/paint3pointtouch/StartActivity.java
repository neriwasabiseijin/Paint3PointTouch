package com.iplab.neriwasabiseijin.paint3pointtouch;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;


public class StartActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);


        Button btn = (Button)findViewById(R.id.button_Paint);
        btn.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                // インテントのインスタンス生成
                Intent intent = new Intent(StartActivity.this, PaintActivity.class);

                // 値引き渡しの設定
                // intent.putExtra("タグ", 値);

                // 次の画面のアクティビティ起動
                startActivity(intent);

                // 現在のアクティビティ終了
                StartActivity.this.finish();
            }

        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
