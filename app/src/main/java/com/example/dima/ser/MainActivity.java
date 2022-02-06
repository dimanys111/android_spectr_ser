package com.example.dima.ser;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;


public class MainActivity extends Activity {
    AudioRecord audioRecord=null;
    boolean isReading = false;
    boolean avto = true;

    double TwoPi = 6.283185307179586;

    double linchast=0;

    double predOt=0;
    double predDoo=4000;

    double predDooSpek=4000;
    double predDooAmpl=4000;

    DrawView draw;
    RelativeLayout vd;
    Button b;
    Button b1;
    EditText e;
    EditText t1;
    EditText t2;

    EditText t3;
    EditText t4;
    int sm;
    String[] data = {"E", "A", "D", "G", "B", "e", "Весь диапазон"};

    ArrayList<Double> FFTAnalysis(ArrayList<Double> ghj)  {
        int i, j, n, m, Mmax, Istp;
        double Tmpr, Tmpi, Wtmp, Theta;
        double Wpr, Wpi, Wr, Wi;
        double[] Tmvl;

        int Nvl=ghj.size();

        if (ghj.size()>0) {
            n = Nvl * 2;
            Tmvl = new double[n];

            for (i = 0; i < Nvl; i++) {
                j = i * 2;
                Tmvl[j] = 0;
                Tmvl[j + 1] = ghj.get(i);
            }

            i = 1;
            j = 1;
            while (i < n) {
                if (j > i) {
                    Tmpr = Tmvl[i];
                    Tmvl[i] = Tmvl[j];
                    Tmvl[j] = Tmpr;
                    Tmpr = Tmvl[i + 1];
                    Tmvl[i + 1] = Tmvl[j + 1];
                    Tmvl[j + 1] = Tmpr;
                }
                i = i + 2;
                m = Nvl;
                while ((m >= 2) && (j > m)) {
                    j = j - m;
                    m = m >> 1;
                }
                j = j + m;
            }

            Mmax = 2;
            while (n > Mmax) {
                Theta = -TwoPi / Mmax;
                Wpi = sin(Theta);
                Wtmp = sin(Theta / 2);
                Wpr = Wtmp * Wtmp * 2;
                Istp = Mmax * 2;
                Wr = 1;
                Wi = 0;
                m = 1;

                while (m < Mmax) {
                    i = m;
                    m = m + 2;
                    Tmpr = Wr;
                    Tmpi = Wi;
                    Wr = Wr - Tmpr * Wpr - Tmpi * Wpi;
                    Wi = Wi + Tmpr * Wpi - Tmpi * Wpr;

                    while (i < n) {
                        j = i + Mmax;
                        Tmpr = Wr * Tmvl[j] - Wi * Tmvl[j - 1];
                        Tmpi = Wi * Tmvl[j] + Wr * Tmvl[j - 1];

                        Tmvl[j] = Tmvl[i] - Tmpr;
                        Tmvl[j - 1] = Tmvl[i - 1] - Tmpi;
                        Tmvl[i] = Tmvl[i] + Tmpr;
                        Tmvl[i - 1] = Tmvl[i - 1] + Tmpi;
                        i = i + Istp;
                    }
                }

                Mmax = Istp;
            }

            Nvl=Nvl/2;

            ArrayList<Double> FTvl;
            FTvl = new ArrayList<>();
            for (i = 0; i < Nvl; i++) {
                FTvl.add((double) 0);
            }

            for (i = 0; i < Nvl; i++) {
                j = Nvl*2+i * 2;
                FTvl.set(Nvl - i - 1, sqrt(pow(Tmvl[j], 2) + pow(Tmvl[j + 1], 2)));
            }
            return FTvl;
        }
        ArrayList<Double> FTvl;
        FTvl = new ArrayList<>();
        return FTvl;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vd=(RelativeLayout)findViewById(R.id.rty);
        draw=new DrawView(this);
        vd.addView(draw);

        e=(EditText)findViewById(R.id.editText);

        e.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    linchast=Float.valueOf(s.toString());
                } catch( NumberFormatException  e){
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
                // Прописываем то, что надо выполнить после изменения текста
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        t1=(EditText)findViewById(R.id.editText2);

        t1.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    predOt = Float.valueOf(s.toString());
                    if (predDoo<=predOt) {
                        predOt = predDoo - 1;
                        t1.setText(String.valueOf(predOt));
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        t2=(EditText)findViewById(R.id.editText3);

        t2.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    predDoo = Float.valueOf(s.toString());
                    if (predDoo<=predOt) {
                        predDoo = predOt + 1;
                        t2.setText(String.valueOf(predDoo));
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        t3=(EditText)findViewById(R.id.editText4);

        t3.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    predDooSpek = Float.valueOf(s.toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        t4=(EditText)findViewById(R.id.editText5);

        t4.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                try {
                    predDooAmpl = Float.valueOf(s.toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });


        b1=(Button)findViewById(R.id.button2);
        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                avto=!avto;
            }
        });

        b=(Button)findViewById(R.id.button);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String s=e.getText().toString();
                try {
                    linchast=Float.valueOf(s);
                } catch( NumberFormatException  e){
                    Toast.makeText(getBaseContext(), "Ошибка ввода = " + s, Toast.LENGTH_SHORT).show();
                }
            }
        });

        // адаптер
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, data);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setAdapter(adapter);
        // заголовок
        spinner.setPrompt("Title");
        // выделяем элемент
        spinner.setSelection(6);
        // устанавливаем обработчик нажатия
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                switch (position) {
                    case 0:
                        linchast = 82.41;
                        predOt=50;
                        predDoo=100;
                        break;
                    case 1:
                        linchast = 110.00;
                        predOt=100;
                        predDoo=120;
                        break;
                    case 2:
                        linchast = 146.82;
                        predOt=120;
                        predDoo=170;
                        break;
                    case 3:
                        linchast = 196.00;
                        predOt=170;
                        predDoo=220;
                        break;
                    case 4:
                        linchast = 246.94;
                        predOt=220;
                        predDoo=270;
                        break;
                    case 5:
                        linchast = 329.63;
                        predOt=270;
                        predDoo=370;
                        break;
                    case 6:
                        linchast = 2000;
                        predOt=0;
                        predDoo=4000;
                        break;
                    default: linchast = 0;
                        break;
                }
                t1.setText(String.valueOf(predOt));
                t2.setText(String.valueOf(predDoo));
                e.setText(String.valueOf(linchast));
            }
            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        createAudioRecorder();

