package com.example.demo.repositories;

import com.example.demo.models.Likes;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Created by student on 7/14/17.
 */
public interface LikesRepo extends CrudRepository<Likes, Long> {
    @Transactional
    void deleteByUserid(Long userid);
    List<Likes> findAllByPhotoid(Long photoid);
    Likes findByPhotoidAndUserid(Long photoid, Long userid);
}
