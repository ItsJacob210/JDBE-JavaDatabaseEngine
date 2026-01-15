package com.dbengine.gui;

import com.dbengine.Database;
import com.dbengine.Database.QueryResult;
import com.dbengine.semantic.DataType;
import com.dbengine.semantic.Schema;
import com.dbengine.storage.TableHeap;
import com.dbengine.storage.Tuple;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.util.List;

/**
 * Graphical User Interface for JavaDBEngine.
 * Demonstrates the database functionality with a clean, modern interface.
 */
public class DatabaseGUI extends JFrame {
    private Database database;
    private TableHeap usersTableHeap;
    private TableHeap productsTableHeap;
    
    //ui components
    private JTextArea queryInput;
    private JTextArea outputArea;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JTextArea syntaxHelp;
    private JComboBox<String> tableSelector;
    private JLabel statusLabel;
    private JPanel examplesPanel;
    
    //color scheme
    private static final Color PRIMARY = new Color(41, 128, 185);
    private static final Color SUCCESS = new Color(39, 174, 96);
    private static final Color ERROR = new Color(231, 76, 60);
    private static final Color BACKGROUND = new Color(236, 240, 241);
    
    public DatabaseGUI() {
        initializeDatabase();
        initializeUI();
        loadSampleDataAfterUI();
        setVisible(true);
    }
    
    private void initializeDatabase() {
        try {
            database = new Database("gui_demo");
            createSampleTables();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, 
                "Failed to initialize database: " + e.getMessage(),
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void createSampleTables() throws IOException {
        //create users table and get the table heap
        Schema usersSchema = new Schema();
        usersSchema.addColumn("id", DataType.INTEGER);
        usersSchema.addColumn("name", DataType.STRING);
        usersSchema.addColumn("age", DataType.INTEGER);
        usersSchema.addColumn("active", DataType.BOOLEAN);
        usersTableHeap = database.createTable("users", usersSchema);
        
        //create products table and get the table heap
        Schema productsSchema = new Schema();
        productsSchema.addColumn("id", DataType.INTEGER);
        productsSchema.addColumn("name", DataType.STRING);
        productsSchema.addColumn("price", DataType.INTEGER);
        productsSchema.addColumn("stock", DataType.INTEGER);
        productsTableHeap = database.createTable("products", productsSchema);
    }
    
    private void loadSampleDataAfterUI() {
        setStatus("Loading sample data...", PRIMARY);
        
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                SampleDataLoader.loadUsersData(usersTableHeap, usersTableHeap.getSchema(), 
                    (current, total, msg) -> publish(msg + " " + current + "/" + total));
                
                SampleDataLoader.loadProductsData(productsTableHeap, productsTableHeap.getSchema(),
                    (current, total, msg) -> publish(msg + " " + current + "/" + total));
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                //update status with latest progress
                if (!chunks.isEmpty()) {
                    setStatus(chunks.get(chunks.size() - 1), PRIMARY);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); //check for exceptions
                    
                    outputArea.append("Loaded sample data successfully\n");
                    outputArea.append("  - 300,000 users inserted\n");
                    outputArea.append("  - 1,000,000 products inserted\n");
                    outputArea.append("-".repeat(60) + "\n\n");
                    setStatus("Sample data loaded (1,300,000 records total)", SUCCESS);
                } catch (Exception e) {
                    outputArea.append("Error loading sample data: " + e.getMessage() + "\n");
                    outputArea.append("-".repeat(60) + "\n\n");
                    setStatus("Failed to load sample data", ERROR);
                }
            }
        };
        
