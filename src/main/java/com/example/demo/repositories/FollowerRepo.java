package com.example.demo.repositories;

import com.example.demo.models.Follower;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Created by student on 7/11/17.
 */
public interface FollowerRepo extends CrudRepository<Follower, Long> {
    List<Follower> findAllByUserid(Long userid);
    Follower findByUseridAndPersonid(Long userid, Long personid);
    @Transactional
    void deleteByUseridAndPersonid(Long userid, Long personid);
}
