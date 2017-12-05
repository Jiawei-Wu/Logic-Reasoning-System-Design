import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class homework {
    private int N, Q;
    private boolean found;

    ArrayList<Statement> kb = new ArrayList<>();
    ArrayList<Statement> queries = new ArrayList<>();

    private static class Param {
        public boolean constant;
        public String param;
        Param(String p, boolean c) {
            constant = c;
            param = p;
        }
    }

    private static class Statement {
        public ArrayList<Clause> clauses;
        public HashSet<String> variables;
        Statement() {
            clauses = new ArrayList<>();
            variables = new HashSet<>();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < clauses.size(); ++i) {
                if (i != 0) sb.append(" | ");
                sb.append(clauses.get(i).toString());
            }
            return sb.toString();
        }
    }

    private static class Clause {
        public boolean neg;
        public ArrayList<Param> params;
        public String name;
        Clause(String n, boolean negative) {
            name = n;
            neg = negative;
            params = new ArrayList<>();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            if (neg) {
                sb.append('~');
            }
            sb.append(name + "(");
            for (int i = 0; i < params.size(); ++i) {
                if (i != 0) sb.append(", ");
                sb.append(params.get(i).param);
            }
            sb.append(")");
            return sb.toString();
        }

        @Override
        public Clause clone() {
            Clause res = new Clause(this.name, this.neg);
            for (Param p : this.params) {
                Param q = new Param(p.param, p.constant);
                res.params.add(q);
            }
            return res;
        }
    }

    static private boolean fullReplaced(Statement s) {
        return s.variables.isEmpty();
    }

    static private Clause parseClause(String s) {
        int neg = s.indexOf("~");
        int b = s.indexOf("("), e = s.indexOf(")");
        String name = s.substring(neg + 1, b).trim();
        Clause clause = new Clause(name, neg != -1);
        String[] ps = s.substring(b + 1, e).split(",");
        for (int i = 0; i < ps.length; ++i) {
            String p = ps[i].trim();
            Param param = new Param(p, Character.isUpperCase(p.charAt(0)));
            clause.params.add(param);
        }
        return clause;
    }

    static private Statement parse(String s) {
        Statement statement = new Statement();
        String[] cs = s.split("\\|");
        for (int i = 0; i < cs.length; ++i) {
            Clause c = parseClause(cs[i]);
            for (int j = 0; j < c.params.size(); ++j) {
                if (!c.params.get(j).constant) {
                    statement.variables.add(c.params.get(j).param);
                }
            }
            statement.clauses.add(c);
        }

        Collections.sort(statement.clauses, new Comparator<Clause>(){
            @Override
            public int compare(Clause a, Clause b){
                return a.name.compareTo(b.name);
            }
        });
        return statement;
    }

    static private HashMap<String, String> unify(Clause a, Clause b) {
        HashMap<String, String> res = new HashMap<>();
        if (a.name.compareTo(b.name) != 0) {
            return res;
        }
        for (int i = 0; i < a.params.size(); ++i) {
            Param pa = a.params.get(i);
            Param pb = b.params.get(i);
            if (pa.constant && pb.constant) {
                if (pa.param.compareTo(pb.param) != 0) {
                    return new HashMap<>();
                }
            } else if (pb.constant) {
                res.put(pa.param, pb.param);
            }
        }
        return res;
    }

    static private Statement replaceVariables(Statement a, Statement b) {
        Statement res = new Statement();
        HashMap<String, String> replace = new HashMap<>();
        for (int i = 0; i < a.clauses.size(); ++i) {
            Clause ca = a.clauses.get(i);
            for (int j = 0; j < b.clauses.size(); ++j) {
                Clause cb = b.clauses.get(j);
                if (ca.name.compareTo(cb.name) == 0) {
                    HashMap<String, String> merge = unify(ca, cb);
                    for (Map.Entry<String, String> entry : merge.entrySet()) {
                        if (replace.containsKey(entry.getKey())) {
                            if (replace.get(entry.getKey()).compareTo(entry.getValue()) != 0) {
                                return res;
                            }
                        }
                    }
                    replace.putAll(merge);
                }
            }
        }
        for(Clause p : a.clauses) {
            Clause q = p.clone();
            for (Param param : q.params) {
                if (!param.constant && replace.containsKey(param.param)) {
                    param.constant = true;
                    param.param = replace.get(param.param);
                }
            }
            res.clauses.add(q);
        }
        res.variables = new HashSet<>(a.variables);
        res.variables.removeAll(replace.keySet());
        return res;
    }

    static boolean canEliminate(Clause a, Clause b) {
        if (a.neg == b.neg) return false;
        if (a.name.compareTo(b.name) != 0) return false;
        if (a.params.size() != b.params.size()) return false;
        for (int i = 0; i < a.params.size(); ++i) {
            Param pa = a.params.get(i);
            Param pb = b.params.get(i);
            if (!pa.constant || !pb.constant) return false;
            if (pa.param.compareTo(pb.param) != 0) return false;
        }
        return true;
    }

    private Statement eliminate(Statement a, Statement b) {
        Statement res = new Statement();
        Statement am = replaceVariables(a, b);
        Statement bm = replaceVariables(b, a);
        boolean[] removea = new boolean[am.clauses.size()];
        boolean[] removeb = new boolean[bm.clauses.size()];
        boolean removed = false;
        for (int i = 0; i < am.clauses.size(); ++i) {
            Clause ca = am.clauses.get(i);
            for (int j = 0; j < bm.clauses.size(); ++j) {
                Clause cb = bm.clauses.get(j);
                if (canEliminate(ca, cb)) {
                    removea[i] = removeb[j] = true;
                    removed = true;
                }
            }
        }
        if (!removed)
            return res;
        int counta = 0, countb = 0;
        for (int i = 0; i < removea.length; ++i) {
            if (removea[i]) counta ++;
        }
        for (int i = 0; i < removeb.length; ++i) {
            if (removeb[i]) countb ++;
        }

        if (counta != removea.length && countb != removeb.length)
            return res;
        for (int i = 0; i < am.clauses.size(); ++i) {
            if (!removea[i]) {
                res.clauses.add(am.clauses.get(i).clone());
            }
        }
        for (int i = 0; i < bm.clauses.size(); ++i) {
            if (!removeb[i]) {
                res.clauses.add(bm.clauses.get(i).clone());
            }
        }
        if (res.clauses.size() == 0) {
            found = true;
        }
        Collections.sort(res.clauses, new Comparator<Clause>(){
            @Override
            public int compare(Clause a, Clause b){
                return a.name.compareTo(b.name);
            }
        });
        res.variables.addAll(am.variables);
        res.variables.addAll(bm.variables);
        return res;
    }

    static Statement neg(Statement s) {
        Statement res = new Statement();
        for(Clause p : s.clauses) {
            Clause q = p.clone();
            q.neg = !q.neg;
            res.clauses.add(q);
        }
        res.variables = new HashSet<>(s.variables);
        return res;
    }

    boolean hasS(ArrayList<Statement> a, Statement s) {
        for (Statement as : a) {
            if (as.clauses.size() != s.clauses.size()) continue;
            if (as.variables.size() != s.variables.size()) continue;
            boolean flag = false;
            for (int i = 0; i < as.clauses.size(); ++i) {
                Clause c1 = as.clauses.get(i);
                Clause c2 = s.clauses.get(i);
                if (c1.neg != c2.neg || c1.name.compareTo(c2.name) != 0) {
                    flag = true;
                    break;
                }
                for (int j = 0; j < c1.params.size(); ++j) {
                    Param p1 = c1.params.get(j);
                    Param p2 = c2.params.get(j);
                    if (p1.constant != p2.constant) {
                        flag = true;
                        break;
                    }
                    if (p1.param.compareTo(p2.param) != 0){
                        flag = true;
                        break;
                    }
                }
                if (flag) break;
            }
            if (flag) continue;
            return true;
        }
        return false;
    }

    boolean work(Statement q) {
        found = false;
        ArrayList<Statement> a = new ArrayList<>();
        a.add(q);
        for (int i = 0; i < kb.size(); ++i) {
            for (int j = i + 1; j < kb.size(); ++j) {
                Statement s = eliminate(kb.get(i), kb.get(j));
                if (s.clauses.size() != 0 && !hasS(a, s)) {
                    a.add(s);
//                    System.out.println(s);
                }
            }
        }
        int length;
        do {
            length = a.size();
            for (int i = 0; i < kb.size(); ++i) {
                for (int j = 0; j < a.size(); ++j) {
                    Statement s = eliminate(kb.get(i), a.get(j));
                    if (found) return true;
                    if (s.clauses.size() != 0 && !hasS(a, s)) {
                        a.add(s);
//                        System.out.println(s);
                    }
                }
            }
            for (int i = 0; i < a.size(); ++i) {
                for (int j = i + 1; j < a.size(); ++j) {
                    Statement s = eliminate(a.get(i), a.get(j));
                    if (found) return true;
                    if (s.clauses.size() != 0 && !hasS(a, s)) {
                        a.add(s);
//                        System.out.println(s);
                    }
                }
            }
        } while (a.size() != length);
        return false;
    }

    void read() {
        try {
            Scanner scanner = new Scanner(new FileInputStream("input.txt"));
            Q = Integer.parseInt(scanner.nextLine());
            for (int i = 0; i < Q; ++i) {
                Statement s = parse(scanner.nextLine());
                queries.add(s);
            }
            N = Integer.parseInt(scanner.nextLine());
            for (int i = 0; i < N; ++i) {
                Statement s = parse(scanner.nextLine());
                kb.add(s);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void getAns() {
        try {
            FileWriter writer = new FileWriter("output.txt");
            for (int i = 0; i < Q; ++i) {
                boolean res = work(neg(queries.get(i)));
                writer.write(res ? "TRUE" : "FALSE");
                writer.write(System.getProperty("line.separator"));
            }
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void run() {
        read();
        getAns();
    }

    public static void main(String[] args) {
        (new homework()).run();
    }
}
