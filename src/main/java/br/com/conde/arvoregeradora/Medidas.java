package br.com.conde.arvoregeradora;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * As MEDIDAS, em passos e em arvores, nunca em segundos.
 *
 * <p>Segundo nao e medida: muda com a maquina, com o que mais esta rodando e com o
 * humor do coletor de lixo. A integracao continua roda em tres sistemas, e tres
 * numeros de segundos nao se comparam.
 */
public final class Medidas {

    private Medidas() {
    }

    // ───────────────────── 1. contra a forca bruta ─────────────────────

    public record LinhaDefinicao(String grafo, int vertices, int arestas, boolean conexo,
                                 Map<String, Long> pesos, String pelaForcaBruta, boolean todosBatem) {
    }

    /**
     * Os tres algoritmos contra a FORCA BRUTA, que testa todos os subconjuntos.
     *
     * <p>A forca bruta nao sabe que o guloso funciona. Se o guloso estivesse errado,
     * ela nao estaria errada junto.
     */
    public static List<LinhaDefinicao> contraAForcaBruta() {
        List<LinhaDefinicao> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            Map<String, Long> pesos = new LinkedHashMap<>();

            for (String nome : Arvores.nomes()) {
                Arvores.Resultado r = Arvores.por(nome, g);

                pesos.put(nome, r.conexo() ? r.peso() : -1);
            }

            String bruta;
            boolean batem;

            if (g.arestas().size() <= 22) {
                Juiz.Bruta b = Juiz.porForcaBruta(g);

                bruta = b.peso() < 0 ? "nao existe" : String.valueOf(b.peso());

                batem = Arvores.nomes().stream()
                        .filter(n -> !n.equals("visitados"))
                        .allMatch(n -> pesos.get(n) == (b.peso() < 0 ? -1 : b.peso()));
            } else {
                bruta = "nao cabe";
                batem = pesos.get("Kruskal").equals(pesos.get("Prim"))
                        && pesos.get("Kruskal").equals(pesos.get("Boruvka"));
            }

            linhas.add(new LinhaDefinicao(g.nome(), g.vertices(), g.arestas().size(),
                    g.conexo(), pesos, bruta, batem));
        }

