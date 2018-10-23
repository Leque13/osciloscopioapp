package com.example.alexf.osciloscopio;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ListaDispositivos extends ListActivity {

    private BluetoothAdapter meuBluetoothAdapter2= null;

    static String End_MAC=null;
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);

        meuBluetoothAdapter2 = BluetoothAdapter.getDefaultAdapter();

        Set<BluetoothDevice> dispositivosPareados = meuBluetoothAdapter2.getBondedDevices();

        if(dispositivosPareados.size() > 0){
            for(BluetoothDevice dispositivo : dispositivosPareados){
                String nomeBt = dispositivo.getName();
                String macBt= dispositivo.getAddress();
                ArrayBluetooth.add(nomeBt + "\n" + macBt);
            }
        }
        setListAdapter(ArrayBluetooth);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        String infoGeral =  ((TextView) v).getText().toString();

        //Toast.makeText(getApplicationContext(), "Info:" + infoGeral, Toast.LENGTH_LONG).show();

        String enderecoMac = infoGeral.substring(infoGeral.length()-17);

        Intent retornaMAC = new Intent();
        retornaMAC.putExtra(End_MAC, enderecoMac);
        setResult(RESULT_OK, retornaMAC);
        finish();//Fecha lista de dispositivos


    }
}
