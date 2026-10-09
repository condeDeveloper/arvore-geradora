package br.com.conde.arvoregeradora;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Um GRAFO NAO DIRIGIDO COM PESOS, guardado como lista de arestas e de adjacencia.
 *
 * <p>As duas representacoes convivem porque os algoritmos pedem coisas diferentes:
 * Kruskal percorre as arestas ordenadas por peso e nao olha para vertice nenhum;
 * Prim cresce a partir de um vertice e precisa dos vizinhos dele. Guardar so uma das
 * duas obrigaria um dos dois a construir a outra, e ai a medida de custo mediria a
 * conversao junto.
 *
 * <p>Os PESOS sao inteiros de proposito. Comparar pesos em ponto flutuante traz de
 * volta a pergunta "estes dois pesos sao iguais?", que e exatamente a pergunta que
 * decide quantas arvores minimas existem. Com inteiros, empate e empate.
 */
public final class Grafo {

    /** Uma aresta, com os vertices em ordem crescente para nao haver duas versoes dela. */
    public record Aresta(int u, int v, long peso) implements Comparable<Aresta> {

        public Aresta {
            if (u > v) {
                int troca = u;

                u = v;
                v = troca;
            }
        }

        public int outro(int ponta) {
            if (ponta == u) return v;
            if (ponta == v) return u;

            throw new IllegalArgumentException(ponta + " nao e ponta de " + this);
        }

        /**
         * Ordena por peso e, no empate, pelos vertices.
         *
         * <p>O desempate pelos vertices nao e enfeite: sem ele, duas execucoes de
         * Kruskal sobre o mesmo grafo podem devolver arvores diferentes conforme a
         * ordenacao seja estavel ou nao, e a tabela mudaria de maquina para maquina.
         */
        @Override
        public int compareTo(Aresta outra) {
            int porPeso = Long.compare(peso, outra.peso);

            if (porPeso != 0) return porPeso;

            int porU = Integer.compare(u, outra.u);

            return porU != 0 ? porU : Integer.compare(v, outra.v);
        }

        @Override
        public String toString() {
            return u + "-" + v + ":" + peso;
        }
    }

    private final String nome;
    private final int vertices;
    private final List<Aresta> arestas;
    private final List<List<Aresta>> incidentes;

    private Grafo(String nome, int vertices, List<Aresta> arestas) {
        this.nome = nome;
        this.vertices = vertices;
        this.arestas = List.copyOf(arestas);

        List<List<Aresta>> porVertice = new ArrayList<>(vertices);

        for (int i = 0; i < vertices; i++) porVertice.add(new ArrayList<>());

        for (Aresta a : arestas) {
            porVertice.get(a.u()).add(a);
            porVertice.get(a.v()).add(a);
        }

        List<List<Aresta>> congelado = new ArrayList<>(vertices);

        for (List<Aresta> l : porVertice) congelado.add(List.copyOf(l));

        this.incidentes = List.copyOf(congelado);
    }

    public static Construtor com(String nome, int vertices) {
        return new Construtor(nome, vertices);
    }

    public static final class Construtor {

        private final String nome;
        private final int vertices;
        private final List<Aresta> arestas = new ArrayList<>();

        private Construtor(String nome, int vertices) {
            this.nome = nome;
            this.vertices = vertices;
        }

        public Construtor aresta(int u, int v, long peso) {
            if (u == v) throw new IllegalArgumentException("laco em " + u + ": nao muda arvore geradora nenhuma");
            if (u < 0 || u >= vertices || v < 0 || v >= vertices)
                throw new IllegalArgumentException("vertice fora de 0 a " + (vertices - 1));

            arestas.add(new Aresta(u, v, peso));

            return this;
        }

        public Grafo pronto() {
            return new Grafo(nome, vertices, arestas);
        }
    }

    public String nome() {
        return nome;
    }

    public int vertices() {
        return vertices;
    }

    public List<Aresta> arestas() {
        return arestas;
    }

    public List<Aresta> incidentesA(int v) {
        return incidentes.get(v);
    }

    public long pesoTotal() {
        return arestas.stream().mapToLong(Aresta::peso).sum();
    }

    /** Quantos pesos DIFERENTES o grafo tem. Quanto menos, mais empates e mais arvores minimas. */
    public long pesosDistintos() {
        return arestas.stream().mapToLong(Aresta::peso).distinct().count();
    }

    /**
     * O mesmo grafo com as arestas escritas na ordem inversa.
     *
     * <p>Mesmo grafo, mesma resposta, outra ordem de escrita. E com isso da para medir
     * o que o desempate faz: com ordem total nas arestas sai a mesma arvore, e sem
     * desempate sai outra arvore do mesmo peso.
     */
    public Grafo comArestasInvertidas() {
        Construtor c = com(nome + " ao contrario", vertices);
        List<Aresta> inversa = new ArrayList<>(arestas);

        java.util.Collections.reverse(inversa);

        for (Aresta a : inversa) c.aresta(a.u(), a.v(), a.peso());

        return c.pronto();
    }

    public boolean conexo() {
        if (vertices == 0) return true;

        boolean[] visto = new boolean[vertices];
        List<Integer> pilha = new ArrayList<>();

        pilha.add(0);
        visto[0] = true;

        int quantos = 1;

        while (!pilha.isEmpty()) {
            int u = pilha.remove(pilha.size() - 1);

            for (Aresta a : incidentes.get(u)) {
                int w = a.outro(u);

                if (!visto[w]) {
                    visto[w] = true;
                    quantos++;
                    pilha.add(w);
                }
            }
        }

        return quantos == vertices;
    }

    @Override
    public String toString() {
        return nome + " (" + vertices + " vertices, " + arestas.size() + " arestas)";
    }