        worker.execute();
    }
    
    private void initializeUI() {
        setTitle("JavaDBEngine - Functional Pipeline Query Database");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1400, 900);
        setLocationRelativeTo(null);
        
        //main container
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BACKGROUND);
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        
        //center panel - split into left (query) and right (help)
        JSplitPane centerSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        centerSplit.setLeftComponent(createQueryPanel());
        centerSplit.setRightComponent(createSyntaxPanel());
        centerSplit.setDividerLocation(900);
        centerSplit.setResizeWeight(0.65);
        mainPanel.add(centerSplit, BorderLayout.CENTER);
        
        //bottom panel - status
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    
    private JPanel createQueryPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BACKGROUND);
        
        //query input section
        JPanel inputSection = new JPanel(new BorderLayout(5, 5));
        inputSection.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY, 2),
            "Query Input",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            PRIMARY
        ));
        inputSection.setBackground(Color.WHITE);
        
        queryInput = new JTextArea(4, 50);
        queryInput.setFont(new Font("Monospace", Font.PLAIN, 14));
        queryInput.setLineWrap(true);
        queryInput.setWrapStyleWord(true);
        queryInput.setBorder(new EmptyBorder(10, 10, 10, 10));
        queryInput.setText("users |> project(id, name, age)");
        
        JScrollPane inputScroll = new JScrollPane(queryInput);
        inputSection.add(inputScroll, BorderLayout.CENTER);
        
        //buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setOpaque(false);
        
        JButton executeBtn = createStyledButton("Execute Query", SUCCESS);
        executeBtn.addActionListener(e -> executeQuery());
        
        JButton explainBtn = createStyledButton("Explain Plan", PRIMARY);
        explainBtn.addActionListener(e -> explainQuery());
        
        JButton clearBtn = createStyledButton("Clear", ERROR);
        clearBtn.addActionListener(e -> clearResults());
        
        JButton loadDataBtn = createStyledButton("Reload Data", new Color(230, 126, 34));
        loadDataBtn.addActionListener(e -> reloadSampleData());
        
        buttonPanel.add(executeBtn);
        buttonPanel.add(explainBtn);
        buttonPanel.add(loadDataBtn);
        buttonPanel.add(clearBtn);
        
        inputSection.add(buttonPanel, BorderLayout.SOUTH);
        
        panel.add(inputSection, BorderLayout.NORTH);
        
        //results section - split into table and text output
        JSplitPane resultsSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        
        //table results
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY, 2),
            "Query Results",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            PRIMARY
        ));
        tablePanel.setBackground(Color.WHITE);
        
        tableModel = new DefaultTableModel();
        resultsTable = new JTable(tableModel);
        resultsTable.setFont(new Font("Monospace", Font.PLAIN, 13));
        resultsTable.setRowHeight(25);
        resultsTable.setGridColor(new Color(189, 195, 199));
        
        //style table header with custom renderer for guaranteed visibility
        var header = resultsTable.getTableHeader();
        header.setFont(new Font("Arial", Font.BOLD, 14));
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 35));
        header.setReorderingAllowed(false);
        
        //use custom renderer to ensure colors are always visible
        header.setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JLabel label = new JLabel(value != null ? value.toString() : "");
                label.setFont(new Font("Arial", Font.BOLD, 14));
                label.setBackground(new Color(52, 73, 94));
                label.setForeground(Color.WHITE);
                label.setOpaque(true);
                label.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                label.setHorizontalAlignment(JLabel.CENTER);
                return label;
            }
        });
        
        JScrollPane tableScroll = new JScrollPane(resultsTable);
        tablePanel.add(tableScroll, BorderLayout.CENTER);
        
        //text output
        JPanel outputPanel = new JPanel(new BorderLayout());
        outputPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY, 2),
            "Output Log",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            PRIMARY
        ));
        outputPanel.setBackground(Color.WHITE);
        
        outputArea = new JTextArea();
        outputArea.setFont(new Font("Monospace", Font.PLAIN, 12));
        outputArea.setEditable(false);
        outputArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        outputArea.setBackground(new Color(44, 62, 80));
        outputArea.setForeground(new Color(236, 240, 241));
        
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputPanel.add(outputScroll, BorderLayout.CENTER);
        
        resultsSplit.setTopComponent(tablePanel);
        resultsSplit.setBottomComponent(outputPanel);
        resultsSplit.setDividerLocation(300);
        resultsSplit.setResizeWeight(0.6);
        
        panel.add(resultsSplit, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createSyntaxPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(PRIMARY, 2),
            "Syntax Reference & Examples",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Arial", Font.BOLD, 14),
            PRIMARY
        ));
        panel.setBackground(Color.WHITE);
        
        syntaxHelp = new JTextArea();
        syntaxHelp.setFont(new Font("Monospace", Font.PLAIN, 12));
        syntaxHelp.setEditable(false);
        syntaxHelp.setBorder(new EmptyBorder(15, 15, 15, 15));
        syntaxHelp.setText(getSyntaxHelp());
        syntaxHelp.setLineWrap(true);
        syntaxHelp.setWrapStyleWord(true);
        
        JScrollPane scroll = new JScrollPane(syntaxHelp);
        panel.add(scroll, BorderLayout.CENTER);
        
        //bottom section with table selector and examples
        JPanel bottomSection = new JPanel(new BorderLayout(5, 5));
        bottomSection.setBackground(Color.WHITE);
        bottomSection.setBorder(new EmptyBorder(5, 10, 10, 10));
        
        //table selector
        JPanel selectorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectorPanel.setBackground(Color.WHITE);
        
        JLabel selectLabel = new JLabel("Active Table: ");
        selectLabel.setFont(new Font("Arial", Font.BOLD, 13));
        selectLabel.setForeground(PRIMARY);
        
        tableSelector = new JComboBox<>(new String[]{"users", "products"});
        tableSelector.setFont(new Font("Monospace", Font.PLAIN, 13));
        tableSelector.setPreferredSize(new Dimension(150, 30));
        tableSelector.addActionListener(e -> updateExampleButtons());
        
        selectorPanel.add(selectLabel);
        selectorPanel.add(tableSelector);
        
        bottomSection.add(selectorPanel, BorderLayout.NORTH);
        
        //quick examples buttons
        JPanel examplesPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        examplesPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        examplesPanel.setBackground(Color.WHITE);
        
        //store reference to examples panel for dynamic updates
        this.examplesPanel = examplesPanel;
        updateExampleButtons();
        
        bottomSection.add(examplesPanel, BorderLayout.CENTER);
        panel.add(bottomSection, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void updateExampleButtons() {
        if (examplesPanel == null) return;
        
        examplesPanel.removeAll();
        String table = (String) tableSelector.getSelectedItem();
        
        if ("users".equals(table)) {
            addExampleButton(examplesPanel, "Simple Project", "users |> project(id, name)");
            addExampleButton(examplesPanel, "Filter & Sort", "users |> filter(age > 25) |> sort(age desc)");
            addExampleButton(examplesPanel, "Complex Query", "users |> filter(active == true) |> project(name, age) |> sort(age desc) |> limit(5)");
            addExampleButton(examplesPanel, "Transactions", "begin\nusers |> filter(id == 1) |> modify(age = 30)\ncommit");
            addExampleButton(examplesPanel, "Explain Plan", "explain users |> filter(age > 25) |> project(name)");
        } else {
            addExampleButton(examplesPanel, "Simple Project", "products |> project(id, name, price)");
            addExampleButton(examplesPanel, "Filter & Sort", "products |> filter(price > 50) |> sort(price desc)");
            addExampleButton(examplesPanel, "Complex Query", "products |> filter(stock > 10) |> project(name, price, stock) |> sort(price asc) |> limit(5)");
            addExampleButton(examplesPanel, "Transactions", "begin\nproducts |> filter(id == 10) |> modify(price = 56)\ncommit");
            addExampleButton(examplesPanel, "Explain Plan", "explain products |> filter(price > 50) |> project(name)");
        }
        
        examplesPanel.revalidate();
        examplesPanel.repaint();
    }
    
    private void addExampleButton(JPanel panel, String label, String query) {
        JButton btn = new JButton(label);
        btn.setFont(new Font("Arial", Font.PLAIN, 12));
        btn.setBackground(new Color(52, 152, 219));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.addActionListener(e -> queryInput.setText(query));
        panel.add(btn);
    }
    
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(44, 62, 80));
        panel.setBorder(new EmptyBorder(10, 15, 10, 15));
        
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        
        panel.add(statusLabel, BorderLayout.WEST);
        
        JLabel versionLabel = new JLabel("v1.0.0 | Java " + System.getProperty("java.version"));
        versionLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        versionLabel.setForeground(new Color(189, 195, 199));
        
        panel.add(versionLabel, BorderLayout.EAST);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, Color color) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setPreferredSize(new Dimension(140, 35));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        //hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(color.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
            }
        });
        
        return button;
    }
    
    private void executeQuery() {
        String query = queryInput.getText().trim();
        if (query.isEmpty()) {
            setStatus("Please enter a query", ERROR);
            return;
        }
        
        try {
            long startTime = System.currentTimeMillis();
            QueryResult result = database.execute(query);
            long duration = System.currentTimeMillis() - startTime;
            
            outputArea.append("Query: " + query + "\n");
            outputArea.append("Status: " + result.message() + "\n");
            outputArea.append("Execution time: " + duration + "ms\n");
            outputArea.append("Rows returned: " + result.tuples().size() + "\n");
            outputArea.append("-".repeat(60) + "\n\n");
            
            displayResults(result.tuples());
            setStatus("Query executed successfully (" + duration + "ms)", SUCCESS);
            
        } catch (Exception e) {
            outputArea.append("ERROR: " + e.getMessage() + "\n");
            outputArea.append("-".repeat(60) + "\n\n");
            setStatus("Query failed: " + e.getMessage(), ERROR);
        }
    }
    
    private void explainQuery() {
        String query = queryInput.getText().trim();
        if (query.isEmpty()) {
            setStatus("Please enter a query", ERROR);
            return;
        }
        
        if (!query.startsWith("explain")) {
            query = "explain " + query;
        }
        
        try {
            QueryResult result = database.execute(query);
            
            outputArea.append("QUERY PLAN:\n");
            outputArea.append(result.message() + "\n");
            outputArea.append("-".repeat(60) + "\n\n");
            
            //clear table for explain
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
            
            setStatus("Query plan generated", PRIMARY);
            
        } catch (Exception e) {
            outputArea.append("ERROR: " + e.getMessage() + "\n");
            outputArea.append("-".repeat(60) + "\n\n");
            setStatus("Failed to generate plan: " + e.getMessage(), ERROR);
        }
    }
    
    private void displayResults(List<Tuple> tuples) {
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        
        if (tuples.isEmpty()) {
            outputArea.append("(No rows returned)\n");
            return;
        }
        
        //get column names from first tuple
        Tuple first = tuples.get(0);
        java.util.Map<String, Integer> columnMap = first.getColumnIndexMap();
        String[] columns = new String[columnMap.size()];
        
        for (java.util.Map.Entry<String, Integer> entry : columnMap.entrySet()) {
            columns[entry.getValue()] = entry.getKey();
        }
        
        tableModel.setColumnIdentifiers(columns);
        
        //add rows
        for (Tuple tuple : tuples) {
            Object[] row = tuple.getValues();
            tableModel.addRow(row);
        }
    }
    
    private void clearResults() {
        outputArea.setText("");
        tableModel.setRowCount(0);
        tableModel.setColumnCount(0);
        setStatus("Results cleared", PRIMARY);
    }
    
    private void reloadSampleData() {
        setStatus("Reloading data...", PRIMARY);
        outputArea.append("Clearing old data and reloading...\n");
        
        SwingWorker<Void, String> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                //re-create table heaps (clears existing data)
                Schema usersSchema = database.getCatalog().getTable("users").orElseThrow().getSchema();
                Schema productsSchema = database.getCatalog().getTable("products").orElseThrow().getSchema();
                
                usersTableHeap = new TableHeap(database.getBufferPool(), usersSchema);
                productsTableHeap = new TableHeap(database.getBufferPool(), productsSchema);
                
                database.getPlanner().registerTableHeap("users", usersTableHeap);
                database.getPlanner().registerTableHeap("products", productsTableHeap);
                
                //load with progress
                SampleDataLoader.loadUsersData(usersTableHeap, usersTableHeap.getSchema(), 
                    (current, total, msg) -> publish(msg + " " + current + "/" + total));
                
                SampleDataLoader.loadProductsData(productsTableHeap, productsTableHeap.getSchema(),
                    (current, total, msg) -> publish(msg + " " + current + "/" + total));
                
                return null;
            }
            
            @Override
            protected void process(java.util.List<String> chunks) {
                //update status with latest progress
                if (!chunks.isEmpty()) {
                    setStatus(chunks.get(chunks.size() - 1), PRIMARY);
                }
            }
            
            @Override
            protected void done() {
                try {
                    get(); //check for exceptions
                    outputArea.append("✓ Data reloaded successfully\n");
                    outputArea.append("  - 300,000 users inserted\n");
                    outputArea.append("  - 1,000,000 products inserted\n");
                    outputArea.append("-".repeat(60) + "\n\n");
                    setStatus("Data reloaded (1,300,000 records total)", SUCCESS);
                } catch (Exception e) {
                    outputArea.append("ERROR: Failed to reload data - " + e.getMessage() + "\n");
                    outputArea.append("-".repeat(60) + "\n\n");
                    setStatus("Failed to reload data", ERROR);
                }
            }
        };
        
        worker.execute();
    }
    
    private void setStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }
    
    private String getSyntaxHelp() {
        return """
            ═══════════════════════════════════════════════════
            QUERY SYNTAX
            ═══════════════════════════════════════════════════
            
            Basic Structure:
            table |> operation |> operation |> ...
            
            ───────────────────────────────────────────────────
            OPERATIONS
            ───────────────────────────────────────────────────
            
            PROJECT - Select specific columns
            users |> project(id, name, age)
            
            FILTER - Filter rows with conditions
            users |> filter(age > 25)
            users |> filter(age >= 18 and active == true)
            
            SORT - Sort results
            users |> sort(age desc)
            users |> sort(name asc)
            
            LIMIT/TAKE - Limit number of results
            users |> limit(10)
            users |> take(5)
            
            SKIP - Skip first n rows
            users |> skip(10)
            
            MODIFY - Update rows
            users |> filter(id == 1) |> modify(age = 30)
            
            REMOVE - Delete rows
            users |> filter(active == false) |> remove
            
            ───────────────────────────────────────────────────
            OPERATORS
            ───────────────────────────────────────────────────
            
            Comparison:  ==  !=  >  >=  <  <=
            Logical:     and  or
            
            ───────────────────────────────────────────────────
            DATA TYPES
            ───────────────────────────────────────────────────
            
            INTEGER:   42, 100, -5
            STRING:    "John", "active" (use double quotes!)
            BOOLEAN:   true, false
            
            ───────────────────────────────────────────────────
            EXAMPLES
            ───────────────────────────────────────────────────
            
            # Simple query
            users |> project(id, name)
            
            # Filter and sort
            users |> filter(age >= 18) 
                  |> sort(age desc) 
                  |> limit(10)
            
            # Complex query
            users |> filter(age > 25 and active == true)
                  |> project(id, name, age)
                  |> sort(name asc)
            
            # Explain query plan
            explain users |> filter(age > 25) |> project(name)
            
            # Transactions
            begin
            users |> filter(id == 1) |> modify(age = 30)
            commit
            
            ───────────────────────────────────────────────────
            AVAILABLE TABLES
            ───────────────────────────────────────────────────
            
            users (id, name, age, active)
            products (id, name, price, stock)
            
            ═══════════════════════════════════════════════════
            """;
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {}
        
        SwingUtilities.invokeLater(() -> new DatabaseGUI());
    }
}
