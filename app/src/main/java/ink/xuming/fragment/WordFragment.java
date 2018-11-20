package ink.xuming.fragment;

import android.database.sqlite.SQLiteBindOrColumnIndexOutOfRangeException;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.scwang.smartrefresh.layout.api.RefreshLayout;
import com.scwang.smartrefresh.layout.listener.OnLoadMoreListener;
import com.scwang.smartrefresh.layout.listener.OnRefreshListener;

import ink.xuming.R;
import ink.xuming.adapter.SentenceAdapter;
import ink.xuming.adapter.WordAdapter;
import ink.xuming.entity.Word;
import ink.xuming.service.PlayVoiceService;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import me.drakeet.materialdialog.MaterialDialog;

public class WordFragment extends Fragment {

    private Realm mRealm;
    private RealmResults<Word> results;
    private ListView listView;
    private WordAdapter adapter;
    private PlayVoiceService.PlayVoiceBinder playVoiceBinder;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_word, container, false);
        //初始化view
        init(rootView);
        //初始化DB
        mRealm = Realm.getDefaultInstance();
        //查询所有数据
        queryWordList();
        //显示listView数据  更新适配器
        setAdapter();
        return rootView;
    }

    /**
     * 设置适配器recyclerViewAdapter
     */
    private void setAdapter() {
        adapter = new WordAdapter(getActivity(), results, playVoiceBinder);
        listView.setAdapter(adapter);
    }

    /**
     * 初始化视图
     * @param rootView
     */
    private void init(View rootView) {
        //初始化控件
        listView = (ListView) rootView.findViewById(R.id.listView);
        //初始化下拉刷新layout
        RefreshLayout refreshLayout = (RefreshLayout)rootView.findViewById(R.id.refreshLayout);
        refreshLayout.setDragRate(0.5f);//显示下拉高度/手指真实下拉高度=阻尼效果
        refreshLayout.setHeaderTriggerRate(1);//触发刷新距离 与 HeaderHeight 的比率1.0.4
        refreshLayout.setFooterTriggerRate(1);//触发加载距离 与 FooterHeight 的比率1.0.4

        refreshLayout.setEnableRefresh(true);//是否启用下拉刷新功能
        refreshLayout.setEnableLoadMore(false);//是否启用上拉加载功能
        refreshLayout.setEnableAutoLoadMore(false);//是否启用列表惯性滑动到底部时自动加载更多
        refreshLayout.setEnablePureScrollMode(false);//是否启用纯滚动模式
        refreshLayout.setEnableNestedScroll(false);//是否启用嵌套滚动
        refreshLayout.setEnableOverScrollBounce(true);//是否启用越界回弹
        refreshLayout.setEnableScrollContentWhenLoaded(true);//是否在加载完成时滚动列表显示新的内容
        refreshLayout.setEnableHeaderTranslationContent(true);//是否下拉Header的时候向下平移列表或者内容
        refreshLayout.setEnableFooterTranslationContent(true);//是否上拉Footer的时候向上平移列表或者内容
        refreshLayout.setEnableLoadMoreWhenContentNotFull(false);//是否在列表不满一页时候开启上拉加载功能
        refreshLayout.setEnableFooterFollowWhenLoadFinished(false);//是否在全部加载结束之后Footer跟随内容1.0.4
        refreshLayout.setEnableOverScrollDrag(false);//是否启用越界拖动（仿苹果效果）1.0.4
        refreshLayout.autoRefresh();//自动刷新
        refreshLayout.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh(RefreshLayout refreshlayout) {
                refreshlayout.finishRefresh();//传入false表示刷新失败
            }
        });
        refreshLayout.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore(RefreshLayout refreshlayout) {
                refreshlayout.finishLoadMore();//传入false表示加载失败
            }
        });
    }

    /**
     * 查询短语列表
     */
    private void queryWordList() {
        results = mRealm.where(Word.class).findAll().sort("addTime", Sort.DESCENDING);
    }

    public void setPlayVoiceBinder(PlayVoiceService.PlayVoiceBinder playVoiceBinder) {
        this.playVoiceBinder = playVoiceBinder;
        adapter.setPlayVoiceBinder(playVoiceBinder);
    }

    /**
     * 更新listView
     */
    public void updateListView() {
        if(adapter!=null){
            adapter.notifyDataSetChanged();
        }
    }
}