/*
 * Copyright 2016 The Android Open Source Project
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

package com.example.androidthings.loopback;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.google.android.things.pio.PeripheralManager;
import com.google.android.things.pio.UartDevice;
import com.google.android.things.pio.UartDeviceCallback;
import com.google.android.things.pio.Gpio;
import com.google.android.things.pio.Pwm;
import java.io.IOException;

/**
 * Example activity that provides a UART loopback on the
 * specified device. All data received at the specified
 * baud rate will be transferred back out the same UART.
 */
public class LoopbackActivity extends Activity {
    private static final String TAG = "LoopbackActivity";

    // UART Configuration Parameters
    private static final int BAUD_RATE = 115200;
    private static final int DATA_BITS = 8;
    private static final int STOP_BITS = 1;

    private static final int CHUNK_SIZE = 512;

    private HandlerThread mInputThread;
    private Handler mInputHandler;

    private UartDevice mLoopbackDevice;

    private static final int INTERVAL_MAIN_MS = 100;
    private int ExeState=0;
    private Handler mExeHandler = new Handler();

    private Runnable mExeRunnable = new Runnable() {
        @Override
        public void run() {
            try
            {
                switch (ExeState){
                    case 0:
                        writeUartData(mLoopbackDevice,"Welcame Lab 2\n\r");
                        writeUartData(mLoopbackDevice,"1) press 1 to run exe 1\n\r");
                        writeUartData(mLoopbackDevice,"2) press 2 to run exe 2\n\r");
                        writeUartData(mLoopbackDevice,"3) press 3 to run exe 3\n\r");
                        writeUartData(mLoopbackDevice,"4) press 4 to run exe 4\n\r");
                        writeUartData(mLoopbackDevice,"4) press 1 to run exe 5\n\r");
                        writeUartData(mLoopbackDevice,"4) press 0 to stop\n\r");
                        mHandler.removeCallbacks(pwnRunnable);
                        ExeState=6;
                        break;
                    case 1:
                        writeUartData(mLoopbackDevice,"exe 1 running\n\r");
                        break;
                    case 2:
                        writeUartData(mLoopbackDevice,"exe 2 running\n\r");
                        break;
                    case 3:
                        writeUartData(mLoopbackDevice,"exe 3 running\n\r");
                        mHandler.post(pwnRunnable);
                        ExeState =6;
                        break;
                    case 4:
                        writeUartData(mLoopbackDevice,"exe 4 running\n\r");
                        break;
                    case 5:
                        writeUartData(mLoopbackDevice,"exe 5 running\n\r");
                        break;
                    default:
                        //writeUartData(mLoopbackDevice,"Erro choose exe\n\r");
                        break;
                }
                mExeHandler.postDelayed(mExeRunnable, INTERVAL_MAIN_MS);
            }catch (IOException e){
                Log.e(TAG, "Error on mExeRunnable ", e);
            }

        }
    };


    private Runnable mTransferUartRunnable = new Runnable() {
        @Override
        public void run() {
            transferUartData();
        }
    };

    private static final int INTERVAL_BETWEEN_BLINKS_MS=1000;
    private Handler mHandler = new Handler();

    private Gpio mLedGpioBlue;
    private boolean mLedStateBlue = false;

    private Gpio mLedGpioGreen;
    private boolean mLedStateGreen = true;

    private Gpio mLedGpioRed;
    private boolean mLedStateRed = true;

    private int Switch_led = 0;

