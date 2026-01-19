p# JavaDatabaseEngine

JDBE is a relational database engine implemented entirely in Java. It features a custom functional pipeline query language, page-based disk storage, B+ tree indexing, query optimization, & transactions with write-ahead logging and crash recovery. JDBE has been tested with up to 5 million records, achieving load times of 35 seconds for 5M records (142k records/sec) and consistent query performance of 3-4 seconds for full table scans. See `PERFORMANCE.md` for detailed benchmarks.

### Key Features

- **Custom Functional Pipeline Language** - Natural, easy to learn, composable query syntax
- **Complete Compilation Pipeline** - Lexer → Parser → Semantic Analysis → Optimizer → Executor
- **Page-Based Storage Engine** - 4KB pages with buffer pool management
- **B+ Tree Indexing** - Automatic index selection during query optimization
- **Query Optimization** - Rule-based optimizer (filter pushdown, projection pruning, index selection)
- **ACID Transactions** - BEGIN/COMMIT/ABORT with Write-Ahead Logging
- **Crash Recovery** - Redo-based recovery for durability
- **Graphical User Interface** - Swing-based GUI with pre-loaded sample data, sytnax reference, table output & console log

### Requirements

- Java 21 or higher
- Gradle 8.4+ 

### Running GUI 

```bash
  .\run-gui.bat
```

The GUI launches with **300,000 sample users and 1,000,000 sample products** (1.3M records total) by default.

### Dynamic Dataset Selection

The GUI includes a dataset selector dropdown menu allowing you to switch between different dataset sizes without restarting:

- **Small (1K)**: 100 users + 900 products - Loads in 240ms
- **Medium (35K)**: 10,000 users + 25,000 products - Loads in 274ms
- **Large (350K)**: 100,000 users + 250,000 products - Loads in 607ms
- **XL (1.3M)**: 300,000 users + 1,000,000 products - Loads in 7.2 seconds (Default)
- **XXL (5M)**: 500,000 users + 4,500,000 products - Loads in 35 seconds

Click **"Reload Data ▼"** to select a dataset size. The database automatically cleans up and reloads with the selected size.

To manually clear the database

```bash
  Remove-Item -Recurse -Force db_data\gui_demo
  .\run-gui.bat
``` 

### Supported Operations

| Operation | Description | Example |
|-----------|-------------|---------|
| `project` | Select specific columns | `project(id, name)` |
| `filter` | Filter rows by predicate | `filter(age >= 18)` |
| `sort` | Sort results | `sort(age desc)` |
| `limit` / `take` | Limit number of rows | `limit(10)` |
| `skip` | Skip rows (offset) | `skip(5)` |
| `modify` | Update rows | `modify(age = 30)` |
| `remove` | Delete rows | `remove` |
| `explain` | Show execution plan | `explain <query>` |

### Operators

**Comparison:** `==`, `!=`, `>`, `>=`, `<`, `<=`  
**Logical:** `and`, `or`  
**Data Types:** INTEGER, STRING, BOOLEAN

### Transactions

Multi-line transaction support with automatic persistence:

```sql
begin
users |> filter(id == 1) |> modify(age = 30)
products |> filter(id == 5) |> modify(stock = 50)
commit
```

Single-line modify operations also work and automatically flush to disk:

```sql
users |> filter(id == 1) |> modify(age = 30)
```

Both approaches guarantee data persistence across restarts.

## Architecture

### System Layers

```
┌─────────────────────────────────────────────────────────┐
│                 GUI Interface                           │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│              Query Compilation Pipeline                 │
│   Lexer → Parser → Semantic → Optimizer → Planner       │
└─────────────────────┬───────────────────────────────────┘
                      │
    ┌─────────────────┴─────────────────┐
    │                                   │
┌───▼──────────────┐         ┌──────────▼──────────┐
│ Execution Engine │         │  Transaction Manager│
│ (Volcano Model)  │         │  + WAL Logging      │
└────────┬─────────┘         └──────────┬──────────┘
         │                              │
    ┌────▼──────────────────────────────▼────┐
    │         Storage Layer                  │
    │  ┌─────────────┐   ┌──────────────┐    │
    │  │ Buffer Pool │   │  B+ Tree     │    │
    │  │   (LRU)     │   │  Indexes     │    │
    │  └──────┬──────┘   └──────────────┘    │
    │         │                              │
    │  ┌──────▼──────┐   ┌──────────────┐    │
    │  │ Disk Manager│   │  Heap Pages  │    │
    │  └─────────────┘   └──────────────┘    │
    └────────────────────────────────────────┘
```

### Component Overview

