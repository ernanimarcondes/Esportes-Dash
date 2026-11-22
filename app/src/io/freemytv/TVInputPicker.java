package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.widget.Toast;
import java.util.*;

/** Inputs menu for custom dashboards, independent of the original launcher's drawer. */
final class TVInputPicker {
    private TVInputPicker(){}
    static void show(Activity activity){
        List<HomeApps.Item> items=new ArrayList<>();HomeApps.Item nativeTv=null;
        if(HomeApps.liveTv(activity)!=null){
            nativeTv=HomeApps.action(HomeApps.LIVE_TV,"live","TV · canais / antena","tv");items.add(nativeTv);
        }
        try{for(HomeApps.Item item:HomeApps.orderedInputs(activity))if(item.kind==HomeApps.INPUT)items.add(item);}catch(RuntimeException ignored){}
        if(items.isEmpty()){Toast.makeText(activity,"Nenhuma entrada de TV disponível",Toast.LENGTH_LONG).show();return;}
        String[] labels=new String[items.size()];
        for(int i=0;i<items.size();i++){
            HomeApps.Item item=items.get(i);String label=item.label;
            if(item.kind==HomeApps.INPUT){
                if(!label.toUpperCase(Locale.ROOT).contains("HDMI"))label="HDMI "+item.number+" · "+label;
                label+=item.connected?" · conectado":" · em espera / sem sinal";
            }
            labels[i]=label;
        }
        final HomeApps.Item fallback=nativeTv;
        new AlertDialog.Builder(activity).setTitle("TV / HDMI").setItems(labels,(dialog,index)->{
            HomeApps.Item item=items.get(index);if(HomeApps.open(activity,item,null))return;
            if(item.kind==HomeApps.INPUT&&fallback!=null&&HomeApps.open(activity,fallback,null))
                Toast.makeText(activity,"Selecione a entrada no aplicativo TV",Toast.LENGTH_LONG).show();
            else Toast.makeText(activity,"Não foi possível abrir esta entrada",Toast.LENGTH_LONG).show();
        }).setNegativeButton("FECHAR",null).show();
    }
}