        return linhas;
    }

    // ───────────────────── 2. a versao plausivel ─────────────────────

    public record LinhaErro(String grafo, int vertices, int arestasDaVersao, long pesoCerto,
                            long pesoDaVersao, boolean vale, String motivo) {
    }

    /** A versao que usa um conjunto de visitados em vez de conjuntos disjuntos. */
    public static List<LinhaErro> aVersaoPlausivel() {
        List<LinhaErro> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo()) continue;

            Arvores.Resultado certa = Arvores.porKruskal(g);
            Arvores.Resultado plausivel = Arvores.porVisitados(g);

            Juiz.Veredicto v = Juiz.conferir(g, plausivel.arestas());

            linhas.add(new LinhaErro(g.nome(), g.vertices(), plausivel.arestas().size(),
                    certa.peso(), plausivel.peso(), v.vale(), v.motivo()));
        }

        return linhas;
    }

    public record LinhaCaca(String familia, int grafos, int kruskalErrou, int primErrou,
                            int boruvkaErrou, int visitadosErrou) {
    }

    /** As quatro versoes em familias de grafos sorteados. */
    public static List<LinhaCaca> cacarErros() {
        List<LinhaCaca> linhas = new ArrayList<>();

        for (double chance : new double[] {0.1, 0.2, 0.4, 0.7}) {
            int[] erros = new int[4];
            List<String> nomes = Arvores.nomes();

            for (long semente = 1; semente <= 200; semente++) {
                Grafo g = Grafo.sorteado(12, chance, 10, semente);
                long certo = Arvores.porKruskal(g).peso();

                for (int i = 0; i < nomes.size(); i++) {
                    Arvores.Resultado r = Arvores.por(nomes.get(i), g);

                    if (!Juiz.conferir(g, r.arestas()).vale() || r.peso() != certo) erros[i]++;
                }
            }

            linhas.add(new LinhaCaca("sorteados de 12 a " + chance, 200,
                    erros[0], erros[1], erros[2], erros[3]));
        }

        return linhas;
    }

    // ───────────────────── 3. Kirchhoff ─────────────────────

    public record LinhaKirchhoff(String grafo, int vertices, int arestas, long porKirchhoff,
                                 String pelaForcaBruta, boolean batem, boolean mesmoApagandoQualquer) {
    }

    /**
     * O TEOREMA DE KIRCHHOFF contra a contagem por forca bruta.
     *
     * <p>E a ultima coluna: o determinante da o MESMO numero qualquer que seja o
     * vertice apagado. Isso nao e obvio nenhum, e e conferido apagando todos.
     */
    public static List<LinhaKirchhoff> oTeoremaDeKirchhoff() {
        List<LinhaKirchhoff> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            long kirchhoff = Juiz.quantasArvores(g);

            boolean mesmo = true;

            for (int apagado = 0; apagado < g.vertices(); apagado++)
                if (Juiz.quantasArvoresApagando(g, apagado) != kirchhoff) mesmo = false;

            String bruta;
            boolean batem;

            if (g.arestas().size() <= 22) {
                long todas = Juiz.porForcaBruta(g).todas();

                bruta = String.valueOf(todas);
                batem = todas == kirchhoff;
            } else {
                bruta = "nao cabe";
                batem = true;
            }

            linhas.add(new LinhaKirchhoff(g.nome(), g.vertices(), g.arestas().size(),
                    kirchhoff, bruta, batem, mesmo));
        }

        return linhas;
    }

    public record LinhaCompleto(int vertices, String porKirchhoff, String emInteiroGrande,
                                String pelaFormula, boolean coubeEmSessentaEQuatro, boolean batem) {
    }

    /**
     * No grafo COMPLETO, Kirchhoff tem de dar n elevado a n menos dois.
     *
     * <p>E a formula de Cayley, de 1889, e ela e um quarto juiz: um numero fechado,
     * sem matriz, sem grafo, sem nada. Com dez vertices sao cem milhoes de arvores
     * geradoras.
     */
    public static List<LinhaCompleto> aFormulaDeCayley() {
        List<LinhaCompleto> linhas = new ArrayList<>();

        for (int n = 2; n <= 18; n++) {
            Grafo g = Grafo.completo(n, 1, 1);

            java.math.BigInteger cayley = java.math.BigInteger.valueOf(n).pow(Math.max(0, n - 2));
            java.math.BigInteger grande = Juiz.quantasArvoresGrande(g);

            String emLong;
            boolean coube;

            try {
                emLong = String.valueOf(Juiz.quantasArvores(g));
                coube = true;
            } catch (Juiz.EstourouException e) {
                emLong = "estourou";
                coube = false;
            }

            linhas.add(new LinhaCompleto(n, emLong, grande.toString(), cayley.toString(),
                    coube, grande.equals(cayley)));
        }

        return linhas;
    }

    // ───────────────────── 4. os empates ─────────────────────

    public record LinhaEmpate(String grafo, long pesosDistintos, int arestas, long arvoresMinimas,
                              boolean asTresDaoAMesmaArvore, long pesoComum) {
    }

    /**
     * Os EMPATES de peso e as arvores diferentes.
     *
     * <p>Com pesos todos distintos a arvore minima e UNICA, e e teorema. Com empates
     * ela deixa de ser, e os tres algoritmos passam a dar arvores diferentes com o
     * mesmo peso.
     *
     * <p>E por isso que comparar ARVORES e errado e comparar PESOS e certo.
     */
    public static List<LinhaEmpate> osEmpates() {
        List<LinhaEmpate> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo()) continue;

            List<Grafo.Aresta> kruskal = Arvores.porKruskal(g).arestas();
            List<Grafo.Aresta> prim = new ArrayList<>(Arvores.porPrim(g).arestas());
            List<Grafo.Aresta> boruvka = Arvores.porBoruvka(g).arestas();

            prim.sort(java.util.Comparator.naturalOrder());

            boolean mesma = kruskal.equals(prim) && kruskal.equals(boruvka);

            long minimas = g.arestas().size() <= 22 ? Juiz.porForcaBruta(g).quantas() : -1;

            linhas.add(new LinhaEmpate(g.nome(), g.pesosDistintos(), g.arestas().size(),
                    minimas, mesma, Arvores.porKruskal(g).peso()));
        }

        return linhas;
    }

    public record LinhaDesempate(String grafo, long arvoresMinimas, boolean comDesempateDaAMesma,
                                 boolean semDesempateDaAMesma, long peso, long pesoAoContrario) {
    }

    /**
     * O que o DESEMPATE faz: com uma ordem total nas arestas, a arvore minima e UNICA.
     *
     * <p>Escrevo o mesmo grafo com as arestas na ordem inversa e comparo. Com
     * desempate, sai a mesma arvore; sem desempate, sai outra arvore do mesmo peso
     * sempre que houver mais de uma arvore minima.
     *
     * <p>O peso e o mesmo nas quatro colunas, em toda linha. E por isso que comparar
     * peso e certo e comparar arvore e errado.
     */
    public static List<LinhaDesempate> oQueODesempateFaz() {
        List<LinhaDesempate> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo()) continue;

            Grafo aoContrario = g.comArestasInvertidas();

            List<Grafo.Aresta> comDesempate = Arvores.porKruskal(g).arestas();
            List<Grafo.Aresta> comDesempateInv = Arvores.porKruskal(aoContrario).arestas();

            List<Grafo.Aresta> semDesempate = new ArrayList<>(Arvores.porKruskalSoPeso(g).arestas());
            List<Grafo.Aresta> semDesempateInv = new ArrayList<>(Arvores.porKruskalSoPeso(aoContrario).arestas());

            semDesempate.sort(java.util.Comparator.naturalOrder());
            semDesempateInv.sort(java.util.Comparator.naturalOrder());

            long minimas = g.arestas().size() <= 22 ? Juiz.porForcaBruta(g).quantas() : -1;

            linhas.add(new LinhaDesempate(g.nome(), minimas,
                    comDesempate.equals(comDesempateInv),
                    semDesempate.equals(semDesempateInv),
                    Arvores.porKruskal(g).peso(),
                    Arvores.porKruskalSoPeso(aoContrario).peso()));
        }

        return linhas;
    }

    // ───────────────────── 5. o custo ─────────────────────

    public record LinhaCusto(String grafo, int vertices, int arestas, Map<String, Long> passos) {
    }

    /** Quantos passos cada algoritmo gasta. */
    public static List<LinhaCusto> oCusto() {
        List<LinhaCusto> linhas = new ArrayList<>();

        for (Grafo g : Grafo.todos().values()) {
            Map<String, Long> passos = new LinkedHashMap<>();

            for (String nome : Arvores.nomes()) passos.put(nome, Arvores.por(nome, g).passos());

            linhas.add(new LinhaCusto(g.nome(), g.vertices(), g.arestas().size(), passos));
        }

        return linhas;
    }

    public record LinhaCrescimento(int vertices, int arestas, long kruskal, long prim, long boruvka) {
    }

    /** Como o custo cresce no grafo completo, onde as arestas crescem como n ao quadrado. */
    public static List<LinhaCrescimento> oCrescimento() {
        List<LinhaCrescimento> linhas = new ArrayList<>();

        for (int n : new int[] {5, 10, 20, 40, 80, 160, 320}) {
            Grafo g = Grafo.completo(n, 1_000, 7);

            linhas.add(new LinhaCrescimento(n, g.arestas().size(),
                    Arvores.porKruskal(g).passos(),
                    Arvores.porPrim(g).passos(),
                    Arvores.porBoruvka(g).passos()));
        }

        return linhas;
    }
}
