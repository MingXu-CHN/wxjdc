package ink.xuming.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import ink.xuming.fragment.SentenceFragment;
import ink.xuming.fragment.WordFragment;
import ink.xuming.service.PlayVoiceService;

public class ViewPagerAdapter extends FragmentPagerAdapter {

    private String[] titles;
    private Fragment[] fragments;

    public ViewPagerAdapter(FragmentManager fm, String[] titles2, Fragment[] fragments) {
        super(fm);
        titles=titles2;
        this.fragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return fragments[position];
    }

    public CharSequence getPageTitle(int position) {
        return titles[position];
    }

    @Override
    public int getCount() {
        return titles.length;
    }

}