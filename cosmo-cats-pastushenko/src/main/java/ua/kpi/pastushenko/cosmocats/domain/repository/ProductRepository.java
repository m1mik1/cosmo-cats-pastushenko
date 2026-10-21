package ua.kpi.pastushenko.cosmocats.domain.repository;

import ua.kpi.pastushenko.cosmocats.domain.model.PageRequest;
import ua.kpi.pastushenko.cosmocats.domain.model.Product;
import ua.kpi.pastushenko.cosmocats.domain.model.ProductPage;

import java.util.Optional;

public interface ProductRepository {

    Product create(Product product);

    Optional<Product> findById(long id);

    ProductPage findPage(PageRequest request);

    Optional<Product> update(long id, Product product);

    boolean deleteById(long id);
}
