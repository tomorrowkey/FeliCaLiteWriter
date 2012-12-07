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

package jp.tomorrowkey.android.felicalitewriter.ndef;

import org.apache.http.util.ByteArrayBuffer;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * RTD-URI のNDEFメッセージを作成するクラス
 * 
 * @author tomorrowkey@gmail.com
 */
public class UriNdefBuilder {

    /**
     * プロトコルリスト
     */
    private static List<String> sProtocolList;
    static {
        sProtocolList = new ArrayList<String>();
        sProtocolList.add("");
        sProtocolList.add("http://www.");
        sProtocolList.add("https://www.");
        sProtocolList.add("http://");
        sProtocolList.add("https://");
        sProtocolList.add("tel:");
        sProtocolList.add("mailto:");
        sProtocolList.add("ftp://anonymous:anonymous@");
        sProtocolList.add("ftp://ftp.");
        sProtocolList.add("ftps://");
        sProtocolList.add("sftp://");
        sProtocolList.add("smb://");
        sProtocolList.add("nfs://");
        sProtocolList.add("ftp://");
        sProtocolList.add("dav://");
        sProtocolList.add("news:");
        sProtocolList.add("telnet://");
        sProtocolList.add("imap:");
        sProtocolList.add("rtsp://");
        sProtocolList.add("urn:");
        sProtocolList.add("pop:");
        sProtocolList.add("sip:");
        sProtocolList.add("sips:");
        sProtocolList.add("tftp:");
        sProtocolList.add("btspp://");
        sProtocolList.add("btl2cap://");
        sProtocolList.add("btgoep://");
        sProtocolList.add("tcpobex://");
        sProtocolList.add("irdaobex://");
        sProtocolList.add("file://");
        sProtocolList.add("urn:epc:id:");
        sProtocolList.add("urn:epc:tag:");
        sProtocolList.add("urn:epc:pat:");
        sProtocolList.add("urn:epc:raw:");
        sProtocolList.add("urn:epc:");
        sProtocolList.add("urn:nfc:");
    }

    /**
     * 文字列のUri
     */
    private String mUriString;

    public UriNdefBuilder(String uriString) {
        if (uriString == null || uriString.length() == 0)
            throw new IllegalArgumentException();

        mUriString = uriString;
    }

    /**
     * NDEFメッセージを組み立てます
     * 
     * @return
     */
    public NdefMessage build() {
        try {
            int index = getProtocolIndex(mUriString);
            String protocol = sProtocolList.get(index);

            String uriBody = mUriString.replace(protocol, "");
            byte[] uriBodyBytes = uriBody.getBytes("UTF-8");

            ByteArrayBuffer buffer = new ByteArrayBuffer(1 + uriBody.length());
            buffer.append((byte)index);
            buffer.append(uriBodyBytes, 0, uriBodyBytes.length);

            byte[] payload = buffer.toByteArray();
            NdefMessage message = new NdefMessage(new NdefRecord[] {
                new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_URI, new byte[0], payload)
            });

            return message;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * プロトコルリストに対応するプロトコルが存在すれば、indexを返します
     * 
     * @param uriString
     * @return
     */
    private int getProtocolIndex(String uriString) {
        String protocol;
        for (int i = 1; i < sProtocolList.size(); i++) {
            protocol = sProtocolList.get(i);
            if (uriString.startsWith(protocol)) {
                return i;
            }
        }
        return 0;
    }
}
