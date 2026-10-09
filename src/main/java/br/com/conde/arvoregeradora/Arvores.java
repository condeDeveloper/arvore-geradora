package br.com.conde.arvoregeradora;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Os TRES algoritmos de arvore geradora minima, e a versao que parece certa.
 *
 * <p>Os tres chegam ao mesmo PESO sempre, e a arvores diferentes quando ha empate de
 * pesos. Essa e a primeira coisa que o repositorio mede, e ela decide como tudo o
 * mais e conferido: comparar ARVORES e errado, comparar PESOS e certo.
 *
 * <ul>
 *   <li>KRUSKAL, 1956: ordena as arestas por peso e aceita cada uma que nao feche
 *       ciclo. Nao olha para vertice nenhum; olha para conjuntos.
 *   <li>PRIM, 1957: cresce uma arvore a partir de um vertice, sempre pela aresta mais
 *       barata que sai dela.
 *   <li>BORUVKA, 1926: o mais antigo dos tres. Cada componente escolhe a aresta mais
 *       barata que sai dele, todas de uma vez, e os componentes se juntam.
 * </ul>
 *
 * <p>Os tres sao gulosos e os tres funcionam pelo mesmo motivo, a PROPRIEDADE DO
 * CORTE: em qualquer corte do grafo, a aresta mais leve que o atravessa esta em
 * alguma arvore minima. Kruskal, Prim e Boruvka sao tres maneiras de escolher o
 * corte.
 */
public final class Arvores {

    private Arvores() {
    }

    /**
     * @param arestas a arvore encontrada
     * @param peso    o peso total
     * @param passos  quantas arestas e uniao de conjuntos foram feitas
     * @param conexo  o grafo era conexo: sem isso, a arvore geradora nao existe
     */
    public record Resultado(List<Grafo.Aresta> arestas, long peso, long passos, boolean conexo) {

        public Resultado {
            arestas = List.copyOf(arestas);
        }
    }

    public static List<String> nomes() {
        return List.of("Kruskal", "Prim", "Boruvka", "visitados");
    }

    public static Resultado por(String nome, Grafo g) {
        return switch (nome) {
            case "Kruskal" -> porKruskal(g);
            case "Prim" -> porPrim(g);
            case "Boruvka" -> porBoruvka(g);
            case "visitados" -> porVisitados(g);
            default -> throw new IllegalArgumentException("algoritmo desconhecido: " + nome);
        };
    }

    // ───────────────────────────── Kruskal ─────────────────────────────

    /**
     * KRUSKAL: ordena por peso e aceita a aresta que nao fecha ciclo.
     *
     * <p>A pergunta "fecha ciclo?" e respondida por CONJUNTOS DISJUNTOS: duas pontas
     * no mesmo conjunto querem dizer que ja existe caminho entre elas. E essa estrutura
     * que torna o algoritmo possivel, e trocar ela por qualquer coisa parecida e o erro
     * que a versao plausivel comete.
     */
    public static Resultado porKruskal(Grafo g) {
        List<Grafo.Aresta> ordenadas = new ArrayList<>(g.arestas());

        ordenadas.sort(Comparator.naturalOrder());

        Conjuntos conjuntos = new Conjuntos(g.vertices());
        List<Grafo.Aresta> escolhidas = new ArrayList<>();
        long peso = 0;
        long passos = 0;

        for (Grafo.Aresta a : ordenadas) {
            passos++;

            if (!conjuntos.juntar(a.u(), a.v())) continue;

            escolhidas.add(a);
            peso += a.peso();

            if (escolhidas.size() == g.vertices() - 1) break;
        }

        passos += conjuntos.passos();

        return new Resultado(escolhidas, peso, passos, escolhidas.size() == Math.max(0, g.vertices() - 1));
    }

    // ───────────────────────────── Prim ─────────────────────────────

