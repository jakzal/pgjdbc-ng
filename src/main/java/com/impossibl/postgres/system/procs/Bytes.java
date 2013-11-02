/**
 * Copyright (c) 2013, impossibl.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of impossibl.com nor the names of its contributors may
 *    be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.impossibl.postgres.system.procs;

import com.impossibl.postgres.system.Context;
import com.impossibl.postgres.types.PrimitiveType;
import com.impossibl.postgres.types.Type;
import static com.impossibl.postgres.system.Settings.FIELD_VARYING_LENGTH_MAX;
import static com.impossibl.postgres.types.PrimitiveType.Binary;

import java.io.IOException;
import static java.lang.Math.min;

import org.jboss.netty.buffer.ChannelBuffer;

public class Bytes extends SimpleProcProvider {

  public static final BinDecoder BINARY_DECODER = new BinDecoder();
  public static final BinEncoder BINARY_ENCODER = new BinEncoder();

  public Bytes() {
    super(new TxtEncoder(), new TxtDecoder(), new BinEncoder(), new BinDecoder(), "bytea");
  }

  static class BinDecoder extends BinaryDecoder {

    public PrimitiveType getInputPrimitiveType() {
      return Binary;
    }

    public Class<?> getOutputType() {
      return byte[].class;
    }

    public byte[] decode(Type type, ChannelBuffer buffer, Context context) throws IOException {

      int length = buffer.readInt();
      if (length == -1) {
        return null;
      }

      byte[] bytes;

      Integer maxLength = (Integer) context.getSetting(FIELD_VARYING_LENGTH_MAX);
      if (maxLength != null) {
        bytes = new byte[min(maxLength, length)];
      }
      else {
        bytes = new byte[length];
      }

      buffer.readBytes(bytes);
      buffer.skipBytes(length - bytes.length);

      return bytes;
    }

  }

  static class BinEncoder extends BinaryEncoder {

    public Class<?> getInputType() {
      return byte[].class;
    }

    public PrimitiveType getOutputPrimitiveType() {
      return Binary;
    }

    public void encode(Type type, ChannelBuffer buffer, Object val, Context context) throws IOException {

      if (val == null) {

        buffer.writeInt(-1);
      }
      else {

        byte[] bytes = (byte[]) val;
  static class TxtDecoder extends TextDecoder {

    @Override
    public PrimitiveType getInputPrimitiveType() {
      return Binary;
    }

    @Override
    public Class<?> getOutputType() {
      return InputStream.class;
    }

    @Override
    public InputStream decode(Type type, CharSequence buffer, Context context) throws IOException {

      byte[] data;

      if (buffer.charAt(0) == '\'' && buffer.charAt(1) == 'x') {
        data = decodeHex(buffer.subSequence(2, buffer.length()));
      }
      else {
        data = decodeEscape(buffer);
      }

      return new ByteArrayInputStream(data);
    }

    byte[] decodeHex(CharSequence buffer) {

      int length = buffer.length();
      byte[] data = new byte[length / 2];
      for (int i = 0; i < length; i += 2) {
        data[i / 2] = (byte) ((Character.digit(buffer.charAt(i), 16) << 4) + Character.digit(buffer.charAt(i + 1), 16));
      }
      return data;
    }

    byte[] decodeEscape(CharSequence buffer) {

      int length = buffer.length();
      byte[] data = new byte[length];
      int out = 0;
      for (int i = 0; i < length; ++i) {

        char ch = buffer.charAt(i);
        switch (ch) {

          case '\\':
            char ch1 = buffer.charAt(++i);
            switch (ch1) {
              case '\\':
                data[++out] = '\\';

              default:
                char ch2 = buffer.charAt(++i);
                char ch3 = buffer.charAt(++i);
                data[++out] = (byte) (ch1 * 64 + ch2 * 8 + ch3);
            }

          default:
            data[++out] = (byte) ch;
        }

      }

      return Arrays.copyOf(data, out);
    }

  }

  static class TxtEncoder extends TextEncoder {

    @Override
    public Class<?> getInputType() {
      return InputStream.class;
    }

    @Override
    public PrimitiveType getOutputPrimitiveType() {
      return Binary;
    }

    private static final char[] hexArray = "0123456789ABCDEF".toCharArray();

    @Override
    public void encode(Type type, StringBuilder buffer, Object val, Context context) throws IOException {

      byte[] bytes = (byte[]) val;

      char[] hexChars = new char[bytes.length * 2];
      int v;
      for (int j = 0; j < bytes.length; j++) {
        v = bytes[j] & 0xFF;
        hexChars[j * 2] = hexArray[v >>> 4];
        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
      }

      buffer.append('\\').append('x').append(hexChars);
    }

  }

}
