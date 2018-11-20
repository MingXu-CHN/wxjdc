package ink.xuming.fragment;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.github.mr5.icarus.Callback;
import com.github.mr5.icarus.Icarus;
import com.github.mr5.icarus.TextViewToolbar;
import com.github.mr5.icarus.button.Button;
import com.github.mr5.icarus.button.TextViewButton;
import com.github.mr5.icarus.entity.Options;
import com.google.gson.Gson;

import java.util.HashMap;

import ink.xuming.R;
import ink.xuming.entity.Sentence;

/**
 * Created by xuming on 2018/3/25.
 */

public class EditorFragment extends Fragment {

    private WebView webView;
    private Icarus icarus;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.editor_main, null);
        //初始化控件
        setViews(view);
        return view;
    }

    /**
     * 初始化空间
     * @param view
     */
    private void setViews(View view) {
        webView = (WebView) view.findViewById(R.id.editor);
        renderIcarus(webView, view);
    }

    private void renderIcarus (WebView webViewEN, View view){
        TextViewToolbar toolbar = new TextViewToolbar();
        Options options = new Options();
        options.setPlaceholder("输入英文短语及释义...");
        icarus = new Icarus(toolbar, options, webViewEN);
        prepareToolbar(toolbar, icarus, view);
        icarus.loadCSS("file:///android_asset/editor.css");
        icarus.loadJs("file:///android_asset/test.js");
        icarus.render();
    }

    /**
     * 准备工具栏
     * @param toolbar
     * @param icarus
     */
    private TextViewToolbar prepareToolbar(TextViewToolbar toolbar, Icarus icarus, View view) {
        Typeface iconfont = Typeface.createFromAsset(getActivity().getAssets(), "Simditor.ttf");
        HashMap<String, Integer> generalButtons = new HashMap<>();
        generalButtons.put(Button.NAME_BOLD, R.id.button_bold);
        generalButtons.put(Button.NAME_UL, R.id.button_list_ul);
        generalButtons.put(Button.NAME_ALIGN_LEFT, R.id.button_align_left);
        generalButtons.put(Button.NAME_ALIGN_CENTER, R.id.button_align_center);
        generalButtons.put(Button.NAME_ALIGN_RIGHT, R.id.button_align_right);
        generalButtons.put(Button.NAME_ITALIC, R.id.button_italic);
        generalButtons.put(Button.NAME_UNDERLINE, R.id.button_underline);
        generalButtons.put(Button.NAME_STRIKETHROUGH, R.id.button_strike_through);

        for (String name : generalButtons.keySet()) {
            TextView textView = (TextView) view.findViewById(generalButtons.get(name));
            if (textView == null) {
                continue;
            }
            textView.setTypeface(iconfont);
            TextViewButton button = new TextViewButton(textView, icarus);
            button.setName(name);
            toolbar.addButton(button);
        }
        return toolbar;
    }


    /**
     * 获取内容中的生词并给出解释
     * @return
     */
    public void getNewWords(final NewWordsCallback callback){
        getContent(new Callback() {
            @Override
            public void run(String params) {
                if(params==null || params.equals("") || !params.contains("</p><p>")){
                    return;
                }
                //解析params，封装hashmap
                Gson gson = new Gson();
                Sentence s = gson.fromJson(params, Sentence.class);
                String content = s.getContent();
                content = content.replaceAll("</p><p>", "|||");
                content = content.replaceAll("<p>", "");
                content = content.replaceAll("</p>", "");
                HashMap<String, String> map = new HashMap<>();
                String[] ss = content.split("\\|\\|\\|");
                String spell = ss[0];
                String meaning = ss[1];
                map.put(spell.substring(spell.indexOf("<b>")+3, spell.indexOf("</b>")), meaning.substring(meaning.indexOf("<b>")+3, meaning.indexOf("</b>")));
                Log.i("info", map.toString());
                callback.getWords(map);
            }
        });
    }

    public interface NewWordsCallback{
        void getWords(HashMap<String, String> words);
    }

    /**
     * 获取编辑内容
     * @return
     */
    public void getContent(Callback callback){
        icarus.getContent(callback);
    }

}