    /**
     * PRIM: cresce uma arvore a partir do vertice zero, sempre pela aresta mais barata
     * que sai dela.
     *
     * <p>A fila de prioridade guarda as arestas que atravessam o corte entre o que ja
     * esta na arvore e o que nao esta. Cada aresta entra na fila uma vez por ponta, e
     * as que ja nao atravessam mais o corte sao descartadas quando saem.
     */
    public static Resultado porPrim(Grafo g) {
        if (g.vertices() == 0) return new Resultado(List.of(), 0, 0, true);

        boolean[] dentro = new boolean[g.vertices()];
        PriorityQueue<Grafo.Aresta> fila = new PriorityQueue<>();
        List<Grafo.Aresta> escolhidas = new ArrayList<>();
        long peso = 0;
        long passos = 0;

        dentro[0] = true;
        fila.addAll(g.incidentesA(0));

        while (!fila.isEmpty() && escolhidas.size() < g.vertices() - 1) {
            Grafo.Aresta a = fila.poll();

            passos++;

            // a aresta so serve se exatamente uma ponta ja estiver dentro
            if (dentro[a.u()] == dentro[a.v()]) continue;

            int novo = dentro[a.u()] ? a.v() : a.u();

            dentro[novo] = true;
            escolhidas.add(a);
            peso += a.peso();

            for (Grafo.Aresta vizinha : g.incidentesA(novo)) {
                passos++;

                if (!dentro[vizinha.outro(novo)]) fila.add(vizinha);
            }
        }

        return new Resultado(escolhidas, peso, passos, escolhidas.size() == g.vertices() - 1);
    }

    // ───────────────────────────── Boruvka ─────────────────────────────

    /**
     * BORUVKA, 1926, que e o mais antigo dos tres e o que menos se ensina.
     *
     * <p>Em cada rodada, CADA componente escolhe a aresta mais barata que sai dele, e
     * todas sao acrescentadas de uma vez. Como cada componente se junta a pelo menos um
     * outro, o numero de componentes cai pela metade a cada rodada: sao no maximo log n
     * rodadas.
     *
     * <p>Ele e o unico dos tres que pode rodar em paralelo sem conversa entre as
     * partes, e e por isso que ele voltou a moda.
     *
     * <p>O DESEMPATE e obrigatorio aqui, e nao e detalhe: se dois componentes
     * escolherem arestas de mesmo peso em direcoes opostas, o algoritmo acrescenta as
     * duas e fecha um ciclo. A ordem total das arestas resolve isso.
     */
    public static Resultado porBoruvka(Grafo g) {
        Conjuntos conjuntos = new Conjuntos(g.vertices());
        List<Grafo.Aresta> escolhidas = new ArrayList<>();
        long peso = 0;
        long passos = 0;
        int componentes = g.vertices();

        while (componentes > 1) {
            Grafo.Aresta[] melhor = new Grafo.Aresta[g.vertices()];
            boolean achouAlguma = false;

            for (Grafo.Aresta a : g.arestas()) {
                passos++;

                int ca = conjuntos.raiz(a.u());
                int cb = conjuntos.raiz(a.v());

                if (ca == cb) continue;

                achouAlguma = true;

                if (melhor[ca] == null || a.compareTo(melhor[ca]) < 0) melhor[ca] = a;
                if (melhor[cb] == null || a.compareTo(melhor[cb]) < 0) melhor[cb] = a;
            }

            if (!achouAlguma) break;

            for (int c = 0; c < g.vertices(); c++) {
                Grafo.Aresta a = melhor[c];

                if (a == null) continue;

                passos++;

                if (!conjuntos.juntar(a.u(), a.v())) continue;

                escolhidas.add(a);
                peso += a.peso();
                componentes--;
            }
        }

        escolhidas.sort(Comparator.naturalOrder());

        return new Resultado(escolhidas, peso, passos, escolhidas.size() == Math.max(0, g.vertices() - 1));
    }

    /**
     * KRUSKAL SEM DESEMPATE: ordena so pelo peso, e no empate fica a ordem em que as
     * arestas foram escritas.
     *
     * <p>Ele existe para mostrar o que o desempate faz. Com uma ordem TOTAL nas
     * arestas, a arvore geradora minima e UNICA, e os tres algoritmos chegam nela. Sem
     * desempate, qualquer uma das arvores minimas pode sair, e qual sai depende da
     * ordem em que o grafo foi escrito.
     *
     * <p>As duas versoes dao o mesmo PESO sempre. E por isso que comparar peso e certo
     * e comparar arvore e errado.
     */
    public static Resultado porKruskalSoPeso(Grafo g) {
        List<Grafo.Aresta> ordenadas = new ArrayList<>(g.arestas());

        // ordenacao estavel, por peso so: no empate fica a ordem de escrita
        ordenadas.sort(Comparator.comparingLong(Grafo.Aresta::peso));

        Conjuntos conjuntos = new Conjuntos(g.vertices());
        List<Grafo.Aresta> escolhidas = new ArrayList<>();
        long peso = 0;
        long passos = 0;

        for (Grafo.Aresta a : ordenadas) {
            passos++;

            if (!conjuntos.juntar(a.u(), a.v())) continue;

            escolhidas.add(a);
            peso += a.peso();
        }

        return new Resultado(escolhidas, peso, passos, escolhidas.size() == Math.max(0, g.vertices() - 1));
    }

