package com.sabotinski.mongodbexample.customerservice.api.controller;

import java.util.List;

import com.sabotinski.mongodbexample.customerservice.api.dao.CustomerDao;
import com.sabotinski.mongodbexample.customerservice.api.models.Customer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/customers")
public class CustomerController {

    @Autowired
    private CustomerDao dao;

    @GetMapping()
    public List<Customer> getCustomers() {
        var customers = dao.getCustomers();
        return customers;
    }

    @GetMapping("{id}")
    public Customer getCustomerById(@PathVariable("id") String id) {
        return dao.getCustomer(id);
    }

    @PostMapping()
    public void createCustomer(@RequestBody Customer customer) {
        dao.createCustomer(customer);
    }
}