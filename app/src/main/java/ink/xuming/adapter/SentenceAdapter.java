package ink.xuming.adapter;

import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import ink.xuming.R;
import ink.xuming.entity.Sentence;
import io.realm.RealmResults;

public class SentenceAdapter extends BaseAdapter {
    private Context context;
    private RealmResults<Sentence> results;
    private LayoutInflater layoutInflater;

    public SentenceAdapter(Context context, RealmResults<Sentence> results) {
        this.context = context;
        this.results = results;
        this.layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public Sentence getItem(int i) {
        return results.get(i);
    }

    @Override
    public long getItemId(int i) {
        return results.get(i).getId();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder holder = null;
        if(view == null){
            view = layoutInflater.inflate(R.layout.item_listview_sentence, null);
            holder = new ViewHolder();
            holder.tvContent = (TextView) view.findViewById(R.id.tvContent);
            view.setTag(holder);
        }
        holder = (ViewHolder) view.getTag();
        //设置参数值
        Log.i("info","-----------content:"+getItem(i).getContent());
        holder.tvContent.setText(Html.fromHtml(getItem(i).getContent(), Html.FROM_HTML_MODE_COMPACT));
        return view;
    }

    class ViewHolder{
        TextView tvContent;
    }

}