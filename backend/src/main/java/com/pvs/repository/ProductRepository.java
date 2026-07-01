package com.pvs.repository;

import java.util.*;
import com.pvs.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllByWidIn(Collection<String> wids);

}