package 任务二;

/***
 * 有问题请看（注意事项.md）文件
 * mysql-connector-j-8.4.0.jar文件使用请看(注意事项.md)文件
 * 数据库调用请看(注意事项.md)文件
 * good.txt中存储的默认商品数据
***/

import java.io.*;
import java.util.ArrayList;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Scanner;

//sql包
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// DBManager 类负责管理数据库连接
class DBManager {
    private static final String URL = "jdbc:mysql://localhost:3306/shopping_platform";
    private static final String USER = "UserName";      // 替换为您的数据库用户名
    private static final String PASSWORD = "Password";  // 替换为您的数据库密码

    // 获取数据库连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    // 关闭连接、语句和结果集
    public static void close(Connection conn, PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) rs.close();
            if (ps != null) ps.close();
            if (conn != null) conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

// 商品类
class Goods {
    protected int id; // 商品序号
    protected String name; // 商品名称
    protected double price; // 商品价格

    public Goods() {
    }

    public Goods(int id, String name, double price) {
        this.id = id;
        this.name = name;
        this.price = price;
    }

    //调用good.txt文本数据
    public static ArrayList<Goods> set_goodsList() {
        ArrayList<Goods> goodsList = new ArrayList<>();
        goodsList.add(null); // 空出下标0

        try (InputStream is = Goods.class.getResourceAsStream("goods.txt");
             Scanner scanner = new Scanner(is)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(" ");
                int id = Integer.parseInt(parts[0]);
                String name = parts[1];
                double price = Double.parseDouble(parts[2]);
                goodsList.add(new Goods(id, name, price));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return goodsList;
    }

    public static void get_goodsList(ArrayList<Goods> goodsList) {
        System.out.println("商品序号/商品名称/商品价格");
        for (int i = 1; i < goodsList.size(); i++) {
            Goods good = goodsList.get(i);
            System.out.println("        " + good.id + ":      " + good.name + "            " + String.format("%.2f", good.price));
        }
    }
}



// 购物车类
class ShopCar extends Goods {
    protected int Number;
    protected double allPrice;

    public ShopCar() {
    }

    public ShopCar(int id, String name, double price, int number, double allPrice) {
        super(id, name, price);
        this.Number = number;
        this.allPrice = allPrice;
    }

