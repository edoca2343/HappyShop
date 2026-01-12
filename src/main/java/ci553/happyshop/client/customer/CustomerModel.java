package ci553.happyshop.client.customer;

import ci553.happyshop.catalogue.Order;
import ci553.happyshop.catalogue.Product;
import ci553.happyshop.catalogue.exceptions.ExcessiveOrderQuantityException;
import ci553.happyshop.catalogue.exceptions.UnderMinimumPayment;
import ci553.happyshop.storageAccess.DatabaseRW;
import ci553.happyshop.orderManagement.OrderHub;
import ci553.happyshop.utility.ProductListFormatter;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * TODO
 * You can either directly modify the CustomerModel class to implement the required tasks,
 * or create a subclass of CustomerModel and override specific methods where appropriate.
 */
public class CustomerModel {
    public CustomerView cusView;
    public DatabaseRW databaseRW; //Interface type, not specific implementation
    public RemoveProductNotifier removeProductNotifier;
    public ExceptionWindow exceptionWindow;
    //Benefits: Flexibility: Easily change the database implementation.

    private  ArrayList<Product> productList = new ArrayList<>();
    private Product theProduct = null; // product found from search
    private ArrayList<Product> trolley = new ArrayList<>(); // a list of products in trolley

    private String ErrorMessage  = " ";
    // Four UI elements to be passed to CustomerView for display updates.

    private String imageName = "imageHolder.jpg";                // Image to show in product preview (Search Page)
    private String displayLaSearchResult = "No Product was searched yet"; // Label showing search result message (Search Page)
    private String displayTaTrolley = "";                                // Text area content showing current trolley items (Trolley Page)
    private String displayTaReceipt = "";                                // Text area content showing receipt after checkout (Receipt Page)

    //SELECT productID, description, image, unitPrice,inStock quantity
    void dosearch() throws SQLException {



            String keyword = cusView.tfSearchKeyword.getText().trim();

            if (!keyword.equals("")) {
                    productList  = databaseRW.searchProduct(keyword);

            } else{
                productList.clear();
                displayLaSearchResult = "Please type ProductID into the search";
                System.out.println("Please type ProductID into the search.");


            }
            updateView();
        }


    void addToTrolley() {
        theProduct  = cusView.obrLvProducts.getSelectionModel().getSelectedItem();
        if (theProduct != null) {

            // trolley.add(theProduct) — Product is appended to the end of the trolley.
            // To keep the trolley organized, add code here or call a method that:
            //TODO
            // 1. Merges items with the same product ID (combining their quantities).
            // 2. Sorts the products in the trolley by product ID.
            makeOrganizedTrolley();
            displayTaTrolley = ProductListFormatter.buildString(trolley); //build a String for trolley so that we can show it
        } else {
            displayLaSearchResult = "Please search for an available product before adding it to the trolley";
            System.out.println("must search and get an available product before add to trolley");
        }
        displayTaReceipt = ""; // Clear receipt to switch back to trolleyPage (receipt shows only when not empty)
        updateView();
    }



    void makeOrganizedTrolley() {
        for (Product p : trolley) {
            if (p.getProductId().equals(theProduct.getProductId())) {
                p.setOrderedQuantity(p.getOrderedQuantity() + theProduct.getOrderedQuantity());
                return;
            }
        }
        Product pNew = new Product(theProduct.getProductId(), theProduct.getProductDescription(), theProduct.getProductImageName(),
                theProduct.getUnitPrice(), theProduct.getStockQuantity());
        trolley.add(pNew);
        Collections.sort(trolley, Comparator.comparing(Product::getProductId));
    }




