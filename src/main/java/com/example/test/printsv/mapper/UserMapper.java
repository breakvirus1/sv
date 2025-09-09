package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.User;
import com.example.test.printsv.request.UserRequest;
import com.example.test.printsv.response.UserResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    //    @Mapping(target = "id", ignore = true)
//    @Mapping(target = "zakazList", ignore = true)
    User toUser(UserRequest userRequest);

    // Optional<UserResponse> toUserResponseOptional(User userForGetInfo);
    //@Mapping(target = "username")
//    @Mapping(target = "role")
    UserResponse toUserResponse(User user);
}