package com.github.ghmxr.ftpshare.activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ghmxr.ftpshare.Constants;
import com.github.ghmxr.ftpshare.R;
import com.github.ghmxr.ftpshare.data.AccountItem;
import com.github.ghmxr.ftpshare.services.FtpService;
import com.github.ghmxr.ftpshare.ui.DialogOfFolderSelector;

import org.apache.log4j.chainsaw.Main;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private Menu menu;
    private SwitchCompat switchCompat;
    private CheckBox cb_wakelock,cb_anonymous_writable;
    private TextView tv_main_value,tv_port,tv_anonymous_path;
    private ListView listview_users;

    private static int MENU_ACCOUNT_ADD =0;
    private static int MENU_ANONYMOUS_SWITCH=1;

    public static final int REQUEST_CODE_ADD=0;
    public static final int REQUEST_CDOE_EDIT=1;

    public static final int MESSAGE_FTP_SERVICE_STARTED=0;
    public static final int MESSAGE_FTP_SERVICE_ERROR=1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView navigation = findViewById(R.id.navigation);
        navigation.setSelectedItemId(R.id.navigation_main);
        navigation.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.navigation_main:
                        onNavigationMainSelected();
                        return true;
                    case R.id.navigation_settings:
                        onNavigationAccountSelected();
                        return true;
                }
                return false;
            }
        });

        switchCompat=findViewById(R.id.main_switch);
        cb_wakelock=findViewById(R.id.wakelock_cb);
        cb_anonymous_writable=findViewById(R.id.anonymous_writable_cb);
        tv_main_value=findViewById(R.id.main_att);
        tv_port=findViewById(R.id.port_att);
        tv_anonymous_path=findViewById(R.id.mode_anonymous_value);
        listview_users=findViewById(R.id.view_user_list);

        initializeValues();
    }

    private void initializeValues(){
        final SharedPreferences settings=getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE);
        final SharedPreferences.Editor editor=settings.edit();
        switchCompat.setChecked(FtpService.isFTPServiceRunning());
        tv_main_value.setText(FtpService.getFTPStatusDescription(this));
        tv_port.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
        cb_wakelock.setChecked(settings.getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT));
        tv_anonymous_path.setText(settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT));
        cb_anonymous_writable.setChecked(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT));
        listview_users.setDivider(null);
        listview_users.setAdapter(new AccountAdapter(FtpService.getUserAccountList(this)));
        listview_users.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return;
                }
                if(isAnonymousMode()) return;
                Intent intent=new Intent(MainActivity.this,EditAccountActivity.class);
                intent.putExtra(EditAccountActivity.EXTRA_POSITION,position);
                startActivityForResult(intent,REQUEST_CDOE_EDIT);
            }
        });

        FtpService.setOnFTPServiceStatusChangedListener(new FtpService.OnFTPServiceStatusChangedListener() {
            @Override
            public void onFTPServiceStarted() {
                switchCompat.setEnabled(true);
                switchCompat.setChecked(true);
                tv_main_value.setText(FtpService.getFTPStatusDescription(MainActivity.this));
                findViewById(R.id.main_area).setClickable(true);
            }

            @Override
            public void onFTPServiceStartError(Exception e) {
                switchCompat.setEnabled(true);
                switchCompat.setChecked(false);
                tv_main_value.setText(FtpService.getFTPStatusDescription(MainActivity.this));
                Toast.makeText(MainActivity.this,e.toString(),Toast.LENGTH_SHORT).show();
                findViewById(R.id.main_area).setClickable(true);
            }

            @Override
            public void onFTPServiceDestroyed() {
                switchCompat.setChecked(false);
                tv_main_value.setText(FtpService.getFTPStatusDescription(MainActivity.this));
                findViewById(R.id.main_area).setClickable(true);
            }
        });

        findViewById(R.id.main_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.main_area).setClickable(false);
                if(!FtpService.isFTPServiceRunning()){
                    switchCompat.setChecked(true);
                    switchCompat.setEnabled(false);
                    tv_main_value.setText(getResources().getString(R.string.attention_opening_ftp));
                    FtpService.startService(MainActivity.this);
                }else{
                    FtpService.stopService();
                }
            }
        });

        findViewById(R.id.port_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return;
                }
                View dialogView=LayoutInflater.from(MainActivity.this).inflate(R.layout.layout_with_edittext,null);
                final EditText edit=dialogView.findViewById(R.id.dialog_edittext);
                edit.setInputType(InputType.TYPE_CLASS_NUMBER);
                edit.setSingleLine(true);
                edit.setHint(getResources().getString(R.string.item_port_hint));
                edit.setText(String.valueOf(settings.getInt(Constants.PreferenceConsts.PORT_NUMBER,Constants.PreferenceConsts.PORT_NUMBER_DEFAULT)));
                final AlertDialog dialog=new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getResources().getString(R.string.item_port))
                        .setView(dialogView)
                        .setPositiveButton(getResources().getString(R.string.dialog_button_confirm),null)
                        .setNegativeButton(getResources().getString(R.string.dialog_button_cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                            }
                        })
                        .show();
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int port=5656;
                        try{
                            port=Integer.parseInt(edit.getText().toString().trim());
                            if(!(port>=1024&&port<=65535)){
                                Toast.makeText(MainActivity.this,getResources().getString(R.string.attention_port_number_out_of_range),Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }catch (Exception e){
                            e.printStackTrace();
                            Toast.makeText(MainActivity.this,getResources().getString(R.string.attention_invalid_port_number),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        if(FtpService.isFTPServiceRunning()){
                            Toast.makeText(MainActivity.this,getResources().getString(R.string.attention_ftp_is_running),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        editor.putInt(Constants.PreferenceConsts.PORT_NUMBER,port);
                        editor.apply();
                        dialog.cancel();
                        tv_port.setText(String.valueOf(port));
                    }
                });
            }
        });

        findViewById(R.id.wakelock_area).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(settings.getBoolean(Constants.PreferenceConsts.WAKE_LOCK,Constants.PreferenceConsts.WAKE_LOCK_DEFAULT)){
                    cb_wakelock.setChecked(false);
                    FtpService.sendEmptyMessage(FtpService.MESSAGE_WAKELOCK_RELEASE);
                    editor.putBoolean(Constants.PreferenceConsts.WAKE_LOCK,false);
                    editor.apply();
                }else{
                    cb_wakelock.setChecked(true);
                    FtpService.sendEmptyMessage(FtpService.MESSAGE_WAKELOCK_ACQUIRE);
                    editor.putBoolean(Constants.PreferenceConsts.WAKE_LOCK,true);
                    editor.apply();
                }
            }
        });

        findViewById(R.id.anonymous_path).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return;
                }
                DialogOfFolderSelector dialog=new DialogOfFolderSelector(MainActivity.this,
                        settings.getString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,Constants.PreferenceConsts.ANONYMOUS_MODE_PATH_DEFAULT));
                dialog.show();
                dialog.setOnFolderSelectorDialogConfirmedListener(new DialogOfFolderSelector.OnFolderSelectorDialogConfirmed() {
                    @Override
                    public void onFolderSelectorDialogConfirmed(String path) {
                        if(FtpService.isFTPServiceRunning()){
                            Toast.makeText(MainActivity.this,getResources().getString(R.string.attention_ftp_is_running),Toast.LENGTH_SHORT).show();
                            return;
                        }
                        editor.putString(Constants.PreferenceConsts.ANONYMOUS_MODE_PATH,path);
                        editor.apply();
                        tv_anonymous_path.setText(path);
                    }
                });
            }
        });

        findViewById(R.id.anonymous_writable).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return;
                }
                if(settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE_DEFAULT)){
                    cb_anonymous_writable.setChecked(false);
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,false);
                    editor.apply();
                }else{
                    cb_anonymous_writable.setChecked(true);
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE_WRITABLE,true);
                    editor.apply();
                }
            }
        });
    }

    private void onNavigationMainSelected(){
        try{
            findViewById(R.id.view_main).setVisibility(View.VISIBLE);
            findViewById(R.id.view_settings).setVisibility(View.GONE);
            menu.getItem(MENU_ACCOUNT_ADD).setVisible(false);
            menu.getItem(MENU_ANONYMOUS_SWITCH).setVisible(false);
            getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
        }catch (Exception e){e.printStackTrace();}

    }

    private void onNavigationAccountSelected(){
        try{
            findViewById(R.id.view_main).setVisibility(View.GONE);
            findViewById(R.id.view_settings).setVisibility(View.VISIBLE);
            menu.getItem(MENU_ACCOUNT_ADD).setVisible(!isAnonymousMode());
            menu.getItem(MENU_ANONYMOUS_SWITCH).setVisible(true);
            findViewById(R.id.mode_anonymous).setVisibility(isAnonymousMode()?View.VISIBLE:View.GONE);
            findViewById(R.id.view_user_list).setVisibility(isAnonymousMode()?View.GONE:View.VISIBLE);
            getSupportActionBar().setTitle(getResources().getString(R.string.title_settings));
        }catch (Exception e){e.printStackTrace();}

    }

    private void showAttentionOfFTPisRunning(){
        try{
            Snackbar.make(findViewById(R.id.container),getResources().getString(R.string.attention_ftp_is_running),Snackbar.LENGTH_SHORT).show();
        }catch (Exception e){e.printStackTrace();}
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        menu.getItem(MENU_ANONYMOUS_SWITCH).setTitle(isAnonymousMode()?getResources().getString(R.string.action_main_anonymous_opened):getResources().getString(R.string.action_main_anonymous_closed));
        this.menu=menu;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            default:break;
            case R.id.action_main_add:{
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return true;
                }
                startActivityForResult(new Intent(this,AddAccountActivity.class),REQUEST_CODE_ADD);
                return true;
            }
            case R.id.action_main_anonymous_switch:{
                if(FtpService.isFTPServiceRunning()){
                    showAttentionOfFTPisRunning();
                    return true;
                }
                try{
                    SharedPreferences settings=getSharedPreferences(Constants.PreferenceConsts.FILE_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor=settings.edit();
                    boolean isAnonymousMode=settings.getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT);
                    editor.putBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,!isAnonymousMode);
                    editor.apply();
                    menu.getItem(MENU_ANONYMOUS_SWITCH).setTitle((!isAnonymousMode)?getResources().getString(R.string.action_main_anonymous_opened):getResources().getString(R.string.action_main_anonymous_closed));
                    menu.getItem(MENU_ACCOUNT_ADD).setVisible(isAnonymousMode);
                    findViewById(R.id.view_user_list).setVisibility((!isAnonymousMode)?View.GONE:View.VISIBLE);
                    findViewById(R.id.mode_anonymous).setVisibility((!isAnonymousMode)?View.VISIBLE:View.GONE);
                    if(isAnonymousMode) listview_users.setAdapter(new AccountAdapter(FtpService.getAccountList(this)));
                }catch (Exception e){e.printStackTrace();}
            }
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            default:break;
            case REQUEST_CODE_ADD: case REQUEST_CDOE_EDIT:{
                if(resultCode==RESULT_OK){
                    listview_users.setAdapter(new AccountAdapter(FtpService.getUserAccountList(this)));
                }
            }
            break;
        }
    }

    private boolean isAnonymousMode(){
        try{
            return getSharedPreferences(Constants.PreferenceConsts.FILE_NAME,Context.MODE_PRIVATE).getBoolean(Constants.PreferenceConsts.ANONYMOUS_MODE,Constants.PreferenceConsts.ANONYMOUS_MODE_DEFAULT);
        }catch (Exception e){e.printStackTrace();}
        return false;
    }

    private class AccountAdapter extends BaseAdapter{
        List<AccountItem> list;
        AccountAdapter(List<AccountItem> list){
            if(list==null) this.list=new ArrayList<>();
            this.list=list;
        }
        @Override
        public int getCount() {
            return list.size()+3;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView==null){
                convertView= LayoutInflater.from(MainActivity.this).inflate(R.layout.item_account,parent,false);
            }
            if(position>=list.size()) convertView.setVisibility(View.INVISIBLE);
            else convertView.setVisibility(View.VISIBLE);

            AccountItem item=null;
            try{
                item=list.get(position);
            }catch (Exception e){e.printStackTrace();}
            if(item==null) {
                convertView.setVisibility(View.GONE);
                return convertView;
            }

            TextView tv_account=convertView.findViewById(R.id.text_account);
            TextView tv_path=convertView.findViewById(R.id.text_path);
            View writable=convertView.findViewById(R.id.area_writable);
            tv_account.setText(item.account);
            tv_path.setText(item.path);
            writable.setVisibility(item.writable?View.VISIBLE:View.GONE);
            return convertView;
        }
    }

}