    void checkOut() throws IOException, SQLException {
        if(!trolley.isEmpty()){
            try{
                if(checkTotalPrice()<5){
                    throw new UnderMinimumPayment("underMinimumPayment");
                }
                if(checkTotalQuantity()>49){
                    throw new ExcessiveOrderQuantityException("ExcessiveOrderQuantityException");
                }


                // Group the products in the trolley by productId to optimize stock checking
                // Check the database for sufficient stock for all products in the trolley.
                // If any products are insufficient, the update will be rolled back.
                // If all products are sufficient, the database will be updated, and insufficientProducts will be empty.
                // Note: If the trolley is already organized (merged and sorted), grouping is unnecessary.
//            ArrayList<Product> groupedTrolley= groupProductsById(trolley);
                ArrayList<Product> insufficientProducts= databaseRW.purchaseStocks(trolley);
                if(insufficientProducts.isEmpty()){ // If stock is sufficient for all products
                    //get OrderHub and tell it to make a new Order
                    OrderHub orderHub =OrderHub.getOrderHub();
                    Order theOrder = orderHub.newOrder(trolley);
                    trolley.clear();
                    displayTaTrolley ="";
                    displayTaReceipt = String.format(
                            "Order_ID: %s\nOrdered_Date_Time: %s\n%s",
                            theOrder.getOrderId(),
                            theOrder.getOrderedDateTime(),
                            ProductListFormatter.buildString(theOrder.getProductList())
                    );
                    System.out.println(displayTaReceipt);
                }
                else{ // Some products have insufficient stock — build an error message to inform the customer
                    StringBuilder errorMsg = new StringBuilder();
                    for(Product p : insufficientProducts){
                        errorMsg.append("\u2022 "+ p.getProductId()).append(", ")
                                .append(p.getProductDescription()).append(" (Only ")
                                .append(p.getStockQuantity()).append(" available, ")
                                .append(p.getOrderedQuantity()).append(" requested)\n");
                    }
                    theProduct=null;



                    //TODO
                    // Add the following logic here:
                    // 1. Remove products with insufficient stock from the trolley.
                    // 2. Trigger a message window to notify the customer about the insufficient stock, rather than directly changing displayLaSearchResult.
                    //You can use the provided RemoveProductNotifier class and its showRemovalMsg method for this purpose.
                    //remember close the message window where appropriate (using method closeNotifierWindow() of RemoveProductNotifier class)



                    for (Product p : insufficientProducts){ trolley.remove(p);}

                    displayTaTrolley = ProductListFormatter.buildString(trolley);
                    ErrorMessage = errorMsg.toString();
                    removeProductNotifier.showRemovalMsg(ErrorMessage);
//reimplemented the error message to string to help improve what errors are shown within the program.


//                displayLaSearchResult = "Checkout failed due to insufficient stock for the following products:\n" + errorMsg.toString();
//                System.out.println("stock is not enough");
                }
            } catch(UnderMinimumPayment e){
                System.out.println("underMinimumPayment exception, order cannot be processed");
                exceptionWindow.showExceptionMsg("Sorry,we do not accept an online order that is under £5 Please Pay with cash. ");
            }
            catch(ExcessiveOrderQuantityException e){
                System.out.println("ExcessiveOrderQuantity exception, order cannot be processed");
                exceptionWindow.showExceptionMsg("We do not accept an order that has 50 or more items in it.");

            }

        }
        else{
            displayTaTrolley = "Your trolley is empty";
            System.out.println("Your trolley is empty");
        }
        updateView();

    }

    private double checkTotalPrice(){
        double price = 0;

        for (Product p : trolley) {
            price = price + p.getUnitPrice() * p.getOrderedQuantity();

        }
        return price;


    }
 private int checkTotalQuantity(){
     int orderQuantity = 0;
     for (Product p : trolley) {
         orderQuantity = orderQuantity + p.getOrderedQuantity();
     }

     return orderQuantity;
 }




    /**
     * Groups products by their productId to optimize database queries and updates.
     * By grouping products, we can check the stock for a given `productId` once, rather than repeatedly
     */
    private ArrayList<Product> groupProductsById(ArrayList<Product> proList) {
        Map<String, Product> grouped = new HashMap<>();
        for (Product p : proList) {
            String id = p.getProductId();
            if (grouped.containsKey(id)) {
                Product existing = grouped.get(id);
                existing.setOrderedQuantity(existing.getOrderedQuantity() + p.getOrderedQuantity());
            } else {
                // Make a shallow copy to avoid modifying the original
                grouped.put(id,new Product(p.getProductId(),p.getProductDescription(),
                        p.getProductImageName(),p.getUnitPrice(),p.getStockQuantity()));
            }
        }
        return new ArrayList<>(grouped.values());
    }

    void cancel(){
        trolley.clear();
        displayTaTrolley="";
        updateView();
    }
    void closeReceipt(){
        displayTaReceipt="";
    }

   void updateView() {
//        if(theProduct != null){
//            imageName = theProduct.getProductImageName();
//            String relativeImageUrl = StorageLocation.imageFolder +imageName; //relative file path, eg images/0001.jpg
//            // Get the full absolute path to the image
//            Path imageFullPath = Paths.get(relativeImageUrl).toAbsolutePath();
//            imageName = imageFullPath.toUri().toString(); //get the image full Uri then convert to String
//            System.out.println("Image absolute path: " + imageFullPath); // Debugging to ensure path is correct
//        }
//        else{
//            imageName = "imageHolder.jpg";
//        }
        cusView.update(productList, displayTaTrolley,displayTaReceipt);
    }
     // extra notes:
     //Path.toUri(): Converts a Path object (a file or a directory path) to a URI object.
     //File.toURI(): Converts a File object (a file on the filesystem) to a URI object

    //for test only
    public ArrayList<Product> getTrolley() {


        return trolley;
    }
    public void setTheProduct(Product theProduct) {
        this.theProduct = theProduct;
    }


}

