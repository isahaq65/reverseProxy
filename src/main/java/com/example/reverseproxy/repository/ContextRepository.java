package com.example.reverseproxy.repository;

import com.example.reverseproxy.entity.Context;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContextRepository extends JpaRepository<Context ,Integer > {


    Context findByServiceName(String serviceName);
}
