package com.bjsdm.mybutterknife;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bjsdm.my_annotations.MyBindView;
import com.bjsdm.my_reflection.MyButterKnife;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {
    @MyBindView(R.id.tv_content)
    TextView tvContent;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MyButterKnife.bind(this);
        tvContent.setText("修改成功！");
    }
}