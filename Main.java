import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

// JFreeChart Imports
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class Main {
    public static void main(String[] args) {
        // Launch Swing GUI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MarketplaceGUI gui = new MarketplaceGUI();
            gui.setVisible(true);
        });
    }
}


class MongoConnection {
    private static MongoConnection instance;
    private final MongoClient mongoClient;
    private final MongoDatabase database;

    private MongoConnection() {
        String uri = System.getenv("MONGO_URI");
        if (uri == null || uri.trim().isEmpty()) {
            // Hardcoded MongoDB Atlas URI Fallback
            uri = "";
        }
        this.mongoClient = MongoClients.create(uri);
        this.database = mongoClient.getDatabase("");
    }

    public static synchronized MongoConnection getInstance() {
        if (instance == null) {
            instance = new MongoConnection();
        }
        return instance;
    }

    public MongoDatabase getDatabase() {
        return database;
    }
}


class InvalidLoginException extends Exception {
    public InvalidLoginException(String message) { super(message); }
}

class InvalidPriceException extends Exception {
    public InvalidPriceException(String message) { super(message); }
}

class OutOfStockException extends Exception {
    public OutOfStockException(String message) { super(message); }
}


abstract class User {
    protected String id;
    protected String name;
    protected String email;
    protected String password;
    protected String role; // BUYER or SELLER

    public User(String id, String name, String email, String password, String role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getRole() { return role; }
}

class Seller extends User {
    public Seller(String id, String name, String email, String password) {
        super(id, name, email, password, "SELLER");
    }
}

class Buyer extends User {
    public Buyer(String id, String name, String email, String password) {
        super(id, name, email, password, "BUYER");
    }
}

class Product {
    private String id;
    private String sellerId;
    private String name;
    private String description;
    private String category;
    private double price;
    private int quantity;

    public Product(String id, String sellerId, String name, String description, String category, double price, int quantity) {
        this.id = id;
        this.sellerId = sellerId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getSellerId() { return sellerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
}

class Order {
    private String id;
    private String buyerId;
    private String productId;
    private int quantity;
    private double totalPrice;
    private String timestamp;

    public Order(String id, String buyerId, String productId, int quantity, double totalPrice, String timestamp) {
        this.id = id;
        this.buyerId = buyerId;
        this.productId = productId;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.timestamp = timestamp;
    }

    public String getId() { return id; }
    public String getBuyerId() { return buyerId; }
    public String getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getTotalPrice() { return totalPrice; }
    public String getTimestamp() { return timestamp; }
}


class UserRepository {
    private final MongoCollection<Document> collection = MongoConnection.getInstance().getDatabase().getCollection("users");

    public void registerUser(User user) {
        Document doc = new Document("_id", user.getId())
                .append("name", user.getName())
                .append("email", user.getEmail())
                .append("password", user.getPassword())
                .append("role", user.getRole());
        collection.insertOne(doc);
    }

    public User login(String email, String password, String expectedRole) throws InvalidLoginException {
        Document doc = collection.find(Filters.and(
                Filters.eq("email", email),
                Filters.eq("password", password),
                Filters.eq("role", expectedRole)
        )).first();

        if (doc == null) {
            throw new InvalidLoginException("Invalid email/password or incorrect login portal!");
        }

        if ("SELLER".equals(expectedRole)) {
            return new Seller(doc.getString("_id"), doc.getString("name"), doc.getString("email"), doc.getString("password"));
        } else {
            return new Buyer(doc.getString("_id"), doc.getString("name"), doc.getString("email"), doc.getString("password"));
        }
    }
}

class ProductRepository {
    private final MongoCollection<Document> collection = MongoConnection.getInstance().getDatabase().getCollection("products");

    public void addProduct(Product p) throws InvalidPriceException {
        if (p.getPrice() <= 0) {
            throw new InvalidPriceException("Product price must be greater than zero.");
        }
        Document doc = new Document("_id", p.getId())
                .append("sellerId", p.getSellerId())
                .append("name", p.getName())
                .append("description", p.getDescription())
                .append("category", p.getCategory())
                .append("price", p.getPrice())
                .append("quantity", p.getQuantity());
        collection.insertOne(doc);
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        for (Document doc : collection.find()) {
            list.add(new Product(
                    doc.getString("_id"),
                    doc.getString("sellerId"),
                    doc.getString("name"),
                    doc.getString("description"),
                    doc.getString("category"),
                    doc.getDouble("price"),
                    doc.getInteger("quantity")
            ));
        }
        return list;
    }

    public List<Product> getProductsBySeller(String sellerId) {
        List<Product> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("sellerId", sellerId))) {
            list.add(new Product(
                    doc.getString("_id"),
                    doc.getString("sellerId"),
                    doc.getString("name"),
                    doc.getString("description"),
                    doc.getString("category"),
                    doc.getDouble("price"),
                    doc.getInteger("quantity")
            ));
        }
        return list;
    }

