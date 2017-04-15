package com.example.butterfly.maps;

import android.util.Log;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.ArrayUtils;

import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

class MyCryptography {
    private byte[] X_bytes = new byte[32];
    private String H_value = "B194BAC80A08F53B366D008E584A5DE48504FA9D1BB6C7AC252E72C202FDCE0D";
    private byte[] H_TABLE = new byte[]{
            -79, -108, -70, -56, 10, 8, -11, 59, 54, 109, 0, -114, 88, 74, 93, -28, -123, 4, -6, -99, 27, -74, -57, -84,
            37, 46, 114, -62, 2, -3, -50, 13, 91, -29, -42, 18, 23, -71, 97, -127, -2, 103, -122, -83, 113, 107, -119,
            11, 92, -80, -64, -1, 51, -61, 86, -72, 53, -60, 5, -82, -40, -32, 127, -103, -31, 43, -36, 26, -30, -126,
            87, -20, 112, 63, -52, -16, -107, -18, -115, -15, -63, -85, 118, 56, -97, -26, 120, -54, -9, -58, -8, 96,
            -43, -69, -100, 79, -13, 60, 101, 123, 99, 124, 48, 106, -35, 78, -89, 121, -98, -78, 61, 49, 62, -104, -75,
            110, 39, -45, -68, -49, 89, 30, 24, 31, 76, 90, -73, -109, -23, -34, -25, 44, -113, 12, 15, -90, 45, -37, 73,
            -12, 111, 115, -106, 71, 6, 7, 83, 22, -19, 36, 122, 55, 57, -53, -93, -125, 3, -87, -117, -10, -110, -67,
            -101, 28, -27, -47, 65, 1, 84, 69, -5, -55, 94, 77, 14, -14, 104, 32, -128, -86, 34, 125, 100, 47, 38, -121,
            -7, 52, -112, 64, 85, 17, -66, 50, -105, 19, 67, -4, -102, 72, -96, 42, -120, 95, 25, 75, 9, -95, 126, -51,
            -92, -48, 21, 68, -81, -116, -91, -124, 80, -65, 102, -46, -24, -118, -94, -41, 70, 82, 66, -88, -33, -77,
            105, 116, -59, 81, -21, 35, 41, 33, -44, -17, -39, -76, 58, 98, 40, 117, -111, 20, 16, -22, 119, 108, -38, 29
    };
    private byte[] s_bytes = new byte[16];
    private byte[] h_bytes;

    MyCryptography() {
//    static {
        try {
            h_bytes = Hex.decodeHex(H_value.toCharArray());
        } catch (DecoderException e) {
            Log.d("ERROR", e.getMessage());
        }
//    }
    }

    private byte MASK = (byte) Integer.parseInt("11111111", 2);
    private byte[] a_X1, b_X2, c_X3, d_X4, Y;
    private long Two_In32 = 4294967296L, Two_In24 = 16777216L, Two_In16 = 65536L, Two_In8 = 256L;
    private int T;
    private int flag;
    private long length;

    /**
     * calculate hash-value
     * @return hash string
     */
    public String getHash(String videoFile) throws DecoderException {
        RandomAccessFile file = null;
        long point = 0;
        try {
            file = new RandomAccessFile(videoFile, "rw");
            length = file.length();
            point = ((int) (length / 32) * 32);
            while (file.getFilePointer() != length) {
                if (file.getFilePointer() == point) {
                    flag = file.read(X_bytes = new byte[(int) (length - file.getFilePointer())]);
                    madeMultiple();
                } else {
                    flag = file.read(X_bytes);
                }
                s_bytes = XOR(s_bytes, getDisplay1(ArrayUtils.addAll(X_bytes, h_bytes)));
                h_bytes = getDisplay2(ArrayUtils.addAll(X_bytes, h_bytes));
            }

        } catch (Throwable e) {
            e.printStackTrace();
        }
        long fileLength = length * 8;
        Y = getDisplay2(ArrayUtils.addAll(getWordForFileLength(fileLength), ArrayUtils.addAll(s_bytes, h_bytes)));
        String result = bytesToHex(Y);
        return result;

    }

    final private char[] hexArray = "0123456789ABCDEF".toCharArray();
    public String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private void madeMultiple() {
        T = (X_bytes.length % 32);
        for (int i = 0; i < (32 - T); i++) {
            X_bytes = Arrays.copyOf(X_bytes, 32);
        }
    }