    private static final String PWM_NAME = "PWM1";
    private static final int PWM_DUTYCYCLE_TIMER = 30;
    private int PwmDutyCycle_val = 0;
    private Pwm mPwm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "Loopback Created");

        // Create a background looper thread for I/O
        mInputThread = new HandlerThread("InputThread");
        mInputThread.start();
        mInputHandler = new Handler(mInputThread.getLooper());

        // Attempt to access the UART device
        try {
            // PWM
            PeripheralManager manager;
            manager = PeripheralManager.getInstance();
            mPwm = manager.openPwm(PWM_NAME);
            mPwm.setPwmFrequencyHz(120);
            mPwm.setPwmDutyCycle(PwmDutyCycle_val);
            //Emable the PWM signal
            mPwm.setEnabled(true);


            mLedGpioBlue = PeripheralManager.getInstance().openGpio("BCM19");
            mLedGpioBlue.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioBlue.setValue(mLedStateBlue);
            //Log.d(TAG, "Start blinking LED Blue GPIO 6");

            mLedGpioGreen = PeripheralManager.getInstance().openGpio("BCM26");
            mLedGpioGreen.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioGreen.setValue(mLedStateGreen);
            //Log.d(TAG,"Start linking LED GREEN GPIO 13");

            mLedGpioRed = PeripheralManager.getInstance().openGpio("BCM6");
            mLedGpioRed.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
            mLedGpioRed.setValue(mLedStateRed);

            //mHandler.post(mBlinkRunnable);
            //mHandler.post(pwnRunnable);
            openUart(BoardDefaults.getUartName(), BAUD_RATE);
            mInputHandler.post(mTransferUartRunnable);
            mExeHandler.post(mExeRunnable);
        } catch (IOException e) {
            Log.e(TAG, "Unable to open UART device", e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Loopback Destroyed");


        // Terminate the worker thread
        if (mInputThread != null) {
            mInputThread.quitSafely();
        }

        // Attempt to close the UART device
        try {
            closeUart();
            mExeHandler.removeCallbacks(mExeRunnable);
            mHandler.removeCallbacks(pwnRunnable);
            mLedGpioBlue.close();
            mLedGpioGreen.close();
            mLedGpioRed.close();

        } catch (IOException e) {
            Log.e(TAG, "Error closing UART device:", e);
        }finally {
            mLedGpioBlue = null;
            mLedGpioGreen =null;
            mLedGpioRed = null;
        }

        if(mPwm != null){
            try {
                mPwm.close();
                mPwm = null;
            }catch (IOException e){
                Log.w(TAG,"Unable to close PWM",e);
            }
        }


    }

    /**
     * Callback invoked when UART receives new incoming data.
     */
    private UartDeviceCallback mCallback = new UartDeviceCallback() {
        @Override
        public boolean onUartDeviceDataAvailable(UartDevice uart) {
            // Queue up a data transfer
            //transferUartData();
            // Read available data from the UART device
            try {
                readUartBuffer(mLoopbackDevice);
            } catch (IOException e) {
                Log.w(TAG, "Unable to access UART device", e);
            }
            //Continue listening for more interrupts
            return true;
        }

        @Override
        public void onUartDeviceError(UartDevice uart, int error) {
            Log.w(TAG, uart + ": Error event " + error);
        }
    };

    /* Private Helper Methods */

    /**
     * Access and configure the requested UART device for 8N1.
     *
     * @param name Name of the UART peripheral device to open.
     * @param baudRate Data transfer rate. Should be a standard UART baud,
     *                 such as 9600, 19200, 38400, 57600, 115200, etc.
     *
     * @throws IOException if an error occurs opening the UART port.
     */
    private void openUart(String name, int baudRate) throws IOException {
        mLoopbackDevice = PeripheralManager.getInstance().openUartDevice(name);
        // Configure the UART
        mLoopbackDevice.setBaudrate(baudRate);
        mLoopbackDevice.setDataSize(DATA_BITS);
        mLoopbackDevice.setParity(UartDevice.PARITY_NONE);
        mLoopbackDevice.setStopBits(STOP_BITS);

        mLoopbackDevice.registerUartDeviceCallback(mInputHandler, mCallback);
    }

    /**
     * Close the UART device connection, if it exists
     */
    private void closeUart() throws IOException {
        if (mLoopbackDevice != null) {
            mLoopbackDevice.unregisterUartDeviceCallback(mCallback);
            try {
                mLoopbackDevice.close();
            } finally {
                mLoopbackDevice = null;
            }
        }
    }

    /**
     * Loop over the contents of the UART RX buffer, transferring each
     * one back to the TX buffer to create a loopback service.
     *
     * Potentially long-running operation. Call from a worker thread.
     */
    private void transferUartData() {
        if (mLoopbackDevice != null) {
            // Loop until there is no more data in the RX buffer.
            try {
                byte[] buffer = new byte[CHUNK_SIZE];
                int read;
                //String data =  new String(b, "US-ASCII");;
                while ((read = mLoopbackDevice.read(buffer, buffer.length)) > 0) {
                    mLoopbackDevice.write(buffer, read);
                }
            } catch (IOException e) {
                Log.w(TAG, "Unable to transfer data over UART", e);
            }
        }
    }
    /*
    * UART Write
    * */
    public void writeUartData(UartDevice uart,String Data) throws IOException {
        byte[] buffer = Data.getBytes();
        int count = uart.write(buffer, buffer.length);
        Log.d(TAG, "Wrote " + count + " bytes to peripheral");
    }
    /*
    *  UART Read
    * */
    public void readUartBuffer(UartDevice uart) throws IOException {
        // Maximum amount of data to read at one time
        final int maxCount = 1;
        byte[] buffer = new byte[maxCount];
        String  data;
        int count;
        while ((count = uart.read(buffer, buffer.length)) > 0) {
            Log.d(TAG, "Read " + count + " bytes from peripheral" );
            //Log.d("Message", new String(buffer));
            data = new String(buffer);
            Log.d("Message", data);
            ExeState= Integer.parseInt(data);
            //char()buffer[count];
            //data = new int(buffer);
            //Log.isLoggable(TAG,data);
        }
        //String s = new String(buffer, "UTF-8");
        //Log.d(TAG, "Read S" + s );
        //String str = IOUtils.toString(buffer, StandardCharsets.UTF_8);
    }

    private Runnable pwnRunnable = new Runnable() {
        @Override
        public void run() {
            try {

                if(PwmDutyCycle_val == 99){
                    Switch_led = (Switch_led + 1)%4;
                }

                switch (Switch_led){
                    case 0:
                        mLedStateBlue = true;
                        mLedStateGreen = false;
                        mLedStateRed = true;

                        mLedGpioBlue.setValue(mLedStateBlue);
                        mLedGpioGreen.setValue(mLedStateGreen);
                        mLedGpioRed.setValue(mLedStateRed);
                        break;
                    case 1:
                        mLedStateBlue = true;
                        mLedStateGreen = true;
                        mLedStateRed = false;

                        mLedGpioBlue.setValue(mLedStateBlue);
                        mLedGpioGreen.setValue(mLedStateGreen);
                        mLedGpioRed.setValue(mLedStateRed);
                        break;
                    case 2:
                        mLedStateBlue = false;
                        mLedStateGreen = true;
                        mLedStateRed = true;

                        mLedGpioBlue.setValue(mLedStateBlue);
                        mLedGpioGreen.setValue(mLedStateGreen);
                        mLedGpioRed.setValue(mLedStateRed);
                        break;
                    case 3:
                        mLedStateBlue = false;
                        mLedStateGreen = false;
                        mLedStateRed = true;

                        mLedGpioBlue.setValue(mLedStateBlue);
                        mLedGpioGreen.setValue(mLedStateGreen);
                        mLedGpioRed.setValue(mLedStateRed);
                        break;
                }

                PwmDutyCycle_val = (PwmDutyCycle_val + 1 )%100;
                mPwm.setPwmDutyCycle(PwmDutyCycle_val);
                mHandler.postDelayed(pwnRunnable, PWM_DUTYCYCLE_TIMER);
                Log.d(TAG,"PwmDutyCycle_val: "+ PwmDutyCycle_val);
            }catch (IOException e){
                Log.e(TAG, "Error on pwnRunnable", e);
            }
        }
    };
}
