package com.dbengine.planner;

import com.dbengine.exec.*;
import com.dbengine.index.BPlusTree;
import com.dbengine.lang.ast.*;
import com.dbengine.semantic.Catalog;
import com.dbengine.semantic.TableMetadata;
import com.dbengine.storage.BufferPool;
import com.dbengine.storage.TableHeap;

import java.util.HashMap;
import java.util.Map;

/**
 * Converts an optimized logical plan (AST) into a physical execution plan.
 */
public class PhysicalPlanner {
    private final Catalog catalog;
    private final BufferPool bufferPool;
    private final Map<String, TableHeap> tableHeaps;
    private final Map<String, Map<String, BPlusTree>> indexes;
    
    public PhysicalPlanner(Catalog catalog, BufferPool bufferPool) {
        this.catalog = catalog;
        this.bufferPool = bufferPool;
        this.tableHeaps = new HashMap<>();
        this.indexes = new HashMap<>();
    }
    
    /**
     * Create a physical execution plan from a logical plan.
     */
    public Operator createPlan(QueryNode node) {
        return buildOperator(node);
    }
    
    private Operator buildOperator(QueryNode node) {
        return switch (node) {
            case SourceNode s -> buildSeqScan(s);
            case FilterNode f -> buildFilter(f);
            case ProjectNode p -> buildProjection(p);
            case SortNode s -> buildSort(s);
            case LimitNode l -> buildLimit(l);
            case TakeNode t -> buildTake(t);
            case SkipNode s -> buildSkip(s);
            case ModifyNode m -> buildModify(m);
            case RemoveNode r -> buildRemove(r);
        };
    }
    
    private Operator buildSeqScan(SourceNode node) {
        TableHeap tableHeap = getTableHeap(node.table());
        return new SeqScanOperator(tableHeap);
    }
    
    private Operator buildFilter(FilterNode node) {
        Operator child = buildOperator(node.input());
        return new FilterOperator(child, node.predicate());
    }
    
    private Operator buildProjection(ProjectNode node) {
        Operator child = buildOperator(node.input());
        return new ProjectionOperator(child, node.columns());
    }
    
    private Operator buildSort(SortNode node) {
        Operator child = buildOperator(node.input());
        return new SortOperator(child, node.column(), node.order());
    }
    
    private Operator buildLimit(LimitNode node) {
        Operator child = buildOperator(node.input());
        return new LimitOperator(child, node.count());
    }
    
    private Operator buildTake(TakeNode node) {
        Operator child = buildOperator(node.input());
        return new LimitOperator(child, node.count());
    }
    
    private Operator buildSkip(SkipNode node) {
        Operator child = buildOperator(node.input());
        return new SkipOperator(child, node.count());
    }
    
    private Operator buildModify(ModifyNode node) {
        Operator child = buildOperator(node.input());
        TableHeap tableHeap = getTableHeapFromNode(node.input());
        return new ModifyOperator(child, node.updates(), tableHeap);
    }
    
    private Operator buildRemove(RemoveNode node) {
        Operator child = buildOperator(node.input());
        TableHeap tableHeap = getTableHeapFromNode(node.input());
        return new RemoveOperator(child, tableHeap);
    }
    
    private TableHeap getTableHeap(String tableName) {
        return tableHeaps.computeIfAbsent(tableName, name -> {
            TableMetadata table = catalog.getTable(name)
                .orElseThrow(() -> new RuntimeException("Table not found: " + name));
            
            try {
                return new TableHeap(bufferPool, table.getSchema());
            } catch (Exception e) {
                throw new RuntimeException("Failed to create table heap", e);
            }
        });
    }
    
    private TableHeap getTableHeapFromNode(QueryNode node) {
        if (node instanceof SourceNode sourceNode) {
            return getTableHeap(sourceNode.table());
        } else if (node instanceof FilterNode filterNode) {
            return getTableHeapFromNode(filterNode.input());
        } else if (node instanceof ProjectNode projectNode) {
            return getTableHeapFromNode(projectNode.input());
        }
        throw new RuntimeException("Cannot determine table heap from node: " + node.getClass());
    }
    
    public void registerTableHeap(String tableName, TableHeap tableHeap) {
        tableHeaps.put(tableName, tableHeap);
    }
    
    public void registerIndex(String tableName, String columnName, BPlusTree index) {
        indexes.computeIfAbsent(tableName, k -> new HashMap<>())
               .put(columnName, index);
    }
}
