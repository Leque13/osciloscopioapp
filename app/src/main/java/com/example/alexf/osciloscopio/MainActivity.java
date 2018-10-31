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



       /* // Grafico
        double y, x;
        x = 0;
        bufferint[0] = 254;
        GraphView graph = (GraphView) findViewById(R.id.graph);
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>();
        // graph.getGridLabelRenderer().setNumVerticalLabels(5);

        graph.getGridLabelRenderer().setVerticalAxisTitle("Volts");
        graph.getGridLabelRenderer().setHorizontalAxisTitle("Tempo");


        graph.getViewport().setScalable(true);
        graph.getViewport().setScrollable(true);

        StaticLabelsFormatter staticLabelsFormatter = new StaticLabelsFormatter(graph);
        //graph.getViewport().setYAxisBoundsManual(true);
        staticLabelsFormatter.setVerticalLabels(new String[]{"0", "1", "2", "3", "4", "5"});
        graph.getGridLabelRenderer().setLabelFormatter(staticLabelsFormatter);
        graph.getViewport().setMinY(0);
        graph.getViewport().setMaxY(5);


        for (int i = 0; i < 1000; i++) {
            x = x + 1;
            if (i < 100 || i > 200) {
                y = 5;
            } else {
                y = 0;
            }
            series.appendData(new DataPoint(x, y), true, 1000);
        }
        float Sx = 1;


        //graph.getViewport().setScalable(true);
        // graph.getViewport().setScrollableY(true);
        // graph.getViewport().setScalableY(true);

        // graph.getViewport().setYAxisBoundsManual(true);

        graph.setScaleY(Sx);
        graph.setScaleX(Sx);
        graph.addSeries(series); */

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
                        inteiro2 = buffer[j + 1] & 0xff;

                        bufferFloat[j] = inteiro;// transformar o byte para float


                        Log.d("Recebido", "Y =  " + bufferFloat[j] + " I= " + i + " Bytes= " + bytes + " Inteiro:  " + inteiro);
                        i++;

                        //Inverte os Bits do int
//                        for (int i = 0; i < 4; i++) {
//                            bufferint[j] = swapBits(bufferint[j], i, 8 - i - 1);
//                        }


                        //Mostra o valor já invertido
                        //Log.d("Recebido", "InputStream " + bufferint[j]+ " Qtd de bytes recebidos: " + bytes + " J: " + j);
                        // TimeUnit.MILLISECONDS.sleep(2000);
                    }

                    TimeUnit.MILLISECONDS.sleep(2000);

                    // blue = false;

                    //new arrawy
                  /*  int[] dados = new int[5];
                    int[] inteiros = new int[buffer.length / 5];
                    int p = 0;
                    int x = 0;
                    for (int i = 0; i < bufferint.length; i++) {
                        //acumula os dads
                        dados[x] = bufferint[i];
                        x++;

                        //verifica se já acumulou 5 dados
                        if ((i + 1) % 5 == 0) {
                            inteiros[p] = moda(dados);
                            p++;

                            //reseta os dados
                            dados = new int[5];
                            x = 0;
                        }


                    }*/

                    if (i > bytes) {
                        i = 0;

                        float[] newbuffer = new float[bufferFloat.length/4];
                        int c = 0;
                        int j = 0;
                        for (int l = 0; l < bufferFloat.length; l ++){

                            if(c == 3){
                                newbuffer[j] = bufferFloat[l];
                                j++;
                            }

                            if(c == 4){
                                c = 0;
                            } else {
                                c++;
                            }
                        }

                        grafico(newbuffer);
                    }

                } catch (Exception e) {
                    break;
                }

            }

        }

       /* private int moda(int[] buffer){
            int nVezes = 0;
            int moda = 0;
            int comparaV = 0;
            for(int p = 0; p < buffer.length; p++){
                nVezes = 0;
                for(int k = p+1; k < buffer.length; k++){
                    if( buffer[p] == buffer[k] ){
                        ++nVezes;
                    }
                }
                if (nVezes > comparaV ){
                    moda = buffer[p];
                    Log.d("Recebido", "Y =  " + moda);
                    comparaV = nVezes;
                }
            }
            return moda;
        }*/

        /*int swapBits(int n, int i, int j) {
            int a = (n >> i) & 1;
            int b = (n >> j) & 1;

            if ((a ^ b) != 0) {
                return n ^= (1 << i) | (1 << j);
            }

            return n;
        }*/

        void grafico(float[] buffer) { // Grafico
            double x, y;
            x = 0.0;
            float yy = 0.0f;
            int tam = 1000;


            GraphView graph = findViewById(R.id.graph);

            PointsGraphSeries<DataPoint> series = new PointsGraphSeries<>();
            // graph.getGridLabelRenderer().setNumVerticalLabels(5);

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

                yy = ((buffer[i] * 5) / 255);
                Log.d("RecebidoB", "Y =  " + yy + " I= " + i);
                series.appendData(new DataPoint(x, yy), true, 1000);

            }
            float Sx = 1;

            graph.removeAllSeries();
            //graph.setScaleY(Sx);
            //  graph.setScaleX(Sx);
            series.setTitle("Random Curve 1");
            series.setColor(Color.RED);
            series.setSize(2f);
//            series.setDrawDataPoints(true);
//            series.setDataPointsRadius(10);
//            series.setThickness(8);
            series.setShape(PointsGraphSeries.Shape.POINT);
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