    // ───────────────────────────── a versao plausivel ─────────────────────────────

    /**
     * A VERSAO PLAUSIVEL: Kruskal com um CONJUNTO DE VISITADOS em vez de conjuntos
     * disjuntos.
     *
     * <p>"Recuse a aresta se os dois vertices dela ja foram visitados" soa como a mesma
     * coisa que "recuse se ela fecha ciclo", e nao e. Dois vertices ja visitados podem
     * estar em PEDACOS SEPARADOS da floresta, e a aresta entre eles e justamente a que
     * junta os dois.
     *
     * <p>Ela acerta em todo grafo em que a arvore minima cresce a partir de um lugar
     * so, como um ciclo ou uma arvore. Erra quando a floresta se forma em pedacos, que
     * e o que acontece assim que o grafo tem alguma estrutura.
     *
     * <p>E ela nao erra de pouco: ela devolve menos de n menos uma arestas e nem
     * percebe que nao ligou tudo.
     */
    public static Resultado porVisitados(Grafo g) {
        List<Grafo.Aresta> ordenadas = new ArrayList<>(g.arestas());

        ordenadas.sort(Comparator.naturalOrder());

        boolean[] visitado = new boolean[g.vertices()];
        List<Grafo.Aresta> escolhidas = new ArrayList<>();
        long peso = 0;
        long passos = 0;

        for (Grafo.Aresta a : ordenadas) {
            passos++;

            if (visitado[a.u()] && visitado[a.v()]) continue;

            visitado[a.u()] = true;
            visitado[a.v()] = true;
            escolhidas.add(a);
            peso += a.peso();
        }

        return new Resultado(escolhidas, peso, passos, escolhidas.size() == Math.max(0, g.vertices() - 1));
    }

    // ───────────────────────────── conjuntos disjuntos ─────────────────────────────

    /**
     * CONJUNTOS DISJUNTOS com compressao de caminho e uniao por tamanho.
     *
     * <p>As duas melhorias juntas fazem cada operacao custar praticamente constante, e
     * o "praticamente" aqui e a funcao inversa de Ackermann, que para qualquer entrada
     * deste mundo vale menos que cinco.
     *
     * <p>A compressao e feita em DUAS PASSADAS e sem recursao: a primeira acha a raiz,
     * a segunda aponta todo mundo do caminho para ela. A versao recursiva e mais curta
     * e estoura a pilha numa arvore degenerada, que e justamente o caso que a uniao por
     * tamanho existe para evitar e que aparece se alguem tirar a uniao por tamanho.
     */
    public static final class Conjuntos {

        private final int[] pai;
        private final int[] tamanho;
        private long passos;

        public Conjuntos(int n) {
            pai = new int[n];
            tamanho = new int[n];

            for (int i = 0; i < n; i++) {
                pai[i] = i;
                tamanho[i] = 1;
            }
        }

        public int raiz(int v) {
            int r = v;

            while (pai[r] != r) {
                passos++;
                r = pai[r];
            }

            while (pai[v] != r) {
                int proximo = pai[v];

                pai[v] = r;
                v = proximo;
                passos++;
            }

            return r;
        }

        /** Junta os dois conjuntos. Devolve falso se ja eram o mesmo. */
        public boolean juntar(int a, int b) {
            int ra = raiz(a);
            int rb = raiz(b);

            if (ra == rb) return false;

            if (tamanho[ra] < tamanho[rb]) {
                int troca = ra;

                ra = rb;
                rb = troca;
            }

            pai[rb] = ra;
            tamanho[ra] += tamanho[rb];
            passos++;

            return true;
        }

        public boolean juntos(int a, int b) {
            return raiz(a) == raiz(b);
        }

        public long passos() {
            return passos;
        }
    }
}
