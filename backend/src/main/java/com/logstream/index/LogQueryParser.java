package com.logstream.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.QueryBuilder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Hand-written parser for the LogStream query language. Lucene's stock
 * QueryParser cannot express `response_time > 1000` and mangles keyword values
 * like `billing-api` by analysing them, so the DSL is parsed here instead.
 *
 * Grammar:
 *   or      := and (OR and)*
 *   and     := unary (AND? unary)*        // whitespace means AND
 *   unary   := NOT unary | '(' or ')' | clause
 *   clause  := field (':' | '>' | '>=' | '<' | '<=') value | bareword
 *
 * Examples:
 *   level:ERROR AND service:billing-api AND response_time > 1000
 *   (level:ERROR OR level:FATAL) AND NOT service:healthcheck
 *   timeout AND timestamp >= now-15m
 */
public class LogQueryParser {

    private final Analyzer analyzer;
    private List<Token> tokens;
    private int pos;

    public LogQueryParser(Analyzer analyzer) {
        this.analyzer = analyzer;
    }

    public Query parse(String input) {
        if (input == null || input.isBlank()) return new MatchAllDocsQuery();
        this.tokens = tokenize(input);
        this.pos = 0;
        Query q = parseOr();
        if (pos < tokens.size()) {
            throw new QuerySyntaxException("Unexpected '" + peek().text + "' at position " + peek().start);
        }
        return q;
    }

    // ---------------------------------------------------------------- parsing

    private Query parseOr() {
        Query left = parseAnd();
        if (!matches(Type.OR)) return left;
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        b.add(left, BooleanClause.Occur.SHOULD);
        while (matches(Type.OR)) {
            next();
            b.add(parseAnd(), BooleanClause.Occur.SHOULD);
        }
        return b.build();
    }

    private Query parseAnd() {
        List<BooleanClause> clauses = new ArrayList<>();
        clauses.add(parseUnary());
        while (pos < tokens.size() && !matches(Type.OR) && !matches(Type.RPAREN)) {
            if (matches(Type.AND)) next();          // explicit AND
            if (pos >= tokens.size() || matches(Type.RPAREN)) break;
            clauses.add(parseUnary());
        }
        if (clauses.size() == 1) return clauses.get(0).getQuery();
        BooleanQuery.Builder b = new BooleanQuery.Builder();
        clauses.forEach(b::add);
        return b.build();
    }

    private BooleanClause parseUnary() {
        if (matches(Type.NOT)) {
            next();
            BooleanClause inner = parseUnary();
            BooleanQuery.Builder b = new BooleanQuery.Builder();
            b.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
            b.add(inner.getQuery(), BooleanClause.Occur.MUST_NOT);
            return new BooleanClause(b.build(), BooleanClause.Occur.MUST);
        }
        if (matches(Type.LPAREN)) {
            next();
            Query q = parseOr();
            expect(Type.RPAREN);
            return new BooleanClause(q, BooleanClause.Occur.MUST);
        }
        return new BooleanClause(parseClause(), BooleanClause.Occur.MUST);
    }

    private Query parseClause() {
        Token t = expectValueToken();
        // field:value / field > value
        if (pos < tokens.size() && peek().type == Type.OP) {
            String field = t.text;
            String op = next().text;
            Token v = expectValueToken();
            return buildFieldQuery(field, op, v.text, v.quoted);
        }
        // bare term -> full-text search over the message body
        return textQuery(Fields.DEFAULT_FIELD, t.text, t.quoted);
    }

    // ---------------------------------------------------------------- queries

    private Query buildFieldQuery(String field, String op, String value, boolean quoted) {
        if (Fields.NUMERIC.contains(field)) {
            long v = parseNumber(field, value);
            return switch (op) {
                case ":"  -> LongPoint.newExactQuery(field, v);
                case ">"  -> LongPoint.newRangeQuery(field, Math.addExact(v, 1), Long.MAX_VALUE);
                case ">=" -> LongPoint.newRangeQuery(field, v, Long.MAX_VALUE);
                case "<"  -> LongPoint.newRangeQuery(field, Long.MIN_VALUE, Math.addExact(v, -1));
                case "<=" -> LongPoint.newRangeQuery(field, Long.MIN_VALUE, v);
                default   -> throw new QuerySyntaxException("Operator '" + op + "' is not valid here");
            };
        }

        if (!":".equals(op)) {
            throw new QuerySyntaxException(
                    "'" + op + "' only works on numeric fields (" + String.join(", ", Fields.NUMERIC) + ")");
        }

        if (Fields.KEYWORD.contains(field)) {
            String v = Fields.LEVEL.equals(field) ? value.toUpperCase() : value;
            if (!quoted && (v.indexOf('*') >= 0 || v.indexOf('?') >= 0)) {
                return new WildcardQuery(new Term(field, v));
            }
            return new TermQuery(new Term(field, v));
        }

        return textQuery(field, value, quoted);
    }