    /**
     * display 1 𝜎1(𝑢) = 𝐹𝑢1||𝑢2(𝑢3 ⊕ 𝑢4) ⊕ 𝑢3 ⊕ 𝑢4
     * XOR - ⊕
     * @param inputWord - input binary word (length 512 bit)
     * @return binary word (length 128 bit)
     */
    private byte[] getDisplay1(byte[] inputWord) {
        byte[] u1_d1 = Arrays.copyOfRange(inputWord, 0, 16);
        byte[] u2_d1 = Arrays.copyOfRange(inputWord, 16, 32);
        byte[] u3_d1 = Arrays.copyOfRange(inputWord, 32, 48);
        byte[] u4_d1 = Arrays.copyOfRange(inputWord, 48, 64);
        byte[] encrypt_result = encrypt(ArrayUtils.addAll(u1_d1, u2_d1), XOR(u3_d1, u4_d1));

        return XOR((XOR(encrypt_result, u3_d1)), u4_d1);
    }

    /**
     * display 2 𝜎2(𝑢) = (𝐹𝜃1(𝑢1) ⊕ 𝑢1) || (𝐹𝜃2(𝑢2) ⊕ 𝑢2)
     * @param inputWord - input binary word (length 512 bit)
     * @return binary word (length 256 bit)
     */
    private byte[] getDisplay2(byte[] inputWord) {
        byte[] u1 = Arrays.copyOfRange(inputWord, 0, 16);
        byte[] u2 = Arrays.copyOfRange(inputWord, 16, 32);
        byte[] key_1 = ArrayUtils.addAll(getDisplay1(inputWord), Arrays.copyOfRange(inputWord, 48, 64));//u4
        byte[] key_2 = ArrayUtils.addAll(XORWithOnes(getDisplay1(inputWord)), Arrays.copyOfRange(inputWord, 32, 48));//u3
        byte[] first_encrypt_path_result = XOR(encrypt(key_1, u1), u1);
        byte[] second_encrypt_path_result = XOR(encrypt(key_2, u2), u2);

        return ArrayUtils.addAll(first_encrypt_path_result, second_encrypt_path_result);
    }

    /**
     * addition on the module 2
     * @param u_bytes1 - bit sequence {0, 1}n
     * @param u_bytes2 - bit sequence {0, 1}n
     * @return bit sequence {0, 1}n
     */
    private byte[] XOR(byte[] u_bytes1, byte[] u_bytes2) {
        byte[] result = new byte[u_bytes1.length];
        for (int i = 0; i < u_bytes1.length; i++)
            result[i] = (byte) (u_bytes1[i] ^ u_bytes2[i]);

        return result;
    }

    private byte[] XORWithOnes(byte[] array) {
        byte[] result = new byte[array.length];
        int currentIndex;
        for (currentIndex = 0; currentIndex < array.length; currentIndex++)
            result[currentIndex] = (byte) (array[currentIndex] ^ MASK);

        return result;
    }

    /**
     * encryption algorithm block
     * word X = X1 || X2 || X3 || X4, where Xi ∈ {0, 1}32
     * key 𝜃 = 𝜃1 || 𝜃2 || ... || 𝜃8 ∈ {0, 1}32
     * a ← X1, b ← X2, c ← X3, d ← X4
     * b ← b ⊕ G5(a square_plus K(7i-6))
     * c ← c ⊕ G21(d square_plus K(7i-5))
     * a ← a square_minus G13(b square_plus K(7i-4))
     * e ← G21(b square_plus c square_plus K(7i-3)) ⊕ <i>32
     * b ← b square_plus e
     * c ← c square_minus e
     * d ← d square_plus G13(c square_plus K(7i-2))
     * b ← b ⊕ G21(a square_plus K(7i-1))
     * c ← c ⊕ G5(d square_plus K(7i))
     * a ↔ b
     * c ↔ d
     * b ↔ c
     * Y ← b || d || a || c
     * @param key - input binary key 𝜃 (length 256 bit)
     * @param value - input binary word 𝑋 (length 128 bit)
     * @return - output binary word 𝑌 (length 128 bit)
     */
    private byte[] encrypt(byte key[], byte[] value) {
        ArrayList<byte[]> K;
        a_X1 = Arrays.copyOfRange(value, 0, 4);
        b_X2 = Arrays.copyOfRange(value, 4, 8);
        c_X3 = Arrays.copyOfRange(value, 8, 12);
        d_X4 = Arrays.copyOfRange(value, 12, 16);
        byte[] e;
        byte[] buff;
        K = getK(key);
        for (int i = 1; i <= 8; i++) {
            b_X2 = XOR(b_X2, G(5, square_plus(a_X1, K.get((7 * i - 6 - 1)))));
            c_X3 = XOR(c_X3, G(21, square_plus(d_X4, K.get((7 * i - 5 - 1)))));
            a_X1 = square_minus(a_X1, G(13, square_plus(b_X2, K.get(7 * i - 4 - 1))));
            e = XOR(G(21, square_plus(K.get(7 * i - 3 - 1), square_plus(b_X2, c_X3))), getWord(i));
            b_X2 = square_plus(b_X2, e);
            c_X3 = square_minus(c_X3, e);
            d_X4 = square_plus(d_X4, G(13, square_plus(c_X3, K.get(7 * i - 2 - 1))));
            b_X2 = XOR(b_X2, G(21, square_plus(a_X1, K.get((7 * i - 1 - 1)))));
            c_X3 = XOR(c_X3, G(5, square_plus(d_X4, K.get((7 * i - 1)))));

            buff = a_X1;
            a_X1 = b_X2;
            b_X2 = buff;

            buff = c_X3;
            c_X3 = d_X4;
            d_X4 = buff;

            buff = b_X2;
            b_X2 = c_X3;
            c_X3 = buff;

        }

        return ArrayUtils.addAll(ArrayUtils.addAll(b_X2, d_X4), ArrayUtils.addAll(a_X1, c_X3));
    }

