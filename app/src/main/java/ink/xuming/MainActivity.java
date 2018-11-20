package ink.xuming;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.github.mr5.icarus.Callback;
import com.google.gson.Gson;
import com.tekinarslan.material.sample.SlidingTabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.xml.transform.ErrorListener;

import ink.xuming.adapter.ViewPagerAdapter;

import ink.xuming.app.MyApplication;
import ink.xuming.entity.Sentence;
import ink.xuming.entity.Word;
import ink.xuming.fragment.EditorFragment;
import ink.xuming.fragment.SentenceFragment;
import ink.xuming.fragment.WordFragment;
import ink.xuming.service.PlayVoiceService;
import ink.xuming.service.PlayVoiceService.PlayVoiceBinder;
import ink.xuming.utils.URLFactory;
import io.realm.Realm;
import me.drakeet.materialdialog.MaterialDialog;

public class MainActivity extends ActionBarActivity {

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle drawerToggle;
    private ListView mDrawerList;
    private SlidingTabLayout slidingTabLayout;

    private FloatingActionButton fabPlus;
    private FloatingActionButton fabWord;
    private FloatingActionButton fabEdit;
    private FrameLayout fabMaskLayout;
    private FrameLayout frameLayoutEditor;
    private MaterialDialog mMaterialDialog;
    private MaterialDialog checkUpdateDialog;
    private View dialogView;

    private ViewPager pager;
    private String[] titles = new String[]{"记短语", "记单词"};
    private Fragment[] fragments =  new Fragment[]{new SentenceFragment(), new WordFragment()};

