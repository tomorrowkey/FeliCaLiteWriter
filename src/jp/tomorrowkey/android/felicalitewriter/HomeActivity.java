/*
 * Copyright 2012 tomorrowkey@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jp.tomorrowkey.android.felicalitewriter;

import android.app.Activity;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import jp.tomorrowkey.android.felicalitewriter.ndef.UriNdefBuilder;

public class HomeActivity extends Activity {

    public static final String LOG_TAG = HomeActivity.class.getSimpleName();

    private EditText mUrlEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initUrlEditText();
        initOKButton();
    }

    private void initUrlEditText() {
        mUrlEditText = (EditText)findViewById(R.id.url_edittext);
        mUrlEditText.setText("");
    }

    private void initOKButton() {
        Button button = (Button)findViewById(R.id.ok_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                requestMoveToWriteActivity();
            }
        });
    }

    private void requestMoveToWriteActivity() {
        String urlString = mUrlEditText.getText().toString();
        if (TextUtils.isEmpty(urlString)) {
            Toast.makeText(getApplicationContext(), R.string.invalid_url, Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        UriNdefBuilder builder = new UriNdefBuilder(urlString);
        NdefMessage ndefMessage = builder.build();
        performMoveToWriteActivity(ndefMessage);
    }

    private void performMoveToWriteActivity(NdefMessage ndefMessage) {
        Intent intent = new Intent(this, WriteActivity.class);
        intent.putExtra(WriteActivity.EXTRA_NDEF_MESSAGE, ndefMessage);
        startActivity(intent);
    }

}
