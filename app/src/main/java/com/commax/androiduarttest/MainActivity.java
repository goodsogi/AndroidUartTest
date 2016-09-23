package com.commax.androiduarttest;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import ioio.lib.api.IOIO;
import ioio.lib.api.Uart;

public class MainActivity extends AppCompatActivity {


    private final IOIO ioio_ = null;
    private final int pin1_ = 0;
    private final int pin2_ = 0;
    private int bytesVerified_;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            openUart(1, 2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean openUart(int inPin, int outPin)
            throws Exception {
        if (outPin == 9) {
            // pin 9 doesn't support peripheral output
            return true;
        }
        final int BYTE_COUNT = 2000;
        final int SEED = 17;

        Uart uart = ioio_.openUart(inPin, outPin, 9600, Uart.Parity.NONE,
                Uart.StopBits.ONE);
        InputStream in = uart.getInputStream();
        OutputStream out = uart.getOutputStream();
        Random rand = new Random(SEED);
        bytesVerified_ = 0;
        Thread reader = new ReaderThread(in, SEED, BYTE_COUNT);
        reader.start();
        try {
            for (int i = 0; i < BYTE_COUNT; ++i) {
                byte value = (byte) rand.nextInt();
                out.write(value);
            }
            reader.join();
        } catch (Exception e) {
            try {
                in.close();
            } catch (IOException e1) {
            }
            reader.interrupt();
            reader.join();
            throw e;
        } finally {
            uart.close();
        }
        if (bytesVerified_ != BYTE_COUNT) {
            Log.w("IOIOTortureTest", "Failed UartTest input: " + inPin
                    + ", output: " + outPin + ". Bytes passed: "
                    + bytesVerified_);
            return false;
        }
        return true;
    }

    class ReaderThread extends Thread {
        private InputStream in_;
        private Random rand_;
        private int count_;

        public ReaderThread(InputStream in, int seed, int count) {
            in_ = in;
            rand_ = new Random(seed);
            count_ = count;
        }

        @Override
        public void run() {
            super.run();
            try {
                while (count_-- > 0) {
                    int expected = rand_.nextInt() & 0xFF;
                    int read = in_.read();
                    if (read != expected) {
                        Log.e("IOIOTortureTest", "Expected: " + expected + " got: " + read);
                        return;
                    } else {
                        bytesVerified_++;
                    }
                }
            } catch (IOException e) {
            }
        }
    }
}
