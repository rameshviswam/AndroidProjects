package com.ramesh.messenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


public class ReceiveMessageActivity extends Activity {

    public static final String EXTRA_MSG="message";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_message);

        Intent intent = getIntent();
        String textViewMsg = intent.getStringExtra(EXTRA_MSG);

        TextView textView = (TextView) findViewById(R.id.receiveMessageTextBox);
        textView.setText(textViewMsg);
    }
}
