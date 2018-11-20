package ink.xuming.entity;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by xuming on 2018/3/26.
 */

public class Word extends RealmObject {
    @PrimaryKey
    private int id;
    private String spell;
    private String meaning;
    private Date addTime;
    private int playCount;

    public Word() {
    }

    public Word(int id, String spell, String meaning) {
        this.id = id;
        this.spell = spell;
        this.meaning = meaning;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSpell() {
        return spell;
    }

    public void setSpell(String spell) {
        this.spell = spell;
    }

    public String getMeaning() {
        return meaning;
    }

    public void setMeaning(String meaning) {
        this.meaning = meaning;
    }

    public Date getAddTime() {
        return addTime;
    }

    public int getPlayCount() {
        return playCount;
    }

    public void setPlayCount(int playCount) {
        this.playCount = playCount;
    }

    public void playCountIncr(){
        this.playCount++;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }
}