    private Toolbar toolbar;
    private FragmentManager fm;
    private EditorFragment editFragment;
    private Realm realm;
    private PlayVoiceBinder playVoiceBinder;
    private String APK_URL;



    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case HANDLER_SAVE_SENTENCE:
                    realm.beginTransaction();
                    Number maxValue = realm.where(Sentence.class).max("id");
                    long pk = (maxValue != null) ? maxValue.intValue() + 1 : 0;
                    Sentence sentence = realm.createObject(Sentence.class, pk);
                    sentence.setAddTime(new Date());
                    sentence.setContent(msg.obj.toString());
                    realm.commitTransaction();
                    break;
                case HANDLER_SAVE_WORD:
                    //存入单词表
                    HashMap<String, String> words = (HashMap<String, String>) msg.obj;
                    if(words!=null && words.size()>0){
                        Iterator<String> iterator = words.keySet().iterator();
                        maxValue = realm.where(Word.class).max("id");
                        pk = (maxValue != null) ? maxValue.intValue() + 1 : 0;
                        while(iterator.hasNext()){
                            String key = iterator.next();
                            String val = words.get(key);
                            //直接把html内容存入DB
                            realm.beginTransaction();
                            Word word = realm.createObject(Word.class, pk++);
                            word.setAddTime(new Date());
                            word.setSpell(key);
                            word.setMeaning(val);
                            realm.commitTransaction();
                        }
                    }
                    try{
                        mMaterialDialog.dismiss();
                        ((WordFragment)fragments[1]).updateListView();
                    }catch (RuntimeException e){
                        e.printStackTrace();
                    }
                    break;
            }
        }
    };

    private static final int HANDLER_SAVE_SENTENCE = 1;
    private static final int HANDLER_SAVE_WORD = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //控件初始化
        init();
        setListener();
        //绑定service
        bindProjectService();
        //初始化数据库
        realm = Realm.getDefaultInstance();
        //检查更新
        //checkUpdate();
    }

    // 检查更新
    private void checkUpdate() {
        String url = URLFactory.getCheckUpdateUrl();
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            public void onResponse(String response) {
                try {
                    JSONObject obj = new JSONObject(response);
                    int code=obj.getInt("VERSION_CODE");
                    APK_URL = obj.getString("APK_URL");
                    if(code > MyApplication.getApp().getCurrentVersionCode()){ //该更新版本了
                        showDownloadApkDialog();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            public void onErrorResponse(VolleyError error) {
            }
        });
        Volley.newRequestQueue(this).add(request);
    }

    /** 显示下载apk对话框 */
    private void showDownloadApkDialog(){
         checkUpdateDialog = new MaterialDialog(this)
                .setTitle("更新")
                .setMessage("发现新版本，是否更新？")
                .setPositiveButton("现在下载", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        downloadApk();
                    }
                }).setNegativeButton("以后再说", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkUpdateDialog.dismiss();
                    }
                });
         checkUpdateDialog.show();
    }

    /** 下载apk */
    private void downloadApk(){
        DownloadManager dManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(APK_URL);
        DownloadManager.Request request = new DownloadManager.Request(uri);
        // 设置下载路径和文件名
        request.setDestinationInExternalPublicDir("download", "我想记单词.apk");
        request.setDescription("哒哒音乐下载");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setMimeType("application/vnd.android.package-archive");
        // 设置为可被媒体扫描器找到
        request.allowScanningByMediaScanner();
        // 设置为可见和可管理
        request.setVisibleInDownloadsUi(true);
        long refernece = dManager.enqueue(request);
        // 把当前下载的ID保存起来
        SharedPreferences sPreferences = getSharedPreferences("downloadcomplete", 0);
        sPreferences.edit().putLong("refernece", refernece).commit();
    }


    /**
     * 绑定service
     */
    private void bindProjectService() {
        Intent intent = new Intent(this, PlayVoiceService.class);
        this.bindService(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
                playVoiceBinder = (PlayVoiceService.PlayVoiceBinder) iBinder;
                SentenceFragment f1 = (SentenceFragment) fragments[0];
                f1.setPlayVoiceBinder(playVoiceBinder);
                WordFragment f2 = (WordFragment) fragments[1];
                f2.setPlayVoiceBinder(playVoiceBinder);
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {

            }
        }, Service.BIND_AUTO_CREATE);
    }

    /**
     * 控件初始化
     */
    private void init() {
        //初始化所有控件
        fabPlus = (FloatingActionButton) findViewById(R.id.fabPlus);
        fabEdit = (FloatingActionButton) findViewById(R.id.fabEdit);
        fabWord = (FloatingActionButton) findViewById(R.id.fabWord);
        fabMaskLayout = (FrameLayout) findViewById(R.id.fabMaskLayout);
        frameLayoutEditor = (FrameLayout) findViewById(R.id.frameLayoutEditor);
        dialogView = View.inflate(MainActivity.this, R.layout.dialog_save_word, null);
        //初始化保存单词对话框
        mMaterialDialog = new MaterialDialog(this)
                .setTitle("记单词")
                .setView(dialogView)
                .setPositiveButton("关闭", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mMaterialDialog.dismiss();
                    }
                })
                .setNegativeButton("保存", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        EditText etSpell = (EditText) dialogView.findViewById(R.id.etSpell);
                        EditText etMeaning = (EditText) dialogView.findViewById(R.id.etMeaning);
                        HashMap<String, String> map = new HashMap<>();
                        map.put(etSpell.getText().toString(), etMeaning.getText().toString());
                        Message msg = new Message();
                        msg.what = HANDLER_SAVE_WORD;
                        msg.obj = map;
                        handler.sendMessage(msg);
                    }
                });
        //初始化左上角图标
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.navdrawer);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            toolbar.setNavigationIcon(R.drawable.ic_ab_drawer);
        }
        //初始化编辑器framelayout
        fm = getSupportFragmentManager();
        FragmentTransaction tr = fm.beginTransaction();
        editFragment = new EditorFragment();
        tr.add(R.id.frameLayoutEditor, editFragment);
        tr.commit();

        //初始化viewpager
        pager = (ViewPager) findViewById(R.id.viewpager);
        slidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        pager.setAdapter(new ViewPagerAdapter(getSupportFragmentManager(), titles, fragments));
        slidingTabLayout.setViewPager(pager);
        slidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return Color.WHITE;
            }
        });
        //左上角图标
        drawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, toolbar, R.string.app_name, R.string.app_name);
        mDrawerLayout.setDrawerListener(drawerToggle);
        String[] values = new String[]{
                "DEFAULT", "RED", "BLUE", "MATERIAL GREY"
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);
        mDrawerList.setAdapter(adapter);
    }

    /**
     * 设置监听
     */
    public void setListener(){
        //点击记单词按钮
        fabWord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDisplayPlusMenu();
                //弹出对话框
                mMaterialDialog.show();
            }
        });

        //点击记短语按钮
        fabEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDisplayPlusMenu();
                //从fabPlus中心显示揭露动画效果
                showEditor();
            }
        });

        //点击加号
        fabPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(frameLayoutEditor.getVisibility() == View.VISIBLE){
                    //保存编辑器的内容
                    saveSentences();
                    //关闭编辑器
                    closeEditor();
                }else {
                    toggleDisplayPlusMenu();
                }
            }
        });

        //策划菜单项
        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                switch (position) {
                    case 0:
                        mDrawerList.setBackgroundColor(getResources().getColor(R.color.material_deep_teal_500));
                        toolbar.setBackgroundColor(getResources().getColor(R.color.material_deep_teal_500));
                        slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.material_deep_teal_500));
                        mDrawerLayout.closeDrawer(Gravity.START);
                        break;
                    case 1:
                        mDrawerList.setBackgroundColor(getResources().getColor(R.color.red));
                        toolbar.setBackgroundColor(getResources().getColor(R.color.red));
                        slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.red));
                        mDrawerLayout.closeDrawer(Gravity.START);

                        break;
                    case 2:
                        mDrawerList.setBackgroundColor(getResources().getColor(R.color.blue));
                        toolbar.setBackgroundColor(getResources().getColor(R.color.blue));
                        slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.blue));
                        mDrawerLayout.closeDrawer(Gravity.START);

                        break;
                    case 3:
                        mDrawerList.setBackgroundColor(getResources().getColor(R.color.material_blue_grey_800));
                        toolbar.setBackgroundColor(getResources().getColor(R.color.material_blue_grey_800));
                        slidingTabLayout.setBackgroundColor(getResources().getColor(R.color.material_blue_grey_800));
                        mDrawerLayout.closeDrawer(Gravity.START);
                        break;
                }

            }
        });
    }

    /**
     * 保存编辑器中的短句内容
     */
    private void saveSentences(){

        //存储短语
        editFragment.getContent(new Callback() {
            @Override
            public void run(String params) {
                Gson gson = new Gson();
                Sentence s = gson.fromJson(params, Sentence.class);
                if(s==null || s.getContent().equals("")){
                    return;
                }
                //直接把html内容存入DB
                Message msg = new Message();
                msg.what = HANDLER_SAVE_SENTENCE;
                msg.obj = s.getContent();
                handler.sendMessage(msg);
            }
        });
        //存储单词
        editFragment.getNewWords(new EditorFragment.NewWordsCallback() {
            @Override
            public void getWords(HashMap<String, String> words) {
                Message msg = new Message();
                msg.what = HANDLER_SAVE_WORD;
                msg.obj = words;
                handler.sendMessage(msg);
            }
        });
    }

    /**
     * 以揭露效果显示编辑器
     */
    private void showEditor() {
        Animator animator = ViewAnimationUtils.createCircularReveal(
                frameLayoutEditor, (int)fabPlus.getX()+fabPlus.getWidth()/2, (int)fabPlus.getY()+fabPlus.getHeight()/2,
                fabPlus.getWidth()/2,
                frameLayoutEditor.getHeight()*2);
        animator.setDuration(500);
        frameLayoutEditor.setVisibility(View.VISIBLE);
        animator.start();
        //吧fabPlus按钮图标改为对勾
        fabPlus.setImageResource(R.drawable.right);
    }

    /**
     * 以揭露效果关闭编辑器
     */
    private void closeEditor() {
        Animator animator = ViewAnimationUtils.createCircularReveal(
                frameLayoutEditor, (int)fabPlus.getX()+fabPlus.getWidth()/2, (int)fabPlus.getY()+fabPlus.getHeight()/2,
                frameLayoutEditor.getHeight()*2,
                fabPlus.getWidth()/2);
        animator.setDuration(500);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                frameLayoutEditor.setVisibility(View.INVISIBLE);
            }
        });
        //吧fabPlus按钮图标改为加号
        fabPlus.setImageResource(R.drawable.plus);
    }

    /**
     * 切换显示加号菜单
     */
    boolean plusMenuVisible = false;
    private void toggleDisplayPlusMenu() {
        if(!plusMenuVisible){
            //遮罩层不可见，则显示菜单
            openPlusMenu();
        }else{
            //遮罩层可见，则隐藏菜单
            closePlusMenu();
        }

    }


    private void openPlusMenu(){
        plusMenuVisible = !plusMenuVisible;
        //遮罩层不可见，则显示菜单
        fabMaskLayout.setVisibility(View.VISIBLE);
        fabEdit.setVisibility(View.VISIBLE);
        fabWord.setVisibility(View.VISIBLE);
        //执行动画
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, "abc", 0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                //两个按钮显示动画
                fabWord.setScaleX(val);
                fabWord.setScaleY(val);
                fabEdit.setScaleX(val);
                fabEdit.setScaleY(val);
                //plus按钮旋转动画
                fabPlus.setRotation(135*val);
                //遮罩层透明动画
                fabMaskLayout.setAlpha(0.3f*val);
            }
        });
        anim.setDuration(200);
        anim.start();

    }

    private void closePlusMenu(){
        plusMenuVisible = !plusMenuVisible;
        //执行动画
        ObjectAnimator anim = ObjectAnimator.ofFloat(null, "abc", 1.0f, 0.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float val = (Float) valueAnimator.getAnimatedValue();
                //两个按钮显示动画
                fabWord.setScaleX(val);
                fabWord.setScaleY(val);
                fabEdit.setScaleX(val);
                fabEdit.setScaleY(val);
                //plus按钮旋转动画
                fabPlus.setRotation(135*val);
                //遮罩层透明动画
                fabMaskLayout.setAlpha(0.3f*val);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fabWord.setVisibility(View.INVISIBLE);
                fabEdit.setVisibility(View.INVISIBLE);
                fabMaskLayout.setVisibility(View.INVISIBLE);
            }
        });
        anim.setDuration(200);
        anim.start();
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (drawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case android.R.id.home:
                mDrawerLayout.openDrawer(Gravity.START);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        drawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        if(frameLayoutEditor.getVisibility()==View.VISIBLE){
            closeEditor();
        }else if(plusMenuVisible){
            closePlusMenu();
        }else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    public Realm getRealm() {
        return realm;
    }
}