    public Product getProductById(String id) {
        Document doc = collection.find(Filters.eq("_id", id)).first();
        if (doc == null) return null;
        return new Product(
                doc.getString("_id"),
                doc.getString("sellerId"),
                doc.getString("name"),
                doc.getString("description"),
                doc.getString("category"),
                doc.getDouble("price"),
                doc.getInteger("quantity")
        );
    }

    public void updateStock(String id, int newQty) {
        collection.updateOne(Filters.eq("_id", id), Updates.set("quantity", newQty));
    }
}

class OrderRepository {
    private final MongoCollection<Document> collection = MongoConnection.getInstance().getDatabase().getCollection("orders");

    public void createOrder(Order order) {
        Document doc = new Document("_id", order.getId())
                .append("buyerId", order.getBuyerId())
                .append("productId", order.getProductId())
                .append("quantity", order.getQuantity())
                .append("totalPrice", order.getTotalPrice())
                .append("timestamp", order.getTimestamp());
        collection.insertOne(doc);
    }

    public List<Order> getOrdersByBuyer(String buyerId) {
        List<Order> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("buyerId", buyerId))) {
            list.add(new Order(
                    doc.getString("_id"),
                    doc.getString("buyerId"),
                    doc.getString("productId"),
                    doc.getInteger("quantity"),
                    doc.getDouble("totalPrice"),
                    doc.getString("timestamp")
            ));
        }
        return list;
    }

    public List<Order> getOrdersByProduct(String productId) {
        List<Order> list = new ArrayList<>();
        for (Document doc : collection.find(Filters.eq("productId", productId))) {
            list.add(new Order(
                    doc.getString("_id"),
                    doc.getString("buyerId"),
                    doc.getString("productId"),
                    doc.getInteger("quantity"),
                    doc.getDouble("totalPrice"),
                    doc.getString("timestamp")
            ));
        }
        return list;
    }
}

class MarketplaceGUI extends JFrame {

    private final UserRepository userRepo = new UserRepository();
    private final ProductRepository productRepo = new ProductRepository();
    private final OrderRepository orderRepo = new OrderRepository();

    private User currentBuyer = null;
    private User currentSeller = null;

    public MarketplaceGUI() {
        setTitle("Second-Hand Marketplace");
        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.addTab("🛒 Buyer Portal", createBuyerPanel());
        tabbedPane.addTab("🏷️ Seller Portal", createSellerPanel());

        add(tabbedPane);
    }

