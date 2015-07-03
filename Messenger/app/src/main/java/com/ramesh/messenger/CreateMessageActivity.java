package com.ramesh.messenger;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;


public class CreateMessageActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_message);
    }

    public void onClickSendMessage(View view) {
        EditText textViewId = (EditText) findViewById(R.id.editTextBox);
        String msg = textViewId.getText().toString();

        //Intent intent = new Intent(this, ReceiveMessageActivity.class);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(intent);
    }
}
