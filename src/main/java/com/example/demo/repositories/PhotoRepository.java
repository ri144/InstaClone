package com.example.demo.repositories;


import com.example.demo.models.Photo;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;


public interface PhotoRepository extends CrudRepository<Photo, Long>{
    Photo findById(Long id);
    List<Photo> findAllByUserid(Long userid);
}
