package com.tbuonomo.dotsindicatorsample;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.tbuonomo.viewpagerdotsindicator.DotsIndicator;
import com.tbuonomo.viewpagerdotsindicator.SpringDotsIndicator;
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.activity_main);

    DotsIndicator dotsIndicator = findViewById(R.id.dots_indicator);
    SpringDotsIndicator springDotsIndicator = findViewById(R.id.spring_dots_indicator);
    WormDotsIndicator wormDotsIndicator = findViewById(R.id.worm_dots_indicator);

    ViewPager viewPager = findViewById(R.id.view_pager);
    DotIndicatorPagerAdapter adapter = new DotIndicatorPagerAdapter();
    viewPager.setAdapter(adapter);
    viewPager.setPageTransformer(true, new ZoomOutPageTransformer());

      List<Integer> colorsList = new ArrayList<>();
      colorsList.add(Color.BLUE);
      colorsList.add(Color.GREEN);
      colorsList.add(Color.RED);
      colorsList.add(Color.MAGENTA);

      wormDotsIndicator.setColorsList(colorsList);

    dotsIndicator.setViewPager(viewPager);
    springDotsIndicator.setViewPager(viewPager);
    wormDotsIndicator.setViewPager(viewPager);

      Button button = findViewById(R.id.button);

    button.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(MainActivity.this, RecyclerActivity.class);
            startActivity(intent);
        }
    });
  }
}
