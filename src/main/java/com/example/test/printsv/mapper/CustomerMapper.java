package com.example.test.printsv.mapper;

import com.example.test.printsv.entity.Customer;
import com.example.test.printsv.request.CustomerRequest;
import com.example.test.printsv.response.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CustomerMapper {

    @Mapping(target = "id", ignore = true)
    CustomerResponse toCustomerResponse(Customer customer);

    @Mapping(target = "id", ignore = true)
    Customer toCustomer(CustomerRequest customerRequest);


}