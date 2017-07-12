package com.example.demo.repositories;


import com.example.demo.models.User;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long>{

    User findByUsername(String username);

    User findByEmail(String email);

    Long countByEmail(String email);

    Long countByUsername(String username);

    List<User> findAllByUsernameIsContaining(String username);

    User findById(Long id);
}

