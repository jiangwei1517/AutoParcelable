package com.jiangwei.autoparcelable;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private Button btn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Person$$Parcelable parcelable = new Person$$Parcelable();
                if (parcelable instanceof Parcelable) {
                    Toast.makeText(MainActivity.this, "Person对象已经序列化完成", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
