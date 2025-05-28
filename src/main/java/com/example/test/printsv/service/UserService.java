package com.example.test.printsv.service;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User createUser(User user) {
        return userRepository.save(user);
    }


    public User getUserById(Long id) {
        return userRepository.findById(id).orElse(null);
    }


    public User getUserByName(String name){
        return userRepository.findByName(name);
    }
    public Iterable<User> getAllUsers() {
        return userRepository.findAll();
    }

    // Update
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .orElse(null);

        if (existingUser != null) {
            existingUser.setName(updatedUser.getName());
            existingUser.setRole(updatedUser.getRole());
            existingUser.setPassword(updatedUser.getPassword());
            return userRepository.save(existingUser);
        }
        return null; // Or throw an exception if user not found
    }

    // Delete
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }
}