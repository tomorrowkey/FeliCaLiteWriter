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

package jp.tomorrowkey.android.felicalitewriter.felicalite;

import android.nfc.NdefMessage;
import android.nfc.Tag;
import android.nfc.TagLostException;
import android.nfc.tech.NfcF;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author tomorrowkey@gmail.com
 */
public class FeliCaLiteTag {

    protected static final String LOG_TAG = FeliCaLiteTag.class.getSimpleName();

    /**
     * 書き込みコマンド
     */
    private static final byte WRITE_WITHOUT_ENCRYPTION = (byte)0x08;

    /**
     * タグ
     */
    private NfcF mNfcF;

    /**
     * TODO UnsupportTagException が発生するパターンに、FeliCa Lite判定を追加する
     * 
     * @param tag
     * @throws UnsupportTagException FeliCa系のタグでない場合に発生します
     */
    public FeliCaLiteTag(Tag tag) throws UnsupportTagException {
        if (tag == null)
            throw new IllegalArgumentException();

        mNfcF = NfcF.get(tag);

        if (mNfcF == null)
            throw new UnsupportTagException();
    }

    /**
     * SYS_OPのNDEFフラグを変更します。<br>
     * 大部分にマジックナンバーを使っているため0次発行の場合のみで動くという制限があります <br>
     * FIXME SYS_OP 以外はタグから現在の値を取得してその値を使うようにしないと書き込みエラーが発生する場合がある
     * 
     * @param isNdef true にした場合NDEF化される。false にした場合NDEFではなくなる。
     * @throws IOException
     * @throws TagLostException
     */
    public void applyNdefFlag(byte[] idm, boolean isNdef) throws TagLostException, IOException {
        // @formatter:off
        byte[] data = new byte[] {
                (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0x01, (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, 
            };
        // @formatter:on
        data[3] = isNdef ? (byte)0x01 : (byte)0;

        // FIXME レスポンスを握りつぶしているので、どうにかする
        writeWithoutEncryption(idm, 0x88, data);
    }

    /**
     * NdefMessageを書き込みます
     * 
     * @param idm IDm
     * @param ndefMessage NDEF
     * @throws SizeOverflowException NdefMessageのサイズが大きすぎる場合に発生します
     * @throws TagLostException
     * @throws IOException
     */
    public void writeNdefMessage(byte[] idm, NdefMessage ndefMessage) throws SizeOverflowException,
            TagLostException, IOException {
        if (idm == null || idm.length == 0)
            throw new IllegalArgumentException();
        if (ndefMessage == null)
            throw new IllegalArgumentException();

        byte[][] datas = mappingBlock(ndefMessage);

        for (int blockNumber = 0; blockNumber <= 13; blockNumber++) {
            // FIXME レスポンスを握りつぶしているので、どうにかする
            writeWithoutEncryption(idm, blockNumber, datas[blockNumber]);
        }
    }

    /**
     * NdefMessageからFeliCa Liteの各ブロックにマッピングします
     * 
     * @param ndefMessage
     * @return
     * @throws SizeOverflowException
     * @throws IOException
     */
    private byte[][] mappingBlock(NdefMessage ndefMessage) throws SizeOverflowException,
            IOException {
        byte[] ndefMessageBytes = ndefMessage.toByteArray();
        int ndefMessageBytesLength = ndefMessageBytes.length;
        int blockCount = (int)Math.ceil(ndefMessageBytesLength / 16.0);
        if (blockCount > 13)
            throw new SizeOverflowException(ndefMessageBytesLength, 16 * 13);

        byte[][] datas = new byte[14][16];
        datas[0] = createNdefHeader(ndefMessageBytesLength);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(ndefMessageBytes);
        try {
            inputStream = new ByteArrayInputStream(ndefMessageBytes);
            int readLength;
            for (int i = 1; i < datas.length; i++) {
                readLength = inputStream.read(datas[i]);
                if (readLength == -1)
                    break;
            }
        } finally {
            try {
                if (inputStream != null)
                    inputStream.close();
            } catch (IOException e) {
                // ignore
            }
        }

        return datas;
    }

    private byte[] createNdefHeader(int ndefLength) {
        ByteBuffer buffer = ByteBuffer.allocate(16);

        // Ver
        buffer.put((byte)0x10);

        // Nbr
        // Read Without Encrypitonで一度に読めるブロック数を指定します
        // FeliCa Liteなので、一度に4ブロック読み込める
        buffer.put((byte)0x04);

        // Nbw
        // Write Without Encryptionで一度に書き込めるブロック数を指定します
        // FeliCa Liteなので、一度に1ブロック書き込める
        buffer.put((byte)0x01);

        // Nmaxb
        // NDEFとして使用できるブロック数
        // FeliCa Liteなので、データ領域は13ブロックまで
        buffer.put((byte)0x00);
        buffer.put((byte)0x0d);

        // unused
        buffer.put((byte)0x00);
        buffer.put((byte)0x00);
        buffer.put((byte)0x00);
        buffer.put((byte)0x00);

        // WriteF
        // 一枚で完結しているので、0x00
        buffer.put((byte)0x00);

        // RW Flag
        // Read Writeなので0x01
        buffer.put((byte)0x01);

        // Ln
        // NDEFデータの長さを指定します
        buffer.put((byte)((ndefLength >>> 16) & 0xff));
        buffer.put((byte)((ndefLength >>> 8) & 0xff));
        buffer.put((byte)(ndefLength & 0xff));

        // Checksum
        // チェックサムを指定します
        buffer.put(checksum(buffer.array()));

        return buffer.array();
    }

    /**
     * チェックサムを作成します<br>
     * すべてのバイト配列の合計を計算します
     * 
     * @param byteArray
     * @return
     */
    private byte[] checksum(byte[] byteArray) {
        int sum = 0;
        for (byte b : byteArray) {
            sum += b & 0xff;
        }
        return new byte[] {
                (byte)((sum >>> 8) & 0xff), (byte)(sum & 0xff)
        };
    }

    /**
     * Write Without Encryptionコマンドを発行します<br>
     * FeliCa Liteなので、1度のコマンド発行で1ブロックだけ書き込めます
     * 
     * @param idm IDm
     * @param blockNumber ブロック番号
     * @param data 書き込みデータ
     * @return レスポンス
     * @throws TagLostException
     * @throws IOException
     */
    public byte[] writeWithoutEncryption(byte[] idm, int blockNumber, byte[] data)
            throws TagLostException, IOException {
        if (idm == null || idm.length == 0)
            throw new IllegalArgumentException();

        ByteBuffer byteBuffer = ByteBuffer.allocate(31);

        // Write Without Encryption
        byteBuffer.put(WRITE_WITHOUT_ENCRYPTION);

        // IDm
        byteBuffer.put(idm);

        // サービス数
        // FeliCa Liteなので1に固定
        byteBuffer.put((byte)0x01);

        // サービスコード（リトルエンディアン）
        // 0x00 0x09
        byteBuffer.put((byte)0x09);
        byteBuffer.put((byte)0x00);

        // ブロック数
        // FeliCa Liteなので1に固定
        byteBuffer.put((byte)0x01);

        // ブロックリスト
        // 長さ 2Byteなので1b
        // アクセスモード FeliCa Liteなので000bに固定
        // サービスコード順番 FeliCa Liteなので0000bに固定
        // ブロック番号 引数から指定
        byteBuffer.put((byte)0x80);
        byteBuffer.put((byte)blockNumber);

        // 書き込みデータ
        byteBuffer.put(data);

        byte[] command = byteBuffer.array();
        byte[] response = executeCommand(command);

        return response;
    }

    /**
     * コマンドを実行します<br>
     * 自動的に先頭にコマンド長を付加します
     * 
     * @param command
     * @return
     * @throws TagLostException
     * @throws IOException
     */
    public byte[] executeCommand(byte[] command) throws TagLostException, IOException {
        if (command == null || command.length == 0)
            throw new IllegalArgumentException();

        int commandLength = command.length;
        byte[] rawCommand = new byte[commandLength + 1];
        int rawCommandLength = rawCommand.length;
        rawCommand[0] = (byte)rawCommandLength;
        System.arraycopy(command, 0, rawCommand, 1, commandLength);

        return executeRawCommand(rawCommand);
    }

    /**
     * コマンドを渡された状態そのままで実行します
     * 
     * @param rawCommand
     * @return
     * @throws TagLostException
     * @throws IOException
     */
    public byte[] executeRawCommand(byte[] rawCommand) throws TagLostException, IOException {
        if (rawCommand == null || rawCommand.length == 0)
            throw new IllegalArgumentException();

        try {
            mNfcF.connect();
            byte[] response = mNfcF.transceive(rawCommand);
            return response;
        } finally {
            try {
                mNfcF.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * 対応していないタグを渡された際に発生する例外です
     * 
     * @author tomorrowkey@gmail.com
     */
    public static class UnsupportTagException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    /**
     * タグに書き込めるサイズを越えた際に発生する例外です
     * 
     * @author tomorrowkey@gmail.com
     */
    public static class SizeOverflowException extends Exception {

        private static final long serialVersionUID = 1L;

        private int mSize;

        private int mCapacitySize;

        public SizeOverflowException(int size, int capacitySize) {
            mSize = size;
            mCapacitySize = capacitySize;
        }

        @Override
        public String getMessage() {
            return "size over, size=" + mSize + ", capacity=" + mCapacitySize;
        }

        public int getSize() {
            return mSize;
        }

        public int getCapacity() {
            return mCapacitySize;
        }
    }
}
