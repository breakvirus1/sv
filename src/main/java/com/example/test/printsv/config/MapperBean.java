package com.example.test.printsv.config;

import java.beans.BeanProperty;

import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.test.printsv.mapper.UserMapper;

@Configuration
public class MapperBean {
    @Bean
    public UserMapper userMapper(){
        return Mappers.getMapper(UserMapper.class);
    }
    
    


}