    /** Runs the value through the same analyzer used at index time. */
    private Query textQuery(String field, String value, boolean quoted) {
        QueryBuilder qb = new QueryBuilder(analyzer);
        Query q = quoted
                ? qb.createPhraseQuery(field, value)
                : qb.createBooleanQuery(field, value, BooleanClause.Occur.MUST);
        if (q == null) {
            // Value was entirely stopwords; match nothing rather than everything.
            return new BooleanQuery.Builder().build();
        }
        return q;
    }

    /** Accepts raw millis, ISO-8601 instants, and relative offsets like now-15m. */
    private long parseNumber(String field, String raw) {
        String v = raw.trim();
        try {
            return Long.parseLong(v);
        } catch (NumberFormatException ignored) { /* fall through */ }

        if (Fields.TIMESTAMP.equals(field)) {
            if (v.equalsIgnoreCase("now")) return System.currentTimeMillis();
            if (v.toLowerCase().startsWith("now-")) {
                return System.currentTimeMillis() - parseDuration(v.substring(4));
            }
            try {
                return Instant.parse(v).toEpochMilli();
            } catch (Exception ignored) { /* fall through */ }
        }
        throw new QuerySyntaxException("'" + raw + "' is not a valid value for " + field);
    }

    private long parseDuration(String d) {
        if (d.length() < 2) throw new QuerySyntaxException("Bad duration: " + d);
        long n = Long.parseLong(d.substring(0, d.length() - 1));
        char unit = Character.toLowerCase(d.charAt(d.length() - 1));
        return switch (unit) {
            case 's' -> n * 1_000L;
            case 'm' -> n * 60_000L;
            case 'h' -> n * 3_600_000L;
            case 'd' -> n * 86_400_000L;
            default  -> throw new QuerySyntaxException("Unknown time unit '" + unit + "' (use s, m, h or d)");
        };
    }

    // -------------------------------------------------------------- tokenizer

    private enum Type { WORD, OP, AND, OR, NOT, LPAREN, RPAREN }

    private record Token(Type type, String text, boolean quoted, int start) {}

    private static List<Token> tokenize(String s) {
        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }

            if (c == '(') { out.add(new Token(Type.LPAREN, "(", false, i++)); continue; }
            if (c == ')') { out.add(new Token(Type.RPAREN, ")", false, i++)); continue; }
            if (c == ':') { out.add(new Token(Type.OP, ":", false, i++)); continue; }
            if (c == '>' || c == '<') {
                int start = i;
                String op = String.valueOf(c);
                i++;
                if (i < s.length() && s.charAt(i) == '=') { op += "="; i++; }
                out.add(new Token(Type.OP, op, false, start));
                continue;
            }
            if (c == '"') {
                int start = i++;
                StringBuilder sb = new StringBuilder();
                while (i < s.length() && s.charAt(i) != '"') sb.append(s.charAt(i++));
                if (i >= s.length()) throw new QuerySyntaxException("Unclosed quote at position " + start);
                i++; // closing quote
                out.add(new Token(Type.WORD, sb.toString(), true, start));
                continue;
            }

            int start = i;
            StringBuilder sb = new StringBuilder();
            while (i < s.length()) {
                char d = s.charAt(i);
                if (Character.isWhitespace(d) || d == '(' || d == ')' || d == ':' || d == '>' || d == '<') break;
                sb.append(d);
                i++;
            }
            String w = sb.toString();
            Type type = switch (w.toUpperCase()) {
                case "AND", "&&" -> Type.AND;
                case "OR", "||"  -> Type.OR;
                case "NOT", "!"  -> Type.NOT;
                default          -> Type.WORD;
            };
            out.add(new Token(type, w, false, start));
        }
        return out;
    }

    private Token peek() { return tokens.get(pos); }

    private Token next() { return tokens.get(pos++); }

    private boolean matches(Type t) { return pos < tokens.size() && tokens.get(pos).type == t; }

    private void expect(Type t) {
        if (!matches(t)) throw new QuerySyntaxException("Expected " + t + " but the query ended or found something else");
        next();
    }

    private Token expectValueToken() {
        if (pos >= tokens.size()) throw new QuerySyntaxException("Query ended unexpectedly");
        Token t = next();
        if (t.type != Type.WORD && t.type != Type.AND && t.type != Type.OR && t.type != Type.NOT) {
            throw new QuerySyntaxException("Expected a value but found '" + t.text + "'");
        }
        return t;
    }

    public static class QuerySyntaxException extends RuntimeException {
        public QuerySyntaxException(String m) { super(m); }
    }
}