//        startService(new Intent(this, WalkingIconService.class));
//        finish();
    }

    @Override
    public void onPause() {
        super.onPause();  // Always call the superclass method first
        if (audioRecord!=null) {
            recordStop();
            readStop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first
        if (audioRecord!=null) {
            recordStart();
            readStart();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (audioRecord!=null) {
            recordStop();
            readStop();
        }
    }

    void createAudioRecorder() {
        int sampleRate = 8000;
        int channelConfig = AudioFormat.CHANNEL_IN_MONO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate,
                channelConfig, audioFormat);
        int internalBufferSize = minInternalBufferSize * 4;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRate, channelConfig, audioFormat, internalBufferSize);
        audioRecord.release();
    }

    public void recordStart() {
        audioRecord.startRecording();
    }

    public void recordStop() {
        audioRecord.stop();
    }

    class ArrayDoubleSynchr extends ArrayList<Double> {
        public ArrayDoubleSynchr()
        {
            super();
        }

        public synchronized void addNew(ArrayList<Double> yui){
            for (int i = 0; i < yui.size(); i++) {
                this.add(yui.get(i));
            }
        }

        public void rem(int tyu){
            for (int i = 0; i < tyu; i++) {
                this.remove(0);
            }
        }

        public synchronized ArrayList<Double> chast(int Nvl){
            ArrayList<Double> r=new ArrayList<>();
            int ghj=this.size()-Nvl;
            if (ghj>=0) {
                for (int i = 0; i < Nvl; i++) {
                    r.add(this.get(ghj + i));
                }
                rem(ghj);
            }
            return r;
        }
    }


    ArrayDoubleSynchr d = new ArrayDoubleSynchr();
    ArrayList<Double> drez;

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            draw.invalidate();
        }
    };

    public void readStart() {
        isReading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (audioRecord == null)
                    return;
                int myBufferSize = 8192;

                byte[] myBuffer = new byte[myBufferSize];

                byte[] perBuf = new byte[16384/32];
                int pos=0;
                int readCount;
                while (isReading) {
                    readCount = audioRecord.read(myBuffer, 0, myBufferSize);
                    int i=0;
                    while (i<readCount)
                    {
                        perBuf[pos]=myBuffer[i];
                        i++;
                        pos++;
                        if (pos>=perBuf.length) {

                            ArrayList<Double> yui=new ArrayList<>();

                            for (int j = 0; j < perBuf.length / 2; j++) {
                                short s = byteArrayToShort(perBuf, j * 2, 2);
                                yui.add((double) s);
                            }

                            d.addNew(yui);

                            handler.sendEmptyMessage(0);

                            pos=0;
                        }
                    }
                }
            }
        }).start();
    }

    public static short byteArrayToShort(byte[] b,int n,int k) {
        final ByteBuffer bb = ByteBuffer.wrap(b,n,k);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    public void readStop() {
        isReading = false;
    }

    class XY{
        public int x;
        public int y;
    }

    float rez;

    int rezold;

    int ghj=0;

    public class DrawView extends View {
        float x1,y1,x2,y2,xpr,ypr;
        Paint paint;
        Paint p;
        XY[] xy;
        Paint fontPaint;
        int fontSize = 50;
        float[] widths;
        float width;
        Path path;
        int Nvl=8192;
        ArrayList<Double> gh;

        public DrawView(Context context) {
            super(context);
            paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

            p = new Paint();
            // толщина линии
            p.setStrokeWidth(2);

            fontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            fontPaint.setTextSize(fontSize);
            fontPaint.setStyle(Paint.Style.STROKE);

            // path, треугольник
            path = new Path();
            path.moveTo(-25, -50);
            path.lineTo(25, 0);
            path.lineTo(-25, 50);
            path.close();

            setOnTouchListener(new OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    float x = event.getX();
                    float y = event.getY();

                    int tyu=event.getAction();

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: // нажатие
                            if(y>t1.getBottom()&& y<(t4.getBottom()-t4.getHeight())) {
                                x1 = x;
                                y1 = y;
                                break;
                            }
                            else
                                return false;
                        case MotionEvent.ACTION_MOVE: // движение
                            if (abs(x-x1)>50) {
                                xpr = x;
                                ypr = y;
                            }
                            else
                            {
                                xpr=0;
                                ypr=0;
                            }

                            break;
                        case MotionEvent.ACTION_UP: // отпускание
                            xpr=0;
                            ypr=0;
                            x2=x;
                            y2=y;
                            if (abs(x2-x1)>50)
                            {
                                if (x2 > x1) {
                                    double rty = (predDoo - predOt) / getWidth();
                                    double predOt_old = predOt;

                                    predOt = x1 * rty + predOt_old;
                                    predDoo = x2 * rty + predOt_old;
                                    linchast = (predDoo + predOt) / 2;
                                    t1.setText(String.format("%.2f", predOt).replace(',', '.'));
                                    t2.setText(String.format("%.2f", predDoo).replace(',', '.'));
                                    e.setText(String.format("%.2f", linchast).replace(',', '.'));
                                } else {
                                    double gh = (predDoo - predOt) / 2;

                                    predOt = predOt - gh;
                                    if (predOt<0)
                                        predOt=0;
                                    predDoo = predDoo + gh;
                                    if (predDoo>0)
                                        predDoo=4000;
                                    linchast = (predDoo + predOt) / 2;
                                    t1.setText(String.format("%.2f", predOt).replace(',', '.'));
                                    t2.setText(String.format("%.2f", predDoo).replace(',', '.'));
                                    e.setText(String.format("%.2f", linchast).replace(',', '.'));
                                }
                            }
                            break;
                        case MotionEvent.ACTION_CANCEL:
                            break;
                    }
                    return true;
                }
            });
        }



        @Override
        protected void onDraw(Canvas canvas) {
            // заливка канвы цветом
            canvas.drawRect(x1, 0, xpr, getHeight()/2, paint);

            canvas.drawARGB(80, 102, 204, 255);

            gh=d.chast(Nvl);

            drez = FFTAnalysis(gh);

            double otP= (drez.size()-1)*predOt/4000;
            double otDoo= (drez.size()-1)*predDoo/4000;

            if (drez.size()>0) {

                // настройка кисти
                p.setColor(Color.BLUE);

                int n = setToch(canvas,gh,0,gh.size()-1,xy,getHeight()/2,getHeight(),0,getWidth(),predDooAmpl,true);

                p.setColor(Color.RED);

                n = setToch(canvas,drez, otP, otDoo,xy,0,getHeight()/2,0,getWidth(),predDooSpek,false);

                if (abs(rezold - n)<20) {
                    ghj++;
                }

                if (ghj==3) {
                    rez = rezold;
                    rez=4000*rez/(drez.size()-1);
                    ghj=0;
                }

                if (rez<linchast)
                {
                    canvas.save();
                    p.setColor(Color.GREEN);
                    canvas.translate(getWidth()/2,250);
                    canvas.drawPath(path, p);
                    canvas.restore();
                }
                else
                {
                    canvas.save();
                    p.setColor(Color.RED);
                    canvas.rotate(180);
                    canvas.translate(-getWidth()/2,-250);
                    canvas.drawPath(path, p);
                    canvas.restore();
                }

                rezold=n;

                sm= (int) (getWidth()*(linchast-predOt)/(predDoo-predOt));

                // настройка кисти
                p.setColor(Color.BLUE);

                canvas.drawLine(sm, 0, sm, getHeight()/2, p);

                sm= (int) (getWidth()*(rez-predOt)/(predDoo-predOt));

                // настройка кисти
                p.setColor(Color.YELLOW);

                canvas.drawLine(sm, 0, sm, getHeight()/2, p);

                String text = String.valueOf(4000*n/(drez.size()-1));

                // ширина текста
                width = fontPaint.measureText(text);

                // посимвольная ширина
                widths = new float[text.length()];
                fontPaint.getTextWidths(text, widths);

                canvas.translate(50, 250);

                // вывод текста
                canvas.drawText(text, 0, 0, fontPaint);

                // линия шириной в текст
                canvas.drawLine(0, 0, width, 0, fontPaint);

                canvas.restore();

                text = String.format("%.2f", rez);
                //text = String.valueOf(rez,);

                // ширина текста
                width = fontPaint.measureText(text);

                // посимвольная ширина
                widths = new float[text.length()];
                fontPaint.getTextWidths(text, widths);

                canvas.translate(getWidth()-50-width, 250);

                // вывод текста
                canvas.drawText(text, 0, 0, fontPaint);

                // линия шириной в текст
                canvas.drawLine(0, 0, width, 0, fontPaint);
            }
        }

        private int setToch(Canvas canvas,ArrayList<Double> drez,double otP,double dooP,XY[] xy,int otH,int dooH,int otW,int dooW, double Max,boolean b) {

//            if (otP<0)
//            {
//                otW= (int) (-otP/dooP*getWidth());
//            }
//
//            if (dooP>drez.size()-1)
//            {
//                dooW= (int) ((drez.size()-1-otP)/(dooP-otP)*getWidth());
//            }
//
//            if (dooW<otW+10) {
//                dooW = otW + 5;
//                otW=otW-5;
//            }
//
//            if (otP<=0)
//                otP=0.001;

            double min;
            double max;

            min = drez.get((int) otP);
            max = drez.get((int) otP);
            int n = (int) otP;

            if (dooP >= drez.size()-1)
                dooP = drez.size()-1.001;

            for (int i = (int) otP; i < dooP; i++) {
                if (min > drez.get(i))
                    min = drez.get(i);
                if (max < drez.get(i)) {
                    max = drez.get(i);
                    n = i;
                }
            }

            if (Max>0 && !avto)
            {
                min=0;
                max=Max;
                if (b)
                    min=-max;
            }

            if (max-min<0.0001)
            {
                max=min+0.0001;
                min=min-0.0001;
            }

            xy = new XY[(dooW-otW)];
            for (int i = 0; i < xy.length; i++) {
                xy[i] = new XY();
            }

            double l = (dooP-otP) / (dooW-otW-1);

            for (int i = 0; i < xy.length-1; i++) {
                double t = drez.get((int) (otP+i * l))
                        - (drez.get((int) (otP+i * l)) - drez.get((int) (otP+i * l) + 1)) * ((i * l - (int)(i * l)+otP-(int)otP)-(int)(i * l - (int)(i * l)+otP-(int)otP));
                xy[i].x = i+otW;
                xy[i].y = -(int) ((t) * (dooH-otH) / (max - min) - (min * (dooH-otH) / (max - min))) + dooH;
            }

            {
                int i = xy.length - 1;
                double t = drez.get((int) (otP + i * l+0.001));
                xy[i].x = i + otW;
                xy[i].y = -(int) ((t) * (dooH - otH) / (max - min) - (min * (dooH - otH) / (max - min))) + dooH;
            }

            for (int i = 0; i < xy.length - 1; i++) {
                canvas.drawLine(xy[i].x, xy[i].y, xy[i + 1].x, xy[i + 1].y, p);
            }
            return n;
        }
    }
}
