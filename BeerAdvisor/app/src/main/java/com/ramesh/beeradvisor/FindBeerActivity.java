package com.ramesh.beeradvisor;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.List;

public class FindBeerActivity extends Activity {
    private BeerExpert expert = new BeerExpert();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_find_beer);
    }
    public void onClickFindBeer(View view){
        TextView textView = (TextView) findViewById(R.id.brands);
        Spinner spinner = (Spinner) findViewById(R.id.color_spinner);
        String beerType = String.valueOf(spinner.getSelectedItem());

        List<String> brandNameList = expert.getBrands(beerType);
        StringBuilder formattedBrandList = new StringBuilder();

        for(String str : brandNameList) {
            formattedBrandList.append(str).append("\n");
        }
        textView.setText(formattedBrandList);
    }
}