    /**
     * splitting key
     * 𝜃 = 𝜃1 || 𝜃2 || ... || 𝜃8 ∈ {0, 1}32
     * @param key - input binary key (length 256 bit)
     * @return tact key K1 = 𝜃1, K2 = 𝜃2, ..., K8 = 𝜃8, K9 = 𝜃1, K10 = 𝜃2, ..., K56 = 𝜃8
     */
    private ArrayList<byte[]> getK(byte key[]) {
        ArrayList<byte[]> K = new ArrayList<>();
        int pos = 0;
        for (int i = 0; i < 56; i++) {
            if (pos >= 8)
                pos = 0;
            K.add(Arrays.copyOfRange(key, pos * 4, (pos + 1) * 4));
            pos++;
        }

        return K;
    }

    /**
     * conversion
     * Gr: {0, 1}32 → {0, 1}32
     * equivalent word u = u1 || u2 || u3 || u4, ui ∈ {0, 1}32
     * to word Gr(u) = RotHi^r(H(u1) || H(u2) || H(u3) || H(u4))
     * @param r - 5, 13 or 21
     * @param value - input binary word (length 32 bit)
     * @return binary word (length 32 bit)
     */
    private byte[] G(int r, byte[] value) {

        return RotHi(r, value);
    }

    private int signedByteToInteger(byte b) {

        return b & 0xFF;
    }

    /**
     * equivalent of the word
     * @param value - bit sequence {0, 1}8n
     * @return 2^7*u1 + 2^6*u2 + ... + u8
     */
    private long getAccordance(byte[] value) {
        long acc = signedByteToInteger(value[0]);
        acc += signedByteToInteger(value[1]) * Two_In8;
        acc += signedByteToInteger(value[2]) * Two_In16;
        acc += signedByteToInteger(value[3]) * Two_In24;

        return acc;
    }

    private byte[] getWordForFileLength(long lg) {
        byte[] buff = ByteBuffer.allocate(8).putLong(lg).array();
        byte[] result = new byte[16];
        for (int i = 0; i < buff.length; i++)
            result[i] = buff[buff.length - i - 1];

        return result;
    }

    private byte[] getWord(long acc) {
        acc = acc % Two_In32;
        byte[] buffer = ByteBuffer.allocate(8).putLong(acc).array();

        return new byte[]{buffer[7], buffer[6], buffer[5], buffer[4]};
    }

    /**
     * square_plus
     * @param u - bit sequence {0, 1}8n
     * @param v - bit sequence (o, 1)8n
     * @return bit sequence <u` + v`>8n
     */
    private byte[] square_plus(byte[] u, byte[] v) {

        return getWord(getAccordance(u) + getAccordance(v));
    }

    /**
     * square_minus
     * u = v square_plus w
     * @param u - bit sequence {0, 1}8n
     * @param v - bit sequence (0, 1)8n
     * @return bit sequence w ∈ {0, 1}8n
     */
    private byte[] square_minus(byte[] u, byte[] v) {

        return getWord(getAccordance(u) - getAccordance(v));
    }

    /**
     * RotHI for u ∈ {0, 1}8n word ShHi(u) ⊕ ShLo^(8n-1)(u)
     * @param cycle - 5, 13 or 21
     * @param val - input binary word (length 32 bit)
     * @return - output binary word (length 32 bit)
     */
    private byte[] RotHi(int cycle, byte[] val) {
        byte[] b = new byte[]{H_TABLE[signedByteToInteger(val[0])],
                H_TABLE[signedByteToInteger(val[1])],
                H_TABLE[signedByteToInteger(val[2])],
                H_TABLE[signedByteToInteger(val[3])]};
        ByteBuffer byteBuffer = ByteBuffer.wrap(b);
        int buf = byteBuffer.order(ByteOrder.LITTLE_ENDIAN).getInt();
        buf = ((buf << cycle) | buf >>> (32 - cycle));
        byteBuffer = ByteBuffer.allocate(4).putInt(buf).order(ByteOrder.BIG_ENDIAN);

        return new byte[]{byteBuffer.get(3), byteBuffer.get(2), byteBuffer.get(1), byteBuffer.get(0)};
    }
}