    private JPanel createBuyerPanel() {
        JPanel mainPanel = new JPanel(new CardLayout());

        // Buyer Login View
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Buyer Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; loginPanel.add(new JLabel("Email:"), gbc);
        JTextField txtEmail = new JTextField(15);
        gbc.gridx = 1; loginPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; loginPanel.add(new JLabel("Password:"), gbc);
        JPasswordField txtPass = new JPasswordField(15);
        gbc.gridx = 1; loginPanel.add(txtPass, gbc);

        JButton btnLogin = new JButton("Login");
        JButton btnRegister = new JButton("Register Buyer Account");
        gbc.gridx = 0; gbc.gridy = 3; loginPanel.add(btnLogin, gbc);
        gbc.gridx = 1; loginPanel.add(btnRegister, gbc);

        // Buyer Dashboard View
        JPanel dashboard = new JPanel(new BorderLayout());
        JLabel welcomeLbl = new JLabel("Welcome, Buyer!", SwingConstants.CENTER);
        welcomeLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        dashboard.add(welcomeLbl, BorderLayout.NORTH);

        String[] cols = {"Product ID", "Name", "Category", "Price ($)", "Available Stock", "Description"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0);
        JTable productTable = new JTable(tableModel);
        dashboard.add(new JScrollPane(productTable), BorderLayout.CENTER);

        JPanel actionPanel = new JPanel();
        JButton btnBuy = new JButton("Buy Selected Product");
        JButton btnRefresh = new JButton("Refresh Catalog");
        JButton btnGraph = new JButton("View Orders Graph");
        JButton btnLogout = new JButton("Logout");
        
        actionPanel.add(btnBuy);
        actionPanel.add(btnRefresh);
        actionPanel.add(btnGraph);
        actionPanel.add(btnLogout);
        dashboard.add(actionPanel, BorderLayout.SOUTH);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(dashboard, "DASHBOARD");

        CardLayout cl = (CardLayout) mainPanel.getLayout();

        // Event Listeners
        btnRegister.addActionListener(e -> {
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            if (email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter email and password.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter Buyer Full Name:");
            if (name != null && !name.isEmpty()) {
                Buyer buyer = new Buyer(UUID.randomUUID().toString(), name, email, pass);
                userRepo.registerUser(buyer);
                JOptionPane.showMessageDialog(this, "Buyer Account Created! You can now login.");
            }
        });

        btnLogin.addActionListener(e -> {
            try {
                currentBuyer = userRepo.login(txtEmail.getText().trim(), new String(txtPass.getPassword()).trim(), "BUYER");
                welcomeLbl.setText("Logged in as Buyer: " + currentBuyer.getName());
                loadProductsToTable(tableModel);
                cl.show(mainPanel, "DASHBOARD");
            } catch (InvalidLoginException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnRefresh.addActionListener(e -> loadProductsToTable(tableModel));

        btnBuy.addActionListener(e -> {
            int selectedRow = productTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a product from the table to purchase.");
                return;
            }
            String productId = (String) tableModel.getValueAt(selectedRow, 0);
            Product p = productRepo.getProductById(productId);

            if (p != null) {
                if (p.getQuantity() <= 0) {
                    JOptionPane.showMessageDialog(this, "Sorry, product is out of stock!", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String qtyStr = JOptionPane.showInputDialog(this, "Enter Quantity to Buy:", "1");
                if (qtyStr == null) return;

                try {
                    int qty = Integer.parseInt(qtyStr);
                    if (qty <= 0) {
                        JOptionPane.showMessageDialog(this, "Quantity must be greater than 0.");
                        return;
                    }
                    if (qty > p.getQuantity()) {
                        JOptionPane.showMessageDialog(this, "Quantity requested exceeds available stock!");
                        return;
                    }
                    // Update Stock & Create Order
                    productRepo.updateStock(p.getId(), p.getQuantity() - qty);
                    Order order = new Order(UUID.randomUUID().toString(), currentBuyer.getId(), p.getId(), qty, p.getPrice() * qty, LocalDateTime.now().toString());
                    orderRepo.createOrder(order);

                    JOptionPane.showMessageDialog(this, "Purchase Complete! Total: $" + (p.getPrice() * qty));
                    loadProductsToTable(tableModel);
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity format.");
                }
            }
        });

        btnGraph.addActionListener(e -> {
            if (currentBuyer == null) {
                JOptionPane.showMessageDialog(this, "Please log in first.");
                return;
            }

            List<Order> orders = orderRepo.getOrdersByBuyer(currentBuyer.getId());
            
            if (orders.isEmpty()) {
                JOptionPane.showMessageDialog(this, "You have no orders to graph.");
                return;
            }

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            for (Order order : orders) {
                String shortTime = order.getTimestamp().length() >= 16 ? order.getTimestamp().substring(0, 16).replace("T", " ") : order.getTimestamp();
                dataset.addValue(order.getTotalPrice(), "Spent ($)", shortTime);
            }

            JFreeChart barChart = ChartFactory.createBarChart(
                    "My Purchase History",
                    "Order Timestamp",
                    "Total Price ($)",
                    dataset
            );

            org.jfree.chart.plot.CategoryPlot plot = barChart.getCategoryPlot();
            org.jfree.chart.renderer.category.BarRenderer renderer = (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
            renderer.setMaximumBarWidth(0.1);

            JFrame chartFrame = new JFrame("Orders Graph");
            chartFrame.setSize(700, 400);
            chartFrame.setLocationRelativeTo(this);
            chartFrame.add(new ChartPanel(barChart));
            chartFrame.setVisible(true);
        });

        btnLogout.addActionListener(e -> {
            currentBuyer = null;
            txtEmail.setText("");
            txtPass.setText("");
            cl.show(mainPanel, "LOGIN");
        });

        return mainPanel;
    }

    private JPanel createSellerPanel() {
        JPanel mainPanel = new JPanel(new CardLayout());

        // Seller Login View
        JPanel loginPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Seller Login", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        loginPanel.add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; loginPanel.add(new JLabel("Email:"), gbc);
        JTextField txtEmail = new JTextField(15);
        gbc.gridx = 1; loginPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy = 2; loginPanel.add(new JLabel("Password:"), gbc);
        JPasswordField txtPass = new JPasswordField(15);
        gbc.gridx = 1; loginPanel.add(txtPass, gbc);

        JButton btnLogin = new JButton("Seller Login");
        JButton btnRegister = new JButton("Register Seller Account");
        gbc.gridx = 0; gbc.gridy = 3; loginPanel.add(btnLogin, gbc);
        gbc.gridx = 1; loginPanel.add(btnRegister, gbc);

        // Seller Dashboard View
        JPanel dashboard = new JPanel(new BorderLayout());
        JLabel welcomeLbl = new JLabel("Seller Dashboard", SwingConstants.CENTER);
        welcomeLbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        dashboard.add(welcomeLbl, BorderLayout.NORTH);

        // Form to Add Product
        JPanel formPanel = new JPanel(new GridLayout(6, 2, 5, 5));
        formPanel.setBorder(BorderFactory.createTitledBorder("Add Second-Hand Item for Sale"));

        JTextField txtName = new JTextField();
        JTextField txtCat = new JTextField();
        JTextField txtPrice = new JTextField();
        JTextField txtQty = new JTextField();
        JTextField txtDesc = new JTextField();

        formPanel.add(new JLabel("Product Name:")); formPanel.add(txtName);
        formPanel.add(new JLabel("Category:")); formPanel.add(txtCat);
        formPanel.add(new JLabel("Price ($):")); formPanel.add(txtPrice);
        formPanel.add(new JLabel("Quantity:")); formPanel.add(txtQty);
        formPanel.add(new JLabel("Description:")); formPanel.add(txtDesc);

        JButton btnAddProduct = new JButton("Publish Listing");
        formPanel.add(new JLabel()); formPanel.add(btnAddProduct);

        // Product Analytics Section
        JPanel analyzePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        analyzePanel.setBorder(BorderFactory.createTitledBorder("📊 Product Analytics & Performance Insights"));
        JTextField txtAnalyzeId = new JTextField(18);
        JButton btnAnalyze = new JButton("Analyze Product ID");
        JButton btnAnalyzeSelected = new JButton("Analyze Selected Row");

        analyzePanel.add(new JLabel("Enter Product ID:"));
        analyzePanel.add(txtAnalyzeId);
        analyzePanel.add(btnAnalyze);
        analyzePanel.add(btnAnalyzeSelected);

        JPanel topContainer = new JPanel(new BorderLayout());
        topContainer.add(formPanel, BorderLayout.CENTER);
        topContainer.add(analyzePanel, BorderLayout.SOUTH);

        dashboard.add(topContainer, BorderLayout.NORTH);

        String[] cols = {"Product ID", "Name", "Category", "Price ($)", "Stock"};
        DefaultTableModel tableModel = new DefaultTableModel(cols, 0);
        JTable sellerTable = new JTable(tableModel);
        dashboard.add(new JScrollPane(sellerTable), BorderLayout.CENTER);

        JButton btnLogout = new JButton("Logout");
        dashboard.add(btnLogout, BorderLayout.SOUTH);

        mainPanel.add(loginPanel, "LOGIN");
        mainPanel.add(dashboard, "DASHBOARD");

        CardLayout cl = (CardLayout) mainPanel.getLayout();

        // Event Listeners
        btnRegister.addActionListener(e -> {
            String email = txtEmail.getText().trim();
            String pass = new String(txtPass.getPassword()).trim();
            if (email.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter email and password.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String name = JOptionPane.showInputDialog(this, "Enter Seller / Store Name:");
            if (name != null && !name.isEmpty()) {
                Seller seller = new Seller(UUID.randomUUID().toString(), name, email, pass);
                userRepo.registerUser(seller);
                JOptionPane.showMessageDialog(this, "Seller Account Created! You can now login.");
            }
        });

        btnLogin.addActionListener(e -> {
            try {
                currentSeller = userRepo.login(txtEmail.getText().trim(), new String(txtPass.getPassword()).trim(), "SELLER");
                welcomeLbl.setText("Seller Dashboard: " + currentSeller.getName());
                loadSellerProducts(tableModel, currentSeller.getId());
                cl.show(mainPanel, "DASHBOARD");
            } catch (InvalidLoginException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Login Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnAddProduct.addActionListener(e -> {
            try {
                String name = txtName.getText().trim();
                String category = txtCat.getText().trim();
                double price = Double.parseDouble(txtPrice.getText().trim());
                int qty = Integer.parseInt(txtQty.getText().trim());
                String desc = txtDesc.getText().trim();

                Product p = new Product(UUID.randomUUID().toString(), currentSeller.getId(), name, desc, category, price, qty);
                productRepo.addProduct(p);

                JOptionPane.showMessageDialog(this, "Product Listed Successfully!");
                loadSellerProducts(tableModel, currentSeller.getId());

                txtName.setText(""); txtCat.setText(""); txtPrice.setText(""); txtQty.setText(""); txtDesc.setText("");
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Please enter valid numbers for Price and Quantity.", "Input Error", JOptionPane.ERROR_MESSAGE);
            } catch (InvalidPriceException ex) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "Price Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Product Analysis Event Handler
        ActionListener analyzeAction = e -> {
            String productId = txtAnalyzeId.getText().trim();
            if (e.getSource() == btnAnalyzeSelected) {
                int selectedRow = sellerTable.getSelectedRow();
                if (selectedRow != -1) {
                    productId = (String) sellerTable.getValueAt(selectedRow, 0);
                    txtAnalyzeId.setText(productId);
                } else {
                    JOptionPane.showMessageDialog(this, "Please select a product row from the table or enter an ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            if (productId.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter or select a Product ID.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Product p = productRepo.getProductById(productId);
            if (p == null) {
                JOptionPane.showMessageDialog(this, "Product not found for ID: " + productId, "Product Not Found", JOptionPane.ERROR_MESSAGE);
                return;
            }

            List<Order> productOrders = orderRepo.getOrdersByProduct(productId);

            int totalOrders = productOrders.size();
            int totalUnitsSold = 0;
            double totalRevenue = 0.0;

            for (Order o : productOrders) {
                totalUnitsSold += o.getQuantity();
                totalRevenue += o.getTotalPrice();
            }

            double avgOrderValue = totalOrders > 0 ? totalRevenue / totalOrders : 0.0;

            // Generate Detailed Report String
            StringBuilder report = new StringBuilder();
            report.append("=========================================\n");
            report.append("         FULL PRODUCT ANALYSIS REPORT     \n");
            report.append("=========================================\n\n");
            report.append("Product ID          : ").append(p.getId()).append("\n");
            report.append("Name                : ").append(p.getName()).append("\n");
            report.append("Category            : ").append(p.getCategory()).append("\n");
            report.append("Unit Price          : $").append(String.format("%.2f", p.getPrice())).append("\n");
            report.append("Stock Remaining     : ").append(p.getQuantity()).append(" units\n");
            report.append("Description         : ").append(p.getDescription()).append("\n");
            report.append("Seller ID           : ").append(p.getSellerId()).append("\n\n");
            report.append("--- PERFORMANCE METRICS ---\n");
            report.append("Total Orders Placed : ").append(totalOrders).append("\n");
            report.append("Total Units Sold    : ").append(totalUnitsSold).append(" units\n");
            report.append("Total Revenue       : $").append(String.format("%.2f", totalRevenue)).append("\n");
            report.append("Average Order Value : $").append(String.format("%.2f", avgOrderValue)).append("\n");

            // Display Analysis Modal Dialog
            JDialog dialog = new JDialog(this, "Product Analysis: " + p.getName(), true);
            dialog.setLayout(new BorderLayout());
            dialog.setSize(850, 500);
            dialog.setLocationRelativeTo(this);

            JTextArea txtReport = new JTextArea(report.toString());
            txtReport.setEditable(false);
            txtReport.setFont(new Font("Monospaced", Font.PLAIN, 12));
            txtReport.setBackground(new Color(245, 245, 245));
            txtReport.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            dialog.add(new JScrollPane(txtReport), BorderLayout.WEST);

            if (!productOrders.isEmpty()) {
                DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                for (Order order : productOrders) {
                    String shortTime = order.getTimestamp().length() >= 16 ? order.getTimestamp().substring(0, 16).replace("T", " ") : order.getTimestamp();
                    dataset.addValue(order.getTotalPrice(), "Revenue ($)", shortTime);
                }
                JFreeChart barChart = ChartFactory.createBarChart("Sales History Timeline", "Order Timestamp", "Revenue ($)", dataset);
                org.jfree.chart.plot.CategoryPlot plot = barChart.getCategoryPlot();
                org.jfree.chart.renderer.category.BarRenderer renderer = (org.jfree.chart.renderer.category.BarRenderer) plot.getRenderer();
                renderer.setMaximumBarWidth(0.1);
                dialog.add(new ChartPanel(barChart), BorderLayout.CENTER);
            } else {
                JLabel noSalesLbl = new JLabel("<html><center>No order history recorded for this product yet.</center></html>", SwingConstants.CENTER);
                noSalesLbl.setFont(new Font("SansSerif", Font.ITALIC, 14));
                dialog.add(noSalesLbl, BorderLayout.CENTER);
            }

            dialog.setVisible(true);
        };

        btnAnalyze.addActionListener(analyzeAction);
        btnAnalyzeSelected.addActionListener(analyzeAction);

        btnLogout.addActionListener(e -> {
            currentSeller = null;
            txtEmail.setText("");
            txtPass.setText("");
            cl.show(mainPanel, "LOGIN");
        });

        return mainPanel;
    }

    private void loadProductsToTable(DefaultTableModel model) {
        model.setRowCount(0);
        List<Product> products = productRepo.getAllProducts();
        for (Product p : products) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getQuantity(), p.getDescription()});
        }
    }

    private void loadSellerProducts(DefaultTableModel model, String sellerId) {
        model.setRowCount(0);
        List<Product> products = productRepo.getProductsBySeller(sellerId);
        for (Product p : products) {
            model.addRow(new Object[]{p.getId(), p.getName(), p.getCategory(), p.getPrice(), p.getQuantity()});
        }
    }
}