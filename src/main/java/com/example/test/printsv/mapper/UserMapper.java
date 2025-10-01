package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserResponse toUserResponse(User user);
}
