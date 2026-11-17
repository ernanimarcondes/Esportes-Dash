package io.freemytv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.*;

/** Shared Android TV actions for the standalone sports home. Original mode lives in HomeMode. */
abstract class RacingHomeView extends View implements HomeData.Listener {
    protected final Activity activity;
    protected final HomeData data;
    protected final RacingAgenda agenda;
    private boolean active;
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Runnable refresh=new Runnable(){@Override public void run(){if(!active)return;data.refreshIfStale();agenda.refresh(()->invalidate());invalidate();main.postDelayed(this,60000);}};
    RacingHomeView(Activity activity){
        super(activity);this.activity=activity;data=HomeData.get(activity);agenda=new RacingAgenda(activity,true);
        setFocusable(true);setFocusableInTouchMode(true);requestFocus();
    }
    void resume(){if(active)return;active=true;data.addListener(this);main.post(refresh);}
    void pause(){active=false;main.removeCallbacks(refresh);data.removeListener(this);}
    void destroy(){pause();agenda.close();}
    abstract void reset(boolean animate);
    abstract boolean onKey(KeyEvent event);
    @Override public void onData(){invalidate();}
    @Override public boolean performClick(){super.performClick();return true;}
    void showRecents(){showAllApps();}
    void showToast(String message){Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
    protected void openApp(int index){
        String[] packages={"com.google.android.youtube.tv","com.amazon.amazonvideo.livingroom","com.spotify.tv.android"};
        try{Intent intent=activity.getPackageManager().getLeanbackLaunchIntentForPackage(packages[index]);
            if(intent==null)intent=activity.getPackageManager().getLaunchIntentForPackage(packages[index]);
            if(intent==null){showToast("Aplicativo não instalado");return;}activity.startActivity(intent);
        }catch(Exception e){showToast("Não foi possível abrir o aplicativo");}
    }
    protected void settings(){
        new AlertDialog.Builder(activity).setTitle("Ajustes / Esportes Dash").setItems(new String[]{"Tela inicial e modo Original","Configurações da TV","Sobre o Esportes Dash"},(dialog,index)->{
            if(index==0)activity.startActivity(new Intent(activity,SettingsActivity.class).putExtra("dashboard_profiles",true));
            else if(index==1)HomeApps.open(activity,HomeApps.action(HomeApps.SETTINGS,HomeApps.TOP_SETTINGS,"Configurações","settings"),null);
            else new AlertDialog.Builder(activity).setTitle("Esportes Dash").setMessage("Painel de esportes baseado no Free My TV, de simbasjungles e colaboradores, e na adaptação TV-Dashboard.\n\nDados: ESPN e Jolpica F1. Clima: Open-Meteo. Circuitos: bacinger/f1-circuits.\n\nFontes públicas, com atualização periódica. O modo Original mantém o retorno à tela de fábrica.").setPositiveButton("FECHAR",null).show();
        }).show();
    }
    protected void showAgenda(int ignored){
        SimpleDateFormat format=new SimpleDateFormat("EEE dd/MM · HH:mm",new Locale("pt","BR"));format.setTimeZone(TimeZone.getTimeZone("America/Sao_Paulo"));
        StringBuilder message=new StringBuilder();for(RacingAgenda.Event e:agenda.currentRaceSessions())message.append(e.title).append("\n").append(format.format(new Date(e.at))).append(e.dateOnly?" · horário a confirmar":"").append("\n\n");
        message.append("Horários de Brasília · sujeitos a alterações");new AlertDialog.Builder(activity).setTitle(agenda.raceName).setMessage(message.toString()).setPositiveButton("FECHAR",null).show();
    }
    void showAllApps(){
        Intent query=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> apps=activity.getPackageManager().queryIntentActivities(query,0);
        apps.sort((a,b)->a.loadLabel(activity.getPackageManager()).toString().compareToIgnoreCase(b.loadLabel(activity.getPackageManager()).toString()));
        String[] names=new String[apps.size()];for(int i=0;i<names.length;i++)names[i]=apps.get(i).loadLabel(activity.getPackageManager()).toString();
        new AlertDialog.Builder(activity).setTitle("Aplicativos").setItems(names,(dialog,index)->{try{ResolveInfo app=apps.get(index);
            activity.startActivity(new Intent(Intent.ACTION_MAIN).setClassName(app.activityInfo.packageName,app.activityInfo.name).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }catch(Exception e){showToast("Não foi possível abrir o aplicativo");}}).show();
    }
    void showInputs(){HomeApps.open(activity,HomeApps.action(HomeApps.INPUTS,HomeApps.TOP_INPUTS,"TV / HDMI","hdmi"),null);}
}
