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

package jp.tomorrowkey.android.felicalitewriter.util;

/**
 * byte型に関するユーティリティ
 * 
 * @author tomorrowkey@gmail.com
 */
public class ByteUtil {

    /**
     * byte配列を16進表記の文字列に変換する<br>
     * byteから16進表記の変換については{@code Util#byteToString(byte)} を利用する.<br>
     * 各文字列の連結には'-'を使用する.
     * 
     * @param bytes byte配列
     * @return
     */
    public static String byteArrayToString(byte[] bytes) {
        StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (byte b : bytes) {
            if (isFirst) {
                isFirst = false;
            } else {
                buffer.append("-");
            }
            buffer.append(byteToString(b));
        }

        return buffer.toString();
    }

    /**
     * byteを16進表記の文字列に変換する.<br>
     * 2桁に満たない場合は、先頭に0を補完する.
     * 
     * @param b byte
     * @return
     */
    public static String byteToString(byte b) {
        String s = Integer.toHexString(b & 0xff).toUpperCase();
        if (s.length() > 1) {
            return s;
        } else {
            return "0" + s;
        }
    }

}