    // ───────────────────────────── as formas ─────────────────────────────

    /**
     * O CICLO com pesos todos diferentes: a arvore minima e o ciclo menos a aresta
     * mais pesada, e ela e UNICA.
     *
     * <p>Serve de controle: pesos distintos garantem arvore minima unica, e esse e um
     * teorema que a tabela de empates confere.
     */
    public static Grafo ciclo(int n) {
        Construtor c = com("ciclo de " + n, n);

        for (int i = 0; i < n; i++) c.aresta(i, (i + 1) % n, i + 1L);

        return c.pronto();
    }

    /**
     * O CICLO COM PESOS IGUAIS: n arvores minimas, uma por aresta retirada.
     *
     * <p>E o caso mais simples em que a resposta nao e unica, e ele mostra por que
     * comparar ARVORES e errado e comparar PESOS e certo.
     */
    public static Grafo cicloUniforme(int n) {
        Construtor c = com("ciclo uniforme de " + n, n);

        for (int i = 0; i < n; i++) c.aresta(i, (i + 1) % n, 1);

        return c.pronto();
    }

    /** O grafo COMPLETO com pesos sorteados. */
    public static Grafo completo(int n, int maiorPeso, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        Construtor c = com("completo de " + n + " ate " + maiorPeso, n);

        for (int u = 0; u < n; u++)
            for (int v = u + 1; v < n; v++) c.aresta(u, v, 1L + sorteio.ate(maiorPeso));

        return c.pronto();
    }

    /** A GRADE: o grafo com que mapa de verdade se parece. */
    public static Grafo grade(int largura, int altura, int maiorPeso, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        Construtor c = com("grade " + largura + "x" + altura, largura * altura);

        for (int y = 0; y < altura; y++)
            for (int x = 0; x < largura; x++) {
                int aqui = y * largura + x;

                if (x + 1 < largura) c.aresta(aqui, aqui + 1, 1L + sorteio.ate(maiorPeso));
                if (y + 1 < altura) c.aresta(aqui, aqui + largura, 1L + sorteio.ate(maiorPeso));
            }

        return c.pronto();
    }

    /**
     * A ESTRELA COM ATALHO: um vertice central caro ligado a todos, e um caminho
     * barato por fora.
     *
     * <p>E a forma em que a versao plausivel, a que usa um conjunto de visitados em
     * vez de conjuntos disjuntos, erra: ela recusa uma aresta barata por achar que ela
     * fecharia ciclo quando os dois lados ainda estao em pedacos separados.
     */
    public static Grafo estrelaComAtalho(int pontas) {
        int n = pontas + 1;
        Construtor c = com("estrela com atalho de " + n, n);

        for (int i = 1; i <= pontas; i++) c.aresta(0, i, 100);

        for (int i = 1; i + 1 <= pontas; i++) c.aresta(i, i + 1, 1);

        return c.pronto();
    }

    /** Um grafo sorteado conexo: uma arvore de base mais arestas a mais. */
    public static Grafo sorteado(int n, double chance, int maiorPeso, long semente) {
        Sorteio sorteio = new Sorteio(semente);
        Construtor c = com("sorteado de " + n + " a " + chance + " (" + semente + ")", n);

        // uma arvore de base garante que o grafo e conexo
        for (int v = 1; v < n; v++) c.aresta(sorteio.ate(v), v, 1L + sorteio.ate(maiorPeso));

        for (int u = 0; u < n; u++)
            for (int v = u + 1; v < n; v++)
                if (sorteio.fracao() < chance) c.aresta(u, v, 1L + sorteio.ate(maiorPeso));

        return c.pronto();
    }

    /** Um grafo DESCONEXO: dois pedacos sem ligacao entre eles. */
    public static Grafo desconexo(int porPedaco) {
        Construtor c = com("desconexo de " + (2 * porPedaco), 2 * porPedaco);

        for (int i = 0; i + 1 < porPedaco; i++) c.aresta(i, i + 1, 1);

        for (int i = porPedaco; i + 1 < 2 * porPedaco; i++) c.aresta(i, i + 1, 1);

        return c.pronto();
    }

    public static Map<String, Grafo> todos() {
        Map<String, Grafo> mapa = new LinkedHashMap<>();

        for (Grafo g : List.of(
                ciclo(5), ciclo(8), cicloUniforme(5), cicloUniforme(7),
                estrelaComAtalho(5), estrelaComAtalho(8),
                completo(6, 20, 1), completo(7, 3, 2),
                grade(3, 3, 9, 3), grade(4, 3, 50, 4),
                sorteado(9, 0.3, 20, 5), sorteado(10, 0.4, 4, 6), desconexo(4)))
            mapa.put(g.nome(), g);

        return mapa;
    }

    /**
     * O SORTEIO, escrito aqui em vez de vir da biblioteca.
     *
     * <p>A integracao continua roda em tres sistemas, e "a arvore minima pesa 47" com
     * grafos diferentes em cada sistema nao e uma medida. O deslocamento de 32 bits
     * tira os bits de baixo, que num congruente linear alternam par e impar a cada
     * passo e fariam os pesos sair alternados.
     */
    public static final class Sorteio {

        private long estado;

        public Sorteio(long semente) {
            this.estado = semente == 0 ? 1 : semente;
        }

        public int proximo() {
            estado = estado * 6364136223846793005L + 1442695040888963407L;
            return (int) (estado >>> 32);
        }

        public int ate(int limite) {
            return (int) (Integer.toUnsignedLong(proximo()) % limite);
        }

        public double fracao() {
            return Integer.toUnsignedLong(proximo()) / 4294967296.0;
        }
    }
}