| Component | Location | Description |
|-----------|----------|-------------|
| **Language Layer** | `com.dbengine.lang.*` | Lexer, parser, AST (sealed interfaces & records) |
| **Semantic Analysis** | `com.dbengine.semantic.*` | Type checking, catalog, schema management |
| **Query Optimizer** | `com.dbengine.planner.*` | Rule-based optimization, physical planning |
| **Execution Engine** | `com.dbengine.exec.*` | Volcano iterator model operators |
| **Storage Layer** | `com.dbengine.storage.*` | Pages, buffer pool, disk manager, heap files |
| **Indexing** | `com.dbengine.index.*` | B+ tree implementation |
| **Transactions** | `com.dbengine.txn.*` | WAL, log manager, recovery |
| **Interfaces** | `com.dbengine.gui.*` | GUI |

### Execution Operators

- **SeqScanOperator** - Sequential table scan
- **IndexScanOperator** - B+ tree index scan
- **FilterOperator** - Predicate evaluation
- **ProjectionOperator** - Column projection
- **SortOperator** - In-memory sorting
- **LimitOperator / SkipOperator** - Result limiting/offsetting
- **ModifyOperator** - Update operations
- **RemoveOperator** - Delete operations

## Storage Engine Details

### Page Structure (4KB Fixed Size)

```
┌─────────────────────────────────────────┐
│ Page Header (9 bytes)                   │
│  ├─ Page Type (1 byte)                  │
│  ├─ Tuple Count (4 bytes)               │
│  └─ Free Space Pointer (4 bytes)        │
├─────────────────────────────────────────┤
│ Slot Directory (grows downward)         │
│  ├─ Slot 0: [Offset | Length]           │
│  ├─ Slot 1: [Offset | Length]           │
│  └─ ...                                 │
├─────────────────────────────────────────┤
│ Free Space                              │
├─────────────────────────────────────────┤
│ Tuple Data (grows upward)               │
│  └─ [Tuple N]...[Tuple 1][Tuple 0]      │
└─────────────────────────────────────────┘
```

### Buffer Pool

- **Policy**: LRU eviction
- **Features**: Pin/unpin semantics, dirty page tracking, automatic disk I/O
- **Size**: Configurable (default: 5,000 pages = 20MB)

### Write-Ahead Logging 

1. Before modifying page → Write log record
2. Log contains: TxnID, PageID, Before/After images
3. On commit → Flush log to disk
4. Dirty pages flushed lazily

## GUI Features

The GUI provides a full-featured interface with:

- **Query Editor** - Multi-line input area with syntax highlighting
- **Results Table** - Clean tabular display with formatted headers
- **Output Log** - Terminal-style execution feedback with query metrics
- **Syntax Reference** - Built-in help panel with examples
- **Dynamic Dataset Selection** - Switch between 1K to 5M records on-the-fly
- **Example Buttons** - One-click query templates that adapt to active table
- **Explain Plans** - Visual query plan display
- **Transaction Support** - Multi-line BEGIN/COMMIT blocks
- **Real-time Progress** - Loading indicators during data operations

### Sample Data

The GUI includes pre-generated realistic data across two tables:

**Users Table**: (id, name, age, active)  
- Randomized names from 50 first names × 50 last names
- Ages 18-82, 75% active users
- Scales from 100 to 500,000 users

**Products Table**: (id, name, price, stock)  
- 4 product categories with realistic variants
- Price ranges based on product type
- Stock levels 0-99
- Scales from 900 to 4,500,000 products

All data generation uses a fixed seed for reproducibility.

## Testing

```bash
  .\gradlew.bat test
```

Tests includes:
- Unit tests (Lexer, Parser, Storage, Operators)
- Integration tests (End-to-end query execution)
- Edge cases (Empty results, large datasets, transactions)

## Project Structure

```
JavaDBEngine/
├── src/
│   ├── main/java/com/dbengine/
│   │   ├── Database.java           # Main engine coordinator
│   │   ├── lang/                   # Lexer, parser, AST
│   │   │   ├── lexer/              # Tokenization
│   │   │   ├── parser/             # Recursive-descent parser
│   │   │   └── ast/                # AST nodes (sealed interfaces)
│   │   ├── semantic/               # Type checking & catalog
│   │   ├── planner/                # Optimization & physical planning
│   │   ├── exec/                   # Physical operators
│   │   ├── storage/                # Pages, buffer pool, disk manager
│   │   ├── index/                  # B+ tree
│   │   ├── txn/                    # Transactions, WAL, recovery
│   │   └── gui/                    # Graphical interface
│   └── test/java/com/dbengine/    # Comprehensive tests
├── build.gradle                    # Gradle build configuration
├── gradlew / gradlew.bat          # Gradle wrapper scripts
├── run-gui.bat                    # Quick GUI launcher (Windows)
└── db_data/                       # Database files (auto-generated)
```

## Performance Characteristics

| Operation | Time Complexity | Notes |
|-----------|----------------|-------|
| Sequential Scan | O(n) pages | Full table scan |
| Index Lookup | O(log n) | B+ tree height |
| Range Scan | O(log n + k) | k = result size |
| Sort | O(n log n) | In-memory |
| Filter | O(n) | Tuples scanned |

**Space Complexity:**
- Buffer Pool: Fixed size
- B+ Tree: O(n) for n keys
- Sort: O(n) materialization
