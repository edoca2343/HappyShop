package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class CustomerModelTest2 {


@Test
public void testBuildStringmultipleproducts(){



Product p2 = new Product("0001", "TV","0001.jpg",12.01,100);
Product p3 = new Product("0002", "DAB Radio","0002.jpg",29.99,2);
    CustomerModel cm = new CustomerModel();
    cm.setTheProduct(p2);
    cm.makeOrganizedTrolley();


    cm.setTheProduct(p3);
    cm.makeOrganizedTrolley();

    ArrayList<Product> tro = cm.getTrolley();

    assertEquals(2, tro.size());
    double total = 0.0;
    for(Product p:tro){
        total += p.getUnitPrice();
    }
    assertEquals(42.0,total, tro.size(),"total is "+total + "total should be 42.0");


    System.out.println("Test passed total is "+ total);
        }
}