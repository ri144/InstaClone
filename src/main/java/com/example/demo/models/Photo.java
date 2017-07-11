package com.example.demo.models;



import com.cloudinary.StoredFile;

import javax.persistence.*;
import java.util.Date;

@Entity(name = "photos")
public class Photo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private long userid;

    @Basic
    private String image;

    @Basic
    private Date createdAt = new Date();

    private int likecounter;

    public Photo(){
        this.likecounter = 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public StoredFile getUpload() {
        StoredFile file = new StoredFile();
        file.setPreloadedFile(image);
        return file;
    }

    public void setUpload(StoredFile file) {
        this.image = file.getPreloadedFile();
    }

    public long getUserid() {
        return userid;
    }

    public void setUserid(long userid) {
        this.userid = userid;
    }

    public int getLikecounter() {
        return likecounter;
    }

    public void setLikecounter(int likecounter) {
        this.likecounter = likecounter;
    }
}
