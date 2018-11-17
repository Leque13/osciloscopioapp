package com.example.alexf.osciloscopio;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.StaticLabelsFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.PointsGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class MainActivity extends AppCompatActivity {
    Button btnConexao;


    private static final int Ativa_Bluetooth = 1; // Verifica se o Bluetooth foi Ativado
    private static final int Solicita_Conexao = 2;

    ConnectedThread connectedThread;

    boolean blue = true;

    BluetoothAdapter meuBluetoothAdapter = null; // Gera o uso bluetooth de celular
    BluetoothDevice meuDevice = null; // Gera o uso conexao bluetooth com device
    BluetoothSocket meuSocket = null;

    boolean conexao = false;

    private static String MAC = null;

    UUID MEU_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);

        //Bluetooth
        btnConexao = (Button) findViewById(R.id.btnConexao);
        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (meuBluetoothAdapter == null) {

            Toast.makeText(getApplicationContext(), "Dispositivo não possui bluetooth", Toast.LENGTH_LONG).show();
        } else if (!meuBluetoothAdapter.isEnabled()) {
            Intent ativabluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativabluetooth, Ativa_Bluetooth);
        }


        btnConexao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (conexao) {
                    //desconectar
                    try {
                        meuSocket.close();
                        conexao = false;
                        btnConexao.setText("Conectar");
                        blue = true;
                        Toast.makeText(getApplicationContext(), "Bluetooth Desconectado", Toast.LENGTH_LONG).show();
                    } catch (IOException erro) {
                        Toast.makeText(getApplicationContext(), "Erro" + erro, Toast.LENGTH_LONG).show();
                    }
                } else {
                    //conectar
                    Intent abreLista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abreLista, Solicita_Conexao);
                }
            }
        });


        final Button btnStop = findViewById(R.id.btnStop);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conexao){
                    if (connectedThread != null){
                        if(connectedThread.isInterrupted()){
                           btnStop.setText("Pausar");
                           connectedThread.start();
                        } else {
                            connectedThread.interrupt();
                            btnStop.setText("Continuar");
                        }
                    }
                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {

            case Ativa_Bluetooth:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getApplicationContext(), "Bluetooth Ativado", Toast.LENGTH_LONG).show();
                } else {

                    Toast.makeText(getApplicationContext(), "Bluetooth Desativado", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;

            case Solicita_Conexao:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.getExtras() != null)
                            MAC = data.getExtras().getString(ListaDispositivos.End_MAC);

                        //Toast.makeText(getApplicationContext(), "MAC" + MAC, Toast.LENGTH_LONG).show();
                        meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);

                        try {
                            meuSocket = meuDevice.createRfcommSocketToServiceRecord(MEU_UUID);

                            meuSocket.connect();
                            conexao = true;

                            connectedThread = new ConnectedThread(meuSocket);
                            connectedThread.start();

                            btnConexao.setText("Desconectar");
                            Toast.makeText(getApplicationContext(), "Conectado" + MAC, Toast.LENGTH_LONG).show();

                        } catch (IOException erro) {
                            conexao = false;
                            Toast.makeText(getApplicationContext(), "Erro ao Conectar" + erro, Toast.LENGTH_LONG).show();
                        }
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Falha ao obter o MAC", Toast.LENGTH_LONG).show();

                }


        }

    }

    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = meuSocket.getInputStream();
                tmpOut = meuSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {


            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()
            int i = 0;
            // Keep listening to the InputStream until an exception occurs
            while (true) {

                try {
                    float bufferFloat[] = new float[1024];
                    int inteiro;
                    int inteiro2;
                    // Read from the InputStream
                    //Conta quantos bytes estão no buffer
                    bytes = mmInStream.read(buffer);
                    for (int j = 0; j < bytes; j++) {

                        inteiro = buffer[j] & 0xff;


                        bufferFloat[j] = inteiro;// transformar o byte para float


                        Log.d("RecebidoFloat", "Y =  " + bufferFloat[j] + " I= " + i + " Bytes= " + bytes + " Inteiro:  " + inteiro);
                        i++;



                    }
                    //TimeUnit.MILLISECONDS.sleep(500);

                    if (i > 1000) {
                        i = 0;

                        grafico(bufferFloat);
                    }

                } catch (Exception e) {
                    break;
                }

            }

        }



        void grafico(float[] buffer) { // Grafico
            double x, y;
            x = 0.0;
            float yy = 0.0f;


            GraphView graph = findViewById(R.id.graph);

            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();

            graph.getGridLabelRenderer().setVerticalAxisTitle("Volts");
            graph.getGridLabelRenderer().setHorizontalAxisTitle("Tempo MS");

            graph.getViewport().setScalable(true);
            graph.getViewport().setScrollable(true);

            StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
            graph.getViewport().setYAxisBoundsManual(true);
            staticLabelsFormatter.setVerticalLabels(new String[]{"-5", "-4.5", "-4.0", "-3.5", "-3.0", "-2.5", "-2.0", "-1.5", "-1.0", "-0.5", "0", "0.5", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5"});
            graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
            graph.getViewport().setMinY(-5);
            graph.getViewport().setMaxY(5);

            for (int i = 0; i < 1000; i++) {
                x = x + 0.01;

                yy = ((buffer[i] * 5)/255);
                Log.d("RecebidoB", "Y =  " + yy + " I= " + i);
                series.appendData(new DataPoint(x, yy), true, 3000);

            }
            float Sx = 1;

            graph.removeAllSeries();
            //graph.setScaleY(Sx);
            //  graph.setScaleX(Sx);
            series.setTitle("Random Curve 1");
            series.setColor(Color.RED);
            graph.addSeries(series);

        }

        /* Envia comandos */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
}
