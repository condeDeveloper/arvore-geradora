package br.com.conde.arvoregeradora;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Os tres algoritmos contra a FORCA BRUTA, que testa todos os subconjuntos de
 * arestas.
 *
 * <p>A forca bruta nao sabe que o guloso funciona. Se o guloso estivesse errado, ela
 * nao estaria errada junto.
 */
class ArvoresTest {

    @Test
    @DisplayName("os tres dao o peso da forca bruta em todo grafo conexo pequeno")
    void osTresDaoOPesoDaForcaBruta() {
        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo() || g.arestas().size() > 22) continue;

            long melhor = Juiz.porForcaBruta(g).peso();

            for (String nome : List.of("Kruskal", "Prim", "Boruvka")) {
                Arvores.Resultado r = Arvores.por(nome, g);

                assertThat(r.peso()).as("%s em %s", nome, g.nome()).isEqualTo(melhor);
                assertThat(Juiz.conferir(g, r.arestas()).vale()).as("%s em %s", nome, g.nome()).isTrue();
            }
        }
    }

    @Test
    @DisplayName("e concordam entre si em oitocentos grafos sorteados")
    void concordamEmGrafosSorteados() {
        for (double chance : new double[] {0.05, 0.2, 0.5, 0.9})
            for (long semente = 1; semente <= 200; semente++) {
                Grafo g = Grafo.sorteado(14, chance, 20, semente);

                long kruskal = Arvores.porKruskal(g).peso();

                for (String nome : List.of("Prim", "Boruvka")) {
                    Arvores.Resultado r = Arvores.por(nome, g);

                    assertThat(r.peso()).as("%s, chance %s semente %d", nome, chance, semente).isEqualTo(kruskal);
                    assertThat(Juiz.conferir(g, r.arestas()).vale())
                            .as("%s, chance %s semente %d", nome, chance, semente).isTrue();
                }
            }
    }

    /**
     * O ACHADO QUE ME CORRIGIU: com o DESEMPATE, os tres dao a MESMA arvore sempre.
     *
     * <p>Eu pus o desempate por vertices para as tabelas nao mudarem de maquina para
     * maquina, e com isso criei uma ORDEM TOTAL nas arestas. Com ordem total a arvore
     * geradora minima e unica, e e teorema. O desempate que eu pus por
     * reprodutibilidade apagou exatamente o fenomeno que eu queria mostrar.
     */
    @Test
    @DisplayName("com desempate os tres dao a mesma arvore, mesmo havendo muitas minimas")
    void comDesempateOsTresDaoAMesmaArvore() {
        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo()) continue;

            List<Grafo.Aresta> kruskal = Arvores.porKruskal(g).arestas();
            List<Grafo.Aresta> prim = new ArrayList<>(Arvores.porPrim(g).arestas());
            List<Grafo.Aresta> boruvka = Arvores.porBoruvka(g).arestas();

            prim.sort(Comparator.naturalOrder());

            assertThat(prim).as("Prim em %s", g.nome()).isEqualTo(kruskal);
            assertThat(boruvka).as("Boruvka em %s", g.nome()).isEqualTo(kruskal);
        }

        // e isso vale mesmo onde existem vinte e sete arvores minimas
        Grafo muitasMinimas = Grafo.completo(7, 3, 2);

        assertThat(Juiz.porForcaBruta(muitasMinimas).quantas()).isEqualTo(27);
    }

    /**
     * E SEM o desempate sai outra arvore, do mesmo peso, quando a ordem de escrita
     * muda.
     */
    @Test
    @DisplayName("sem desempate a arvore muda com a ordem de escrita, e o peso nao")
    void semDesempateAArvoreMuda() {
        int mudaram = 0;

        for (Grafo g : Grafo.todos().values()) {
            if (!g.conexo()) continue;

            Grafo aoContrario = g.comArestasInvertidas();

            assertThat(Arvores.porKruskalSoPeso(aoContrario).peso())
                    .as("o peso em %s", g.nome())
                    .isEqualTo(Arvores.porKruskal(g).peso());

            List<Grafo.Aresta> aqui = new ArrayList<>(Arvores.porKruskalSoPeso(g).arestas());
            List<Grafo.Aresta> la = new ArrayList<>(Arvores.porKruskalSoPeso(aoContrario).arestas());

            aqui.sort(Comparator.naturalOrder());
            la.sort(Comparator.naturalOrder());

            if (!aqui.equals(la)) mudaram++;

            // com desempate nunca muda
            assertThat(Arvores.porKruskal(aoContrario).arestas())
                    .as("com desempate em %s", g.nome())
                    .isEqualTo(Arvores.porKruskal(g).arestas());
        }

        assertThat(mudaram).isGreaterThan(5);
    }

    /**
     * O ACHADO PRINCIPAL: a versao que usa um CONJUNTO DE VISITADOS nem percebe que
     * nao ligou o grafo.
     *
     * <p>"Recuse a aresta se os dois vertices ja foram visitados" soa como "recuse se
     * ela fecha ciclo", e nao e: dois vertices visitados podem estar em pedacos
     * separados da floresta, e a aresta entre eles e justamente a que junta os dois.
     */
    @Test
    @DisplayName("a versao com visitados devolve menos arestas do que uma arvore precisa")
    void aVersaoComVisitadosNaoLiga() {
        Grafo g = Grafo.completo(6, 20, 1);

        Arvores.Resultado plausivel = Arvores.porVisitados(g);
        Juiz.Veredicto v = Juiz.conferir(g, plausivel.arestas());

        assertThat(v.vale()).isFalse();
        assertThat(v.motivo()).contains("precisaria de");
        assertThat(plausivel.arestas()).hasSizeLessThan(g.vertices() - 1);

        // e o peso dela e MENOR que o certo, porque ela devolve menos arestas
        assertThat(plausivel.peso()).isLessThan(Arvores.porKruskal(g).peso());
    }

    @Test
    @DisplayName("em grafos sorteados ela erra em quase todos")
    void aVersaoComVisitadosErraQuaseSempre() {
        int errou = 0;

        for (long semente = 1; semente <= 200; semente++) {
            Grafo g = Grafo.sorteado(12, 0.2, 10, semente);

            if (!Juiz.conferir(g, Arvores.porVisitados(g).arestas()).vale()) errou++;
        }

        assertThat(errou).isGreaterThan(180);
    }

    @Test
    @DisplayName("num grafo desconexo nenhum dos tres acha arvore geradora")
    void grafoDesconexo() {
        Grafo g = Grafo.desconexo(4);

        assertThat(g.conexo()).isFalse();

        for (String nome : List.of("Kruskal", "Prim", "Boruvka"))
            assertThat(Arvores.por(nome, g).conexo()).as(nome).isFalse();

        assertThat(Juiz.quantasArvores(g)).isZero();
    }

    @Test
    @DisplayName("os conjuntos disjuntos aguentam uma corrente de cem mil uniões")
    void osConjuntosAguentamCemMil() {
        Arvores.Conjuntos c = new Arvores.Conjuntos(100_000);

        for (int i = 0; i + 1 < 100_000; i++) assertThat(c.juntar(i, i + 1)).isTrue();

        assertThat(c.juntos(0, 99_999)).isTrue();
        assertThat(c.juntar(0, 99_999)).isFalse();
    }
}
