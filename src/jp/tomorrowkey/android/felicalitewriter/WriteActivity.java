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
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;

import jp.tomorrowkey.android.felicalitewriter.felicalite.FeliCaLiteTag;
import jp.tomorrowkey.android.felicalitewriter.felicalite.FeliCaLiteTag.SizeOverflowException;
import jp.tomorrowkey.android.felicalitewriter.felicalite.FeliCaLiteTag.UnsupportTagException;

public class WriteActivity extends Activity {

    public static final String LOG_TAG = WriteActivity.class.getSimpleName();

    public static final String EXTRA_NDEF_MESSAGE = "ndef_message";

    private NfcAdapter mNfcAdapter;

    private NdefMessage mNdefMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write);

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        mNdefMessage = (NdefMessage)extras.getParcelable(EXTRA_NDEF_MESSAGE);

        if (mNdefMessage == null) {
            Log.w(LOG_TAG, "not found NdefMessage object in intent extra");
            finish();
            return;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(getApplicationContext(), "not found NFC feature", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getApplicationContext(), "NFC feature is not available",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
                getClass()), 0);
        IntentFilter[] intentFilter = new IntentFilter[] {
            new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED),
        };
        String[][] techList = new String[][] {
            {
                android.nfc.tech.NfcF.class.getName()
            }
        };
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilter, techList);

    }

    @Override
    public void onPause() {
        super.onPause();

        mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {

        Tag tag = (Tag)intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        byte[] idm = intent.getByteArrayExtra(NfcAdapter.EXTRA_ID);

        FeliCaLiteTag felicaLiteTag = null;
        try {
            felicaLiteTag = new FeliCaLiteTag(tag);
        } catch (UnsupportTagException e) {
            Toast.makeText(getApplicationContext(), "this is not felica tag", Toast.LENGTH_SHORT)
                    .show();
            return;
        }

        try {
            felicaLiteTag.applyNdefFlag(idm, true);
        } catch (TagLostException e) {
            Log.e(LOG_TAG, "TagLostException", e);
            Toast.makeText(getApplicationContext(), "TagLostException", Toast.LENGTH_SHORT).show();
            return;
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException", e);
            Toast.makeText(getApplicationContext(), "IOException", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Log.d(LOG_TAG, "write ndef message");
            felicaLiteTag.writeNdefMessage(idm, mNdefMessage);

        } catch (TagLostException e) {
            Log.e(LOG_TAG, "TagLostException", e);
            return;
        } catch (IOException e) {
            Log.e(LOG_TAG, "IOException", e);
            return;
        } catch (SizeOverflowException e) {
            Toast.makeText(getApplicationContext(), "size over", Toast.LENGTH_SHORT).show();
            Log.w(LOG_TAG, e.getMessage());
            return;
        }

        Toast.makeText(getApplicationContext(), R.string.wrote_ndef, Toast.LENGTH_LONG).show();
    }
}