    // 添加到购物车列表
    public static boolean add_shopCarList(int goodId, int goodNumber, ArrayList<Goods> goodsList, ArrayList<ShopCar> shopCarList) {
        if (goodId < 1 || goodId > goodsList.size() - 1) {
            return false;
        } else {
            Goods good = goodsList.get(goodId);
            ShopCar scar = new ShopCar(good.id, good.name, good.price, goodNumber, good.price * goodNumber);
            shopCarList.add(scar);

            String sql = "INSERT INTO shop_car (goods_id, goods_name, goods_price, number, all_price) VALUES (?, ?, ?, ?, ?)";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, good.id);
                ps.setString(2, good.name);
                ps.setDouble(3, good.price);
                ps.setInt(4, goodNumber);
                ps.setDouble(5, good.price * goodNumber);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean delete_shopCarList(int number, ArrayList<ShopCar> shopCarList) {
        if (number < 1 || number > shopCarList.size() - 1) {
            return false;
        } else {
            ShopCar scar = shopCarList.get(number);
            shopCarList.remove(number);

            String sql = "DELETE FROM shop_car WHERE goods_id = ?";
            try (Connection conn = DBManager.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, scar.id);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static void get_shopCarList(ArrayList<ShopCar> shopCarList) {
        String sql = "SELECT * FROM shop_car";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.isBeforeFirst()) {
                System.out.println("当前购物车为空");
                return;
            }

            System.out.println("购物车序号/商品序号/商品名称/商品单价/商品数量/商品总价");
            int index = 1;
            while (rs.next()) {
                int id = rs.getInt("goods_id");
                String name = rs.getString("goods_name");
                double price = rs.getDouble("goods_price");
                int number = rs.getInt("number");
                double allPrice = rs.getDouble("all_price");
                ShopCar scar = new ShopCar(id, name, price, number, allPrice);
                shopCarList.add(scar);
                System.out.println("        " + index + ".        " + id + "        " + name + "        " + String.format("%.2f", price) + "        " + number + "        " + String.format("%.2f", allPrice));
                index++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    //用于提交订单后删除购物车shop_car表中的数据
    public static void clearShopCarTable() {
        String sql = "DELETE FROM shop_car";
        try (Connection conn = DBManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}


// 订单类
class Order {
    protected ArrayList<ShopCar> shopCarList;

    // 无参构造
    public Order() {
        shopCarList = new ArrayList<>();
    }

    // 有参构造
    public Order(ArrayList<ShopCar> shopCarList) {
        this.shopCarList = shopCarList;
    }

    // 生成添加订单
    public static void add_orderList(ArrayList<Order> orderList, ArrayList<ShopCar> shopCarList) {
        Order or = new Order(shopCarList);
        orderList.add(or);
    }

    // 删除某个订单
    public static boolean delete_orderList(int number, ArrayList<Order> orderList) {
        if (number < 1 || number > orderList.size() - 1) {
            return false;
        } else {
            orderList.remove(number);
        }
        return true;
    }
}

// 主窗口类
public class MainFrame extends JFrame {
    private ArrayList<Goods> goodsList;
    private ArrayList<Order> orderList;
    private ArrayList<ShopCar> shopCarList;

    private JTextArea goodsTextArea;
    private JTextArea shopCarTextArea;
    private JTextArea orderTextArea;

    public MainFrame() {
        goodsList = Goods.set_goodsList();
        orderList = new ArrayList<>();
        orderList.add(null);
        shopCarList = new ArrayList<>();
        shopCarList.add(null);

        setTitle("购物平台");
        setSize(1600, 1000);  // 设置窗口大小
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setLocationRelativeTo(null);  // 窗口居中

        JPanel mainPanel = new JPanel(new GridLayout(1, 3));


// 商品列表面板
        JPanel goodsPanel = new JPanel(new BorderLayout());
        goodsTextArea = new JTextArea(20, 30);
        goodsTextArea.setEditable(false);
        goodsTextArea.setFont(new Font("SimSun", Font.PLAIN, 18));
        updateGoodsTextArea();
        goodsPanel.add(new JScrollPane(goodsTextArea), BorderLayout.CENTER);

        // 添加到购物车按钮
        JButton addToCartButton = new JButton("添加到购物车");
        addToCartButton.setFont(new Font("SimSun", Font.PLAIN, 16));
        addToCartButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(MainFrame.this, "输入商品序号和数量（用空格隔开）:");
            if (input != null && !input.isEmpty()) {
                String[] parts = input.split(" ");
                if (parts.length == 2) {
                    try {
                        int id = Integer.parseInt(parts[0]);
                        int quantity = Integer.parseInt(parts[1]);
                        boolean success = ShopCar.add_shopCarList(id, quantity, goodsList, shopCarList);
                        if (success) {
                            updateShopCarTextArea();
                            JOptionPane.showMessageDialog(MainFrame.this, "添加成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(MainFrame.this, "添加失败，商品序号错误！", "错误", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(MainFrame.this, "输入格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                    JOptionPane.showMessageDialog(MainFrame.this, "输入格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        goodsPanel.add(addToCartButton, BorderLayout.SOUTH);
        mainPanel.add(goodsPanel);

        // 购物车面板
        JPanel shopCarPanel = new JPanel(new BorderLayout());
        shopCarTextArea = new JTextArea(20, 30);
        shopCarTextArea.setEditable(false);
        shopCarTextArea.setFont(new Font("SimSun", Font.PLAIN, 18));
        updateShopCarTextArea();
        shopCarPanel.add(new JScrollPane(shopCarTextArea), BorderLayout.CENTER);

        // 从购物车删除和提交订单按钮
        JPanel shopCarButtonPanel = new JPanel(new GridLayout(1, 2));
        JButton removeFromCartButton = new JButton("从购物车删除");
        removeFromCartButton.setFont(new Font("SimSun", Font.PLAIN, 16));
        removeFromCartButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(MainFrame.this, "输入购物车序号:");
            if (input != null && !input.isEmpty()) {
                try {
                    int number = Integer.parseInt(input);
                    boolean success = ShopCar.delete_shopCarList(number, shopCarList);
                    if (success) {
                        updateShopCarTextArea();
                        JOptionPane.showMessageDialog(MainFrame.this, "删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "删除失败，购物车序号错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "输入格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        JButton submitOrderButton = new JButton("提交订单");
        submitOrderButton.setFont(new Font("SimSun", Font.PLAIN, 16));
        submitOrderButton.addActionListener(e -> {
            if (shopCarList.size() == 1) {
                JOptionPane.showMessageDialog(MainFrame.this, "购物车为空，无法提交订单！", "警告", JOptionPane.WARNING_MESSAGE);
            } else {
                Order.add_orderList(orderList, new ArrayList<>(shopCarList));
                shopCarList.clear();
                shopCarList.add(null);
                ShopCar.clearShopCarTable(); // 清空数据库中的购物车表
                updateShopCarTextArea();
                updateOrderTextArea();
                JOptionPane.showMessageDialog(MainFrame.this, "订单提交成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        shopCarButtonPanel.add(removeFromCartButton);
        shopCarButtonPanel.add(submitOrderButton);
        shopCarPanel.add(shopCarButtonPanel, BorderLayout.SOUTH);
        mainPanel.add(shopCarPanel);

        // 订单面板
        JPanel orderPanel = new JPanel(new BorderLayout());
        orderTextArea = new JTextArea(20, 30);
        orderTextArea.setEditable(false);
        orderTextArea.setFont(new Font("SimSun", Font.PLAIN, 18));
        updateOrderTextArea();
        orderPanel.add(new JScrollPane(orderTextArea), BorderLayout.CENTER);

        JButton removeOrderButton = new JButton("删除订单");
        removeOrderButton.setFont(new Font("SimSun", Font.PLAIN, 16));
        removeOrderButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(MainFrame.this, "输入订单序号:");
            if (input != null && !input.isEmpty()) {
                try {
                    int number = Integer.parseInt(input);
                    boolean success = Order.delete_orderList(number, orderList);
                    if (success) {
                        updateOrderTextArea();
                        JOptionPane.showMessageDialog(MainFrame.this, "删除成功！", "成功", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainFrame.this, "删除失败，订单序号错误！", "错误", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(MainFrame.this, "输入格式错误！", "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        orderPanel.add(removeOrderButton, BorderLayout.SOUTH);
        mainPanel.add(orderPanel);

        add(mainPanel, BorderLayout.CENTER);
    }

    private void updateGoodsTextArea() {
        StringBuilder sb = new StringBuilder();
        sb.append("商品序号/商品名称/商品价格\n");
        for (int i = 1; i < goodsList.size(); i++) {
            Goods good = goodsList.get(i);
            sb.append("        ").append(good.id).append(":      ").append(good.name).append("            ").append(String.format("%.2f", good.price)).append("\n");
        }
        goodsTextArea.setText(sb.toString());
    }

    private void updateShopCarTextArea() {
        StringBuilder sb = new StringBuilder();
        if (shopCarList.size() == 1) {
            sb.append("当前购物车为空\n");
        } else {
            sb.append("购物车序号/商品序号/商品名称/商品单价/商品数量/商品总价\n");
            for (int i = 1; i < shopCarList.size(); i++) {
                ShopCar scar = shopCarList.get(i);
                sb.append("        ").append(i).append(".        ").append(scar.id).append("        ").append(scar.name).append("        ").append(String.format("%.2f", scar.price)).append("        ").append(scar.Number).append("        ").append(String.format("%.2f", scar.allPrice)).append("\n");
            }
        }
        shopCarTextArea.setText(sb.toString());
    }

    private void updateOrderTextArea() {
        StringBuilder sb = new StringBuilder();
        if (orderList.size() == 1) {
            sb.append("您的订单空空如也\n");
        } else {
            sb.append("您的订单信息如下 ：\n");
            sb.append("共有").append(orderList.size() - 1).append("个订单\n");
            for (int i = 1; i < orderList.size(); i++) {
                Order or = orderList.get(i);
                sb.append("订单").append(i).append(":        \n");
                for (int j = 1; j < or.shopCarList.size(); j++) {
                    ShopCar scar = or.shopCarList.get(j);
                    sb.append("        ").append(j).append(".        ").append(scar.id).append("        ").append(scar.name).append("        ").append(String.format("%.2f", scar.price)).append("        ").append(scar.Number).append("        ").append(String.format("%.2f", scar.allPrice)).append("\n");
                }
            }
        }
        orderTextArea.setText(sb.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}

