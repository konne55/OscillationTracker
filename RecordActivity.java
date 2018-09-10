package de.bmsapp.niko.oscillation;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

public class RecordActivity extends AppCompatActivity implements SensorEventListener{

    private SensorManager sensorManager;
    private Sensor senAccelerometer;
    private Sensor senGyroscope;
    private Sensor senTemperatur;
    private boolean isRecording = false;

    private double THRESHOLD = 0.05;
    private double FRAMERATE = 500;

    TextView tvAccX;
    TextView tvAccY;
    TextView tvAccZ;
    TextView tvSpeedX;
    TextView tvSpeedY;
    TextView tvSpeedZ;
    TextView tvDistanceX;
    TextView tvDistanceY;
    TextView tvDistanceZ;
    TextView tvDistance;
    Button recordButton;
    SeekBar sbLowPass;
    SeekBar sbSaveRate;
    TextView timeDiff;
    Boolean firstRun = true;

    GraphView graph;
    LineGraphSeries<DataPoint> seriesAx;
    LineGraphSeries<DataPoint> seriesAy;
    LineGraphSeries<DataPoint> seriesAz;

    ArrayList <Double> dt,x,y,z;
    ArrayList <Double> vx,vy,vz,v;
    ArrayList <Double> dx, dy,dz,d;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);

        graph = (GraphView) findViewById(R.id.accelerationGraph);
        seriesAx = new LineGraphSeries<>();
        seriesAy = new LineGraphSeries<>();
        seriesAz = new LineGraphSeries<>();
        graph.addSeries(seriesAx);
        graph.addSeries(seriesAy);
        graph.addSeries(seriesAz);
        seriesAx.setColor(Color.BLUE);
        seriesAy.setColor(Color.RED);
        seriesAz.setColor(Color.GREEN);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setMinX(System.currentTimeMillis());
        graph.getViewport().setMaxX(System.currentTimeMillis()+100000);
        graph.getViewport().setScalable(true);
        graph.getViewport().setScalableY(true);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senAccelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        senGyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        senTemperatur = sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        lastUpdate = System.currentTimeMillis();

        tvAccX = findViewById(R.id.accX);
        tvAccY = findViewById(R.id.accY);
        tvAccZ = findViewById(R.id.accZ);
        tvSpeedX = findViewById(R.id.speedX);
        tvSpeedY = findViewById(R.id.speedY);
        tvSpeedZ = findViewById(R.id.speedZ);
        tvDistanceX = findViewById(R.id.distanceX);
        tvDistanceY = findViewById(R.id.distanceY);
        tvDistanceZ = findViewById(R.id.distanceZ);
        tvDistance = findViewById(R.id.distance);
        recordButton = findViewById(R.id.button);
        timeDiff = findViewById(R.id.timeDiff);

        sbLowPass = findViewById(R.id.sb_LowPassThreshold);
        sbLowPass.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                Log.i("NiKo","THRESHOLD Progress: " + (double)progress/1000);
                THRESHOLD = (double) progress/1000;
                Log.i("NiKo","THRESHOLD: " + THRESHOLD);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        sbSaveRate = findViewById(R.id.sb_SaveFramerate);
        sbSaveRate.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                FRAMERATE = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    public void Record(View v){
        if(isRecording){
            sensorManager.unregisterListener(this);
            recordButton.setText("Start recording");
            isRecording = false;
            firstRun = true;
        }else{
            sensorManager.registerListener(this, senAccelerometer,SensorManager.SENSOR_DELAY_FASTEST);
            recordButton.setText("Stop recording");
            isRecording = true;
        }
    }

    public void resetValues(View btn){
        dt = new ArrayList<>();
        x = new ArrayList<>();
        y = new ArrayList<>();
        z = new ArrayList<>();
        vx = new ArrayList<>();
        vy = new ArrayList<>();
        vz = new ArrayList<>();
        v = new ArrayList<>();
        dx = new ArrayList<>();
        dy = new ArrayList<>();
        dz = new ArrayList<>();
        d = new ArrayList<>();
        x.add(0.0);
        y.add(0.0);
        z.add(0.0);
        vx.add(0.0);
        vy.add(0.0);
        vz.add(0.0);
        dx.add(0.0);
        dy.add(0.0);
        dz.add(0.0);
        d.add(0.0);
        dt.add(0.0);
        firstRun = true;
    }





    long lastUpdate;
    long firstUpdate;
    double a0,v0,w0,a1;
    long curTime;
    double am;
    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (firstRun) {
            lastUpdate = System.currentTimeMillis();
            firstUpdate = lastUpdate;
            graph.getViewport().setMinX(lastUpdate);
            firstRun = false;
            a0 = 0;
            v0 = 0;
            w0 = 0;

        }else{

               if (sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
/*
            Log.i("NiKo", "Schranke: " + String.valueOf(Math.abs(event.values[2]) > THRESHOLD));
            if (Math.abs(event.values[2]) > THRESHOLD) {
                curTime = System.currentTimeMillis();
                double dt = (double) curTime - lastUpdate;
                double a1 = event.values[2]-am;
                double v1 = linearIntegration(dt, a0, event.values[2]-am, v0);
                double w1 = linearIntegration(dt, v0, ((a1+event.values[2]-am)/2)*dt, w0);
                a0 = a1;
                v0 = v1;
                w0 = w1;
                Log.i("NiKo", "a1, v1, w1, dt" + a1 + "," + v1 + "," + w1 + "," + dt);
                seriesAx.appendData(new DataPoint(lastUpdate-firstUpdate,a0), true, 1000000);
                seriesAy.appendData(new DataPoint(lastUpdate-firstUpdate, v0), true, 1000000);
                seriesAz.appendData(new DataPoint(lastUpdate-firstUpdate, w0), true, 1000000);

                lastUpdate = System.currentTimeMillis();
            }*/
            a1 = 0.0;
            curTime = System.currentTimeMillis();
            if (Math.abs(event.values[0]) > THRESHOLD) {
                a1 = event.values[0];
            }
                double dt = (double) curTime - lastUpdate;
                double v1 = linearIntegration(dt, a0,a1)+v0;
                double w1 = linearIntegration(dt, v0, v1)+w0;
                a0 = a1;
                v0 = v1;
                w0 = w1;
                if((lastUpdate-firstUpdate) == 0){
                    firstUpdate = lastUpdate-10;
                }
                Log.i("NiKo", "dT: " + (lastUpdate-firstUpdate) + " ;lastUpdate: " + lastUpdate + " ;firstUpdate: " + firstUpdate + " ;curTime: " + curTime);
                seriesAx.appendData(new DataPoint(lastUpdate-firstUpdate,a1), true, 1000000);
                seriesAy.appendData(new DataPoint(lastUpdate-firstUpdate, v1), true, 1000000);
                seriesAz.appendData(new DataPoint(lastUpdate-firstUpdate, w1), true, 1000000);

                lastUpdate = curTime;

        }
        }
    }

    public double linearIntegration(double dt, double a0, double a1){
        Double am = (a0+a1)/2;
        return am*(dt/1000);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
