package ink.xuming.entity;

import java.util.Date;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

/**
 * Created by xuming on 2018/3/26.
 */

public class Sentence extends RealmObject{

    @PrimaryKey
    private int id;
    private String content;
    private Date addTime;


    public Sentence() {

    }

    public Sentence(int id, String content, Date addTime) {
        this.id = id;
        this.content = content;
        this.addTime = addTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getAddTime() {
        return addTime;
    }

    public void setAddTime(Date addTime) {
        this.addTime = addTime;
    }
}

