package com.dbengine.semantic;

import com.dbengine.lang.ast.*;

import java.util.HashSet;
import java.util.Map;

/**
 * Performs semantic analysis on the AST.
 * Responsibilities:
 * - Table existence checking
 * - Column resolution
 * - Type checking
 * - Predicate legality
 * - Operator compatibility
 */
public class SemanticAnalyzer {
    private final Catalog catalog;
    
    public SemanticAnalyzer(Catalog catalog) {
        this.catalog = catalog;
    }
    
    /**
     * Analyze a query and return its output schema.
     */
    public Schema analyze(QueryNode node) {
        return analyzeNode(node);
    }
    
    private Schema analyzeNode(QueryNode node) {
        return switch (node) {
            case SourceNode s -> analyzeSource(s);
            case FilterNode f -> analyzeFilter(f);
            case ProjectNode p -> analyzeProject(p);
            case SortNode s -> analyzeSort(s);
            case LimitNode l -> analyzeLimit(l);
            case TakeNode t -> analyzeTake(t);
            case SkipNode s -> analyzeSkip(s);
            case ModifyNode m -> analyzeModify(m);
            case RemoveNode r -> analyzeRemove(r);
        };
    }
    
    private Schema analyzeSource(SourceNode node) {
        TableMetadata table = catalog.getTable(node.table())
            .orElseThrow(() -> new SemanticException("Table '" + node.table() + "' does not exist"));
        return table.getSchema();
    }
    
    private Schema analyzeFilter(FilterNode node) {
        Schema inputSchema = analyzeNode(node.input());
        DataType predicateType = analyzeExpr(node.predicate(), inputSchema);
        
        if (predicateType != DataType.BOOLEAN && predicateType != DataType.NULL) {
            throw new SemanticException("Filter predicate must be boolean, got " + predicateType);
        }
        
        return inputSchema;
    }
    
    private Schema analyzeProject(ProjectNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        //verify all projected columns exist
        for (String column : node.columns()) {
            if (!inputSchema.hasColumn(column)) {
                throw new SemanticException("Column '" + column + "' does not exist in schema " + inputSchema);
            }
        }
        
        return inputSchema.project(new HashSet<>(node.columns()));
    }
    
    private Schema analyzeSort(SortNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        if (!inputSchema.hasColumn(node.column())) {
            throw new SemanticException("Sort column '" + node.column() + "' does not exist in schema " + inputSchema);
        }
        
        return inputSchema;
    }
    
    private Schema analyzeLimit(LimitNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        if (node.count() < 0) {
            throw new SemanticException("Limit count must be non-negative");
        }
        
        return inputSchema;
    }
    
    private Schema analyzeTake(TakeNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        if (node.count() < 0) {
            throw new SemanticException("Take count must be non-negative");
        }
        
        return inputSchema;
    }
    
    private Schema analyzeSkip(SkipNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        if (node.count() < 0) {
            throw new SemanticException("Skip count must be non-negative");
        }
        
        return inputSchema;
    }
    
    private Schema analyzeModify(ModifyNode node) {
        Schema inputSchema = analyzeNode(node.input());
        
        for (Map.Entry<String, Expr> entry : node.updates().entrySet()) {
            String column = entry.getKey();
            Expr value = entry.getValue();
            
            if (!inputSchema.hasColumn(column)) {
                throw new SemanticException("Column '" + column + "' does not exist");
            }
            
            DataType expectedType = inputSchema.getColumnType(column).get();
            DataType actualType = analyzeExpr(value, inputSchema);
            
            if (!expectedType.isCompatible(actualType)) {
                throw new SemanticException(
                    "Type mismatch for column '" + column + "': expected " + 
                    expectedType + ", got " + actualType);
            }
        }
        
        return inputSchema;
    }
    
    private Schema analyzeRemove(RemoveNode node) {
        return analyzeNode(node.input());
    }
    
    private DataType analyzeExpr(Expr expr, Schema schema) {
        return switch (expr) {
            case IdentifierExpr i -> analyzeIdentifier(i, schema);
            case LiteralExpr l -> analyzeLiteral(l);
            case BinaryExpr b -> analyzeBinary(b, schema);
        };
    }
    
    private DataType analyzeIdentifier(IdentifierExpr expr, Schema schema) {
        return schema.getColumnType(expr.name())
            .orElseThrow(() -> new SemanticException("Column '" + expr.name() + "' does not exist"));
    }
    
    private DataType analyzeLiteral(LiteralExpr expr) {
        return switch (expr.type()) {
            case INTEGER -> DataType.INTEGER;
            case STRING -> DataType.STRING;
            case BOOLEAN -> DataType.BOOLEAN;
            case NULL -> DataType.NULL;
        };
    }
    
    private DataType analyzeBinary(BinaryExpr expr, Schema schema) {
        DataType leftType = analyzeExpr(expr.left(), schema);
        DataType rightType = analyzeExpr(expr.right(), schema);
        
        return switch (expr.op()) {
            case OR, AND -> {
                if (leftType != DataType.BOOLEAN && leftType != DataType.NULL) {
                    throw new SemanticException("Logical operator requires boolean operands, got " + leftType);
                }
                if (rightType != DataType.BOOLEAN && rightType != DataType.NULL) {
                    throw new SemanticException("Logical operator requires boolean operands, got " + rightType);
                }
                yield DataType.BOOLEAN;
            }
            case EQ, NE -> {
                if (!leftType.isCompatible(rightType)) {
                    throw new SemanticException("Incompatible types in equality: " + leftType + " and " + rightType);
                }
                yield DataType.BOOLEAN;
            }
            case LT, LE, GT, GE -> {
                if (leftType != DataType.INTEGER && leftType != DataType.NULL) {
                    throw new SemanticException("Relational operator requires integer operands, got " + leftType);
                }
                if (rightType != DataType.INTEGER && rightType != DataType.NULL) {
                    throw new SemanticException("Relational operator requires integer operands, got " + rightType);
                }
                yield DataType.BOOLEAN;
            }
        };
    }
}
