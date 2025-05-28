package com.example.test.printsv.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.test.printsv.entity.User;


public interface UserRepository extends JpaRepository<User,Long>{
    User findByName(String name);
    


}
