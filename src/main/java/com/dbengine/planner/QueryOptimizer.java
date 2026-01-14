package com.dbengine.planner;

import com.dbengine.lang.ast.*;
import com.dbengine.lang.ast.BinaryExpr.BinaryOp;
import com.dbengine.semantic.Catalog;
import com.dbengine.semantic.TableMetadata;

import java.util.*;

/**
 * Rule-based query optimizer that transforms the AST to improve performance.
 * 
 * Optimization rules:
 * 1. Filter pushdown - move filters as close to the source as possible
 * 2. Projection pruning - eliminate unnecessary columns early
 * 3. Index selection - use indexes when available
 * 4. Operator reordering - optimize execution order
 */
public class QueryOptimizer {
    private final Catalog catalog;
    
    public QueryOptimizer(Catalog catalog) {
        this.catalog = catalog;
    }
    
    /**
     * Optimize a query AST.
     */
    public QueryNode optimize(QueryNode root) {
        QueryNode optimized = root;
        
        //apply optimization rules
        optimized = pushDownFilters(optimized);
        optimized = pruneProjections(optimized);
        optimized = selectIndexes(optimized);
        
        return optimized;
    }
    
    /**
     * Rule 1: Push filters down towards the source.
     */
    private QueryNode pushDownFilters(QueryNode node) {
        if (node instanceof FilterNode filterNode) {
            QueryNode input = filterNode.input();
            
            //try to push filter through project
            if (input instanceof ProjectNode projectNode) {
                Set<String> usedColumns = getColumnsInExpr(filterNode.predicate());
                Set<String> projectedColumns = new HashSet<>(projectNode.columns());
                
                if (projectedColumns.containsAll(usedColumns)) {
                    //filter can be pushed down
                    QueryNode newInput = pushDownFilters(new FilterNode(projectNode.input(), filterNode.predicate()));
                    return new ProjectNode(newInput, projectNode.columns());
                }
            }
            
            //recursively optimize input
            return new FilterNode(pushDownFilters(input), filterNode.predicate());
        }
        
        if (node instanceof ProjectNode projectNode) {
            return new ProjectNode(pushDownFilters(projectNode.input()), projectNode.columns());
        }
        
        if (node instanceof SortNode sortNode) {
            return new SortNode(pushDownFilters(sortNode.input()), sortNode.column(), sortNode.order());
        }
        
        if (node instanceof LimitNode limitNode) {
            return new LimitNode(pushDownFilters(limitNode.input()), limitNode.count());
        }
        
        if (node instanceof TakeNode takeNode) {
            return new TakeNode(pushDownFilters(takeNode.input()), takeNode.count());
        }
        
        if (node instanceof SkipNode skipNode) {
            return new SkipNode(pushDownFilters(skipNode.input()), skipNode.count());
        }
        
        return node;
    }
    
    /**
     * Rule 2: Prune unnecessary projections.
     */
    private QueryNode pruneProjections(QueryNode node) {
        //for now, keep projections as-is
        //a more sophisticated optimizer would track which columns are actually needed
        return node;
    }
    
    /**
     * Rule 3: Select indexes for filter predicates.
     * This marks nodes for index usage, which the planner will use.
     */
    private QueryNode selectIndexes(QueryNode node) {
        if (node instanceof FilterNode filterNode) {
            QueryNode input = filterNode.input();
            
            if (input instanceof SourceNode sourceNode) {
                //check if we can use an index
                Optional<TableMetadata> tableOpt = catalog.getTable(sourceNode.table());
                if (tableOpt.isPresent()) {
                    TableMetadata table = tableOpt.get();
                    
                    //try to extract index-eligible predicates
                    Optional<IndexHint> indexHint = extractIndexHint(filterNode.predicate(), table);
                    if (indexHint.isPresent()) {
                        //mark for index usage (for now, just return the filter node)
                        //in a more sophisticated system, we'd have a proper IndexHintNode
                        return filterNode;
                    }
                }
            }
            
            return new FilterNode(selectIndexes(input), filterNode.predicate());
        }
        
        if (node instanceof ProjectNode projectNode) {
            return new ProjectNode(selectIndexes(projectNode.input()), projectNode.columns());
        }
        
        if (node instanceof SortNode sortNode) {
            return new SortNode(selectIndexes(sortNode.input()), sortNode.column(), sortNode.order());
        }
        
        if (node instanceof LimitNode limitNode) {
            return new LimitNode(selectIndexes(limitNode.input()), limitNode.count());
        }
        
        return node;
    }
    
    /**
     * Extract columns used in an expression.
     */
    private Set<String> getColumnsInExpr(Expr expr) {
        Set<String> columns = new HashSet<>();
        collectColumns(expr, columns);
        return columns;
    }
    
    private void collectColumns(Expr expr, Set<String> columns) {
        if (expr instanceof IdentifierExpr identExpr) {
            columns.add(identExpr.name());
        } else if (expr instanceof BinaryExpr binaryExpr) {
            collectColumns(binaryExpr.left(), columns);
            collectColumns(binaryExpr.right(), columns);
        }
    }
    
    /**
     * Try to extract an index hint from a filter predicate.
     */
    private Optional<IndexHint> extractIndexHint(Expr predicate, TableMetadata table) {
        if (predicate instanceof BinaryExpr binaryExpr) {
            //look for simple equality predicates: column = value
            if (binaryExpr.op() == BinaryOp.EQ) {
                if (binaryExpr.left() instanceof IdentifierExpr idExpr &&
                    binaryExpr.right() instanceof LiteralExpr litExpr) {
                    
                    String column = idExpr.name();
                    if (table.hasIndex(column)) {
                        return Optional.of(new IndexHint(column, litExpr.value()));
                    }
                }
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Hint for using an index.
     */
    public record IndexHint(String columnName, Object value) {}
}
