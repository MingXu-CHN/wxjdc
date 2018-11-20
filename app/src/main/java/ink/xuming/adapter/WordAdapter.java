package ink.xuming.adapter;

import android.content.Context;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import ink.xuming.R;
import ink.xuming.entity.Sentence;
import ink.xuming.entity.Word;
import ink.xuming.service.PlayVoiceService;
import io.realm.Realm;
import io.realm.RealmResults;
import me.drakeet.materialdialog.MaterialDialog;
import q.rorbin.badgeview.QBadgeView;

public class WordAdapter extends BaseAdapter {
    private PlayVoiceService.PlayVoiceBinder playVoiceBinder;
    private Context context;
    private RealmResults<Word> results;
    private LayoutInflater layoutInflater;
    private MaterialDialog mMaterialDialog;

    public WordAdapter(Context context, RealmResults<Word> results, PlayVoiceService.PlayVoiceBinder playVoiceBinder) {
        this.context = context;
        this.results = results;
        this.layoutInflater = LayoutInflater.from(context);
        this.playVoiceBinder = playVoiceBinder;
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Word getItem(int i) {
        return results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return results.get(i).getId();
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = null;
        if(view == null){
            view = layoutInflater.inflate(R.layout.item_listview_word, null);
            holder = new ViewHolder();
            holder.tvContent = (TextView) view.findViewById(R.id.tvContent);
            holder.ivPlayVoice = (ImageView) view.findViewById(R.id.ivPlayVoice);
            view.setTag(holder);
        }
        holder = (ViewHolder) view.getTag();
        //设置参数值
        final Word word = getItem(i);
        holder.tvContent.setText(Html.fromHtml("<p>"+word.getSpell()+"</p><p>"+word.getMeaning()+"</p>", Html.FROM_HTML_MODE_COMPACT));
        new QBadgeView(context).bindTarget(holder.ivPlayVoice).setBadgeNumber(word.getPlayCount());
        //点击播放按钮播放声音
        holder.ivPlayVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                playVoiceBinder.play("http://dict.youdao.com/dictvoice?audio="+word.getSpell());
            }
        });
        holder.tvContent.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                mMaterialDialog = new MaterialDialog(context)
                        .setTitle("删除")
                        .setMessage("确认删除吗？")
                        .setPositiveButton("关闭", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mMaterialDialog.dismiss();
                            }
                        })
                        .setNegativeButton("删除", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Realm mRealm = Realm.getDefaultInstance();
                                mRealm.beginTransaction();
                                mRealm.where(Word.class).equalTo("id", results.get(i).getId()).findAll().deleteFirstFromRealm();
                                mRealm.commitTransaction();
                                WordAdapter.this.notifyDataSetChanged();
                                mMaterialDialog.dismiss();
                            }
                        });
                mMaterialDialog.show();
                return false;
            }
        });
        return view;
    }

    public void setPlayVoiceBinder(PlayVoiceService.PlayVoiceBinder playVoiceBinder) {
        this.playVoiceBinder = playVoiceBinder;
    }

    class ViewHolder{
        TextView tvContent;
        ImageView ivPlayVoice;
    }

}